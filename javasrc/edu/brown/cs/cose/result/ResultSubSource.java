/********************************************************************************/
/*										*/
/*		ResultSubSource.java						*/
/*										*/
/*	Source for a portion of a file source					*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
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



package edu.brown.cs.cose.result;

import edu.brown.cs.cose.cosecommon.CoseSource;

public class ResultSubSource implements CoseSource
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CoseSource base_source;
private int code_offset;
private int code_length;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ResultSubSource(CoseSource src,int off,int len)
{
   base_source = src;
   code_offset = off;
   code_length = len;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProjectId()
{
   return base_source.getProjectId();
}

@Override public String getPathName()
{
   return base_source.getPathName();
}


@Override public double getScore()
{
   return base_source.getScore();
}


@Override public String getName()
{
   return base_source.getName() + "@" + code_offset + ":" + code_length;
}


@Override public String getDisplayName()
{
   return base_source.getDisplayName() + "@" + code_offset + ":" + code_length;
}


@Override public int getOffset()		{ return code_offset; }

@Override public int getLength()		{ return code_length; }

@Override public CoseSource getBaseSource()     { return base_source; }

@Override public String getLicenseUid()                
{
   return base_source.getLicenseUid();
}




}	// end of class ResultSubSource




/* end of ResultSubSource.java */

