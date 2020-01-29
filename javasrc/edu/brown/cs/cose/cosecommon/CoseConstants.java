/********************************************************************************/
/*										*/
/*		CoseConstants.java						*/
/*										*/
/*	Constants for cose search common code					*/
/*										*/
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

package edu.brown.cs.cose.cosecommon;






public interface CoseConstants
{


enum CoseSearchLanguage {
   JAVA,
   JAVASCRIPT,
   XML,
}


enum CoseSearchType {
   METHOD,
   CLASS,
   PACKAGE,
   ANDROIDUI,
   TESTCLASS,
}


enum CoseScopeType {
   FILE,
   PACKAGE,
   PACKAGE_UI,
   SYSTEM,
}


enum CoseSearchEngine {
   GITHUB,
   CODEEX,
   GITZIP, 
   SEARCHCODE,
}


enum CoseResultType {
   FILE,
   CLASS,
   METHOD,
   PACKAGE
}



/********************************************************************************/
/*                                                                              */
/*     License information                                                      */
/*                                                                              */
/********************************************************************************/

String LICENSE_DATABASE = "s6";

String SCORE_DATA_FILE = "/ws/volfred/s6/scores.data";

}	// end of interface CoseConstants



/*   end of CoseConstants.java */


