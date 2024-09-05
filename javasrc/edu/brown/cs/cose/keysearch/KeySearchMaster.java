/********************************************************************************/
/*										*/
/*		KeySearchMaster.java						*/
/*										*/
/*	General implementation of keyword search of repositories		*/
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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseResultSet;
import edu.brown.cs.cose.cosecommon.CoseDefaultResultSet;
import edu.brown.cs.cose.cosecommon.CoseMaster;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResource;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.cose.result.ResultFactory;
import edu.brown.cs.cose.scorer.ScorerAnalyzer;
import edu.brown.cs.ivy.file.ConcurrentHashSet;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.file.IvyLog.LoggerThread;
import edu.brown.cs.ivy.xml.IvyXml;



public class KeySearchMaster implements KeySearchConstants, CoseMaster
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<KeySearchRepo>	search_repos;
private ThreadPool		thread_pool;
private CoseRequest		cose_request;
private CoseResultSet		result_set;
private Map<CoseSource,Map<String,Set<String>>> package_items;
private Set<String>             repo_pkg_done;
private Set<CoseSource> 	used_sources;
private ResultFactory           result_factory;
private PrintWriter             score_data_file;
private FileChannel             score_channel;
private Set<CoseResult>         do_recheck;
private Map<CoseResult,Set<LoadPackageResult>> recheck_items;
private AtomicInteger           base_result_count;
private AtomicInteger           package_result_count;
private AtomicInteger           package_output_count;
private AtomicInteger           package_test_count;

private static final Set<String> RESOURCE_EXTENSIONS;



static {
   RESOURCE_EXTENSIONS = new HashSet<String>();
   RESOURCE_EXTENSIONS.add(".png");
   RESOURCE_EXTENSIONS.add(".jpg");
   RESOURCE_EXTENSIONS.add(".jpeg");
   RESOURCE_EXTENSIONS.add(".gif");
   RESOURCE_EXTENSIONS.add(".xml");
   RESOURCE_EXTENSIONS.add(".mp3");
   RESOURCE_EXTENSIONS.add(".wav");
   RESOURCE_EXTENSIONS.add(".ogg");
   RESOURCE_EXTENSIONS.add(".bmp");
   RESOURCE_EXTENSIONS.add(".db");
   RESOURCE_EXTENSIONS.add(".lnk");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public KeySearchMaster(CoseRequest req)
{
   cose_request = req;
   result_set = null;
   search_repos = new ArrayList<KeySearchRepo>();
   recheck_items = new HashMap<>();
   do_recheck = new ConcurrentHashSet<>();
   base_result_count = new AtomicInteger();
   package_result_count = new AtomicInteger();
   package_output_count = new AtomicInteger();
   package_test_count = new AtomicInteger();
   
   for (CoseSearchEngine seng : req.getEngines()) {
      KeySearchRepo next = null;
      switch (seng) {
	 case GITHUB :
	    next = new KeySearchRepoGithub(req);
	    break;
         case GITZIP :
	    next = new KeySearchRepoGitZip(req);
	    break;
         case GITREPO :
            next = new KeySearchRepoGitRepo(req);
            break;
         case CODEEX :
	    next = new KeySearchRepoCodeExchange(req);
	    break;
	 case SEARCHCODE :
	    next = new KeySearchRepoSearchCode(req);
	    break;
       }
      if (next != null) search_repos.add(next);
    }
   if (search_repos.size() == 0) {
      search_repos.add(new KeySearchRepoSearchCode(req));
    }
   thread_pool = new ThreadPool(req.getNumberOfThreads());
   package_items = new HashMap<CoseSource,Map<String,Set<String>>>();
   repo_pkg_done = new HashSet<>();
   used_sources = new HashSet<>();
   result_factory = new ResultFactory(req);
   
   score_data_file = null;
   score_channel = null;
   try {
      File f = new File(SCORE_DATA_FILE);
      if (f.exists()) {
         FileOutputStream fos = new FileOutputStream(SCORE_DATA_FILE,true);
         score_channel = fos.getChannel();
         score_data_file = new PrintWriter(new OutputStreamWriter(fos));
       }
    }
   catch (IOException e) {
      score_data_file = null;
      score_channel = null;
    }
}



/********************************************************************************/
/*										*/
/*	Get solutions for a search request					*/
/*										*/
/********************************************************************************/

@Override public CoseResultSet computeSearchResults(CoseResultSet crs)
{
   if (crs == null) crs = new CoseDefaultResultSet();
   result_set = crs;
   
   long start = System.currentTimeMillis();
   base_result_count.set(0);
   package_result_count.set(0);
   package_output_count.set(0);

   Iterable<String> spsrc = cose_request.getSpecificSources();
   if (spsrc != null) {
      for (String src : spsrc) {
	 for (KeySearchRepo repo : search_repos) {
	    if (getSpecificSolutionFromRepo(repo,src)) break;
	  }
       }
    }
   else {
      int ct = search_repos.size();
      int lclrslts = (cose_request.getNumberOfResults() + ct - 1)/ct;
      int sz = cose_request.getCoseKeywordSets().size();
      if (sz > 1) lclrslts = (lclrslts + sz -1)/sz;

      for (KeySearchRepo repo : search_repos) {
	 getSolutionsFromRepo(repo,lclrslts);
       }
    }

   thread_pool.waitForAll();
   
   if (cose_request.getCoseSearchType() == CoseSearchType.PACKAGE) {
      IvyLog.logS("COSE","Search Time: " + (System.currentTimeMillis() - start));
      IvyLog.logS("COSE","Base Results: " + base_result_count.get());
      IvyLog.logS("COSE","Package Count: " + package_result_count.get());
      IvyLog.logS("COSE","Final Package: " + package_output_count.get());
      IvyLog.logS("COSE","Tests Removed: " + package_test_count.get());
    }
         
   return result_set;
}


@Override public CoseResult createResult(Element xml)
{
   return result_factory.createResult(xml);
}


private void getSolutionsFromRepo(KeySearchRepo repo,int ct)
{
   int rpp = repo.getResultsPerPage();
   int npages = (ct + rpp -1)/rpp;
   ScanSearchResults ssr = new ScanSearchResults(repo,0,npages);
   addTask(ssr);
}


private boolean getSpecificSolutionFromRepo(KeySearchRepo repo,String src)
{
   URI uri = repo.getURIFromSourceString(src);
   if (uri != null) {
      buildResultsForURI(repo,uri,0);
      return true;
    }

   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Handle result analysis                                                  */
/*                                                                              */
/********************************************************************************/

private void analyzeResult(CoseResult cr)
{
   ScorerAnalyzer scorer = ScorerAnalyzer.createAnalyzer(cose_request);
   if (scorer == null) return;
   
   Map<String,Object> props = scorer.analyzeProperties(cr);
   
   if (score_data_file != null) {
      synchronized (score_data_file) {
         try {
            FileLock lock = score_channel.lock(); 
            try {
               score_data_file.println("-----------------------------------" + new Date());
               score_data_file.println("SOURCE: " + cr.getSource().getName());
               for (Map.Entry<String,Object> ent : props.entrySet()) {
                  if (ent.getValue() != null) {
                     score_data_file.println(ent.getKey() + ": " + ent.getValue().toString());
                   }
                }
               score_data_file.flush();
             }
            finally {
               lock.release();
             }
          }
         catch (IOException e) { }
       }
    }
}


/********************************************************************************/
/*										*/
/*	Class to get and scan a results page					*/
/*										*/
/********************************************************************************/

private class ScanSearchResults implements Runnable {

   private KeySearchRepo for_repo;
   private int page_number;
   private int max_page;

   ScanSearchResults(KeySearchRepo repo,int page,int maxpage) {
      for_repo = repo;
      page_number = page;
      max_page = maxpage;
    }

   @Override public void run() {
      int idx = for_repo.getResultsPerPage() * page_number;
      
      boolean cont = true;
      for (CoseRequest.CoseKeywordSet kws : cose_request.getCoseKeywordSets()) {
         URI uri = for_repo.getURIForSearch(kws.getWords(),cose_request.getLanguage(),cose_request.getProjectId(),page_number,true);
         String txt = null;
         if (uri == null)
            txt = for_repo.getResultPage(kws.getWords(),cose_request.getLanguage(),cose_request.getProjectId(),page_number);
         else
            txt = for_repo.getResultPage(uri);
         if (txt == null) continue;
   
         if (page_number < max_page && for_repo.hasMoreSearchPages(uri,txt,page_number) && cont) {
            ScanSearchResults ssr1 = new ScanSearchResults(for_repo,page_number+1,max_page);
            addTask(ssr1);
            cont = false;
          }
         List<URI> uris = for_repo.getSearchPageResults(uri,txt);
         for (URI u : uris) {
            buildResultsForURI(for_repo,u,idx);
            ++idx;
          }
       }
    }

}	// end of inner class ScanSearchResults




/********************************************************************************/
/*										*/
/*	Result builder task							*/
/*										*/
/********************************************************************************/

private void buildResultsForURI(KeySearchRepo repo,URI uri,int idx)
{
   ResultBuilder fb = new ResultBuilder(repo,uri,idx);
   addTask(fb);
}



private class ResultBuilder implements Runnable {

   private KeySearchRepo for_repo;
   private URI initial_uri;
   private int result_index;

   ResultBuilder(KeySearchRepo repo,URI uri,int idx) {
      for_repo = repo;
      initial_uri = uri;
      result_index = idx;
    }

   @Override public void run() {
      String txt = for_repo.getSourcePage(initial_uri);
      if (txt == null || txt.trim().length() == 0) return;
      CoseSource src = for_repo.createSource(initial_uri,txt,result_index);
      if (src == null) return;
      
      IvyLog.logI("COSE","CREATE RESULT FOR " + initial_uri);
      base_result_count.incrementAndGet();
      
      CoseResult pfrag = null;
      switch (cose_request.getCoseSearchType()) {
         case METHOD :
         case ANDROIDUI :
         case CLASS :
         case FILE :
            break;
         case PACKAGE :
            ScorerAnalyzer sa = ScorerAnalyzer.createAnalyzer(cose_request);
            if (sa.isTestCase(txt)) {
               IvyLog.logI("COSE","REMOVE TEST RESULT " + initial_uri);
               package_test_count.incrementAndGet();
               return;
             }
            break;
         case TESTCLASS :
            break;
       }
      
      switch (cose_request.getCoseSearchType()) {
         case METHOD :
         case CLASS :
         case FILE :
         case TESTCLASS :
            addSolutions(txt,src);
            break;
         case PACKAGE :
            pfrag = result_factory.createPackageResult(src);
            package_result_count.incrementAndGet();
            addPackageSolutions(for_repo,pfrag,src,txt);
            break;
         case ANDROIDUI :
            addInitialAndroidSolutions(for_repo,src,txt);
            break;
       }
    }


}	// end of inner class ResultBuilder




/********************************************************************************/
/*										*/
/*	Add solutions for a given result file (non-package)			*/
/*										*/
/********************************************************************************/

private void addSolutions(String code,CoseSource source)
{
   if (source != null && !useSource(source)) return;
   if (source != null) used_sources.add(source);

   CoseResult cr = result_factory.createFileResult(source,code);
   if (cr == null) return;
   if (cose_request.getCoseSearchType() == CoseSearchType.FILE) {
      result_set.addResult(cr);
      return;
    }
   
   for (CoseResult subfrag : cr.getResults(cose_request.getCoseSearchType())) {
      analyzeResult(subfrag);
      result_set.addResult(subfrag);
    }
}



private boolean useSource(CoseSource source)
{
   Set<String> srcs = cose_request.getSpecificSources();
   if (srcs != null) return srcs.contains(source.getName());
   return true;
}




/********************************************************************************/
/*										*/
/*	Package result methods						*/
/*										*/
/********************************************************************************/

void addPackageSolutions(KeySearchRepo repo,CoseResult pfrag,CoseSource source,String code)
{
   if (code == null) return;

   if (cose_request.getCoseScopeType() == CoseScopeType.FILE) {
      addPackageSolution(code,source,source,pfrag);
      return;
    }

   Object fnd = pfrag.getFindStructure(code);
   String pkg = pfrag.findPackageName(code,fnd);
   if (pkg == null || pkg.equals("")) return;
   
   String pid = source.getProjectId();
   String id = pid + "@" + pkg;
   if (!repo_pkg_done.add(id)) {
      IvyLog.logI("COSE","REMOVE DUPLICATE PACKAGE SOLUTION");
      return;
    }

   addPackageSolution(code,source,source,pfrag);

   KeySearchQueue subwaits = new KeySearchQueue();

   // Get the package name
   // Ensure package is unique
   // if (!checkPackageSolution(code,source,source)) return; 

   // now expand to include the rest of the package
   ScanPackageSearchResults spsr = new ScanPackageSearchResults(repo,pkg,source,pfrag,subwaits);
   addSubtask(spsr,subwaits);

   FinishPackageTask fpt = new FinishPackageTask(repo,pfrag,source,subwaits);
   addTask(fpt);
}




private boolean addPackageSolution(String code,CoseSource source,CoseSource lclsrc,CoseResult pfrag)
{
   if (isTooComplex(pfrag,cose_request)) return false;
   if (!pfrag.getSource().isRelatedRepository(lclsrc,false)) return false;
   if (!checkPackageSolution(pfrag,code,source,lclsrc)) return false;
   CoseResult rslt = result_factory.createFileResult(source,code);
   if (rslt == null) return false;

   pfrag.addInnerResult(rslt);
   
   return true;
}


private boolean checkPackageSolution(CoseResult pfrag,String code,CoseSource source,CoseSource lclsrc)
{
   Object str = pfrag.getFindStructure(code);
   String pkg = pfrag.findPackageName(code,str);
   String cnm = pfrag.findClassName(code,str);
   if (cnm != null && cnm.length() == 0) cnm = null;
  
   synchronized (package_items) {
      Map<String,Set<String>> sitms = package_items.get(source);
      if (sitms == null) {
	 sitms = new HashMap<>();
	 package_items.put(source,sitms);
       }
      Set<String> items = sitms.get(pkg);
      if (items == null) {
         items = new HashSet<>();
         sitms.put(pkg,items);
       }
      synchronized (items) {
         if (!items.add(lclsrc.getName())) return false;
         if (cnm != null && !items.add(cnm)) return false;
       }
    }
   
   return true;
}




/********************************************************************************/
/*										*/
/*	Handle android ui search						*/
/*										*/
/********************************************************************************/

private void addInitialAndroidSolutions(KeySearchRepo repo,CoseSource src,String code)
{
   IvyLog.logI("COSE","MANIFEST START: " + src.getDisplayName());
   findAndroidManifest(repo,src);
}


private void addAndroidSolutions(KeySearchRepo repo,CoseResult pfrag,CoseSource source,String code)
{
   if (source == null || source.getDisplayName() == null) {
      IvyLog.logE("COSE","BAD SOURCE " + source);
      return;
    }

   if (source.getDisplayName().endsWith(".xml")) {
      if (!source.getDisplayName().endsWith("AndroidManifest.xml")) return;
      // could edit the manifest here
      code = cleanAndroidManifest(code);
      if (code == null) {
	 IvyLog.logE("COSE","NULL CODE for android manifest xml");
	 return;
       }
      AndroidResource rsrc = new AndroidResource("AndroidManifest.xml",code.getBytes(CHAR_SET));
      pfrag.addResource(rsrc);
      addManifestClasses(repo,pfrag,source,code);
    }
}



private String cleanAndroidManifest(String code)
{
   if (cose_request.getCoseSearchType() != CoseSearchType.ANDROIDUI) return code;

   Element xml = IvyXml.convertStringToXml(code);
   if (xml == null)
      return code;

   boolean chng = false;
   for (Element appxml : IvyXml.children(xml,"application")) {
      List<Element> rems = new ArrayList<Element>();
      for (Element actxml : IvyXml.children(appxml)) {
	 switch (actxml.getNodeName()) {
	    case "service":
	    case "receiver" :
	    case "provider" :
	       rems.add(actxml);
	       chng = true;
	       break;
	    case "activity_alias" :
	       // check android:targetActivity
	       break;
	    default :
	       break;
	  }
       }
      for (Element e : rems) {
	 appxml.removeChild(e);
       }
    }

   if (chng) {
      code = IvyXml.convertXmlToString(xml);
    }

   return code;
}


private void addManifestClasses(KeySearchRepo repo,CoseResult pfrag,CoseSource src,String code)
{
   KeySearchQueue subwaits = new KeySearchQueue();

   while (code.startsWith("?")) code = code.substring(1);

   org.w3c.dom.Element xml = IvyXml.convertStringToXml(code);
   if (!IvyXml.isElement(xml,"manifest")) {
      IvyLog.logI("COSE","NON_MANIFEST RETURNED: " + src.getDisplayName() + " :: " + code);
      return;
    }
   String pkg = IvyXml.getAttrString(xml,"package");
   synchronized (package_items) {
      Map<String,Set<String>> sitms = package_items.get(src);
      if (sitms == null) {
	 sitms = new HashMap<String,Set<String>>();
	 package_items.put(src,sitms);
       }
      Set<String> items = sitms.get(pkg);
      if (items != null) {
	 IvyLog.logI("COSE","DUPLICATE MANIFEST " + src.getDisplayName() + " " + pkg);
	 return;
       }
      sitms.put(pkg,new HashSet<String>());
    }

   IvyLog.logI("COSE","WORK ON MANIFEST " + src.getDisplayName());

   for (org.w3c.dom.Element appxml : IvyXml.children(xml,"application")) {
      String icls = IvyXml.getAttrString(appxml,"android:name");
      if (icls == null) icls = IvyXml.getAttrString(appxml,"name");
      if (icls != null) {
	 loadAndroidClass(repo,pfrag,src,subwaits,pkg,icls);
       }
      for (org.w3c.dom.Element actxml : IvyXml.children(appxml)) {
	 switch (actxml.getNodeName()) {
	    case "activity" :
	    case "service" :
	    case "provider" :
	    case "activity-alias":
	       String cls = IvyXml.getAttrString(actxml,"android:name");
	       if (cls == null) cls = IvyXml.getAttrString(actxml,"name");
	       if (cls != null) {
		  loadAndroidClass(repo,pfrag,src,subwaits,pkg,cls);
		}
	       break;
	    default :
	       break;
	  }
       }
    }

   AndroidResourceLoader arl = new AndroidResourceLoader(repo,pfrag,src,0,subwaits);
   addSubtask(arl,subwaits);

   FinishPackageTask fpt = new FinishPackageTask(repo,pfrag,src,subwaits);
   addTask(fpt);
}



private void loadAndroidClass(KeySearchRepo repo,CoseResult pfrag,CoseSource src,KeySearchQueue subwaits,
      String pkg,String cls)
{
   String lpkg = pkg;
   if (cls.startsWith(".")) cls = cls.substring(1);
   else {
      int idx1 = cls.lastIndexOf(".");
      if (idx1 > 0) {
	 lpkg = cls.substring(0,idx1);
	 cls = cls.substring(idx1+1);
       }
    }
   IvyLog.logI("COSE","ADD Android CLASS " + pkg + " " + cls);
   AndroidClassLoader acl = new AndroidClassLoader(repo,src,lpkg,cls,pfrag,subwaits);
   addSubtask(acl,subwaits);
}



private class AndroidResourceLoader implements Runnable {

   private KeySearchRepo for_repo;
   private CoseResult package_result;
   private CoseSource for_source;
   private KeySearchQueue sub_waits;
   private URI load_uri;

   AndroidResourceLoader(KeySearchRepo repo,CoseResult pfrag,CoseSource src,int page,KeySearchQueue subwaits) {
      for_repo = repo;
      package_result = pfrag;
      for_source = src;
      sub_waits = subwaits;
      load_uri = null;
    }

   AndroidResourceLoader(AndroidResourceLoader arl,URI u) {
      for_repo = arl.for_repo;
      package_result = arl.package_result;
      for_source = arl.for_source;
      sub_waits = arl.sub_waits;
      load_uri = u;
    }

   @Override public void run() {
      loadByDirectory();
    }

   private void loadByDirectory() {
      if (load_uri == null) load_uri = for_repo.getURIForPath(for_source,"res");
      CoseSource nsrc = for_repo.createSource(load_uri,null,0);
      String sfx = nsrc.getDisplayName();
      int idx1 = sfx.lastIndexOf("/");
      if (idx1 > 0) sfx = sfx.substring(idx1+1);
      int idx = sfx.lastIndexOf(".");
      if (idx > 0) sfx = sfx.substring(idx);
      else sfx = "";
      // might need others here
      if (RESOURCE_EXTENSIONS.contains(sfx)) {
	 loadResourceData(nsrc,null);
       }
      else {
	 List<URI> uris = for_repo.getDirectoryContentsURIs(load_uri,for_source);
	 ByteArrayOutputStream rslts = null;
	 if (uris == null) {
	    rslts = for_repo.getResultBytes(load_uri);
	    if (rslts == null) return;
	    String cnts = KeySearchRepo.getString(rslts);
	    uris = for_repo.getDirectoryContentsURIs(load_uri,for_source,cnts);
	  }
	 if (uris == null || uris.size() == 0) {
	    if (!loadResourceData(nsrc,rslts))
	       IvyLog.logI("COSE","NO CONTENTS FOUND FOR " + load_uri);
	    return;
	  }
	 for (URI ux : uris) {
	    AndroidResourceLoader arl = new AndroidResourceLoader(this,ux);
	    IvyLog.logI("COSE","ADD RESOURCE SUBTASK FOR " + ux);
	    addSubtask(arl,sub_waits);
	  }
       }
   }


   private boolean loadResourceData(CoseSource nsrc,ByteArrayOutputStream ots)
   {
      byte [] cnts = null;
      if (ots != null) cnts = ots.toByteArray();
      if (cnts == null || cnts.length == 0) cnts = for_repo.getBinaryPage(load_uri);
      if (cnts == null || cnts.length == 0) return false;
      String path = nsrc.getDisplayName();
      int idx2 = path.indexOf("/res/");
      if (idx2 > 0) path = path.substring(idx2+1);
      AndroidResource arsrc = new AndroidResource(path,cnts);
      package_result.addResource(arsrc);
      IvyLog.logI("COSE","RESOURCE: " + nsrc.getName() + " " + nsrc.getProjectId() + " " +
	    nsrc.getDisplayName());
      if (path.contains("layout/") && path.endsWith(".xml")) {
	 String xmlstr = new String(cnts);
	 Element xml = IvyXml.convertStringToXml(xmlstr);
	 addResourceReferencedClasses(xml);
       }
      return true;
   }



   private void addResourceReferencedClasses(Element xml) {
      String eltnam = xml.getNodeName();
      if (eltnam.contains(".") && !eltnam.startsWith(".")) {
	 loadAndroidClass(for_repo,package_result,for_source,sub_waits,null,eltnam);
       }
      for (Element frag : IvyXml.elementsByTag(xml,"result")) {
	 String cnm = IvyXml.getAttrString(frag,"android:name");
	 if (cnm == null) cnm = IvyXml.getAttrString(frag,"name");
	 if (cnm == null) cnm = IvyXml.getAttrString(frag,"class");
	 if (cnm != null && cnm.contains(".") && !cnm.startsWith(".") &&
	     !cnm.startsWith("com.google.android.")) {
	    loadAndroidClass(for_repo,package_result,for_source,sub_waits,null,cnm);
	  }
       }
   }

}	// end of inner class AndroidResourceLoader


private class AndroidClassLoader implements Runnable {

   private KeySearchRepo for_repo;
   private CoseSource manifest_source;
   private CoseResult package_result;
   private String package_name;
   private String class_name;
   private Set<String> local_items;
   private int page_number;
   private KeySearchQueue sub_waits;

   AndroidClassLoader(KeySearchRepo repo,CoseSource src,String pkg,String cls,CoseResult pfrag,
	 KeySearchQueue subwaits) {
      for_repo = repo;
      manifest_source = src;
      package_result = pfrag;
      sub_waits = subwaits;
      Map<String,Set<String>> sitms = package_items.get(src);
      local_items = sitms.get(pkg);

      String pfx = pkg;
      if (cls.startsWith(".")) cls = cls.substring(1);
      int idx = cls.lastIndexOf(".");
      if (idx > 0) {
	 if (pkg == null) pfx = cls.substring(0,idx);
	 else pfx = pkg + "." + cls.substring(0,idx);
	 cls = cls.substring(idx+1);
       }
      package_name = pfx;
      class_name = cls;
      page_number = 0;
    }

   @Override public void run() {
      KeySearchClassData kcd = for_repo.getPackageClassResult(package_result,manifest_source,
            package_name,class_name,page_number);
   
      if (kcd != null && kcd.getURI() != null) {
         synchronized (local_items) {
            if (!local_items.add(kcd.getSource().getName())) {
               return;
             }
          }
         try {
            CoseResult rslt = result_factory.createFileResult(kcd.getSource(),kcd.getCode());
            if (rslt == null) return;
            package_result.addInnerResult(rslt);
          }
         catch (Throwable t) {
            IvyLog.logE("COSE","PACKAGE PROBLEM",t);
          }
       }
      else {
         IvyLog.logI("COSE","ANDROID class " + package_name + "." + class_name + " not found on page " + page_number);
         if (kcd != null) {
            ++page_number;
            addSubtask(this,sub_waits);
          }
       }
    }

}	// end of inner class AndroicClassLoader



private void findAndroidManifest(KeySearchRepo repo,CoseSource src)
{
   List<String> keys = new ArrayList<String>();
   keys.add("manifest");
   keys.add("application");
   keys.add("activity");
   keys.add("android");
   // should wee add the original class name here?
   // keys.add("schemas.android.com/apk/res/android");
   URI uri = repo.getURIForSearch(keys,CoseSearchLanguage.XML,src.getProjectId(),0,false);
   String rslts = repo.getResultPage(uri);
   List<URI> uris = repo.getSearchPageResults(uri,rslts);

   Map<CoseSource,String> usesrc = new HashMap<CoseSource,String>();
   for (URI u : uris) {
      String code = repo.getSourcePage(u);
      CoseSource nsrc = repo.createSource(u,code,0);
      checkUseManifest(nsrc,code,usesrc);
    }
   for (Map.Entry<CoseSource,String> ent : usesrc.entrySet()) {
      CoseSource nsrc = ent.getKey();
      String  code = ent.getValue();
      if (code == null) continue;
      CoseResult nrslt = result_factory.createPackageResult(nsrc);
      addAndroidSolutions(repo,nrslt,nsrc,code);
    }
}


private void checkUseManifest(CoseSource nsrc,String ncode,Map<CoseSource,String> osrcs)
{
   if (nsrc == null) return;

   String ns1 = nsrc.getDisplayName();
   if (!ns1.endsWith("AndroidManifest.xml")) return;
   int nidx1 = ns1.lastIndexOf("/");
   String ns2 = ns1.substring(0,nidx1);

   for (Iterator<CoseSource> it = osrcs.keySet().iterator(); it.hasNext(); ) {
      CoseSource osrc = it.next();
      String os1 = osrc.getDisplayName();
      int oidx1 = os1.lastIndexOf("/");
      String os2 = os1.substring(0,oidx1);
      if (os2.startsWith(ns2)) {
	 it.remove();
	 continue;
       }
      if (ns2.startsWith(os2))
	 return;
    }

   osrcs.put(nsrc,ncode);
}



/********************************************************************************/
/*										*/
/*	Load package solutions							*/
/*										*/
/********************************************************************************/

private class ScanPackageSearchResults implements Runnable {

   private KeySearchRepo for_repo;
   private String package_name;
   private String project_id;
   private String use_project;
   private CoseSource package_source;
   private CoseResult package_result;
   private int page_number;
   private KeySearchQueue package_queue;

   ScanPackageSearchResults(KeySearchRepo repo,String pkg,CoseSource pkgsrc,
         CoseResult pkgfrag,KeySearchQueue pkgq) {
      for_repo = repo;
      project_id = pkgsrc.getProjectId();
      use_project = project_id;
      package_name = pkg;
      package_result = pkgfrag;
      package_source = pkgsrc;
      page_number = 0;
      package_queue = pkgq;
    }

   @Override public void run() {
      if (isTooComplex(package_result,cose_request)) return;
      List<URI> uris = new ArrayList<>();
      
      boolean more = for_repo.getClassesInPackage(package_name,use_project,page_number,uris);
      if (uris == null || uris.size() == 0) {
         if (page_number == 0) {
            more = for_repo.getClassesInPackage(package_name,null,page_number,uris);
            if (uris != null && uris.size() > 1) use_project = null;
          }
       }
      if (use_project == null && uris != null) {
         int idx1 = project_id.indexOf("/");
         if (idx1 > 0) {
            String pfx = project_id.substring(0,idx1+1);
            List<URI> rslt = new ArrayList<>();
            for (URI u : uris) {
               if (u.toString().contains(pfx)) rslt.add(u);
             }
            uris = rslt;
          }
       }
      if (uris == null) return;
      
      for (URI u : uris) {
         LoadPackageResult lrp = new LoadPackageResult(for_repo,u,package_name,package_result,package_source);
         addSubtask(lrp,package_queue);
       }
   
      if (page_number < 20 && more) {
         ++page_number;  
         addSubtask(this,package_queue);
       }
    }

}	// end of inner class ScanPackageSearchResults



private class LoadPackageResult implements Runnable {

   private KeySearchRepo for_repo;
   private URI page_uri;
   private String package_name;
   private String orig_package;
   private CoseResult package_result;
   private CoseSource package_source;

   LoadPackageResult(KeySearchRepo repo,URI uri,String pkg,CoseResult pkgfrag,
         CoseSource pkgsrc) {
      for_repo = repo;
      page_uri = uri;
      package_name = pkg;
      package_source = pkgsrc;
      package_result = pkgfrag;
      orig_package = null;
      orig_package = pkgfrag.getBasePackage();
    }

   @Override public void run() {
      if (isTooComplex(package_result,cose_request)) return;
      String code = for_repo.getSourcePage(page_uri);
      if (code == null) return;
      String liccode = code;
      if (package_result.getInnerResults().size() > 0) liccode = null;
      CoseSource src = for_repo.createSource(page_uri,liccode,0);
      Object str = package_result.getFindStructure(code);
      String pkg = package_result.findPackageName(code,str);
      if (!pkg.equals(package_name)) return;
      String cls = package_result.findTypeName(code,str);
      if (cls == null) return;
      
      package_result.addPackage(pkg);
      IvyLog.logD("COSE","Check load package result " + pkg + "@" + cls + " FOR " + orig_package);
      IvyLog.logD("COSE","Check type " + cose_request.getCoseScopeType() + " " + " FOR " + orig_package);
      if (orig_package != null && !pkg.equals(orig_package)) {
         // handle code from other packages
         if (cose_request.getCoseScopeType() == CoseScopeType.PACKAGE_IFACE) {
            String ifc = package_result.findInterfaceName(code,str);
            if (ifc == null || ifc.equals("")) return;
            boolean fnd = package_result.getEditText().contains(ifc);
            updateRecheck(fnd);
            IvyLog.logD("COSE","ADD IFACE " + ifc + " " + fnd + " " + package_name + " " + orig_package);
            // should make sure iface is actually used
            if (!fnd) return;
          }
         else if (cose_request.getCoseScopeType() == CoseScopeType.PACKAGE_USED) {
            if (cls == null || cls.equals("")) {
               IvyLog.logD("COSE","EMPTY CLASS");
               return;
             }
            boolean fnd = package_result.containsText(cls);
            IvyLog.logD("COSE","READY TO UPDATE RECHECK FOR " + orig_package);
            updateRecheck(fnd);
            IvyLog.logD("COSE","ADD USED " + cls + " " + fnd + " " + package_name + " " + orig_package);
            if (!fnd) return;
          }
         else {
            IvyLog.logD("COSE","ADD OTHER " + cls + " " + package_name + " " + orig_package);
          }
       }
      else {
         IvyLog.logD("COSE","SAME PACKAGE ADD "  + cls + " " + package_name + " " + orig_package);
       }
      
      if (cose_request.getCoseScopeType() == CoseScopeType.PACKAGE_UI) {
         String ext = package_result.findExtendsName(code,str);
         boolean isui = false;
         if (ext != null) {
            if (ext.equals("Result") || ext.equals("Fragment") || ext.equals("Activity")) {
               isui = true;
             }
          }
         if (!isui) return;
       }
      if (cose_request.getCoseSearchType() == CoseSearchType.ANDROIDUI) {
         if (cls != null && cls.equals("R")) return;
         if (cls != null && cls.equals("BuildConfig")) return;
       }
      
      if (addPackageSolution(code,package_source,src,package_result)) {
         IvyLog.logD("COSE","SUCCESS ADD " + cls + " " + package_name + " TO " + orig_package); 
         do_recheck.add(package_result);
       }
      else {
         IvyLog.logD("COSE","FAILED ADD " + cls + " " + package_name + " TO " + orig_package); 
       }
      
   }
   
   
   
   private void updateRecheck(boolean fnd)
   {
      synchronized (recheck_items) {
         Set<LoadPackageResult> lprs = recheck_items.get(package_result);
         if (lprs == null) {
            lprs = new HashSet<>();
            recheck_items.put(package_result,lprs);
          }
         if (!fnd) {
            IvyLog.logD("COSE","Add to recheck " + package_name + " for " + orig_package);
            lprs.add(this);
            return;
          }
         IvyLog.logD("COSE","Remove recheck " + package_name + " for " + orig_package);
         lprs.remove(this);
       }   
   }

}	// end of inner class LoadPackageResult




/********************************************************************************/
/*										*/
/*	Finish building a package						*/
/*										*/
/********************************************************************************/

private class FinishPackageTask implements Runnable {

   private KeySearchRepo for_repo;
   private KeySearchQueue package_queue;
   private CoseSource package_source;
   private CoseResult package_result;
   private Set<String> checked_packages;

   FinishPackageTask(KeySearchRepo repo,CoseResult pkgfrag,CoseSource pkgsrc,KeySearchQueue pkgq) {
      for_repo = repo;
      package_queue = pkgq;
      package_source = pkgsrc;
      package_result = pkgfrag;
      checked_packages = new HashSet<>();
    }

   @Override public void run() {
      if (!checkIfDone(package_queue)) {
         // try again later
         addTask(this);
         return;
       }
   
      // here is were we extend the solution to include other packages
      Set<String> pkgs = null;
   
      switch (cose_request.getCoseScopeType()) {
         case SYSTEM :
         case PACKAGE_IFACE :
         case PACKAGE_USED :
            pkgs = package_result.getRelatedPackages();
            break;
         case PACKAGE :
            pkgs = package_result.getUsedProjects();
            break;
         default :
            break;
       }
   
      if (pkgs != null) {
         pkgs.removeAll(checked_packages);
         checked_packages.addAll(pkgs);
         if (pkgs.size() > 0) {
            for (String pkg : pkgs) {
               ScanPackageSearchResults spsr = new ScanPackageSearchResults(for_repo,pkg,
                     package_source,package_result,package_queue);
               addSubtask(spsr,package_queue);
             }
            addTask(this);
            return;
          }
       }
      
      while (do_recheck.remove(package_result)) {
         IvyLog.logD("COSE","RECHECK " + package_result.getBasePackage());
         List<LoadPackageResult> lprs;
         synchronized (recheck_items) {
            Collection<LoadPackageResult> clpr = recheck_items.get(package_result);
            if (clpr == null) {
               IvyLog.logD("COSE","Nothing to recheck for " + package_result.getBasePackage());
               break;
             }
            lprs = new ArrayList<>(clpr);
          }
         if (isTooComplex(package_result,cose_request)) break;
         if (!lprs.isEmpty()) {
            for (LoadPackageResult lpr : lprs) {
               IvyLog.logD("COSE","DO RECHECK OF " + lpr.page_uri + " FOR " + 
                     package_result.getBasePackage());
               lpr.run();
             }
            addTask(this);
            return;
          }
       }
      
      if (isTooComplex(package_result,cose_request)) {
         result_set.removeResult(package_result);
       }
      else {
         result_set.addResult(package_result);
         package_output_count.incrementAndGet();
       }
    }

}	// end of inner class FinishPackageTask




/********************************************************************************/
/*										*/
/*	Task management methods 						*/
/*										*/
/********************************************************************************/

private void addTask(Runnable r)
{
   thread_pool.execute(r);
}


private void addSubtask(Runnable r,KeySearchQueue queue)
{
   RunnableFuture<Boolean> task = new FutureTask<>(r,true);
   thread_pool.execute(task);
   synchronized (queue) {
      queue.add(task);
    }
}



private boolean checkIfDone(KeySearchQueue queue)
{
   int waitct = 0;
   synchronized (queue) {
      for (Future<Boolean> fb : queue) {
	 if (!fb.isDone()) ++waitct;
       }
    }
   return waitct == 0;
}





















static boolean isTooComplex(CoseResult rslt,CoseRequest cr)
{
   if (rslt.getInnerResults() != null) {
      if (rslt.getInnerResults().size() > cr.getMaxPackageFiles()) return true;
    }
   if (rslt.getPackages() != null) {
      if (rslt.getPackages().size() > cr.getMaxPackages()) return true;
    }
   
   return false;
}


/********************************************************************************/
/*										*/
/*	Resource for android ui search						*/
/*										*/
/********************************************************************************/

private static class AndroidResource implements CoseResource {

   private String path_name;
   private byte [] file_contents;

   AndroidResource(String path,byte [] cnts) {
      path_name = path;
      file_contents = cnts;
    }

   @Override public byte [] getContents()	{ return file_contents; }
   @Override public String getPathName()	{ return path_name; }

}	// end of inner class AndroidResource



/********************************************************************************/
/*										*/
/*	Thread pool for executing searches					*/
/*										*/
/********************************************************************************/

private static AtomicInteger thread_counter = new AtomicInteger(0);


private class ThreadPool extends ThreadPoolExecutor implements ThreadFactory {

   private int exec_count;

   ThreadPool(int nth) {
      super(nth/2,nth,5000L,TimeUnit.MILLISECONDS,new LinkedBlockingDeque<Runnable>());
      setThreadFactory(this);
      exec_count = 0;
    }

   @Override public Thread newThread(Runnable r) {
      Thread th = new CoseSearchThread(r);
      return th;
    }

   @Override public synchronized void execute(Runnable r) {
      // System.err.println("BEGIN " + r);
      ++exec_count;
      super.execute(r);
    }

   @Override protected synchronized void afterExecute(Runnable r,Throwable t) {
      // System.err.println("FINISH " + r + " " + t);
      --exec_count;
      super.afterExecute(r,t);
      if (exec_count <= 0) notifyAll();
      if (t != null) {
         IvyLog.logE("COSE","Problem during search",t);
       }
    }

   synchronized void waitForAll() {
      while (getQueue().size() > 0 || exec_count > 0) {
         try {
            wait(2000);
          }
         catch (InterruptedException e) { }
       }
    }

}	// end of inner class ThreadPool



private static class CoseSearchThread extends Thread implements LoggerThread {

   private int thread_id;
   
   CoseSearchThread(Runnable r) { 
      super(r);
      thread_id = thread_counter.incrementAndGet();
      setName("Cose_Search_" + thread_id);
      setDaemon(true);
    }
   
   @Override public int getLogId()              { return thread_id; }
   
}       // end of inner class CoseSearchThread


}	// end of class KeySearchMaster




/* end of KeySearchMaster.java */

