/********************************************************************************/
/*                                                                              */
/*              ScorerAnalyzer.java                                             */
/*                                                                              */
/*      Code to analyze a potential fragment and score it                       */
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



package edu.brown.cs.cose.scorer;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.cose.cosecommon.CoseSignature;
import edu.brown.cs.cose.cosecommon.CoseRequest.CoseKeywordSet;

abstract public class ScorerAnalyzer implements ScorerConstants
{



/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

public static ScorerAnalyzer createAnalyzer(CoseRequest req)
{
   switch (req.getLanguage()) {
      case JAVA :
         return new ScorerAnalyzerJava(req,null);
      case JAVASCRIPT :
      case XML :
         break;
    }
   
   return null;
}



public static ScorerAnalyzer createAnalyzer(CoseRequest req,Object struct)
{
   switch (req.getLanguage()) {
      case JAVA :
         return new ScorerAnalyzerJava(req,(ASTNode) struct);
      case JAVASCRIPT :
      case XML :
         break;
    }
   
   return null;
}




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CoseRequest             base_request;

protected CoseScores            value_map;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected ScorerAnalyzer(CoseRequest req)
{
   base_request = req;
   value_map = new CoseScores();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

protected CoseSearchType getSearchType()
{
   return base_request.getCoseSearchType();
}



protected List<CoseKeywordSet> getKeywordSets()
{
   return base_request.getCoseKeywordSets();
}

protected List<String> getKeyTerms()
{
   return base_request.getKeyTerms();
}


protected CoseSignature getSignature()
{
   return base_request.getCoseSignature();
}



/********************************************************************************/
/*                                                                              */
/*      Main analysis entry                                                     */
/*                                                                              */
/********************************************************************************/

abstract public CoseScores analyzeProperties(CoseResult cr);

abstract public boolean isTestCase(String src);

}       // end of class ScorerAnalyzer




/* end of ScorerAnalyzer.java */

