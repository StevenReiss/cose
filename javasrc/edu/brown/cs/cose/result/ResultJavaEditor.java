/********************************************************************************/
/*                                                                              */
/*              ResultJavaEditor.java                                           */
/*                                                                              */
/*      Editor to fix java version problems                                     */
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.brown.cs.cose.cosecommon.CoseResultEditor;

public class ResultJavaEditor implements CoseResultEditor
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final Pattern    assert_pattern;

static {
   assert_pattern = Pattern.compile("(void|boolean|\\.)\\s*(assert)\\s*(\\(|\\=|\\;)");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ResultJavaEditor()
{ }


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public String editFileResult(String orig)
{
   if (!orig.contains("assert")) return orig;
   
   for (int idx = orig.indexOf("assert"); idx >= 0; idx = orig.indexOf("assert",idx+1)) {
      int idx1 = Math.max(idx-10,0);
      int idx2 = Math.min(idx+20,orig.length());
      System.err.println("FOUND: " + orig.substring(idx1,idx2));
    }
   
   String txt = orig;
   Matcher m = assert_pattern.matcher(orig);
   while (m.find()) {
      int idx = m.start(2);
      int eidx = m.end(2);
      // replacement must be of same length to avoid restarting matcher
      String ntxt = orig.substring(0,idx) + "assrtt" + orig.substring(eidx);
      txt = ntxt;
    }
   
   return txt;
}







}       // end of class ResultJavaEditor




/* end of ResultJavaEditor.java */

