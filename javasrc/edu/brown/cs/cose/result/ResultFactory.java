/********************************************************************************/
/*                                                                              */
/*              ResultFactory.java                                              */
/*                                                                              */
/*      Class to create the appropriate fragments                               */
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

import edu.brown.cs.cose.cosecommon.CoseResult;

import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.cose.remote.RemoteSource;
import edu.brown.cs.ivy.xml.IvyXml;


public class ResultFactory implements ResultConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CoseRequest for_request;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ResultFactory(CoseRequest cr)
{
   for_request = cr;
}


/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

public CoseResult createPackageResult(CoseSource src)
{
   switch (for_request.getLanguage()) {
      case JAVA :
         return new ResultJava.JavaPackageResult(src);
      case JAVASCRIPT :
      case XML :
      case OTHER :
         break;
    }
   
   return null;
}


public CoseResult createFileResult(CoseSource source,String code)
{
   switch (for_request.getLanguage()) {
      case JAVA :
         code = for_request.editSource(code);
         return new ResultJava.JavaFileResult(source,code);
      case JAVASCRIPT :
      case XML :
      case OTHER :
         return new ResultTextFile(source,code);
    }
   
   return null;
} 



public CoseResult createResult(Element xml)
{
   if (xml == null) return null;
   if (!IvyXml.isElement(xml,"RESULT")) {
      xml = IvyXml.getChild(xml,"RESULT");
      if (xml == null) return null;
    }
   
   Element srcxml = IvyXml.getChild(xml,"SOURCE");
   CoseSource src = RemoteSource.createSource(srcxml);
   CoseResultType rtype = IvyXml.getAttrEnum(xml,"RESULTTYPE",CoseResultType.FILE);
   String ftyp = IvyXml.getAttrString(xml,"TYPE");
   if (ftyp.equals("CLONED")) {
      // create cloned result
      return null;
    }
   else {
      switch (rtype) {
         case FILE :
            String cnts = IvyXml.getTextElement(xml,"CONTENTS");
            return createFileResult(src,cnts); 
         case PACKAGE :
            CoseResult prslt = createPackageResult(src);
            for (Element innerxml : IvyXml.children(xml,"INNER")) {
               CoseResult inner = createResult(innerxml);
               if (inner != null) prslt.addInnerResult(inner);
             }
            return prslt;
         case METHOD :
         case CLASS :
            break;
       }
    }
   
   return null;
}


}       // end of class ResultFactory




/* end of ResultFactory.java */

