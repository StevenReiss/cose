/********************************************************************************/
/*                                                                              */
/*              LicenseCopyrightFinder.java                                     */
/*                                                                              */
/*      Code to extract a copyright from a file                                 */
/*                                                                              */
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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



package edu.brown.cs.cose.license;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LicenseCopyrightFinder
{


/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Pattern COPYRIGHT_PATTERN = Pattern.compile("(((\\(C\\))|(COPYRIGHT))\\s+[12])|" +
      "(Public License Version)",
      Pattern.CASE_INSENSITIVE);




/********************************************************************************/
/*                                                                              */
/*      Method to get the copyright                                             */
/*                                                                              */
/********************************************************************************/

public static String getCopyright(Reader source) throws IOException
{
   BufferedReader br = new BufferedReader(source);
   StringBuffer cpy = new StringBuffer();
   StringBuilder ccmt = new StringBuilder();
   
   boolean incmmt = false;
   
   for ( ; ; ) {
      String s = br.readLine();
      if (s == null) break;
      
      int state = (incmmt ? 20 : 0);
      int quote = 0;
      
      for (int i = 0; i < s.length(); ++i) {
	 int ch = s.charAt(i);
	 if (Character.isWhitespace(ch)) continue;
	 if (quote != 0 && ch == '\\') ++i;
	 else if (quote != 0 && ch == quote) {
	    quote = 0;
	  }
	 else if (quote == 0 && (ch == '"' || ch == '\'')) {
	    quote = ch;
	    if (state == 0) state = 1;
	    else if (state == 21) state = 22;
	  }
	 else if (ch == '/' && i+1 < s.length() && !incmmt && s.charAt(i+1) == '/') {
	    if (state == 0) state = 10;
	    else break;
	  }
	 else if (ch == '/' && i+1 < s.length() && !incmmt && s.charAt(i+1) == '*') {
	    if (state == 0) state = 20;
	    incmmt = true;
	  }
	 else if (incmmt && ch == '*' && i+1 < s.length() && s.charAt(i+1) == '/') {
	    ++i;
	    incmmt = false;
	    if (state == 20) state = 21;
	  }
	 else {
	    if (state == 0) state = 1;
	    else if (state == 21) state = 22;
	  }
       }
      
      if (state == 10) {		// // line found
	 ccmt.setLength(0);
	 ccmt.append(s);
       }
      else if (state == 21 || (incmmt && state == 20)) {	   // /* line found */
	 if (ccmt.length() > 0) ccmt.append("\n");
	 ccmt.append(s);
       }
      else if (!incmmt && (state == 22 || state == 1)) ccmt.setLength(0);
      
      if (ccmt.length() > 0 && !incmmt) {
	 Matcher m = COPYRIGHT_PATTERN.matcher(ccmt);
	 if (m.find()) {
	    if (cpy.length() > 0) cpy.append("\n");
	    cpy.append(ccmt);
	  }
	 ccmt.setLength(0);
       }
    }
   
   if (cpy.length() == 0) {
      cpy.append("// NO COPYRIGHT OR LICENSE FOUND IN SOURCE\n");
    }
   
   return cpy.toString();
}


}       // end of class LicenseCopyrightFinder




/* end of LicenseCopyrightFinder.java */
