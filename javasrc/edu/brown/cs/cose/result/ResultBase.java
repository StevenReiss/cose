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
import java.util.HashSet;
import java.util.Set;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseResource;
import edu.brown.cs.cose.cosecommon.CoseSource;

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


@Override public boolean addPackage(String pkg)                 { return false; }
@Override public Collection<String> getPackages()               { return null; }
@Override public String getBasePackage()                        { return null; }
@Override public void addInnerResult(CoseResult cf)             { }
@Override public Collection<CoseResult> getInnerResults()       { return null; }

@Override public Collection<CoseResult> getResults(CoseSearchType t)        
{
   return null;
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

@Override public Set<String> getRelatedProjects()               { return null; }
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




}       // end of class ResultBase




/* end of ResultBase.java */

