/********************************************************************************/
/*                                                                              */
/*              ResultGroup.java                                                */
/*                                                                              */
/*      Result that is a collection of results                                  */
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract class ResultGroup extends ResultBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected List<CoseResult>      inner_results;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ResultGroup(CoseSource src)
{
   super(src);
   inner_results = new ArrayList<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public CoseResultType getResultType()     { return CoseResultType.PACKAGE; }


@Override synchronized public void addInnerResult(CoseResult cf)
{
   inner_results.add(cf);
}

@Override public Collection<CoseResult> getInnerResults()
{
   return inner_results;
}


@Override public boolean containsText(String text) {
   for (CoseResult cr : inner_results) {
      if (cr.containsText(text)) return true;
    }
   return false;
}

@Override protected void localOutputXml(IvyXmlWriter xw) {
   for (CoseResult cr : inner_results) {
      xw.begin("INNER");
      cr.outputXml(xw);
      xw.end("INNER");
    }
}

}       // end of class ResultGroup




/* end of ResultGroup.java */

