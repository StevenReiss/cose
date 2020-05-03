/********************************************************************************/
/*                                                                              */
/*              KeySearchRepoGitRepo.java                                       */
/*                                                                              */
/*      Seraching via GITHUB by doing a repository search first                 */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
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
import edu.brown.cs.ivy.file.IvyLog;

class KeySearchRepoGitRepo extends KeySearchRepoGithub
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<String> use_repos;

private final static String	SOURCE_PREFIX = "GITREPO:";




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

KeySearchRepoGitRepo(CoseRequest sr)
{
   super(sr);
   
   use_repos = new ArrayList<>();
}


/********************************************************************************/
/*                                                                              */
/*      Abstract methods                                                        */
/*                                                                              */
/********************************************************************************/

protected boolean isRelevantSource(String src)
{
   return src.startsWith(SOURCE_PREFIX);
}



@Override CoseSource createSource(URI uri,String cnts,int idx)
{
   return new GitRepoSource(uri.toString(),cnts,idx);
}


@Override int getResultsPerPage()
{
   return 1;
}


@Override boolean hasMoreSearchPages(URI u,String cnts,int page)
{
   if (page+1 < use_repos.size()) return true;
   return false;
}


/********************************************************************************/
/*                                                                              */
/*      Search Strategy                                                         */
/*                                                                              */
/********************************************************************************/

@Override protected URI getURIForSearch(List<String> keys,CoseSearchLanguage lang,String projectid,int page)
{
   if (projectid == null) return null;
   return super.getURIForSearch(keys,lang,projectid,page);
}


   
@Override String getResultPage(List<String> keywords,CoseSearchLanguage lang,
      String projectid,int page)
{
   if (page == 0) {
     getRepoSet(keywords,lang);
    }
   String repo = null;
   synchronized (use_repos) {
      if (use_repos.size() <= page) return null;
      repo = use_repos.get(page);
    }
   
   List<String> skeys = new ArrayList<>();
   for (String s : keywords) {
      if (s.startsWith("^")) skeys.add(s.substring(1));
      else if (s.startsWith("@")) ;
      else skeys.add(normalizeKeyword(s)); 
    }
   
   URI u1 = super.getURIForSearch(skeys,lang,repo,0);
   String txt = getResultPage(u1);   

   return txt;
}



private void getRepoSet(List<String> keywords,CoseSearchLanguage lang)
{
   List<String> rkeys = new ArrayList<>();
   for (String s : keywords) {
      if (s.startsWith("@")) rkeys.add(s.substring(1));
      else if (s.startsWith("^")) ;
      else rkeys.add(normalizeKeyword(s)); 
    }
   
   URI uri = getRepoSearchURI(rkeys,lang);
   String cnts = getResultPage(uri);
   if (cnts == null) return;
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
         String s = jobj.getString("full_name");
         synchronized (use_repos) {
            if (!use_repos.contains(s)) use_repos.add(s);
          }
       }
    }
   catch (JSONException e) {
      IvyLog.logE("COSE","Problem parsing github json return",e);
    }
}
      



private URI getRepoSearchURI(List<String> keys,CoseSearchLanguage lang)
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
      if (i++ > 0) q += " ";
      if (s.contains(" ")) q += "\"" + s + "\"";
      else q += s;
    }
   try {
      if (lang != null) q += " language:" + langstr;
      q += "&per_page=100";
      URI uri = new URI(GITHUB_SCHEME,"api.github.com","/search/repositories",q,null);
      return uri;
    }
   catch (URISyntaxException e) { }
   
   return null;
}



/********************************************************************************/
/*										*/
/*	GitRepo Source								*/
/*										*/
/********************************************************************************/

private static class GitRepoSource extends GithubSource implements CoseSource {

   GitRepoSource(String base,String code,int idx) {
      super(base,code,idx);
    }
   
   @Override public String getName()		{ return SOURCE_PREFIX + base_link; }
   
}	// end of subclass GitRepoSource


}       // end of class KeySearchRepoGitRepo




/* end of KeySearchRepoGitRepo.java */

