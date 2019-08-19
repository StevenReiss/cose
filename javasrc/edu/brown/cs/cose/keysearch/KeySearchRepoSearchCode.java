/********************************************************************************/
/*										*/
/*		KeySearchRepoSearchCode.java					*/
/*										*/
/*	description of class							*/
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseSource;


class KeySearchRepoSearchCode extends KeySearchRepo
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected final static String	SCODE_SCHEME = "https";
protected final static String	SCODE_AUTHORITY = "searchcode.com";
private final static String	SCODE_PATH = "/api/codesearch_I/";
protected final static String	SCODE_FRAGMENT = null;
private final static String	SCODE_QUERY_TAIL = "&per_page=100";
private final static String	SCODE_FILE_AUTHORITY = "searchcode.com";

private final static String	SOURCE_PREFIX = "SRCCOD:";

private final static int	RESULTS_PER_PAGE = 100;
private final static int	SIMULTANEOUS_SEARCH = 10;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

KeySearchRepoSearchCode(CoseRequest sr)
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
   if (!src.startsWith(SOURCE_PREFIX)) return null;

   try {
      int idx = src.indexOf(":");
      return new URI(src.substring(idx+1));
    }
   catch (URISyntaxException _ex) { }

   return null;
}


@Override int getResultsPerPage()
{
   return RESULTS_PER_PAGE;
}



@Override URI getURIForSearch(List<String> keys,CoseSearchLanguage lang,String projectid,int page)
{
   String q = "";
   String langstr = null;
   switch (lang) {
      case JAVA :
	 langstr = "23";
	 break;
      case JAVASCRIPT :
         langstr = "22";
         break;
      case XML :
	 break;
    }

   q += "q=";
   int i = 0;
   for (String s : keys) {
      if (i++ > 0) q += " ";
      if (s.contains(" ")) q += "\"" + s + "\"";
      else q += s;
    }
   if (projectid != null) q += " repo: " + projectid;

   try {
      URI uri = null;
      if (page > 0) q += "&p=" + page;
      if (langstr != null) q += "&lan=" + langstr;
      q += SCODE_QUERY_TAIL;
      uri = new URI(SCODE_SCHEME,SCODE_AUTHORITY,SCODE_PATH,q,null);
      return uri;
    }
   catch (URISyntaxException e) { }

   return null;
}



@Override CoseSource createSource(URI uri,String cnts,int idx)
{
   return new SearchCodeSource(uri,cnts,idx);
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
      JSONObject srslt = new JSONObject(cnts);
      // System.err.println("TOP RESULT: " + srslt);
      JSONArray jarr = srslt.getJSONArray("results");
      for (int i = 0; i < jarr.length(); ++i) {
	 JSONObject jobj = jarr.getJSONObject(i);
	 // System.err.println("RESULT: " + jobj);
	 URI uri2 = null;
	 try {
	    String urls = jobj.getString("url");
	    URI url1 = new URI(urls);
	    String path = url1.getPath();
	    path = path.replace("/view/","/raw/");
	    int id = jobj.getInt("id");
	    String nm = jobj.getString("name");
	    String fnm = jobj.getString("filename");
	    String loc = jobj.getString("location");
	    String q = id + "&" + nm + "&" + fnm + "&" + loc;
	    uri2 = new URI(SCODE_SCHEME,SCODE_FILE_AUTHORITY,path,q,null);
	    rslt.add(uri2);
	  }
	 catch (URISyntaxException e) {
	    System.err.println("BAD URI: " + e);
	  }
       }
    }
   catch (JSONException e) {
      System.err.println("Cose: Problem parsing github json return: " + e);
    }

   return rslt;
}



@Override protected URI getRawFileURI(URI base)
{
   String path = base.getPath();
   try {
      URI uri = new URI(SCODE_SCHEME,SCODE_FILE_AUTHORITY,path,null,null);
      return uri;
    }
   catch (URISyntaxException _ex) {}

   return null;
}



@Override boolean hasMoreSearchPages(URI uri,String cnts,int page)
{
   if (cnts == null) return false;

   try {
      JSONObject srslt = new JSONObject(cnts);
      if (srslt.get("nextpage") != null) return true;
    }
   catch (JSONException e) {
      System.err.println("Cose: Problem parsing github json return: " + e);
    }

   return false;
}



@Override URI getURIForPath(CoseSource arg0,String arg1)
{
   return null;
}




/********************************************************************************/
/*										*/
/*	SearchCode Source							*/
/*										*/
/********************************************************************************/

private static class SearchCodeSource extends KeySearchSource implements CoseSource {

   private String base_link;
   private String base_path;
   private String file_name;
   private String project_id;

   SearchCodeSource(URI uri,String code,int idx) {
      super(code,idx);
      base_link = uri.toString();
      String q = uri.getQuery();
      String [] cmps = q.split("&");
      project_id = cmps[1];
      file_name = cmps[2];
      if (cmps.length >= 4) base_path = cmps[3];
      else base_path = "";
    }

   @Override public String getName()		{ return SOURCE_PREFIX + base_link; }
   @Override public String getDisplayName()	{ return file_name; }

   @Override public String getProjectId()	{ return project_id; }

   @Override public String getPathName()	{ return base_path + "/" + file_name; }

}	// end of subclass GithubSource








}	// end of class KeySearchRepoSearchCode




/* end of KeySearchRepoSearchCode.java */

