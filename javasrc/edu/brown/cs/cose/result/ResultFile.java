/********************************************************************************/
/*                                                                              */
/*              ResultFile.java                                                 */
/*                                                                              */
/*      File Fragment base class                                                */
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

import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract class ResultFile extends ResultBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String file_contents;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected ResultFile(CoseSource src,String cnts)
{
   super(src);
   file_contents = cnts;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public CoseResultType getResultType()     { return CoseResultType.FILE; }



@Override public String getText()                       { return file_contents; }

@Override public String getKeyText()                    { return getText(); }

@Override public String getEditText()                   { return getText(); }

@Override public Object getStructure()                  { return null; }

@Override public Object checkStructure()                { return null; }

@Override public Object clearStructure()                { return null; }

@Override protected void localOutputXml(IvyXmlWriter xw)
{
   xw.cdataElement("CONTENTS",file_contents);
}


}       // end of class ResultFile




/* end of ResultFile.java */

