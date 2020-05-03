/********************************************************************************/
/*                                                                              */
/*              ResultDelta.java                                                */
/*                                                                              */
/*      Contains the delta between two results                                  */
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

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.file.IvyLog;

abstract class ResultDelta
{

/**
 * This class represents an edit to a file made by a transformation.  It keeps
 * track of the changes using a DiffStruct which is line-based.  It returns
 * the text associated with the delta by getting the text for its base result
 * (what it is an edit of), and then applying the edits.  
 *
 * Individual subclasses are responsible for returning the appropriate substring
 * from this file text if the result is not the whole file and for returning the
 * appropriate internal structure (e.g. AST node).
 *
 **/

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ResultBase      base_result;
private DiffStruct      diff_list;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ResultDelta(ResultBase par,String newtext)
{
   base_result = par;
   String origtext = par.getEditText();
   computeDiffs(parse(origtext),parse(newtext));
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

abstract protected String getText();
abstract protected String getKeyText();


String getEditText()
{
   String origtext = base_result.getEditText();
   return apply(origtext);
}


protected ResultBase getBaseResult()            { return base_result; }

CoseResult cloneResult(CoseResult cr,Object o,Object data)    { return null; }
Object getDeltaStructure()                      { return null; }




/********************************************************************************/
/*										*/
/*	Create array of lines to compare					*/
/*										*/
/********************************************************************************/

private static List<String> parse(String s)
{
   ArrayList<String> ret = new ArrayList<>();
   
   int tmp = 0;
   for(int i = 0; i < s.length(); i++) {
      if(s.charAt(i) == '\n') {
	 ret.add(s.substring(tmp, i));
	 tmp = i + 1;
       }
    }
   if (tmp < s.length() - 1) {
      ret.add(s.substring(tmp, s.length()));
    }
   
   return ret;
}




/********************************************************************************/
/*										*/
/*	Difference computation							*/
/*										*/
/********************************************************************************/

private void computeDiffs(List<String> a,List<String> b)
{
   int m = a.size();
   int n = b.size();
   int maxd = m + n;
   int origin = maxd;
   int [] lastd = new int[2*maxd+2];
   DiffStruct [] script = new DiffStruct [2*maxd+2];
   diff_list = null;
   
   int row = 0;
   while (row < m && row < n && a.get(row).equals(b.get(row))) row++;
   
   int col = 0;
   lastd[0+origin] = row;
   script[0+origin] = null;
   
   int lower = (row == m ? origin+1 : origin-1);
   int upper = (row == n ? origin-1 : origin+1);
   if (lower > upper) return;
   
   for (int d = 1; d <= maxd; ++d) {
      for (int k = lower; k <= upper; k+= 2) {
	 if (k == origin-d || (k != origin+d && lastd[k+1] >= lastd[k-1])) {
	    row = lastd[k+1] + 1;
	    script[k] = new DiffStruct(script[k+1],true,null,row-1);
	  }
	 else {
	    row = lastd[k-1];
	    // script[k] = new DiffStruct(script[k-1],false,b.get(row+k-origin-1),row-1);
	    script[k] = new DiffStruct(script[k-1],false,b.get(row+k-origin-1),row);
	  }
	 col = row + k - origin;
	 while (row < m && col < n && a.get(row).equals(b.get(col))) {
	    ++row;
	    ++col;
	  }
	 lastd[k] = row;
	 if (row == m && col == n) {
	    diff_list = script[k].createEdits();
	    return;
	  }
	 if (row == m) lower = k+2;
	 if (col == n) upper = k-2;
       }
      lower = lower-1;
      upper = upper+1;
    }
   
   return;
}




/********************************************************************************/
/*										*/
/*	Application methods -- use the delta					*/
/*										*/
/********************************************************************************/

String apply(String orig)
{
   List<String> tmp = new ArrayList<>();
   tmp = parse(orig);
   tmp = applyDiff(tmp);
   StringBuilder ret = new StringBuilder();
   for (String s : tmp) {
      ret.append(s);
      ret.append("\n");
    }
   
   return ret.toString();
}




private List<String> applyDiff(List<String> orig)
{
   List<String> ret = new ArrayList<>();
   
   int count = 0;
   for (DiffStruct ds = diff_list; ds != null; ds = ds.getNext()) {
      while(ds.getIndex() > count) {
         if (orig.size() <= count) {
            IvyLog.logE("COSE","PROBLEM WITH DIFFS");
          }
	 ret.add(orig.get(count));
	 count++;
       }
      int ndel = ds.getNumDelete();
      
      if (ndel == 0) ret.add(ds.getData());
      else count += ndel;
    }
   
   while (count < orig.size()) {
      ret.add(orig.get(count));
      ++count;
    }
   
   return ret;
}



/********************************************************************************/
/*										*/
/*	Hold difference information						*/
/*										*/
/********************************************************************************/

private static class DiffStruct {
   
   private int delete_count;
   private String replace_data;
   private int line_index;
   private DiffStruct next_edit;
   
   public DiffStruct(DiffStruct prior,boolean del, String dat, int i) {
      next_edit = prior;
      delete_count = (del ? 1 : 0);
      replace_data = dat;
      line_index = i;
    }
   
   public int getNumDelete()		{ return delete_count; }
   
   public String getData()		{ return replace_data; }
   
   public int getIndex()		{ return line_index; }
   
   public DiffStruct getNext()		{ return next_edit; }
   
   DiffStruct createEdits() {
      DiffStruct shead = this;
      DiffStruct ep = null;
      DiffStruct behind = null;
      while (shead != null) {
         behind = ep;
         if (ep != null && ep.delete_count > 0 && shead.delete_count > 0 &&
               ep.line_index == shead.line_index + 1) {
            shead.delete_count += ep.delete_count;
            behind = ep.next_edit;
          }
         ep = shead;
         shead = shead.next_edit;
         ep.next_edit = behind;
       }
      return ep;
    }
   
   private void stringify(StringBuilder buf) {
      buf.append("@");
      buf.append(line_index);
      buf.append(" ");
      if (delete_count > 0) {
         buf.append("DELETE ");
         buf.append(delete_count);
         buf.append(" ");
       }
      if (replace_data != null) {
         buf.append("INSERT ");
         buf.append(replace_data);
       }
      buf.append("\n");
      if (next_edit != null) next_edit.stringify(buf);
    }
   
   @Override public String toString() {
      StringBuilder buf = new StringBuilder();
      stringify(buf);
      return buf.toString();
    }
   
}	// end of inner class DiffStruct




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return diff_list.toString();
}



}       // end of class ResultDelta




/* end of ResultDelta.java */

