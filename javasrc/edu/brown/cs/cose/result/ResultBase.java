/********************************************************************************/
/*                                                                              */
/*              ResultBase.java                                                 */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.cose.result;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResource;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.cose.scorer.ScorerAnalyzer;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract class ResultBase implements CoseResult
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CoseSource fragment_source;
private ResultBase parent_fragment;
private Collection<CoseResource> resource_set;
protected CoseScores result_scores;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected ResultBase(CoseSource src)
{
   fragment_source = src;
   parent_fragment = null;
   resource_set = null;
   result_scores = null;
}


protected ResultBase(ResultBase par,CoseSource src)
{
   fragment_source = src;
   parent_fragment = par;
   resource_set = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public ResultBase getParent()                 { return parent_fragment; }


@Override public CoseSource getSource()
{
   return fragment_source;
}

protected void setSource(CoseSource src)
{
   fragment_source = src;
}

@Override public CoseScores getScores(CoseRequest req)
{
   if (result_scores != null) return result_scores;
   
   ScorerAnalyzer sanal = ScorerAnalyzer.createAnalyzer(req);
   CoseScores scores = sanal.analyzeProperties(this);
   result_scores = scores;
   return scores;
}



@Override public CoseScores getScores(CoseRequest req,Object struct)
{
   ScorerAnalyzer sanal = ScorerAnalyzer.createAnalyzer(req,struct);
   CoseScores scores = sanal.analyzeProperties(this);
   return scores;
}


@Override public boolean addPackage(String pkg)                 { return false; }
@Override public Collection<String> getPackages()               { return null; }
@Override public String getBasePackage()                        { return null; }
@Override public void addInnerResult(CoseResult cf)             { }
@Override public Collection<CoseResult> getInnerResults()       { return null; }

@Override public Collection<CoseResult> getResults(CoseSearchType t)        
{
   return Collections.singleton(this);
}




/********************************************************************************/
/*                                                                              */
/*      Resource methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void addResource(CoseResource cr)
{
   if (resource_set == null) resource_set = new HashSet<>();
   resource_set.add(cr);
}



@Override public Collection<CoseResource> getResources()
{
   Collection<CoseResource> rslt = null;
   if (parent_fragment != null) rslt = parent_fragment.getResources();
   if (resource_set != null) {
      if (rslt == null) rslt = new HashSet<>();
      rslt.addAll(resource_set);
    }
   return rslt;
}

@Override public Set<String> getRelatedPackages()               { return null; }
@Override public Set<String> getUsedProjects()                  { return null; }


@Override public CoseResult cloneResult(Object diffs,Object data) 
{
   return baseCloneResult(diffs);
}

@Override public boolean isCloned()                             { return false; }

@Override public Object clearStructure()                        { return null; }



protected CoseResult baseCloneResult(Object diffs) 
{
   if (diffs instanceof ResultDelta) {
      ResultDelta rd = (ResultDelta) diffs;
      return new ResultCloned(this,rd);
    }
   else if (diffs == null) {
      return new ResultCloned(this,null);
    }
   return null;
}


protected Object getDeltaStructure(ResultDelta rd)
{
   Object ds = rd.getDeltaStructure();
   if (ds != null) return ds;
   
   return getStructure();
}



/********************************************************************************/
/*                                                                              */
/*      Name finding methods                                                    */
/*                                                                              */
/********************************************************************************/

@Override public Object getFindStructure(String code)           { return null; }
   

@Override public String findPackageName(String text,Object str)
{
   return null;
}


@Override public String findTypeName(String text,Object str)
{
   String pats = "\\s*((public|abstract)\\s+)*(class|interface|enum)\\s+(\\w+)";
   Pattern pat = Pattern.compile(pats,Pattern.MULTILINE);
   Matcher mat = pat.matcher(text);
   if (!mat.find()) return null;
   String cls = mat.group(4);
   return cls;
}
   


@Override public String findInterfaceName(String text,Object str)
{
   return null;
}


@Override public String findClassName(String text,Object str)
{
   return null;
}


@Override public String findExtendsName(String text,Object str)
{
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw) 
{
   xw.begin("RESULT");
   
   xw.field("RESULTTYPE",getResultType());
   
   localOutputXml(xw);
   
   if (fragment_source != null) {
      outputSource(fragment_source,xw);
    }
   if (parent_fragment != null) {
      xw.begin("PARENT");
      parent_fragment.outputXml(xw);
      xw.end("PARENT");
    }
   if (resource_set != null) {
      for (CoseResource cr : resource_set) {
         xw.begin("RESOURCE");
         xw.text(cr.toString());
         xw.end("RESOURCE");
       }
    }
   
   xw.end("RESULT");
}


private void outputSource(CoseSource src,IvyXmlWriter xw) 
{
   if (src == null) return;
   
   xw.begin("SOURCE");
   xw.field("NAME",src.getName());
   xw.field("DISPLAY",src.getDisplayName());
   xw.field("PATH",src.getPathName());
   xw.field("LENGTH",src.getLength());
   xw.field("OFFSET",src.getOffset());
   xw.field("LICENSE",src.getLicenseUid());
   xw.field("PROJECT",src.getProjectId());
   xw.field("SCORE",src.getScore());
   if (src.getBaseSource() != null) {
      xw.begin("BASE");
      outputSource(src.getBaseSource(),xw);
      xw.end("BASE");
    }
   xw.end("SOURCE");
}


abstract protected void localOutputXml(IvyXmlWriter xw);



}       // end of class ResultBase




/* end of ResultBase.java */

