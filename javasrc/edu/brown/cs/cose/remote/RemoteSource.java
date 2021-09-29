/********************************************************************************/
/*										*/
/*		RemoteSource.java						*/
/*										*/
/*	Source from a remote server						*/
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



package edu.brown.cs.cose.remote;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.ivy.xml.IvyXml;

public class RemoteSource implements CoseSource
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	source_name;
private String	display_name;
private String	source_path;
private int	source_length;
private int	source_offset;
private String	source_license;
private String	source_project;
private CoseSource base_source;
private double	source_score;

static private Map<String,RemoteSource> known_sources = new HashMap<>();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static synchronized RemoteSource createSource(Element xml)
{
   if (xml == null) return null;

   String nm = IvyXml.getAttrString(xml,"DISPLAY");
   RemoteSource rs = known_sources.get(nm);
   if (rs == null) {
      rs = new RemoteSource(xml);
      known_sources.put(nm,rs);
    }

   return rs;
}



private RemoteSource(Element xml)
{
   source_name = IvyXml.getAttrString(xml,"NAME");
   display_name = IvyXml.getAttrString(xml,"DISPLAY");
   source_path = IvyXml.getAttrString(xml,"PATH");
   source_length = IvyXml.getAttrInt(xml,"LENGTH");
   source_offset = IvyXml.getAttrInt(xml,"OFFSET");
   source_license = IvyXml.getAttrString(xml,"LICENSE");
   source_project = IvyXml.getAttrString(xml,"PROJECT");
   source_score = IvyXml.getAttrDouble(xml,"SCORE");
   base_source = null;
   Element base = IvyXml.getChild(xml,"BASE");
   if (base != null) {
      base_source = createSource(IvyXml.getChild(base,"SOURCE"));
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override public double getScore()
{
   return source_score;
}


@Override public String getPathName()
{
   return source_path;
}


@Override public String getProjectId()
{
   return source_project;
}


@Override public int getOffset()
{
   return source_offset;
}


@Override public int getLength()
{
   return source_length;
}


@Override public String getLicenseUid()
{
   return source_license;
}


@Override public CoseSource getBaseSource()
{
   return base_source;
}


@Override public String getName()
{
   return source_name;
}


@Override public String getDisplayName()
{
   return display_name;
}



}	// end of class RemoteSource




/* end of RemoteSource.java */

