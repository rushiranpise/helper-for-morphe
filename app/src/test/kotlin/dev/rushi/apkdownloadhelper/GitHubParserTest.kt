package dev.rushi.apkdownloadhelper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubParserTest {

    private val api = "https://api.github.com/repos/owner/repo"

    @Test
    fun `every releases link shape Morphe answers with is read`() {
        assertEquals(
            GitHubReleaseLink("owner", "repo", tag = "v1.14.2"),
            gitHubReleaseLinkOf("https://github.com/owner/repo/releases/tag/v1.14.2")
        )
        assertEquals(
            GitHubReleaseLink("owner", "repo", tag = "v1.14.2"),
            gitHubReleaseLinkOf("https://github.com/owner/repo/releases/tag/v1.14.2/")
        )
        assertEquals(
            GitHubReleaseLink("owner", "repo", tag = "v1.14.2"),
            gitHubReleaseLinkOf("https://github.com/owner/repo/releases#release-v1.14.2")
        )
        assertEquals(
            GitHubReleaseLink("owner", "repo", tag = null),
            gitHubReleaseLinkOf("https://github.com/owner/repo/releases")
        )
        assertEquals(
            GitHubReleaseLink("owner", "repo", tag = null),
            gitHubReleaseLinkOf("https://www.github.com/owner/repo/releases/latest")
        )
        assertEquals(
            GitHubReleaseLink("owner", "repo", tag = "v1.0", assetName = "app release.apk"),
            gitHubReleaseLinkOf("https://github.com/owner/repo/releases/download/v1.0/app%20release.apk")
        )
    }

    @Test
    fun `other GitHub pages and other hosts are not release links`() {
        assertNull(gitHubReleaseLinkOf("https://github.com/owner/repo"))
        assertNull(gitHubReleaseLinkOf("https://github.com/owner/repo/issues/1"))
        assertNull(gitHubReleaseLinkOf("https://gitlab.com/owner/repo/releases"))
        assertNull(gitHubReleaseLinkOf("https://www.apkmirror.com/apk/owner/repo/releases"))
        assertNull(gitHubReleaseLinkOf("not a url"))
    }

    @Test
    fun `a leading v before the number is not part of the version`() {
        assertEquals("1.14.2", gitHubVersionOfTag("v1.14.2"))
        assertEquals("1.14.2", gitHubVersionOfTag("1.14.2"))
        assertEquals("vanilla-2.0", gitHubVersionOfTag("vanilla-2.0"))
    }

    @Test
    fun `the device ABI wins, then universal, and other ABIs are dropped`() {
        val assets = listOf(
            asset("app-x86_64-release.apk"),
            asset("app-universal-release.apk"),
            asset("app-armeabi-v7a-release.apk"),
            asset("app-arm64-v8a-release.apk"),
            asset("checksums.txt")
        )

        val picked = gitHubInstallableAssets(assets, listOf("arm64-v8a", "armeabi-v7a"))

        assertEquals(
            listOf("app-arm64-v8a-release.apk", "app-armeabi-v7a-release.apk", "app-universal-release.apk"),
            picked.map { it.name }
        )
    }

    @Test
    fun `a name without an ABI is universal and a debug build comes last`() {
        val picked = gitHubInstallableAssets(
            listOf(asset("app-debug.apk"), asset("app-release.apk")),
            listOf("arm64-v8a")
        )

        assertEquals(listOf("app-release.apk", "app-debug.apk"), picked.map { it.name })
    }

    @Test
    fun `a debug build comes after every other build, even one for this ABI`() {
        val picked = gitHubInstallableAssets(
            listOf(asset("app-arm64-v8a-debug.apk"), asset("app-universal-release.apk")),
            listOf("arm64-v8a")
        )

        assertEquals(listOf("app-universal-release.apk", "app-arm64-v8a-debug.apk"), picked.map { it.name })
    }

    @Test
    fun `Brave's Mono build is picked for each device ABI`() {
        // brave/brave-browser's release assets, in the order GitHub lists them.
        val assets = listOf(
            asset("Bravearm64Universal.apk"),
            asset("BraveMonoarm.apk"),
            asset("BraveMonoarm64.apk"),
            asset("BraveMonox64.apk"),
            asset("BraveMonox86.apk")
        )

        assertEquals(
            listOf("BraveMonoarm64.apk", "Bravearm64Universal.apk", "BraveMonoarm.apk"),
            gitHubInstallableAssets(assets, listOf("arm64-v8a", "armeabi-v7a", "armeabi")).map { it.name }
        )
        assertEquals(
            listOf("BraveMonoarm.apk"),
            gitHubInstallableAssets(assets, listOf("armeabi-v7a", "armeabi")).map { it.name }
        )
        assertEquals(
            listOf("BraveMonox64.apk", "BraveMonox86.apk", "BraveMonoarm.apk"),
            gitHubInstallableAssets(assets, listOf("x86_64", "x86")).map { it.name }
        )
    }

    @Test
    fun `one clear best file is not asked about`() {
        val candidates = choiceCandidates(
            "Bravearm64Universal.apk",
            "BraveMonoarm.apk",
            "BraveMonoarm64.apk"
        )

        assertTrue(gitHubTiedChoices(candidates, listOf("arm64-v8a", "armeabi-v7a")).isEmpty())
    }

    @Test
    fun `files tied for the best fit are offered to the user`() {
        val candidates = choiceCandidates(
            "app-foss-arm64-v8a.apk",
            "app-play-arm64-v8a.apk",
            "app-universal.apk",
            "app-foss-arm64-v8a-debug.apk"
        )

        assertEquals(
            listOf("app-foss-arm64-v8a.apk", "app-play-arm64-v8a.apk"),
            gitHubTiedChoices(candidates, listOf("arm64-v8a")).map { it.variantLabel }
        )
    }

    @Test
    fun `several unlabelled builds are a question too`() {
        val candidates = choiceCandidates("app-foss.apk", "app-play.apk")

        assertEquals(2, gitHubTiedChoices(candidates, listOf("arm64-v8a")).size)
    }

    @Test
    fun `only a sha256 digest is used`() {
        val hex = "a".repeat(64)
        assertEquals(hex, gitHubSha256Of("sha256:${hex.uppercase()}"))
        assertNull(gitHubSha256Of("sha512:$hex"))
        assertNull(gitHubSha256Of(null))
    }

    @Test
    fun `the requested version comes from the release the resolver names`() = runBlocking {
        val parser = parser(
            resolved = "https://github.com/owner/repo/releases/tag/v1.2.0",
            pages = mapOf("$api/releases/tags/v1.2.0" to release("v1.2.0"))
        )

        val candidates = parser.findCandidates(testRequest(versionName = "1.2.0"), CandidateOption.REQUESTED)

        assertEquals(1, candidates.size)
        val candidate = candidates.single()
        assertEquals("1.2.0", candidate.versionName)
        assertTrue(candidate.directDownload)
        assertEquals(
            "https://github.com/owner/repo/releases/download/v1.2.0/app-arm64-v8a.apk",
            candidate.files.single().url
        )
    }

    @Test
    fun `a link to the list of releases is searched for the requested tag`() = runBlocking {
        val parser = parser(
            resolved = "https://github.com/owner/repo/releases",
            pages = mapOf("$api/releases?per_page=50" to "[${release("v1.3.0")},${release("v1.2.0")}]")
        )

        val candidates = parser.findCandidates(testRequest(versionName = "1.2.0"), CandidateOption.REQUESTED)

        assertEquals(listOf("1.2.0"), candidates.map { it.versionName })
    }

    @Test
    fun `latest falls back to the newest listed release when only pre-releases exist`() = runBlocking {
        val parser = parser(
            resolved = "https://github.com/owner/repo/releases",
            pages = mapOf("$api/releases?per_page=50" to "[${release("v2.0.0-beta", prerelease = true)}]")
        )

        val candidates = parser.findCandidates(testRequest(versionName = "1.2.0"), CandidateOption.LATEST)

        assertEquals(listOf("2.0.0-beta"), candidates.map { it.versionName })
    }

    @Test
    fun `nothing is looked up when the resolver does not answer with GitHub`() = runBlocking {
        val parser = parser(
            resolved = "https://www.apkmirror.com/apk/google-inc/youtube/",
            pages = emptyMap()
        )

        assertTrue(parser.findCandidates(testRequest(versionName = "1.2.0"), CandidateOption.REQUESTED).isEmpty())
        assertTrue(parser.resolveHistory(testRequest(versionName = "1.2.0")).isEmpty())
    }

    @Test
    fun `a resolver that could not be reached is asked again`() = runBlocking {
        val answers = ArrayDeque(listOf(null, "https://github.com/owner/repo/releases/tag/v1.2.0"))
        val parser = GitHubParser(
            testParserContext(
                pages = mapOf("$api/releases/tags/v1.2.0" to release("v1.2.0")),
                resolveViaMorphe = { answers.removeFirst() }
            )
        )
        val request = testRequest(versionName = "1.2.0")

        assertTrue(parser.findCandidates(request, CandidateOption.REQUESTED).isEmpty())
        assertEquals(listOf("1.2.0"), parser.findCandidates(request, CandidateOption.REQUESTED).map { it.versionName })
    }

    private fun parser(resolved: String, pages: Map<String, String>) =
        GitHubParser(testParserContext(pages = pages, resolveViaMorphe = { resolved }))

    private fun choiceCandidates(vararg fileNames: String): List<DownloadCandidate> =
        fileNames.map { name ->
            DownloadCandidate(
                source = DownloadSource.GITHUB,
                name = "Example App",
                packageName = "com.example.app",
                versionName = "1.0.0",
                versionCode = null,
                url = "https://github.com/owner/repo/releases/tag/v1.0.0",
                fileKind = "apk",
                option = CandidateOption.REQUESTED,
                directDownload = true,
                versionStatus = VersionStatus.REQUESTED,
                formatMatches = true,
                variantLabel = name
            )
        }

    private fun asset(name: String) = GitHubAsset(name = name, downloadUrl = "https://example.invalid/$name")

    private fun release(tag: String, prerelease: Boolean = false): String = """
        {
          "tag_name": "$tag",
          "html_url": "https://github.com/owner/repo/releases/tag/$tag",
          "draft": false,
          "prerelease": $prerelease,
          "assets": [
            {
              "name": "app-arm64-v8a.apk",
              "browser_download_url": "https://github.com/owner/repo/releases/download/$tag/app-arm64-v8a.apk",
              "size": 1024
            },
            {
              "name": "app-x86.apk",
              "browser_download_url": "https://github.com/owner/repo/releases/download/$tag/app-x86.apk",
              "size": 1024
            }
          ]
        }
    """.trimIndent()
}
