/********************************************************************************/
/*										*/
/*		KeySearchRepoCodeExchange.java					*/
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.ivy.xml.IvyXml;



class KeySearchRepoCodeExchange extends KeySearchRepo
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final static String	CODEEX_SCHEME = "http";
private final static String	CODEEX_AUTHORITY = "codeexchange.ics.uci.edu";
private final static String	CODEEX_PATH = "/solr/CodeExchangeIndex/select/";
private final static String	CODEEX_FILEPATH = "/getPage.php";
private final static String	CODEEX_FRAGMENT = null;

private final static String	SOURCE_PREFIX = "CODEEX:";

private final static int	RESULTS_PER_PAGE = 100;
private final static int	SIMULTANEOUS_SEARCH = 0;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

KeySearchRepoCodeExchange(CoseRequest sr)
{
   super(sr,SIMULTANEOUS_SEARCH);
}




/********************************************************************************/
/*										*/
/*	Search Property Methods 						*/
/*										*/
/********************************************************************************/

@Override public int getResultsPerPage()	{ return RESULTS_PER_PAGE; }


@Override boolean hasMoreSearchPages(URI uri,String cnts,int page)
{
   return false;
}



/********************************************************************************/
/*										*/
/*	Search Metohds								*/
/*										*/
/********************************************************************************/

@Override URI getURIForSearch(List<String> keywords,CoseSearchLanguage lang,String projectid,int page)
{
   StringBuffer buf = new StringBuffer();
   buf.append("q=snippet_code:(");
   int ct = 0;
   for (String s : keywords) {
      if (ct++ > 0) buf.append(" ");            // could be " AND "
      buf.append(s);
    }
   buf.append(")");
   if (projectid != null) {
      buf.append(" AND snippet_project_name:(");
      buf.append(projectid);
      buf.append(")");
    }
   buf.append("&rows=");
   buf.append(RESULTS_PER_PAGE);
   buf.append("&fl=id snippet_project_address snippet_project_name snippet_address");

   try {
      return new URI(CODEEX_SCHEME,CODEEX_AUTHORITY,CODEEX_PATH,buf.toString(),CODEEX_FRAGMENT);
    }
   catch (URISyntaxException e) {
      System.err.println("S6: Problem with codeexchange URI: " + e);
    }

   return null;
}



@Override URI getURIFromSourceString(String src)
{
   if (!src.startsWith(SOURCE_PREFIX)) return null;

   try {
      return new URI(src.substring(SOURCE_PREFIX.length()));
    }
   catch (URISyntaxException e) { }

   return null; // method body goes here
}



@Override URI getURIForPath(CoseSource src,String path)
{
   // method body goes here

   return null;
}



/********************************************************************************/
/*										*/
/*	Search Processing							*/
/*										*/
/********************************************************************************/

@Override List<URI> getSearchPageResults(URI uri,String cnts)
{
   ArrayList<URI> rslt = new ArrayList<URI>();

   Element root = IvyXml.convertStringToXml(cnts);
   Element relt = IvyXml.getChild(root,"result");
   for (Element delt : IvyXml.children(relt,"doc")) {
      Map<String,String> vals = new HashMap<String,String>();
      for (Element selt : IvyXml.children(delt,"str")) {
	 String id = IvyXml.getAttrString(selt,"name");
	 String val = IvyXml.getText(selt);
	 vals.put(id,val);
       }
      try {
	 String q = "callback=s6&url=" + vals.get("snippet_address");
	 q += "&pid=" + vals.get("project_name");
	 q += "&paddr=" + vals.get("project_address");
	 URI u = new URI(CODEEX_SCHEME,CODEEX_AUTHORITY,CODEEX_FILEPATH,q,CODEEX_FRAGMENT);
	 rslt.add(u);
       }
      catch (URISyntaxException e) {
	 System.err.println("S6: Problem creating codeex file uri: " + e);
       }
    }

   return rslt;
}


@Override List<URI> getDirectoryContentsURIs(URI baseuri,CoseSource src,String cnts)
{
   return null;
}


@Override protected String getRawSourcePage(URI uri)
{
   String cnts = getResultPage(uri);
   if (cnts.startsWith("s6(")) {
      int idx = cnts.lastIndexOf(")");
      cnts = cnts.substring(3,idx);
    }
   if (cnts.trim().startsWith("Warning:")) {
      System.err.println("S6: Problem getting page: " + uri + ": " + cnts.trim());
      return null;
    }

   try {
      JSONObject top = new JSONObject(cnts);
      String rslt = top.getString("code");
      if (!rslt.endsWith("\n"))
	 rslt += "\n";
      return rslt;
    }
   catch (JSONException e) {
      System.err.println("S6: Problem getting codeexchange source: " + e);
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Source management							*/
/*										*/
/********************************************************************************/

@Override CoseSource createSource(URI uri,String cnts,int idx)
{
   return new CodeExSource(uri.toString(),cnts,idx);
}



private static class CodeExSource extends KeySearchSource implements CoseSource {

   private String base_link;

   CodeExSource(String base,String code,int idx) {
      super(code,idx);
      base_link = base;
    }

   @Override public String getName()		{ return SOURCE_PREFIX + base_link; }
   @Override public String getDisplayName()	{ return base_link; }

   @Override public String getProjectId() {
      return getArg("pid");
    }

   @Override public String getPathName() {
      String paddr = getArg("paddr");
      String url = getArg("url");
      return url.substring(paddr.length());
    }

   private String getArg(String id) {
      int idx = base_link.indexOf(id + "=");
      int idx1 = base_link.indexOf("&",idx);
      idx += + id.length() + 1;
      if (idx1 < 0) return base_link.substring(idx);
      return base_link.substring(idx,idx1);
    }

}	// end of subclass HunterSource




}	// end of class KeySearchRepoCodeExchange




/* end of KeySearchRepoCodeExchange.java */

