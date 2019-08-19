/********************************************************************************/
/*										*/
/*		KeySearchConstants.java 					*/
/*										*/
/*	Constants for keyword based initial search				*/
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

package edu.brown.cs.cose.keysearch;


import java.util.LinkedList;
import java.util.concurrent.Future;
import java.nio.charset.Charset;
import java.net.URI;

import edu.brown.cs.cose.cosecommon.CoseConstants;
import edu.brown.cs.cose.cosecommon.CoseSource;



interface KeySearchConstants extends CoseConstants {
 


/********************************************************************************/
/*										*/
/*	File Definitions							*/
/*										*/
/********************************************************************************/

String CACHE_DIRECTORY = "/ws/volfred/s6/cache";

String CACHE_URL_FILE = "URL";
String CACHE_DATA_FILE = "DATA";

long CACHE_TIME_OUT = 1000L*60L*60L*24L*365L;

String ZIPCACHE_DIRECTORY = "/ws/volfred/s6/zips";
String TOKEN_FILE = "/ws/volfred/s6/tokens";


Charset CHAR_SET = Charset.forName("UTF-8");



/********************************************************************************/
/*										*/
/*	Wait queue								*/
/*										*/
/********************************************************************************/

class KeySearchQueue extends LinkedList<Future<Boolean>> {

   private static final long serialVersionUID = 1;

}


class KeySearchClassData {

   URI class_uri;
   String class_path;
   CoseSource class_source;
   String class_code;

   KeySearchClassData(URI u,String p,CoseSource src,String cd) {
      class_uri = u;
      class_path = p;
      class_source = src;
      class_code = cd;
    }

   URI getURI() 			{ return class_uri; }
   String getPath()			{ return class_path; }
   CoseSource getSource() 		{ return class_source; }
   String getCode()			{ return class_code; }

}	// end of inner class KeySearchClassData




}	// end of interface KeySearchConstants



/*   end of KeySearchConstants.java */

