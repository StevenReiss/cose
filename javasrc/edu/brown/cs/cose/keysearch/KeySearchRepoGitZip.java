/********************************************************************************/
/*										*/
/*		KeySearchRepoGitZip.java					*/
/*										*/
/*	Handle retrieval from GITHUB using repository zips			*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.cose.keysearch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import edu.brown.cs.cose.cosecommon.CoseException;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;



class KeySearchRepoGitZip extends KeySearchRepoGithub
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final static String	SOURCE_PREFIX = "GITZIP:";


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

KeySearchRepoGitZip(CoseRequest sr)
{
   super(sr);
}



/********************************************************************************/
/*										*/
/*	Top level methods							*/
/*										*/
/********************************************************************************/

@Override protected boolean isRelevantSource(String src)
{
   return src.startsWith(SOURCE_PREFIX);
}


@Override CoseSource createSource(URI uri,String cnts,int idx)
{
   return new GitZipSource(uri.toString(),cnts,idx);
}



/********************************************************************************/
/*										*/
/*	Set up zips for search page results					*/
/*										*/
/********************************************************************************/

List<URI> getSearchPageResults(Element jsoup)
{
   List<URI> rslt = new ArrayList<URI>();
   Elements results = jsoup.select("div.code-list-item");
   for (Element result : results) {
      Elements uris = result.select("p.title a:eq(0)");
      Element tag = uris.get(0);
      String href = tag.attr("href");
      href = href.replace("%2524","$");
      Elements fileuris = result.select("p.title a:eq(1)");
      Element filetag = fileuris.get(0);
      String filehref = filetag.attr("href");
      filehref = filehref.replace("%2524","$");

      try {
	 URI uri = new URI(GITHUB_SCHEME,GITHUB_AUTHORITY,href,GITHUB_FRAGMENT);
	 URI fileuri = new URI(GITHUB_SCHEME,GITHUB_AUTHORITY,filehref,GITHUB_FRAGMENT);
	 String projectpage = getResultPage(uri);
	 if (projectpage == null) continue;
	 Element projsoup = Jsoup.parse(projectpage,uri.toString());
	 Elements zipz = null;
	 zipz = projsoup.select("a.get-repo-btn");
	 if (zipz.size() == 0) {
	    zipz = projsoup.select(".file-navigation-option a.btn");
	  }
	 URI zipuri = null;
	 String zippfx = null;
	 for (Element projzip : zipz) {
	    String zref = projzip.attr("href");
	    if (!zref.contains(".zip")) continue;
	    zipuri = new URI(GITHUB_SCHEME,GITHUB_AUTHORITY,zref,GITHUB_FRAGMENT);
	    zippfx = zref;
	    int idx1 = zippfx.indexOf("/archive/");
	    if (idx1 > 0) zippfx = zippfx.substring(0,idx1);
	    break;
	  }
	 if (zipuri == null) {
	    rslt.add(fileuri);
	    continue;
	  }
	 // check if we already have the binary before loading it
	 File zipdir = getZipDirectory(zippfx);
	 File dataf = new File(zipdir,"DATA.zip");
	 long dlm = dataf.lastModified();

	 try {
	    URL url = zipuri.toURL();
	    IvyLog.logI("COSE","Load Zip file " + zippfx + " from " + url);
	    InputStream ins = url_cache.getCacheStream(url,this,dlm);
	    if (ins != null) {
	       storeZipData(fileuri,uri,zippfx,ins);
	       ins.close();
	     }
	  }
	 catch (Exception e) {
	    byte [] zipcnts = getResultBinary(zipuri);
	    storeZipData(fileuri,uri,zippfx,zipcnts);
	  }
	 String path = filehref;
	 int idx2 = path.indexOf("/blob/");
	 int idx3 = path.indexOf("/",idx2+7);
	 path = path.substring(idx3);
	 URI ruri = new URI("http","GITZIP",zippfx,path);
	 rslt.add(ruri);
       }
      catch (URISyntaxException e) { }
    }
   return rslt;
}


@Override protected URI convertSearchResults(JSONObject jobj)
{
   try {
      String fileurl = jobj.getString("html_url");
      URI fileuri = new URI(fileurl);
      JSONObject robj = jobj.getJSONObject("repository");
      String archurl = robj.getString("archive_url");
      archurl = archurl.replace("{archive_format}","zipball");
      archurl = archurl.replace("{/ref}","");
      String zippfx = "/" + robj.getString("full_name");

      File zipdir = getZipDirectory(zippfx);
      File dataf = new File(zipdir,"DATA.zip");
      long dlm = dataf.lastModified();
      URL url = new URL(archurl);
      InputStream ins = url_cache.getCacheStream(url,this,dlm);
      if (ins != null) {
	 URI repouri = new URI("/https://github.com/" + robj.getString("full_name"));
	 storeZipData(fileuri,repouri,zippfx,ins);
	 ins.close();
       }
      String path = fileurl;
      int idx2 = path.indexOf("/blob/");
      int idx3 = path.indexOf("/",idx2+7);
      path = path.substring(idx3);
      URI ruri = new URI("http","GITZIP",zippfx,path);
      return ruri;
    }
   catch (URISyntaxException e) {
      IvyLog.logE("COSE","PROBLEM WITH URI",e);
    }
   catch (JSONException e) {
      IvyLog.logE("COSE","PROBLEM WITH JSON",e);
      return null;
    }
   catch (MalformedURLException e) {
      IvyLog.logE("COSE","PROBLEM WITH URL",e);
    }
   catch (IOException e) {
      IvyLog.logE("COSE","Problem reading zip file",e);
    }
   return null;
}








/********************************************************************************/
/*										*/
/*	Path access methods							*/
/*										*/
/********************************************************************************/

@Override URI getURIForPath(CoseSource src,String path)
{
   // generate URI using the zip file
   String b1 = src.getDisplayName();
   int idx1 = b1.indexOf("/",1);
   int idx2 = b1.indexOf("#",idx1+1);

   String rep = b1.substring(idx1,idx2);
   if (!path.startsWith("/")) {
      String opath = b1.substring(idx2+1);
      int idx3 = opath.lastIndexOf("/");
      if (idx3 >= 0) opath = opath.substring(0,idx3);
      path = opath + "/" + path;
    }
   try {
      URI ruri = new URI("http","GITZIP",rep,path);
      return ruri;
    }
   catch (URISyntaxException e) {
      IvyLog.logE("COSE","Problem with path URI",e);
    }

   return super.getURIForPath(src,path);
}


URI getURIFromSourceString(String source)
{
   // if we can use the repository, do so
   return super.getURIFromSourceString(source);
}


@Override List<URI> getDirectoryContentsURIs(URI baseuri,CoseSource src)
{
   List<URI> rslt = null;
   String path = baseuri.getFragment();
   String zid = null;

   try {
      String b1 = src.getDisplayName();
      int idx1 = b1.indexOf("/",1);
      int idx2 = b1.indexOf("#",idx1+1);

      String rep = b1.substring(idx1,idx2);

      File dirf = new File(ZIPCACHE_DIRECTORY);
      zid = rep.replace("/","@");
      File zipdir = new File(dirf,zid);
      File zipf = new File(zipdir,"DATA.zip");
      if (zipf.exists()) {
	 ZipFile zf = new ZipFile(zipf);
	 String pfx = null;
	 int len0 = path.length();
	 for (Enumeration<? extends ZipEntry> enm = zf.entries(); enm.hasMoreElements(); ) {
	    ZipEntry ze = enm.nextElement();
	    if (pfx == null) {
	       String zpfx = ze.getName();
	       int idx = zpfx.indexOf("/");
	       if (idx > 0) {
		  pfx = zpfx.substring(0,idx);
		  len0 = pfx.length() + path.length();
		}
	     }
	    String nm = ze.getName();
	    if (nm.startsWith(pfx + path + "/")) {
	       if (nm.endsWith("/")) nm = nm.substring(0,nm.length()-1);
	       int idx = nm.indexOf("/",len0 + 1);
	       if (idx < 0 && nm.length() > len0+1) {
		  String xpath = nm.substring(pfx.length());
		  URI uri = new URI("http","GITZIP",rep,xpath);
		  if (rslt == null) rslt = new ArrayList<URI>();
		  rslt.add(uri);
		}
	     }
	  }
	 zf.close();
       }
    }
   catch (Exception e) {
      IvyLog.logE("COSE","Problem reading zip file " + zid,e);
    }

   return rslt;
}


boolean getClassesInPackage(String pkg,String project,int page,List<URI> rslt)
{
   String rep = "/" + project;
   String path = pkg.replace(".","/");
   String zid = null;

   try {
      File dirf = new File(ZIPCACHE_DIRECTORY);
      zid = rep.replace("/","@");
      File zipdir = new File(dirf,zid);
      File zipf = new File(zipdir,"DATA.zip");
      if (zipf.exists()) {
	 ZipFile zf = new ZipFile(zipf);
	 String pfx = null;
	 String best = null;
	 for (Enumeration<? extends ZipEntry> enm = zf.entries(); enm.hasMoreElements(); ) {
	    ZipEntry ze = enm.nextElement();
	    if (pfx == null) {
	       String zpfx = ze.getName();
	       int idx = zpfx.indexOf("/");
	       if (idx > 0) {
		  pfx = zpfx.substring(0,idx);
		}
	     }
	    String nm = ze.getName();
	    int idx = nm.indexOf(path);
	    if (nm.endsWith(".java") && idx > 0 && !nm.contains("/gen/")) {
	       int idx1 = nm.indexOf("/",idx+path.length()+1);
	       if (idx1 < 0) {
		  int idx2 = nm.lastIndexOf("/");
		  String npfx = nm.substring(0,idx2+1);
		  if (best == null || best.length() > npfx.length()) best = npfx;
		}
	     }
	  }
	 if (best != null) {
	    for (Enumeration<? extends ZipEntry> enm = zf.entries(); enm.hasMoreElements(); ) {
	       ZipEntry ze = enm.nextElement();
	       String nm = ze.getName();
	       if (nm.startsWith(best) && nm.endsWith(".java")) {
		  int idx = nm.indexOf("/",best.length() + 1);
		  if (idx < 0) {
		     String rpath = nm.substring(pfx.length());
		     URI uri = new URI("http","GITZIP",rep,rpath);
		     rslt.add(uri);
		   }
		}
	     }
	  }
	 zf.close();
       }
    }
   catch (Exception e) {
      IvyLog.logE("COSE","Problem reading zip file " + zid,e);
    }

   return false;
}


KeySearchClassData getPackageClassResult(CoseSource base,String pkg,String cls,int page)
{
   KeySearchClassData rslt = null;
   String zid = null;

   try {
      String b1 = base.getDisplayName();
      int idx1 = b1.indexOf("/",1);
      int idx2 = b1.indexOf("#",idx1+1);

      String rep = b1.substring(idx1,idx2);
      String path = pkg.replace(".","/") + "/" + cls + ".java";

      File dirf = new File(ZIPCACHE_DIRECTORY);
      zid = rep.replace("/","@");
      File zipdir = new File(dirf,zid);
      File zipf = new File(zipdir,"DATA.zip");
      if (zipf.exists()) {
	 ZipFile zf = new ZipFile(zipf);
	 String pfx = null;
	 for (Enumeration<? extends ZipEntry> enm = zf.entries(); enm.hasMoreElements(); ) {
	    ZipEntry ze = enm.nextElement();
	    if (pfx == null) {
	       String zpfx = ze.getName();
	       int idx = zpfx.indexOf("/");
	       if (idx > 0) {
		  pfx = zpfx.substring(0,idx);
		}
	     }
	    String nm = ze.getName();
	    if (nm.endsWith(path)) {
	       URI ruri = new URI("http","GITZIP",rep,path);
	       InputStream ins = zf.getInputStream(ze);
	       ByteArrayOutputStream baos = new ByteArrayOutputStream();
	       byte [] buf = new byte[8192];
	       for ( ; ; ) {
		  int ln = ins.read(buf);
		  if (ln <= 0) break;
		  baos.write(buf,0,ln);
		}
	       ins.close();
	       String code = getString(baos);
	       CoseSource rsrc = createSource(ruri,code,0);
	       rslt = new KeySearchClassData(ruri,path,rsrc,code);
	       break;
	     }
	  }
	 zf.close();
       }
    }
   catch (Exception e) {
      IvyLog.logE("COSE","Problem reading zip file " + zid,e);
    }

   return rslt;
}


@Override protected URI getRawFileURI(URI u)
{
   if (u.getFragment() != null) return u;

   return super.getRawFileURI(u);
}



@Override protected String getRawSourcePage(URI uri)
{
   String cnts = getResultPage(uri);

   return cnts;
}


/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

protected ByteArrayOutputStream loadURIBinary(URI uri,boolean cache,
       boolean reread) throws CoseException
{
   if (!uri.getAuthority().equals("GITZIP"))
      return super.loadURIBinary(uri,cache,reread);

   String rep = uri.getPath();
   String file = uri.getFragment();
   String zid = rep.replace("/","@");

   try {
      File dirf = new File(ZIPCACHE_DIRECTORY);
      File zipdir = new File(dirf,zid);
      File zipf = new File(zipdir,"DATA.zip");
      if (zipf.exists()) {
	 ZipFile zf = new ZipFile(zipf);
	 String pfx = "";
	 for (Enumeration<? extends ZipEntry> enm = zf.entries(); enm.hasMoreElements(); ) {
	    ZipEntry ze = enm.nextElement();
	    String zpfx = ze.getName();
	    int idx = zpfx.indexOf("/");
	    if (idx > 0) {
	       pfx = zpfx.substring(0,idx);
	       break;
	     }
	  }
	 ZipEntry ent = zf.getEntry(pfx + file);
	 if (ent != null) {
	    InputStream ins = zf.getInputStream(ent);
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte [] buf = new byte[8192];
	    for ( ; ; ) {
	       int ln = ins.read(buf);
	       if (ln <= 0) break;
	       baos.write(buf,0,ln);
	     }
	    ins.close();
	    zf.close();
	    return baos;
	  }
	 zf.close();
       }
    }
   catch (IOException e) {
      IvyLog.logE("COSE","Problem reading zip file " + zid,e);
    }

   return null;
}






/********************************************************************************/
/*										*/
/*	Store zip file information						*/
/*										*/
/********************************************************************************/

private void storeZipData(URI base,URI repo,String id,InputStream ins)
{
   if (ins == null) return;

   File zipdir = getZipDirectory(id);
   File ctxf = new File(zipdir,"CONTEXT");
   File dataf = new File(zipdir,"DATA.zip");
   try {
      FileWriter fw = new FileWriter(ctxf);
      fw.write("BASE: " + base.toString() + "\n");
      fw.write("REPO: " + repo.toString() + "\n");
      fw.write("ID: " + id + "\n");
      fw.close();
      FileOutputStream ots = new FileOutputStream(dataf);
      byte [] buf = new byte[8192];
      for ( ; ; ) {
	 int ln = ins.read(buf);
	 if (ln <= 0) break;
	 ots.write(buf,0,ln);
       }
      ots.close();
      fixupZip(zipdir);
    }
   catch (IOException e) {
      IvyLog.logE("COSE","Problem writing zip cache",e);
    }
}



private void storeZipData(URI base, URI repo,String id,byte [] cnts)
{
   File zipdir = getZipDirectory(id);
   if (zipdir.exists()) {
      File zipf = new File(zipdir,"DATA.zip");
      if (zipf.exists() && zipf.length() == cnts.length) return;
    }
   else zipdir.mkdirs();

   File ctxf = new File(zipdir,"CONTEXT");
   File dataf = new File(zipdir,"DATA.zip");
   try {
      FileWriter fw = new FileWriter(ctxf);
      fw.write("BASE: " + base.toString() + "\n");
      fw.write("REPO: " + repo.toString() + "\n");
      fw.write("ID: " + id + "\n");
      fw.close();
      FileOutputStream ots = new FileOutputStream(dataf);
      ots.write(cnts);
      ots.close();
      fixupZip(zipdir);
    }
   catch (IOException e) {
      IvyLog.logE("COSE","Problem writing zip cache",e);
    }
}


private void fixupZip(File dir) throws IOException
{
   File zipf = new File(dir,"DATA.zip");
   if (!zipf.exists()) return;
   long len = zipf.length();
   if (len < 4L*1024L*1024L*1024L) return;

   // need to fix up a zip file > 4Gb

   String cmd = IvyFile.expandName("$(Cose)/bin/fixbigzip");
   cmd += " " + dir.getPath();

   IvyExec ex = new IvyExec(cmd);
   int sts = ex.waitFor();
   if (sts == 0) return;

   IvyLog.logI("COSE","Problem fixing big zip file");
}



private File getZipDirectory(String id)
{
   String fid = id.replace("/","@");
   File d = new File(ZIPCACHE_DIRECTORY);
   File zipdir = new File(d,fid);
   if (!zipdir.exists()) zipdir.mkdirs();
   return zipdir;
}




/********************************************************************************/
/*										*/
/*	GitZipSource								*/
/*										*/
/********************************************************************************/

private static class GitZipSource extends KeySearchSource implements CoseSource {

   private String base_link;
   private String base_path;

   GitZipSource(String base,String code,int idx) {
      super(code,idx);
      base_link = base;
      int pos = 1;
      for (int i = 0; i < 3; ++i) {
         pos = base.indexOf("/",pos);
       }
      base_path = base.substring(pos+1);
    }

   @Override public String getName()		{ return SOURCE_PREFIX + base_link; }
   @Override public String getDisplayName()	{ return base_path; }

   @Override public String getProjectId() {
      String base = base_link;
      int idx = base.indexOf("//");
      idx = base.indexOf("/",idx+2);
      int idx1 = base.indexOf("#",idx);
      if (idx1 < 0) {
         idx1 = base.indexOf("/blob/",idx);
       }
      return base.substring(idx+1,idx1);
    }

   @Override public String getPathName() {
      String spath = base_path;
      int idx1 = spath.indexOf("/blob/");
      if (idx1 > 0) {
	 int idx2 = spath.indexOf("/",idx1+7);
	 spath = spath.substring(idx2); 	// skip /blob/<key>/ :: path remoaint
       }
      return spath;
    }

}	// end of subclass GitZipSource




}	// end of class KeySearchRepoGitZip




/* end of KeySearchRepoGitZip.java */

