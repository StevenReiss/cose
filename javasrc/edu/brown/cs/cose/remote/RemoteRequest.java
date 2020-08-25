/********************************************************************************/
/*                                                                              */
/*              RemoteRequest.java                                              */
/*                                                                              */
/*      Remote version of a Cose Request                                        */
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



package edu.brown.cs.cose.remote;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseDefaultRequest;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseSignature;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class RemoteRequest extends CoseDefaultRequest implements RemoteConstants
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

RemoteRequest(Element xml)
{
   setDoDebug(IvyXml.getAttrBool(xml,"DEBUG"));
   setMaxPackageFiles(IvyXml.getAttrInt(xml,"MAXPKG"));
   setNumberOfResults(IvyXml.getAttrInt(xml,"NUMRSLT"));
   // setNumberOfThreads(IvyXml.getAttrInt(xml,"NUMTHRD"));
   setProjectId(IvyXml.getAttrString(xml,"PROJECT"));
   setCoseScopeType(IvyXml.getAttrEnum(xml,"SCOPE",CoseScopeType.FILE));
   setCoseSearchType(IvyXml.getAttrEnum(xml,"SEARCHTYPE",CoseSearchType.FILE));
   setSearchLanguage(IvyXml.getAttrEnum(xml,"LANGUAGE",CoseSearchLanguage.JAVA));
   int ct = 0;
   for (Element eng : IvyXml.children(xml,"ENGINE")) {
      CoseSearchEngine cse = IvyXml.getAttrEnum(eng,"NAME",CoseSearchEngine.GITHUB);
      if (ct++ == 0) setSearchEngine(cse);
      else addSearchEngine(cse);
    }
   for (Element kterm : IvyXml.children(xml,"TERM")) {
      String k = IvyXml.getText(kterm);
      addKeyTerm(k);
    }
   for (Element kset : IvyXml.children(xml,"KEYSET")) {
      List<String> wds = new ArrayList<>();
      for (Element wset : IvyXml.children(kset,"WORD")) {
         String s = IvyXml.getText(wset);
         if (s != null) wds.add(s);
       }
      if (!wds.isEmpty()) addKeywordSet(wds);
    }
   // handle signature if present
}



/********************************************************************************/
/*                                                                              */
/*      Create text from request                                                */
/*                                                                              */
/********************************************************************************/

static void createRequest(CoseRequest cr,IvyXmlWriter xw)
{
   xw.begin("REQUEST");
   xw.field("DEBUG",cr.doDebug());
   xw.field("MAXPKG",cr.getMaxPackageFiles());
   xw.field("NUMRSLT",cr.getNumberOfResults());
   xw.field("NUMTHRD",cr.getNumberOfThreads());
   if (cr.getProjectId() != null) xw.field("PROJECT",cr.getProjectId());
   xw.field("SCOPE",cr.getCoseScopeType());
   xw.field("SEARCHTYPE",cr.getCoseSearchType());
   xw.field("LANGUAGE",cr.getLanguage());
   for (CoseSearchEngine cse : cr.getEngines()) {
      xw.begin("ENGINE");
      xw.field("NAME",cse);
      xw.end("ENGINE");
    }
   CoseSignature csgn = cr.getCoseSignature();
   if (csgn != null) {
      xw.textElement("SIGNATURE",csgn.toString());
    }
   if (cr.getKeyTerms() != null) {
      for (String s : cr.getKeyTerms()) {
         xw.textElement("TERM",s);
       }
    }
   for (CoseKeywordSet cks : cr.getCoseKeywordSets()) {
      xw.begin("KEYSET");
      for (String s : cks.getWords()) {
         xw.textElement("WORD",s);
       }
      xw.end("KEYSET");
    }
   xw.end("REQUEST");
}



}       // end of class RemoteRequest




/* end of RemoteRequest.java */

