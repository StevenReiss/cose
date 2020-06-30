/********************************************************************************/
/*										*/
/*		KeySearchCache.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.cose.keysearch;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class KeySearchCache implements KeySearchConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		base_directory;
private boolean 	use_cache;

private static KeySearchCache cur_cache = new KeySearchCache();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static KeySearchCache getCache()
{
   return cur_cache;
}



private  KeySearchCache()
{
   base_directory = new File(CACHE_DIRECTORY);
   use_cache = true;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setUseCache(boolean fg)		{ use_cache = fg; }



/********************************************************************************/
/*										*/
/*	Main entry points							*/
/*										*/
/********************************************************************************/

public BufferedReader getReader(URL url,boolean cache,boolean reread)
	throws IOException
{
   return getReader(url,null,cache,reread);
}



public BufferedReader getReader(URL url,KeySearchAuthorizer auth,boolean cache,boolean reread)
	throws IOException
{
   if (!use_cache || !cache) {
      return getURLReader(url,auth);
    }

   File dir = getDirectory(url,auth,reread);
   if (dir != null) {
      File df = new File(dir,CACHE_DATA_FILE);
      return new BufferedReader(new FileReader(df));
    }

   return getURLReader(url,auth);
}



public InputStream getInputStream(URL url,boolean cache,boolean reread)
	throws IOException
{
   return getInputStream(url,null,cache,reread);
}




public InputStream getInputStream(URL url,KeySearchAuthorizer auth,boolean cache,boolean reread)
	throws IOException
{
   if (!use_cache || !cache) {
      return getURLStream(url,auth);
    }

   File dir = getDirectory(url,auth,reread);
   if (dir != null) {
      File df = new File(dir,CACHE_DATA_FILE);
      return new FileInputStream(df);
    }

   return getURLStream(url,auth);
}


public InputStream getCacheStream(URL url,KeySearchAuthorizer auth,long dlm) throws IOException
{
   File dir = getDirectory(url,auth,false);
   if (dir != null) {
      File df = new File(dir,CACHE_DATA_FILE);
      long ddlm = df.lastModified();
      if (ddlm <= dlm) return null;
      return new FileInputStream(df);
    }
   return getURLStream(url,auth);
}



public boolean checkIfForced(URL url) throws IOException
{
   File dir = getDirectory(url,null,false);
   if (dir == null) return false;
   File f1 = new File(dir,CACHE_URL_FILE);
   try {
      BufferedReader br = new BufferedReader(new FileReader(f1));
      br.readLine();	// skip the url line
      String frc = br.readLine();
      br.close();
      if (frc != null && frc.equals("FORCE")) return true;
    }
   catch (IOException e) { }
   return false;
}




public void markForced(URL url) throws IOException
{
   File dir = getDirectory(url,null,false);
   if (dir == null) return;

   File f1 = new File(dir,CACHE_URL_FILE);
   try {
      FileWriter fw = new FileWriter(f1,true);
      fw.write("FORCE\n");
      fw.close();
    }
   catch (IOException e) {
      IvyLog.logE("COSE","Problem with mark forced: " + e);
    }
   IvyLog.logI("COSE","Mark Forced " + dir + " FOR " + url);
}





/********************************************************************************/
/*										*/
/*	HTTP methods								*/
/*										*/
/********************************************************************************/

private BufferedReader getURLReader(URL url,KeySearchAuthorizer auth) throws IOException
{
   InputStream ins = getURLStream(url,auth);
   return new BufferedReader(new InputStreamReader(ins,"UTF-8"));
}



private InputStream getURLStream(URL url,KeySearchAuthorizer auth) throws IOException
{
   IvyLog.logD("COSE","GET " + url);
   URLConnection uc = url.openConnection();
   uc.setReadTimeout(15000);
   uc.setRequestProperty("User-Agent","s6");
   if (auth != null) {
      String auths = auth.getAuthorization();
      if (auths != null) uc.setRequestProperty("Authorization",auths);
    }
   // uc.setAllowUserInteraction(false);
   // uc.setDoOutput(false);
   // uc.addRequestProperty("Connection","close");
   return uc.getInputStream();
}



/********************************************************************************/
/*										*/
/*	Directory methods							*/
/*										*/
/********************************************************************************/

private File getDirectory(URL u,KeySearchAuthorizer auth,boolean reread) throws IOException
{
   if (u == null) return null;

   String urlname = u.toExternalForm().toLowerCase();
   if (urlname.length() == 0) return null;
   int hvl = 0;
   try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] dvl = md.digest(urlname.getBytes());
      for (int i = 0; i < dvl.length; ++i) {
	 int j = i % 4;
	 int x = (dvl[i] & 0xff);
	 hvl ^= (x << (j * 8));
       }
    }
   catch (NoSuchAlgorithmException e) {
      hvl = urlname.hashCode();
    }
   hvl &= 0x7fffffff;
   int h1 = hvl % 512;
   int h2 = (hvl / 512) % 512;
   int h3 = (hvl / 512 / 512) % 4096;

   File dtop = new File(base_directory,"S6$" + h1);   
   dtop = new File(dtop,"S6$" + h2);
   if (!dtop.exists() && !dtop.mkdirs()) return null;

   String dir0 = "S6$" + h3;
   for (int i = 0; i < 26 * 27; ++i) {
      StringBuilder sb = new StringBuilder(dir0);
      int j0 = i % 26;
      int j1 = i / 26;
      if (j1 > 0) sb.append((char) ('a' + j1 - 1));
      sb.append((char) ('a' + j0));
      File dir = new File(dtop,sb.toString());
      File urlf = new File(dir,CACHE_URL_FILE);
      File dataf = new File(dir,CACHE_DATA_FILE);
      boolean fg = dir.mkdirs();
      if (!fg && !dir.exists()) return null;
      if (!fg && dir.exists() && dir.listFiles().length == 0
	    && dir.lastModified() < System.currentTimeMillis() - 300000) fg = true;
      if (CACHE_TIME_OUT != 0 && dataf.exists()
	       && dataf.lastModified() < System.currentTimeMillis() - CACHE_TIME_OUT) {
	 fg = true;
       }

      if (fg) { // we own the directory
	 InputStream br = null;
	 try {
	    br = getURLStream(u,auth); // throw exception on bad url
	  }
         catch (FileNotFoundException e) {
            byte [] buf = new byte[0];
            br = new ByteArrayInputStream(buf);
          }
	 catch (IOException e) {
	    dir.delete();
	    throw e;
	  }
	 try (FileOutputStream dw = new FileOutputStream(dataf)) {
	    byte[] buf = new byte[8192];
	    for (;;) {
	       int ln = br.read(buf);
	       if (ln < 0) break;
	       dw.write(buf, 0, ln);
	     }
	    br.close();
	    FileWriter fw = new FileWriter(urlf);
	    fw.write(u.toExternalForm() + "\n");
	    fw.close();
	    IvyFile.updatePermissions(dir,0777);
	    return dir;
	  }
	 catch (IOException e) {
	    IvyLog.logE("COSE","Failed to create URL cache file: " + e);
	    return null;
	  }
       }
      else { // directory already exists
	 boolean urlfg = checkUrlFile(urlf, u);
	 if (urlfg) {
	    IvyLog.logI("COSE","Use CACHE: " + dir + " FOR " + u);
	    return dir;
	  }
	 if (!urlf.exists()) {
	    IvyLog.logE("COSE","Bad cache directory: " + dir);
	    dir.delete();
	    return null;
	  }
       }
    }

   return null;
}


private boolean checkUrlFile(File urlf,URL u)
{
   for (int k = 0; k < 20 && !urlf.exists(); ++k) {
      try {
	 Thread.sleep(1);
       }
      catch (InterruptedException e) { }
    }
   if (!urlf.exists()) return false;
   try {
      BufferedReader br = new BufferedReader(new FileReader(urlf));
      String ln = br.readLine();
      br.close();
      if (ln == null) return false;
      ln = ln.trim();
      String s1 = u.toExternalForm();
      if (ln.equalsIgnoreCase(s1)) return true;
      if (s1.length() == ln.length()) {
         s1 = s1.toLowerCase();
         ln = ln.toLowerCase();
         boolean match = true;
         for (int i = 0; match && i < s1.length(); ++i) {
            if (s1.charAt(i) == ln.charAt(i)) continue;
            if (s1.charAt(i) >= 256 && ln.charAt(i) == '?') continue;
            match = false;
          }
         if (match) return true;
       }
    }
   catch (IOException e) {
      IvyLog.logE("COSE","Problem reading URL cache file",e);
    }
   return false;
}




}	// end of class KeySearchCache




/* end of KeySearchCache.java */

