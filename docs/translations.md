# Translations

The app's strings live in [`app/src/main/res/values/strings.xml`](../app/src/main/res/values/strings.xml),
and translations come from Crowdin, configured by [`crowdin.yml`](../crowdin.yml) and synced by
[`.github/workflows/crowdin.yml`](../.github/workflows/crowdin.yml).

## The app had one string

Until recently `strings.xml` held exactly that: `app_name`. Every piece of text a person read was
a literal in the Compose code, and nothing referenced `R.string.` anywhere, so there was nothing
for a translator to translate and no way to hand them anything.

**83 strings have been moved**, which is every static label, heading, button, tooltip and
accessibility description in the UI — 97 call sites across `MainActivity.kt` and
`MorpheColorPicker.kt`. They are quoted in `strings.xml` and read back with
`stringResource(R.string.…)`, and the rendered text is unchanged; the work was checked by
screenshotting the app before and after, not by reading the diff.

Two things were deliberately not touched, and they are the reason this is a first pass:

| Left alone | Why | Roughly how many |
| --- | --- | --- |
| Literals built with `$` interpolation (`"Scan failed · ${…}"`) | They need to become format strings - `<string name="…">Scan failed · %1$s</string>` and `stringResource(R.string.…, message)` - which is a different edit at every site, not a rename | 26 |
| Literals in code that is not a `@Composable` (notification text in `DownloadService`, `Toast`, and the `title`/`description` fields of the enums in `SettingsRepository`) | `stringResource()` can only be called from a composable, so these need `context.getString(…)` or, for the enums, a change from `val title: String` to `@StringRes val title: Int` | 21 |

Until those are done the enums in `SettingsRepository.kt` — the scan mode, fast-mode policy, theme
mode, theme style, download location and network policy labels — stay English, which is the most
visible of what is left.

## What is still needed

**The project does not exist in Crowdin yet.** Creating it is the one thing that cannot be done
from the repository: the account holds an open-source licence — unlimited projects, strings and
members on the free plan — but Crowdin grants that licence **per project**, and its API answers
`403 Request an open source license to create another open source project` until the new one has
been granted its own. It is applied for on the website and read by a person:

- form: <https://crowdin.com/product/for-open-source>
- what it wants: an OSI-approved licence (this repository is GPL-3.0), public sources, no
  commercial product, the project lead, and a project that has been going for at least three months

Two things have to be in place before the workflow does anything:

| Where | What | Notes |
| --- | --- | --- |
| *Settings → Secrets and variables → Actions → **Variables*** | `CROWDIN_PROJECT_ID` | The number in the project's URL. Every job skips itself until this is set, so the workflow can sit in the tree doing nothing. |
| *Settings → Secrets and variables → Actions → **Secrets*** | `CROWDIN_PERSONAL_TOKEN` | A Crowdin personal access token with *Source files & strings* and *Translations* at Read and Write. Already set. |

## What runs when

| When | What |
| --- | --- |
| A push to `master` that changes `values/strings.xml` | `upload sources` — Crowdin learns what there is to translate |
| Nightly at 04:23, or *Run workflow* | `download translations` into a pull request on the `l10n` branch |
| That pull request | merged automatically, squashed, if it only touches `values-*/strings.xml` and every file in it holds at least one string |

Nothing can push a translation into the tree on its own, which is deliberate: a locale folder here
is a claim that the language is translated, and that claim is worth a look before it is made.

## Adding a language

1. **Create the folder** — `app/src/main/res/values-<code>/strings.xml`. The check that guards the
   translation pull requests refuses any file whose folder does not exist, so a language appears in
   the app because someone decided it should, never because an export brought it along.
2. **Add it to the Crowdin project** and map it in [`crowdin.yml`](../crowdin.yml), which has no
   mapping yet because there are no languages yet. Crowdin's Android code for a language is
   regional (`de-rDE`, `fr-rFR`), so without the mapping the export writes folders this app does
   not carry.
3. **Let the next sync fill it** — the folder is the only thing that has to be made by hand.
