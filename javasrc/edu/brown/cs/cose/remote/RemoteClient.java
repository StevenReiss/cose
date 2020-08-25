/********************************************************************************/
/*                                                                              */
/*              RemoteClient.java                                               */
/*                                                                              */
/*      Handle sending request to remote server                                 */
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



package edu.brown.cs.cose.remote;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResultSet;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlReader;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class RemoteClient implements RemoteConstants, AutoCloseable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Socket          remote_conn;
private IvyXmlReader    remote_reader;
private PrintWriter     remote_writer;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RemoteClient() throws IOException
{ 
   remote_conn = new Socket(COSE_REMOTE_HOST,COSE_REMOTE_PORT);
   remote_reader = new IvyXmlReader(remote_conn.getInputStream());
   remote_writer = new PrintWriter(new OutputStreamWriter(remote_conn.getOutputStream()));
   
}




/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

public CoseResultSet computeSearchResults(CoseRequest cr,CoseResultSet crs)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("COSE");
   xw.field("COMMAND","SEARCH");
   RemoteRequest.createRequest(cr,xw);
   xw.end("COSE");
   Element rslt = sendMessage(xw.toString());
   xw.close();
   if (rslt == null) return null;
   // create result set from results
   return crs;
}


@Override public void close()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("COSE");
   xw.field("COMMAND","EXIT");
   xw.end("COSE");
   sendMessage(xw.toString());
   xw.close();
}



/********************************************************************************/
/*                                                                              */
/*      Send message and recieve result                                         */
/*                                                                              */
/********************************************************************************/

private Element sendMessage(String msg)
{
   try {
      remote_writer.println(msg);
      String xmls = remote_reader.readXml();
      if (xmls == null) return null;
      return IvyXml.convertStringToXml(xmls);
    }
   catch (IOException e) {
      return null;
    }
}

}       // end of class RemoteClient




/* end of RemoteClient.java */

