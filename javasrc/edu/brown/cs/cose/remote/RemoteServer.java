/********************************************************************************/
/*                                                                              */
/*              RemoteServer.java                                               */
/*                                                                              */
/*      Server for handling remote search requests                              */
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
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseDefaultResultSet;
import edu.brown.cs.cose.cosecommon.CoseMaster;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchType;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlReaderThread;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class RemoteServer implements RemoteConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main Program                                                            */
/*                                                                              */
/********************************************************************************/

public void main(String ... args)
{
   RemoteServer rs = new RemoteServer(args);
   rs.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int     port_number;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RemoteServer(String [] args)
{
   port_number = COSE_REMOTE_PORT;
   
   scanArgs(args);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   ServerThread sthread = new ServerThread();
   sthread.start();
}



/********************************************************************************/
/*                                                                              */
/*      Argument processing                                                     */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if (args[i].startsWith("-p") && i + 1 < args.length) {         // -port <port>
            try {
               port_number = Integer.parseInt(args[++i]);
             }
            catch (NumberFormatException e) {
               badArgs();
             }
          }
         else badArgs();
       }
      else badArgs();
    }
}


private void badArgs()
{
   System.err.println("COSESERVER: CoseServer [-p <port>]");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Command handling                                                        */
/*                                                                              */
/********************************************************************************/

private boolean handleCommand(Element xml,IvyXmlWriter xw)
{
   String cmd = IvyXml.getAttrString(xml,"COMMAND");
   if (cmd == null) {
      xw.textElement("ERROR","No command");
      return false;
    }
   switch (cmd) {
      case "PING" :
         xw.textElement("PONG",null);
         break;
      case "SEARCH" :
         Element rqelt = IvyXml.getChild(xml,"REQUEST");
         RemoteRequest cr = new RemoteRequest(rqelt);
         switch (cr.getCoseSearchType()) {
            case CLASS :
            case TESTCLASS :
            case METHOD :
               cr.setCoseSearchType(CoseSearchType.FILE);
               break;
          }
         CoseMaster cm = CoseMaster.createMaster(cr);
         CoseDefaultResultSet rset = new CoseDefaultResultSet();
         rset = (CoseDefaultResultSet) cm.computeSearchResults(rset);
         outputResults(rset,xw);
         break;
      case "EXIT" :
         return true;
      default :
         xw.textElement("ERROR","Unknown command " + cmd);
         break;
    }
   return false;
}


private void outputResults(CoseDefaultResultSet rset,IvyXmlWriter xw)
{
   xw.begin("RESULTS");
   xw.field("COUNT",rset.getResults().size());
   xw.field("REMOVED",rset.getNumberRemoved());
   for (CoseResult cr : rset.getResults()) {
      cr.outputXml(xw);
    }
   xw.end("RESULTS");
}



/********************************************************************************/
/*                                                                              */
/*      Server Socket Thread                                                    */
/*                                                                              */
/********************************************************************************/

private class ServerThread extends Thread {

   private ServerSocket server_socket;

   ServerThread() {
      super("CoseRemoteServerThread");
      try {
         server_socket = new ServerSocket(port_number);
       }
      catch (IOException e) {
         System.err.println("COSESERVER: Can't create server socket on " + port_number);
         System.exit(1);
       }
    }
   
   @Override public void run() {
      for ( ; ; ) {
         try { 
            Socket client = server_socket.accept();
            createClient(client);
          }
         catch (IOException e) {
            System.err.println("COSESERVER: Error on accept");
            break;
          }
       }
      System.exit(0);
    }
   
}       // end of inner class ServerThread



/********************************************************************************/
/*                                                                              */
/*      Client management                                                       */
/*                                                                              */
/********************************************************************************/

private void createClient(Socket s)
{
   try {
      ClientThread cthread = new ClientThread(s);
      cthread.start();
    }
   catch (IOException e) {
      System.err.println("COSESERVER: Problem creating client: " + e);
    }
}




private class ClientThread extends IvyXmlReaderThread {

   private Socket client_socket;
   private IvyXmlWriter xml_writer;
   
   ClientThread(Socket s) throws IOException {
      super("CoseRemoteClient_" + s.getRemoteSocketAddress(),
            new InputStreamReader(s.getInputStream()));
      client_socket = s;
      xml_writer = new IvyXmlWriter(s.getOutputStream());
      xml_writer.setSingleLine(true);
    }
   
   @Override protected void processXmlMessage(String msg) {
      Element xml = IvyXml.convertStringToXml(msg,true);
      try {
         boolean done = handleCommand(xml,xml_writer);
         if (done) {
            try {
               client_socket.close();
             }
            catch (IOException e) { }
          }
       }
      catch (Throwable t) {
         xml_writer.textElement("ERROR",t.toString());
       }
      xml_writer.println();
      xml_writer.flush();
    }
   
   @Override synchronized protected void processDone() {
      if (client_socket == null) return;
      try {
         client_socket.close();
         client_socket = null;
       }
      catch (IOException e) { }
    }
   
   @Override protected void processIoError(IOException e) {
      processDone();
    }
   
}       // end of inner class ClientThread




}       // end of class RemoteServer




/* end of RemoteServer.java */

