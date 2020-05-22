/********************************************************************************/
/*                                                                              */
/*              KeySearchTester.java                                            */
/*                                                                              */
/*      Junit tests for key search                                              */
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseResultSet;
import edu.brown.cs.cose.cosecommon.CoseSignature;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseRequest.CoseKeywordSet;

public class KeySearchTester implements KeySearchConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public KeySearchTester()
{
   
}


/********************************************************************************/
/*                                                                              */
/*      Simple test case                                                        */
/*                                                                              */
/********************************************************************************/

@Test
public void searchTest1()
{
    SimpleTestRequest tr1 = new SimpleTestRequest("roman","numeral");
    KeySearchMaster ksm = new KeySearchMaster(tr1);
    ksm.computeSearchResults(tr1);
    List<CoseResult> rslt = tr1.getResults();
    Assert.assertTrue(rslt.size() >= 1);
}


@Test
public void searchTest2()
{
   SimpleTestRequest tr1 = new SimpleTestRequest("roman","numeral");
   tr1.setSearchEngines(CoseSearchEngine.GITHUB,CoseSearchEngine.SEARCHCODE);
   KeySearchMaster ksm = new KeySearchMaster(tr1);
   ksm.computeSearchResults(tr1);
   List<CoseResult> rslt = tr1.getResults();
   Assert.assertTrue(rslt.size() >= 1);
}



@Test
public void searchTest3()
{
   SimpleTestRequest tr2 = new SimpleTestRequest("embedded","web","server");
   tr2.setSearchType(CoseSearchType.CLASS);
   KeySearchMaster ksm = new KeySearchMaster(tr2);
   ksm.computeSearchResults(tr2);
   List<CoseResult> rslt = tr2.getResults();
   Assert.assertTrue(rslt.size() >= 1);
}


@Test
public void searchTest4()
{
   SimpleTestRequest tr2 = new SimpleTestRequest("embedded","web","server");
   tr2.setSearchType(CoseSearchType.PACKAGE);
   tr2.setScopeType(CoseScopeType.PACKAGE);
   KeySearchMaster ksm = new KeySearchMaster(tr2);
   ksm.computeSearchResults(tr2);
   List<CoseResult> rslt = tr2.getResults();
   Assert.assertTrue(rslt.size() >= 1);
}



@Test
public void searchTest5()
{
   SimpleTestRequest tr2;
   KeySearchMaster ksm;
   List<CoseResult> rslt;
   
   tr2 = new SimpleTestRequest("contact","@management");
   tr2.setSearchType(CoseSearchType.PACKAGE);
   tr2.setScopeType(CoseScopeType.PACKAGE);
   tr2.setSearchEngines(CoseSearchEngine.GITREPO);
   ksm = new KeySearchMaster(tr2);
   ksm.computeSearchResults(tr2);
   rslt = tr2.getResults();
   Assert.assertTrue(rslt.size() >= 1);
   
   tr2 = new SimpleTestRequest("@embedded","@web","server");
   tr2.setSearchType(CoseSearchType.PACKAGE);
   tr2.setScopeType(CoseScopeType.PACKAGE);
   tr2.setSearchEngines(CoseSearchEngine.GITREPO);
   ksm = new KeySearchMaster(tr2);
   ksm.computeSearchResults(tr2);
   rslt = tr2.getResults();
   Assert.assertTrue(rslt.size() >= 1);
}



/********************************************************************************/
/*                                                                              */
/*      Dummy request structure                                                 */
/*                                                                              */
/********************************************************************************/

private class SimpleTestRequest implements CoseRequest, CoseResultSet {
   
   private List<CoseKeywordSet> key_words;
   private List<CoseResult> result_set;
   private CoseSearchType search_type;
   private CoseScopeType scope_type;
   private Set<CoseSearchEngine> search_engines;
   private CoseSearchLanguage search_language;
   
   SimpleTestRequest(String... words) {
      key_words = new ArrayList<>();
      key_words.add(new WordSet(words));
      result_set = new ArrayList<>();
      search_type = CoseSearchType.METHOD;
      scope_type = CoseScopeType.FILE;
      search_engines = EnumSet.of(CoseSearchEngine.SEARCHCODE);
      search_language = CoseSearchLanguage.JAVA;
    }
   
   List<CoseResult> getResults()                      { return result_set; }
   
   void setSearchType(CoseSearchType st)                { search_type = st; }
   void setScopeType(CoseScopeType st)                  { scope_type = st; }
   void setSearchEngines(CoseSearchEngine e0,CoseSearchEngine ... e) { 
      search_engines = EnumSet.of(e0,e);
    }
      
   @Override public int getNumberOfThreads()            { return 4; }
   @Override public int getNumberOfResults()            { return 200; }
   @Override public CoseSearchType getCoseSearchType()      { return search_type; }
   @Override public CoseScopeType getCoseScopeType()        { return scope_type; }
   @Override public List<CoseKeywordSet> getCoseKeywordSets() {
      return key_words;
    }
   @Override public CoseSearchLanguage getLanguage()    { return search_language; }
   @Override public Set<CoseSearchEngine> getEngines()  { return search_engines; }
   @Override public Set<String> getSpecificSources()    { return null; }
   @Override public boolean useAndroid()                { return false; }
   @Override public boolean doDebug()                   { return true; }
   @Override public CoseSignature getCoseSignature()    { return null; }
   @Override public List<String> getKeyTerms()          { return new ArrayList<>(); }
   @Override public String editSource(String orig)      { return orig; }
   
   @Override public synchronized void addResult(CoseResult sr) {
      result_set.add(sr);
    }
   
}       // end of inner class SimpleTestRequest  



private static class WordSet implements CoseKeywordSet {
   
   List<String> word_list; 
   
   WordSet(String... words) {
      word_list = new ArrayList<>();
      for (String w : words) word_list.add(w);
    }
   @Override public List<String> getWords()     { return word_list; }
   
}       // end of inner class WordSet








}       // end of KeySearchTester



/* end of KeySearchTester.java */

