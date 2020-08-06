/********************************************************************************/
/*                                                                              */
/*              CoseDefaultRequest.java                                         */
/*                                                                              */
/*      Basic request that can be extended or used directly                     */
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



package edu.brown.cs.cose.cosecommon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoseDefaultRequest implements CoseRequest, CoseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean do_debug;
private int     num_results;
private List<CoseDefaultKeywordSet> keyword_sets;
private Set<String> key_terms;
private int     num_threads;
private int     max_files;
private CoseSearchType search_type;
private CoseSearchLanguage search_language;
private CoseSearchEngine search_engine;
private CoseScopeType scope_type;
private CoseSignature search_signature;
private Set<String> specific_sources;
private CoseResultEditor result_editor;
private String  project_id;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CoseDefaultRequest()
{
   do_debug = false;
   num_results = 200;
   max_files = 100;
   keyword_sets = new ArrayList<>();
   key_terms = new HashSet<>();
   num_threads = 1;
   search_type = CoseSearchType.METHOD;
   scope_type = CoseScopeType.FILE;
   search_language = CoseSearchLanguage.JAVA;
   search_engine = CoseSearchEngine.SEARCHCODE;
   search_signature = null;
   specific_sources = null;
   result_editor = CoseMaster.createJavaEditor();
   project_id = null;
}



/********************************************************************************/
/*                                                                              */
/*      Basic access methods                                                    */
/*                                                                              */
/********************************************************************************/

@Override public boolean doDebug()                      { return do_debug; }
public void setDoDebug(boolean fg)                      { do_debug = fg; }

@Override public boolean useAndroid()                   { return false; }

@Override public int getNumberOfResults()               { return num_results; }
public void setNumberOfResults(int nr)                  { num_results = nr; }

@Override public int getNumberOfThreads()               { return num_threads; }
public void setNumberOfThreads(int nt)                  { num_threads = nt; }

@Override public int getMaxPackageFiles()               { return max_files; }
public void setMaxPackageFiles(int nf)                  { max_files = nf; }

@Override public CoseSearchType getCoseSearchType()     { return search_type; }
public void setCoseSearchType(CoseSearchType st)        { search_type = st; }

@Override public CoseSearchLanguage getLanguage()       { return search_language; }
public void setSearchLanguage(CoseSearchLanguage sl)    { search_language = sl; }

@Override public Set<String> getSpecificSources()       { return specific_sources; }
public void addSpecificSource(Collection<String> srcs) 
{
   if (specific_sources == null) specific_sources = new HashSet<>();
   specific_sources.addAll(srcs);
}
public void addSpecificSource(String src) 
{
   if (specific_sources == null) specific_sources = new HashSet<>();
   specific_sources.add(src);
}

@Override public Set<CoseSearchEngine> getEngines()
{
   return Collections.singleton(search_engine);
}
public void setSearchEngine(CoseSearchEngine se)        { search_engine = se; }

@Override public CoseScopeType getCoseScopeType()       { return scope_type; }
public void setCoseScopeType(CoseScopeType st)          { scope_type = st; }

@Override public CoseSignature getCoseSignature()       { return search_signature; }
public void setCoseSignature(CoseSignature cs)          { search_signature = cs; }

@Override public String getProjectId()                  { return project_id; }
public void setProjectId(String s)                      { project_id = s; }

@Override public List<String> getKeyTerms()            
 { 
   return new ArrayList<>(key_terms);
}

@Override public String editSource(String orig)
{
   if (result_editor == null || search_language != CoseSearchLanguage.JAVA) return orig;
   return result_editor.editFileResult(orig);
}


public CoseResultEditor getResultEditor()     { return result_editor; }
public void setResultEditor(CoseResultEditor re)        { result_editor = re; }



/********************************************************************************/
/*                                                                              */
/*      Keyword management                                                      */
/*                                                                              */
/********************************************************************************/

@Override public List<CoseKeywordSet> getCoseKeywordSets()
{
   if (keyword_sets.size() > 1) {
      CoseKeywordSet lset = keyword_sets.get(keyword_sets.size() - 1);
      if (lset.getWords().isEmpty()) {
         keyword_sets.remove(lset);
       }
    }
   return new ArrayList<>(keyword_sets);
}


public void addKeywordSet(String ... keys) 
{
   if (keyword_sets.size() > 1) {
      CoseKeywordSet lset = keyword_sets.get(keyword_sets.size() - 1);
      if (lset.getWords().isEmpty()) {
         keyword_sets.remove(lset);
       }
    }
   keyword_sets.add(new CoseDefaultKeywordSet(keys));
   for (String s : keys) key_terms.add(s);
}

public void addKeywordSet(List<String> keys) 
{
   keyword_sets.add(new CoseDefaultKeywordSet(keys));
   key_terms.addAll(keys);
}

public void addKeyword(String k)
{
   if (keyword_sets.isEmpty()) {
      keyword_sets.add(new CoseDefaultKeywordSet());
    }
   keyword_sets.get(keyword_sets.size()-1).addWord(k);
   key_terms.add(k);
}

public void addKeyTerm(String k)
{
   key_terms.add(k);
}




/********************************************************************************/
/*                                                                              */
/*      Keyword set                                                             */
/*                                                                              */
/********************************************************************************/

private static class CoseDefaultKeywordSet implements CoseKeywordSet {

   private List<String> key_words;

   CoseDefaultKeywordSet(String... wds) {
      key_words = new ArrayList<>(Arrays.asList(wds));
    }
   
   CoseDefaultKeywordSet(List<String> wds) {
      key_words = new ArrayList<>(wds);
    }
   
   void addWord(String wd) {
      key_words.add(wd);
    }
   
   @Override public List<String> getWords() {
      return key_words;
    }
   
}       // end of inner class ScrapKeywordSet


}       // end of class CoseDefaultRequest




/* end of CoseDefaultRequest.java */

