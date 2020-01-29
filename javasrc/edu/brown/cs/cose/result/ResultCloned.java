/********************************************************************************/
/*                                                                              */
/*              ResultCloned.java                                               */
/*                                                                              */
/*      Result derived from editing another result                              */
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

class ResultCloned extends ResultBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ResultDelta     result_delta;
private Object          structure_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ResultCloned(ResultBase base,ResultDelta delta)
{
   super(base,base.getSource());
   result_delta = delta;
   structure_data = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getKeyText()
{
   return result_delta.getKeyText();
}


@Override public CoseResultType getResultType()
{
   return getParent().getResultType();
}



/********************************************************************************/
/*                                                                              */
/*      Methods to apply and use the delta                                      */
/*                                                                              */
/********************************************************************************/

@Override public Object getStructure()
{
   if (structure_data == null) {
      structure_data = getParent().getDeltaStructure(result_delta);
    }
   return structure_data;
}

@Override public Object checkStructure()
{
   return structure_data;
}

@Override public Object clearStructure()
{
   Object rslt = structure_data;
   structure_data = null;
   return rslt;
}

@Override public String getText()
{
   return result_delta.getText();
}

@Override public String getEditText()
{
   return result_delta.getEditText();
}

@Override public boolean isCloned()             { return true; }

@Override public CoseResult cloneResult(Object o,Object data)
{
   CoseResult cr = result_delta.cloneResult(this,o,data);
   if (cr != null) return cr;
   
   return super.cloneResult(o,data);
}




}       // end of class ResultCloned




/* end of ResultCloned.java */

