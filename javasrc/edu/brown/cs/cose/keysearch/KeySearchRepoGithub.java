/********************************************************************************/
/*										*/
/*		KeySearchRepoGithub.java					*/
/*										*/
/*	Interface to GITHUB repository						*/
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

import edu.brown.cs.cose.cosecommon.CoseException;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

import java.io.*;
import java.net.*;
import java.util.*;

import org.json.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;


class KeySearchRepoGithub extends KeySearchRepo
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected final static String	GITHUB_SCHEME = "https";
protected final static String	GITHUB_AUTHORITY = "github.com";
protected final static String	GITHUB_FRAGMENT = null;
private final static String	GITHUB_FILE_AUTHORITY = "raw.github.com";

private final static String	SOURCE_PREFIX = "GITHUB:";

private final static int	RESULTS_PER_PAGE = 100;
private final static int	SIMULTANEOUS_SEARCH = 1;

protected static OAuthData	oauth_token = null;
protected static String	github_auth;


private final static String	COSE_CLIENT_ID = "92367cf10da5b70932fa";
private final static String	COSE_CLIENT_SECRET = "53e04859dec97346e3cd9f886b4e847c4d7cc2dc";
private final static String	COSE_FINGERPRINT;


static {
   COSE_FINGERPRINT = "Cose_" + Math.round(Math.random()*1000000000);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

KeySearchRepoGithub(CoseRequest sr)
{
   super(sr,SIMULTANEOUS_SEARCH);
}


/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override URI getURIFromSourceString(String src)
{
   if (!isRelevantSource(src)) return null;

   try {
      int idx1 = src.lastIndexOf("@");
      if (idx1 > 0) src = src.substring(0,idx1);
      int idx = src.indexOf(":");
      return new URI(src.substring(idx+1));
    }
   catch (URISyntaxException e) { }

   return null;
}



protected boolean isRelevantSource(String src)
{
   return src.startsWith(SOURCE_PREFIX);
}


@Override int getResultsPerPage()
{
   return RESULTS_PER_PAGE;
}



@Override public String getAuthorization()
{
   getOAuthToken();
   if (oauth_token == null) {
      if (github_auth != null) return github_auth;
    }
   return "token " + oauth_token.getToken();
}


@Override protected URI getURIForSearch(List<String> keys,CoseSearchLanguage lang,String projectid,int page)
{
   String q = "";
   String langstr = null;
   switch (lang) {
      case JAVA :
	 langstr = "java";
	 break;
      case JAVASCRIPT :
         langstr = "JavaScript";
         break;
      case XML :
	 langstr = "xml";
	 break;
    }

   q += "q=";
   int i = 0;
   for (String s : keys) {
      s = normalizeKeyword(s);
      if (i++ > 0) q += " ";
      if (s.contains(" ")) q += "\"" + s + "\"";
      else q += s;
    }
   if (projectid != null) q += " repo:" + projectid;

   try {
      if (lang != null) q += " language:" + langstr;
      if (page > 0) q+= "&page=" + (page+1);
      q += "&per_page=" + RESULTS_PER_PAGE;
      URI uri = new URI(GITHUB_SCHEME,"api.github.com","/search/code",q,null);
      return uri;
    }
   catch (URISyntaxException e) { }

   return null;
}



@Override CoseSource createSource(URI uri,String cnts,int idx)
{
   return new GithubSource(uri.toString(),cnts,idx);
}



/********************************************************************************/
/*										*/
/*	Search page scanning							*/
/*										*/
/********************************************************************************/

List<URI> getSearchPageResults(URI uri,String cnts)
{
   List<URI> rslt = new ArrayList<URI>();
   try {
      JSONArray jarr = null;
      if (cnts.startsWith("{")) {
	 JSONObject jobj = new JSONObject(cnts);
	 jarr = jobj.getJSONArray("items");
       }
      else if (cnts.startsWith("[")) {
	 jarr = new JSONArray(cnts);
       }
      else jarr = new JSONArray();
      for (int i = 0; i < jarr.length(); ++i) {
	 JSONObject jobj = jarr.getJSONObject(i);
	 URI uri2 = convertSearchResults(jobj);
	 if (uri2 != null) rslt.add(uri2);
       }
    }
   catch (JSONException e) {
      IvyLog.logE("COSE","Problem parsing github json return",e);
    }

   return rslt;
}


protected URI convertSearchResults(JSONObject jobj)
{
   try {
      URI uri2 = new URI(jobj.getString("html_url"));
      return uri2;
    }
   catch (URISyntaxException e) {
      IvyLog.logE("COSE","BAD URI: " + e);
    }
   catch (JSONException e) {
      IvyLog.logE("COSE","BAD JSON: " + e);
    }

   return null;
}


List<URI> getSearchPageResults(Element jsoup)
{
   List<URI> rslt = new ArrayList<URI>();
   Elements results = jsoup.select("div.code-list-item");
   for (Element result : results) {
      Elements uris = result.select("p.title a:eq(1)");
      Element tag = uris.get(0);
      String href = tag.attr("href");
      try {
	 href = href.replace("%2524","$");
	 URI uri = new URI(GITHUB_SCHEME,GITHUB_AUTHORITY,href,GITHUB_FRAGMENT);
	 rslt.add(uri);
	 Elements codes = result.select("td.blob-code");
	 StringBuffer buf = new StringBuffer();
	 for (Element codeelt : codes) {
	    buf.append(codeelt.text());
	    buf.append("\n");
	  }
       }
      catch (URISyntaxException e) { }
    }
   return rslt;
}



@Override boolean hasMoreSearchPages(URI uri,String cnts,int page)
{
   if (cnts == null) return false;
   if (cnts.startsWith("{")) {
      return cnts.contains("\"incomplete_results\":true");
    }
   return cnts.contains("class=\"next_page\"");
}




/********************************************************************************/
/*										*/
/*	Path access methods							*/
/*										*/
/********************************************************************************/

@Override URI getURIForPath(CoseSource src,String path)
{
   if (!(src instanceof GithubSource)) return null;

   GithubSource gsrc = (GithubSource) src;
   String spath = src.getDisplayName();
   if (path == null || !path.startsWith("/")) {
      int idx1 = spath.indexOf("/blob/");
      if (idx1 > 0) {
	 int idx2 = spath.indexOf("/",idx1+7);
	 spath = spath.substring(idx2); 	// skip /blob/<key>/ :: path remoaint
       }
      int idx3 = spath.lastIndexOf("/");
      if (idx3 > 0) spath = spath.substring(0,idx3);	  // remove AndroidManifest.xml
      else spath = "";
      if (path == null) path = spath;
      else path = spath + "/" + path;
    }
   return gsrc.getPathURI(path);
}


@Override List<URI> getDirectoryContentsURIs(URI baseuri,CoseSource src,Element jsoup)
{
   List<URI> rslt = new ArrayList<URI>();

   Elements results = jsoup.select("a.js-directory-link");
   if (results.size() == 0) {
      results = jsoup.select("table.files td.content a.js-navigation-open");
    }
   for (Element result : results) {
      String href = result.attr("href");
      href = href.replace("%2524","$");
      try {
	 URI u = new URI(GITHUB_SCHEME,GITHUB_AUTHORITY,href,null);
	 rslt.add(u);
       }
      catch (URISyntaxException e) { }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Result page scanning							*/
/*										*/
/********************************************************************************/

@Override protected URI getRawFileURI(URI base)
{
   String url = base.getPath();
   String blob = url.replace("/blob","");
   blob = blob.replace("%2524","$");
   try {
      URI uri = new URI(GITHUB_SCHEME,GITHUB_FILE_AUTHORITY,blob,null,null);
      return uri;
    }
   catch (URISyntaxException e) { }

   return null;
}




@Override protected boolean shouldRetry(CoseException e)
{
   if (e.getMessage().contains(": 429")) return true;
   if (e.getMessage().contains(": 403")) return true;
   return false;
}



/********************************************************************************/
/*										*/
/*	Github Source								*/
/*										*/
/********************************************************************************/

protected static class GithubSource extends KeySearchSource implements CoseSource {

   protected String base_link;
   protected String base_path;

   GithubSource(String base,String code,int idx) {
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
      int idx1 = base.indexOf("/blob/",idx);
      return base.substring(idx+1,idx1);
    }

   URI getPathURI(String path) {
      String pid = getProjectId();
      int idx = base_link.indexOf("/blob/");
      int idx1 = base_link.indexOf("/",idx+7);
      String hex = base_link.substring(idx+6,idx1);
      String qp = "/" + pid + "/tree/" + hex + path;
      try {
	 URI u = new URI(GITHUB_SCHEME,GITHUB_AUTHORITY,qp,null);
	 return u;
       }
      catch (URISyntaxException e) { }
      return null;
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

}	// end of subclass GithubSource


/********************************************************************************/
/*										*/
/*	GitHub authentication							*/
/*										*/
/********************************************************************************/

private synchronized static void getOAuthToken()
{
   if (oauth_token != null) return;

   String altauth = null;
   if (github_auth == null) {
      String token = loadGithubUserToken();
      if (token != null) {
         github_auth = "token " + token;
         oauth_token = new OAuthData(token);
         return;
       }      
      String userpass = loadGithubUserInfo();
      if (userpass != null) {
         if (github_auth == null) github_auth = "Basic " + userpass;
         else altauth = "Basic " + userpass;
       }
    }

   Object o1 = doGithubAuthenticate(null,"GET",null);
   if (o1 == null && altauth != null) {
      github_auth = altauth;
      o1 = doGithubAuthenticate(null,"GET",null);
    }
   if (o1 == null) return;
   
   JSONArray j1 = (JSONArray) o1;
   try {
      for (int i = 0; i < j1.length(); ++i) {
	 JSONObject key = j1.getJSONObject(i);
	 IvyLog.logI("COSE","RESULT [" + i + "] IS " + key);
	 OAuthData od = new OAuthData(key);
	 if (od.isValid()) {
	    oauth_token = od;
	    Runtime.getRuntime().addShutdownHook(new OAuthRemover(od));
	    return;
	  }
	 else if (od.shouldRemove()) {
	    doGithubAuthenticate("/authorizations/" + od.getId(),"DELETE",null);
	  }
       }
    }
   catch (JSONException e) { }

   Map<String,Object> q1 = new HashMap<String,Object>();
   q1.put("scopes",new String [] { "public_repo" });
   q1.put("note","s6_access");
   q1.put("note_url","http://conifer.cs.brown.edu/s6");
   q1.put("client_id",COSE_CLIENT_ID);
   q1.put("client_secret",COSE_CLIENT_SECRET);
   q1.put("fingerprint",COSE_FINGERPRINT);
   Object o2 = doGithubAuthenticate(null,"POST",q1);
   JSONObject j2 = (JSONObject) o2;
   OAuthData od = new OAuthData(j2);
   if (od.isValid()) {
      oauth_token = od;
      Runtime.getRuntime().addShutdownHook(new OAuthRemover(od));
      return;
    }
}


private static String loadGithubUserToken()
{
   try {
      File path = IvyFile.expandFile("$(HOME)/.githubtoken");
      try (BufferedReader fr = new BufferedReader(new FileReader(path))) {
         for ( ; ; ) {
            String ln = fr.readLine();
            if (ln == null) break;
            ln = ln.trim();
            if (ln.length() == 0) continue;
            if (ln.startsWith("#") || ln.startsWith("%")) continue;
            fr.close();
            return ln;
          }
       }
    }
   catch (IOException e) { }
   return null;
}


private static String loadGithubUserInfo()
{
   try {
      File path = IvyFile.expandFile("$(HOME)/.github");
      try (BufferedReader fr = new BufferedReader(new FileReader(path))) {
         for ( ; ; ) {
            String ln = fr.readLine();
            if (ln == null) break;
            ln = ln.trim();
            if (ln.length() == 0) continue;
            if (ln.startsWith("#") || ln.startsWith("%")) continue;
            fr.close();
            return Base64.getEncoder().encodeToString(ln.getBytes());
          }
       }
    }
   catch (IOException e) { }
   return null;
}



private static Object doGithubAuthenticate(String path,String type,Map<String,Object> input)
{
   String ustr = "https://api.github.com";
   if (path != null) ustr += path;
   else ustr += "/authorizations";

   String inps = null;
   if (input != null) {
      JSONObject jobj = new JSONObject(input);
      inps = jobj.toString();
    }

   try {
      URL url1 = new URL(ustr);
      HttpURLConnection hc1 = (HttpURLConnection) url1.openConnection();
      hc1.setDoInput(true);
      hc1.setRequestProperty("Authorization",github_auth);
      hc1.setRequestProperty("User-Agent","S6");
      if (type != null) hc1.setRequestMethod(type);

      if (inps != null) {
	 hc1.setDoOutput(true);
	 OutputStream ots = hc1.getOutputStream();
	 ots.write(inps.getBytes());
	 ots.close();
       }
      else {
	 hc1.setDoOutput(false);
       }

      InputStream ins = hc1.getInputStream();
      
      try (BufferedReader r = new BufferedReader(new InputStreamReader(ins))) {
         StringBuffer buf = new StringBuffer();
         for ( ; ; ) {
            String ln = r.readLine();
            if (ln == null) break;
            buf.append(ln);
            buf.append("\n");
          }
         hc1.disconnect();
         
         String cnts = buf.toString().trim();
         if (cnts.startsWith("[")) return new JSONArray(cnts);
         else if (cnts.startsWith("{")) return new JSONObject(cnts);
         else if (cnts.equals("")) ;
         else {
            IvyLog.logE("COSE","Bad json contents: " + cnts);
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("COSE","I/O Error accessing github: " + e);
    }
   catch (JSONException e) {
      IvyLog.logE("COSE","JSON Error accessing github: " + e);
    }

   return null;
}








@SuppressWarnings("unused")
private static class OAuthData {

   private String oauth_id;
   private boolean is_s6token;
   private String token_id;
   private String hash_token;
   private String token_last;
   private String token_url;
   private String app_name;

   OAuthData(JSONObject obj) {
      oauth_id = obj.optString("id");
      token_id = obj.optString("token");
      hash_token = obj.optString("hashed_token");
      token_last = obj.optString("token_last_eight");
      token_url = obj.optString("url");
      is_s6token = false;
      JSONObject app = obj.optJSONObject("app");
      if (app == null) return;
      app_name = app.optString("name");
      if (app_name == null) return;
      if (!app_name.equals("S6") && !app_name.equals("Cose") &&
            !app_name.equals("Haccur")) return;
      if (token_id != null && token_id.equals("")) token_id = null;
      is_s6token = true;
    }
   
   OAuthData(String tok) {
      token_id = tok;
      is_s6token = false;
      oauth_id = null;
    }

   String getToken()			{ return token_id; }
   boolean isValid()			{ return is_s6token && token_id != null; }
   String getId()			{ return oauth_id; }
   boolean shouldRemove() {
      return is_s6token && token_id == null && oauth_id != null;
    }
   String getAuthId()			{ return oauth_id; }

   private void saveToken() {
      if (token_id != null && is_s6token) {
	 try {
	    FileWriter fw = new FileWriter(TOKEN_FILE,true);
	    fw.write(hash_token);
	    fw.write(",");
	    fw.write(token_id);
	    fw.write("\n");
	    fw.close();
	  }
	 catch (IOException e) {
	    IvyLog.logE("COSE","GITHUB: PROBLEM SAVING TOKEN: " + e);
	  }
       }
    }

   private void loadToken() {
      if (token_id == null && hash_token != null) {
         try (BufferedReader fr = new BufferedReader(new FileReader(TOKEN_FILE))) {
            for ( ; ; ) {
               String ln = fr.readLine();
               if (ln == null) break;
               int idx = ln.indexOf(",");
               if (idx < 0) continue;
               String key = ln.substring(0,idx);
               if (key.equals(hash_token)) {
                  token_id = ln.substring(idx+1);
                  break;
                }
             }
          }
         catch (IOException e) {
            IvyLog.logE("COSE","Problem reading github token: " + e);
          }
       }
    }

}	// end of inner class OAuthData


private static class OAuthRemover extends Thread
{
   private OAuthData auth_data;

   OAuthRemover(OAuthData ad) {
      auth_data = ad;
    }

   @Override public void run() {
      String auth = COSE_CLIENT_ID + ":" +COSE_CLIENT_SECRET;
      auth = Base64.getEncoder().encodeToString(auth.getBytes());
      github_auth = "Basic " + auth;
      Map<String,Object> delinp = new HashMap<>();
      delinp.put("access_token",auth_data.getToken());
      doGithubAuthenticate("/applications/" + COSE_CLIENT_ID + "/token",
        	   "DELETE",delinp);
    }

}	// end of inner class OAuthRemover




}	// end of class KeySearchRepoGithub




/* end of KeySearchRepoGithub.java */

