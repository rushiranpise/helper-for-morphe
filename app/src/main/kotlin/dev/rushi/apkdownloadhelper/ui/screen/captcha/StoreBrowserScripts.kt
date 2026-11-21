package dev.rushi.apkdownloadhelper.ui.screen.captcha

import dev.rushi.apkdownloadhelper.DOWNLOAD_FILE_KIND_SET
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import java.util.Locale
import org.json.JSONObject

/**
 * Walks APKMirror's search -> release -> variant -> download chain in the
 * in-app browser. The HTTP parser already does this with plain requests; this
 * is the same walk on a page that can pass Cloudflare, with the requested
 * version, ABI and build code used to pick the right variant.
 */
internal fun apkMirrorBrowserScript(
    candidate: DownloadCandidate,
    request: HelperRequest?,
    appName: String?,
    version: String?
): String {
    val fileType = candidate.fileKind.lowercase(Locale.US)
        .takeIf { it in DOWNLOAD_FILE_KIND_SET }
        ?: request?.requestedFileKinds?.orderedFileKinds()?.firstOrNull()
        ?: "apk"
    return """
        (function () {
        if (window.__mhApkMirrorStarted) return;
        window.__mhApkMirrorStarted = true;
        var pkg=__MH_PKG__,ver=__MH_VER__,name=__MH_NAME__,fileType=__MH_TYPE__,abi=__MH_ABI__;
        var codes=__MH_CODES__;
        var nameLower=(name||'').toLowerCase();
        var nameSlug=nameLower.replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'');
        var pkgParts=(pkg||'').toLowerCase().split('.').filter(function(p){
            return p.length>2&&p!=='com'&&p!=='org'&&p!=='net'&&p!=='android';
        });
        var verSlug=(ver||'').toLowerCase().split('.').join('-');
        var verCore=(ver||'').split('-')[0];
        var verCoreSlug=(verCore||'').toLowerCase().split('.').join('-');
        function txt(el){return (el&&el.innerText||'').trim();}
        function digits(s){return (s||'').replace(/[^0-9]/g,'');}
        function hasVer(s){
            var d=digits(s);
            if(ver&&d.indexOf(digits(ver))>=0)return true;
            return verCore&&verCore!==ver&&d.indexOf(digits(verCore))>=0;
        }
        function pathOf(href){
            try{return new URL(href,location.href).pathname.toLowerCase();}
            catch(e){return (href||'').toLowerCase();}
        }
        function segments(p){return p.split('/').filter(function(s){return s.length>0;});}
        function isApkPath(p){var s=segments(p);return s.length>=3&&s[0]==='apk';}
        function isRelease(p){return /-release\/?$/.test(p);}
        function isVariant(p){
            return /-android-(apk|apkm|apks)-download\/?$/.test(p);
        }
        function isAppPage(p){var s=segments(p);return s.length===3&&s[0]==='apk';}
        function rowCode(row){
            if(!row)return -1;
            var spans=row.querySelectorAll('span.colorLightBlack');
            for(var i=0;i<spans.length;i++){
                var span=spans[i];
                if(span.classList.contains('wrapText'))continue;
                if(span.querySelector('.dateyear_utc'))continue;
                var t=txt(span);
                if(/^\d+$/.test(t)){
                    var v=parseInt(t,10);
                    if(!isNaN(v)&&v>0)return v;
                }
            }
            var cells=row.querySelectorAll('.table-cell');
            for(var c=0;c<cells.length;c++){
                var cellText=txt(cells[c]);
                if(/^\d+$/.test(cellText)){
                    var cv=parseInt(cellText,10);
                    if(!isNaN(cv)&&cv>0)return cv;
                }
            }
            var parts=(row.innerText||'').split(/[^0-9]+/);
            for(var k=0;k<codes.length;k++){
                if(parts.indexOf(String(codes[k]))>=0)return codes[k];
            }
            var m=(row.innerText||'').match(/\b\d{6,}\b/g);
            if(!m)return -1;
            for(var j=0;j<m.length;j++){
                var mv=parseInt(m[j],10);
                if(!isNaN(mv)&&mv>0)return mv;
            }
            return -1;
        }
        function pageCode(){
            var vals=document.querySelectorAll('.appspec-value');
            for(var i=0;i<vals.length;i++){
                var m=txt(vals[i]).match(/\((\d{1,18})\)\s*$/);
                if(m){
                    var v=parseInt(m[1],10);
                    if(!isNaN(v)&&v>0)return v;
                }
            }
            var rows=document.querySelectorAll('tr');
            for(var r=0;r<rows.length;r++){
                var th=rows[r].querySelector('th'),td=rows[r].querySelector('td');
                if(!th||!td)continue;
                if(txt(th).toLowerCase()!=='version')continue;
                var tm=txt(td).match(/\((\d{1,18})\)\s*$/);
                if(tm){
                    var tv=parseInt(tm[1],10);
                    if(!isNaN(tv)&&tv>0)return tv;
                }
            }
            return -1;
        }
        function pickSearch(){
            var bestRelease=null,bestReleaseScore=-1,bestApp=null,bestAppScore=-1;
            var firstRelease=null,ls=document.querySelectorAll('a[href]');
            for(var i=0;i<ls.length;i++){
                var a=ls[i],h=a.href||'';
                if(!h||h.indexOf('#')>=0)continue;
                var hp=pathOf(h);
                if(!isApkPath(hp))continue;
                var rel=isRelease(hp),appPage=isAppPage(hp);
                if(!rel&&!appPage)continue;
                var t=txt(a).toLowerCase();
                if(!t)continue;
                var seg=segments(hp);
                var app=seg.length>2?seg[2]:'';
                var score=0;
                if(nameSlug&&app===nameSlug)score+=120;
                else if(nameSlug&&app.indexOf(nameSlug)>=0)score+=60;
                for(var j=0;j<pkgParts.length;j++){
                    if(app===pkgParts[j])score+=80;
                    else if(app.indexOf(pkgParts[j])>=0)score+=30;
                }
                if(nameLower&&t.indexOf(nameLower)>=0)score+=60;
                if(rel){
                    if(!firstRelease)firstRelease=a;
                    if(ver&&!(hp.indexOf(verSlug)>=0||hp.indexOf(verCoreSlug)>=0
                        ||hasVer(t)||hasVer(hp)))continue;
                    score+=20;
                    if(score>bestReleaseScore){bestReleaseScore=score;bestRelease=a;}
                }else if(appPage){
                    if(score>bestAppScore){bestAppScore=score;bestApp=a;}
                }
            }
            if(bestRelease&&bestReleaseScore>=60)return bestRelease;
            if(bestApp&&bestAppScore>=60)return bestApp;
            if(!ver&&bestRelease)return bestRelease;
            if(firstRelease){
                var relPath=pathOf(firstRelease.href);
                var slash=relPath.lastIndexOf('/');
                if(slash>0)return relPath.substring(0,slash+1);
            }
            return null;
        }
        function pickRelease(){
            if(ver&&!(location.pathname.toLowerCase().indexOf(verSlug)>=0
                ||location.pathname.toLowerCase().indexOf(verCoreSlug)>=0
                ||hasVer(location.pathname)))return null;
            var vs=document.querySelectorAll('a[href]');
            var fallback=null,plain=null,plainAbi=null,plainExact=null;
            var plainExactAbi=null,plainUniversal=null,bundle=null,bundleAbi=null;
            var bundleExact=null,bundleExactAbi=null;
            for(var v=0;v<vs.length;v++){
                var va=vs[v],vh=va.href||'';
                if(!vh||vh.indexOf('#')>=0)continue;
                var vp=pathOf(vh);
                if(!isVariant(vp))continue;
                var row=va.closest('.table-row'),rt=txt(row).toLowerCase();
                if(!rt){
                    var par=va.parentElement;
                    for(var u=0;u<4&&par;u++){
                        rt=txt(par).toLowerCase();
                        if(rt.indexOf('apk')>=0||rt.indexOf('bundle')>=0)break;
                        par=par.parentElement;
                    }
                }
                var isBundle=rt.indexOf('bundle')>=0||rt.indexOf('apkm')>=0
                    ||rt.indexOf('apks')>=0;
                var isPlain=!isBundle;
                var universal=rt.indexOf('universal')>=0;
                var hasAbi=abi&&rt.indexOf(abi.toLowerCase())>=0;
                var rc=rowCode(row);
                if(codes.length&&(rc<=0||codes.indexOf(rc)<0))continue;
                var exact=rc>0&&codes.indexOf(rc)>=0;
                if(!fallback)fallback=va;
                if(isPlain&&exact&&hasAbi&&!plainExactAbi)plainExactAbi=va;
                if(isPlain&&exact&&!plainExact)plainExact=va;
                if(isPlain&&universal&&!plainUniversal)plainUniversal=va;
                if(isPlain&&hasAbi&&!plainAbi)plainAbi=va;
                if(isPlain&&!plain)plain=va;
                if(isBundle&&exact&&hasAbi&&!bundleExactAbi)bundleExactAbi=va;
                if(isBundle&&exact&&!bundleExact)bundleExact=va;
                if(isBundle&&hasAbi&&!bundleAbi)bundleAbi=va;
                if(isBundle&&!bundle)bundle=va;
            }
            var wantBundle=fileType&&fileType.toLowerCase()!=='apk';
            if(wantBundle){
                if(bundleExactAbi)return bundleExactAbi;
                if(bundleExact)return bundleExact;
                if(plainExactAbi)return plainExactAbi;
                if(plainExact)return plainExact;
                return bundleAbi||bundle||plain||fallback;
            }
            if(plainExactAbi)return plainExactAbi;
            if(plainExact)return plainExact;
            return plainUniversal||plainAbi||plain||fallback;
        }
        function pickApp(){
            var rs=document.querySelectorAll('a[href]'),picked=null,first=null;
            for(var r=0;r<rs.length;r++){
                var ra=rs[r],rh=ra.href||'';
                if(!rh||rh.indexOf('#')>=0)continue;
                if(!isRelease(pathOf(rh)))continue;
                if(!first)first=ra;
                if(ver&&(rh.toLowerCase().indexOf(verSlug)>=0
                    ||rh.toLowerCase().indexOf(verCoreSlug)>=0
                    ||hasVer(txt(ra)))){picked=ra;break;}
            }
            return picked||(ver?null:first);
        }
        function pickDownload(){
            if(codes.length){
                var pc=pageCode();
                if(pc>0&&codes.indexOf(pc)<0){
                    if(!window.__mhBuildMismatch){
                        window.__mhBuildMismatch=true;
                        location.href='morphe-helper://build-mismatch?code='
                            +encodeURIComponent(pc);
                    }
                    return null;
                }
            }
            var ds=document.querySelectorAll('a,button');
            for(var d=0;d<ds.length;d++){
                var dh=ds[d].href||'';
                if(dh.indexOf('download.php')>=0
                    ||dh.indexOf('/download/?key=')>=0)return ds[d];
                var dt=txt(ds[d]).toLowerCase();
                if(dt.indexOf('download apk')>=0)return ds[d];
            }
            return null;
        }
        function pick(){
            var p=location.pathname.toLowerCase();
            var q=location.search.toLowerCase();
            if(q.indexOf('searchtype=')>=0)return pickSearch();
            if(isRelease(p))return pickRelease();
            if(isAppPage(p))return pickApp();
            if(isVariant(p))return pickDownload();
            return pickDownload();
        }
        var n=0,t=setInterval(function(){
            n++;
            var target=pick();
            if(target){
                clearInterval(t);
                if(typeof target==='string'){location.href=target;return;}
                try{target.scrollIntoView({block:'center'});}catch(e){}
                setTimeout(function(){try{target.click();}catch(e){}},300);
                return;
            }
            if(n>900)clearInterval(t);
        },400);
        })();
    """.trimIndent()
        .replace("__MH_PKG__", JSONObject.quote(candidate.packageName))
        .replace("__MH_VER__", JSONObject.quote(version.orEmpty()))
        .replace("__MH_NAME__", JSONObject.quote(appName.orEmpty()))
        .replace("__MH_TYPE__", JSONObject.quote(fileType))
        .replace(
            "__MH_ABI__",
            JSONObject.quote(request?.availableAbis?.firstOrNull().orEmpty())
        )
        .replace(
            "__MH_CODES__",
            request?.requestedVersionCodes
                ?.joinToString(separator = ",", prefix = "[", postfix = "]")
                ?: "[]"
        )
}

/**
 * Walks APKPure's search -> versions -> app -> download chain in the in-app
 * browser, the last resort after the direct endpoint and web parsers fail.
 */
internal fun apkPureBrowserScript(
    candidate: DownloadCandidate,
    appName: String?,
    version: String?
): String = """
    (function () {
      if (window.__mhApkPureStarted) return;
      window.__mhApkPureStarted = true;
      var pkg=__MH_PKG__,ver=__MH_VER__,name=__MH_NAME__;
      var nameLower=(name||'').toLowerCase();
      var pkgLower=(pkg||'').toLowerCase();
      function txt(el){return (el&&el.innerText||'').trim();}
      function digits(s){return (s||'').replace(/[^0-9]/g,'');}
      function hasVer(s){
        var d=digits(s);
        return !!ver&&d.indexOf(digits(ver))>=0;
      }
      function pathOf(href){
        try{return new URL(href,location.href).pathname.toLowerCase();}
        catch(e){return (href||'').toLowerCase();}
      }
      function pickSearch(){
        var ls=document.querySelectorAll('a');
        for(var i=0;i<ls.length;i++){
          var h=ls[i].href||'',t=txt(ls[i]).toLowerCase();
          if(!h||h.indexOf('/search')>=0)continue;
          if(h.toLowerCase().indexOf(pkgLower)>=0
            ||(nameLower.length>2&&t.indexOf(nameLower)>=0&&t.length<100))return ls[i];
        }
        return null;
      }
      function pickVersions(){
        var vs=document.querySelectorAll('a');
        for(var j=0;j<vs.length;j++){
          var vt=txt(vs[j]),vh=vs[j].href||'';
          if(hasVer(vt)||hasVer(vh))return vs[j];
        }
        return null;
      }
      function pickInfo(){
        var canonical=document.querySelector('link[rel=canonical]');
        if(canonical&&canonical.href
          &&canonical.href.toLowerCase().indexOf(pkgLower)>=0
          &&canonical.href.toLowerCase().indexOf('/apk-info')<0)return canonical.href;
        var ls=document.querySelectorAll('a[href]');
        for(var i=0;i<ls.length;i++){
          var h=ls[i].href||'';
          if(h.toLowerCase().indexOf(pkgLower)>=0
            &&h.toLowerCase().indexOf('/apk-info')<0)return ls[i];
        }
        return null;
      }
      function pickDownloadLink(){
        var a=document.querySelector('a[href*="d.apkpure.com"]');
        if(a)return a;
        var vs=document.querySelectorAll(
          'a.go-version-btn[href],a.dt-version-name-link[href],a#download_link[href]');
        for(var v=0;v<vs.length;v++){
          var vh=vs[v].href||'';
          if(!ver||hasVer(txt(vs[v]))||hasVer(vh))return vs[v];
        }
        var bs=document.querySelectorAll('a,button');
        for(var k=0;k<bs.length;k++){
          var bt=txt(bs[k]).toLowerCase();
          if(bt.indexOf('download apk')>=0&&bt.indexOf('apkpure app')<0)return bs[k];
        }
        return null;
      }
      function pick(){
        var p=location.pathname.toLowerCase();
        if(p.indexOf('/search')>=0)return pickSearch();
        if(p.indexOf('/apk-info')>=0)return pickInfo();
        if(p.indexOf('/versions')>=0)return pickVersions();
        var direct=pickDownloadLink();
        if(direct)return direct;
        if(p.indexOf('/download')<0&&ver&&!hasVer(document.body.innerText)){
          var old=document.querySelector('a[href*="/versions"]');
          if(old)return old;
        }
        return null;
      }
      var n=0,t=setInterval(function(){
        n++;
        var target=pick();
        if(target){
          clearInterval(t);
          if(typeof target==='string'){location.href=target;return;}
          try{target.scrollIntoView({block:'center'});}catch(e){}
          setTimeout(function(){try{target.click();}catch(e){}},500);
          return;
        }
        if(n>80)clearInterval(t);
      },700);
    })();
""".trimIndent()
    .replace("__MH_PKG__", JSONObject.quote(candidate.packageName))
    .replace("__MH_VER__", JSONObject.quote(version.orEmpty()))
    .replace("__MH_NAME__", JSONObject.quote(appName.orEmpty()))
