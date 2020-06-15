/********************************************************************************/
/*                                                                              */
/*              ScorerAnalyzerJava.java                                         */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.cose.scorer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.cose.cosecommon.CoseSignature;
import edu.brown.cs.cose.cosecommon.CoseRequest.CoseKeywordSet;
import edu.brown.cs.ivy.file.IvyStringDiff;
import edu.brown.cs.ivy.jcomp.JcompAst;

class ScorerAnalyzerJava extends ScorerAnalyzer
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected CoseResultType  result_type;
private ASTNode           for_node;

private static List<String>      standard_imports;

private static Set<String> primitive_types;

static {
   primitive_types = new HashSet<>();
   primitive_types.add("void");
   primitive_types.add("byte");
   primitive_types.add("short");
   primitive_types.add("char");
   primitive_types.add("int");
   primitive_types.add("long");
   primitive_types.add("float");
   primitive_types.add("double");
   primitive_types.add("boolean");
}


static {
   standard_imports = new ArrayList<>();
   standard_imports.add("com.sun.");
   standard_imports.add("java.");
   standard_imports.add("javafx.");
   standard_imports.add("javax.");
   standard_imports.add("jdk.");
   standard_imports.add("org.ietf.");
   standard_imports.add("org.omg.");
   standard_imports.add("org.w3c.dom.");
   standard_imports.add("org.xml.sax.");
}


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScorerAnalyzerJava(CoseRequest cr,ASTNode n)
{
   super(cr);
   result_type = null;
   for_node = n;
}



/********************************************************************************/
/*                                                                              */
/*      Main analysis entries                                                   */
/*                                                                              */
/********************************************************************************/

@Override public synchronized CoseScores analyzeProperties(CoseResult cr)
{
   result_type = cr.getResultType();
   ASTNode obj;
   String text;
   if (for_node == null) {
      obj = (ASTNode) cr.getStructure();
      text = cr.getText();
    }
   else {
      obj = for_node;
      text = for_node.toString();
    }
  
   value_map.put("RANK",cr.getSource().getScore()); 
   
   checkTestCase(obj);
   checkCodeComplexity(obj,text);
   handleFileMeasures(obj);
   handlePropertyMeasures(obj);
   handleKeywordMatch(text,obj);
   handleTargetMatch(obj);
   
   double x = value_map.getDouble("TERMMATCHES");
   double y = value_map.getDouble("NODES");
   if (y == 0) x = 0;
   else x = x/y;
   value_map.put("TERMDENSITY",x);
   
   return value_map;
}

@Override public boolean isTestCase(String src)
{
   ASTNode obj = JcompAst.parseSourceFile(src);
   checkTestCase(obj);
   boolean rslt = value_map.getBoolean("TESTCASE");
   return rslt;
}





/********************************************************************************/
/*                                                                              */
/*      Check if test case                                                      */
/*                                                                              */
/********************************************************************************/

private void checkTestCase(ASTNode obj)
{
   Boolean fg = null;
   
   switch (obj.getNodeType()) {
      case ASTNode.ENUM_DECLARATION :
         fg = false;
         break;
      case ASTNode.ANNOTATION_TYPE_DECLARATION :
         fg = false;
         break;
      case ASTNode.TYPE_DECLARATION :
         fg = isTestClass((TypeDeclaration) obj);
         break;
      case ASTNode.COMPILATION_UNIT :
         if (result_type == CoseResultType.PACKAGE) fg = isTestPackage((CompilationUnit) obj);
         else fg = isTestFile((CompilationUnit) obj);
         break;
      case ASTNode.METHOD_DECLARATION :
         fg = isTestMethod((MethodDeclaration) obj);
         break;
    }
   
   if (fg != null) value_map.put("TESTCASE",fg);
}



private boolean isTestMethod(MethodDeclaration md)
{
   if (md.getName().getIdentifier().startsWith("test") &&
         Modifier.isPublic(md.getModifiers()) &&
         md.parameters().size() == 0) return true;
   
   for (Object o : md.modifiers()) {
      if (o instanceof Annotation) {
         Annotation an = (Annotation) o;
         String nm = an.getTypeName().getFullyQualifiedName();
         if (nm.equals("org.junit.Test") || nm.equals("Test")) return true;
       }
    }
   
   return false;
}


private boolean isTestClass(TypeDeclaration td)
{
   Type t = td.getSuperclassType();
   if (t != null) {
      String tnm = t.toString();
      if (tnm.endsWith("TestCase")) return true;
    }
   
   for (Object o : td.bodyDeclarations()) {
      if (o instanceof MethodDeclaration) {
         if (isTestMethod((MethodDeclaration) o)) return true;
       }
    }
   
   return false;
}



private boolean isTestFile(CompilationUnit cu)
{
   for (Object o : cu.types()) {
      if (o instanceof TypeDeclaration) {
         if (isTestClass((TypeDeclaration) o)) return true;
       }
    }
   
   return false;
}

private boolean isTestPackage(CompilationUnit cu)
{
   PackageDeclaration pd = cu.getPackage();
   if (pd != null) {
      String pnm = pd.getName().getFullyQualifiedName();
      if (pnm.contains("test")) return true;
    }
   
   int ntest = 0;
   int nclass = 0;
   for (Object o : cu.types()) {
      if (o instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) o;
         if (td.isInterface()) continue;
         if (isTestClass(td)) ++ntest;
         ++nclass;
       }
    }
   
   if (ntest > nclass/2) return true;
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Code complexity measures                                                */
/*                                                                              */
/********************************************************************************/

private void checkCodeComplexity(ASTNode n,String text)
{
   ComplexityMeasure cm = new ComplexityMeasure();
   n.accept(cm);
   value_map.put("NODES",cm.getNodeCount());
   value_map.put("LINES",getCodeLines(text));
   value_map.put("TRIVIAL",cm.getNodeCount() <= 10);
   if (n instanceof MethodDeclaration) 
      checkLoopComplexity((MethodDeclaration) n);
   else if (n instanceof AbstractTypeDeclaration) 
      checkClassComplexity((AbstractTypeDeclaration) n);
   else if (n instanceof CompilationUnit) 
      checkPackageComplexity((CompilationUnit) n);
}



private static class ComplexityMeasure extends ASTVisitor {

   private int num_nodes;

   ComplexityMeasure() {
      num_nodes = 0;
    }
   
   int getNodeCount()				{ return num_nodes; }
   
   public void preVisit(ASTNode n)		{ ++num_nodes; }
   
}	// end of subclass ComplexityMeasure


private int getCodeLines(String code)
{
   StringTokenizer tok = new StringTokenizer(code,"\n");
   int codelines = 0;
   boolean incmmt = false;
   while (tok.hasMoreTokens()) {
      String lin = tok.nextToken();
      boolean hascode = false;
      for (int i = 0; i < lin.length() && !hascode; ++i) {
	 int ch = lin.charAt(i);
	 if (Character.isWhitespace(ch)) continue;
	 if (incmmt) {
	    if (ch == '*' && i+1 < lin.length() && lin.charAt(i+1) == '/') {
	       ++i;
	       incmmt = false;
	     }
	  }
	 else if (ch == '/' && i+1 < lin.length()) {
	    if (lin.charAt(i+1) == '/') break;
	    else if (lin.charAt(i+1) == '*') {
	       incmmt = true;
	     }
	  }
	 else hascode = true;
       }
      if (hascode) ++codelines;
    }
   
   return codelines;
}


private void checkLoopComplexity(MethodDeclaration md)
{
    boolean first = true;
    List<MethodDeclaration> todo = new LinkedList<>();
    todo.add(md);
    LoopVisitor lv = new LoopVisitor();
    while (!todo.isEmpty()) {
       MethodDeclaration domd = todo.remove(0);
       domd.accept(lv);
       if (first) {
          value_map.put("BASELOOPS",lv.getLoopCount());
          value_map.put("BASECALLS",lv.getCallCount());
          first = false;
        }
       todo.addAll(lv.getTodo());
     }
    value_map.put("LOOPS",lv.getLoopCount());
    value_map.put("CALLS",lv.getCallCount());
    value_map.put("NO_LOOPS",lv.getLoopCount() == 0);
    value_map.put("NO_CALLS",lv.getCallCount() == 0);
}



private void checkClassComplexity(AbstractTypeDeclaration atd)
{
   int nmethod = 0;
   int nfield = 0;
   int ntype = 0;
   int naccess = 0;
   
   for (Object o : atd.bodyDeclarations()) {
      BodyDeclaration bd = (BodyDeclaration) o;
      if (!Modifier.isPrivate(bd.getModifiers())) ++naccess;
      switch (bd.getNodeType()) {
         case ASTNode.FIELD_DECLARATION :
            if (!Modifier.isFinal(bd.getModifiers()) &&
                 !Modifier.isStatic(bd.getModifiers())) {
               ++nfield;
             }
            break;
         case ASTNode.ENUM_DECLARATION :
         case ASTNode.TYPE_DECLARATION :
            ++ntype;
            break;
         case ASTNode.METHOD_DECLARATION :
            ++nmethod;
            break;
         default :
            break;
       }
    }
   
   value_map.put("METHODS",nmethod);
   value_map.put("FIELDS",nfield);
   value_map.put("INNER",ntype);
   value_map.put("ACCESSIBLE",naccess);
}



/********************************************************************************/
/*                                                                              */
/*      Package complexity results                                              */
/*                                                                              */
/********************************************************************************/

private void checkPackageComplexity(CompilationUnit cu)
{
   PackageStats ps = new PackageStats();
   cu.accept(ps);
   ps.saveStatistics();
}


private class PackageStats extends ASTVisitor {

   private int num_types;
   private int num_inner;
   private int num_iface;
   private int num_method;
   private int num_field;
   private int nest_level;
   
   PackageStats() {
      num_types = 0;
      num_inner = 0;
      num_iface = 0;
      num_method = 0;
      num_field = 0;
      nest_level = 0;
    }
   
   void saveStatistics() {
      value_map.put("METHODS",num_method);
      value_map.put("FIELDS",num_field);
      value_map.put("INNER",num_inner);
      value_map.put("INTERFACES",num_iface);
      value_map.put("TYPES",num_types);
    }
   
   @Override public boolean visit(TypeDeclaration td) {
      ++num_types;
      if (nest_level > 0) ++num_inner;
      if (td.isInterface()) ++num_iface;
      ++nest_level;
      return true;
    }
   
   @Override public void endVisit(TypeDeclaration td) {
      --nest_level;
    }
   
   @Override public boolean visit(EnumDeclaration ed) {
      ++num_types;
      return false;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      ++num_method;
      return false;
    }
   
   @Override public boolean visit(FieldDeclaration fd) {
      num_field += fd.fragments().size();
      return false;
    }
   
}       // end of inner class PackageStats



private static class LoopVisitor extends ASTVisitor {

   private Set<MethodDeclaration> todo_methods;
   private Set<MethodDeclaration> done_methods;
   private int loop_count;
   private int num_calls;
   
   LoopVisitor() {
      done_methods = new HashSet<>();
      todo_methods = new HashSet<>();
      loop_count = 0;
      num_calls = 0;
    }
   
   Set<MethodDeclaration> getTodo() {
      Set<MethodDeclaration> rslt = todo_methods;
      todo_methods.clear();
      return rslt;
    }
   int getLoopCount()                           { return loop_count; }
   int getCallCount()                           { return num_calls; }
   
   @Override public boolean visit(MethodDeclaration md) {
      if (done_methods.contains(md)) return false;
      done_methods.add(md);
      return true;
    }
   @Override public void endVisit(WhileStatement n) {
      ++loop_count;
    }
   @Override public void endVisit(DoStatement n) {
      ++loop_count;
    }
   @Override public void endVisit(ForStatement n) {
      ++loop_count;
    }
   @Override public void endVisit(IfStatement n) {
      ++loop_count;
    }
   
   @Override public void endVisit(EnhancedForStatement n) {
      ++loop_count;
    }
   
   @Override public void endVisit(MethodInvocation mi) {
      String nm = mi.getName().getIdentifier();
      boolean fnd = false;
      if (mi.getExpression() == null || mi.getExpression() instanceof ThisExpression) {
         AbstractTypeDeclaration atd = null;
         for (ASTNode p = mi; p != null; p = p.getParent()) {
            if (p instanceof AbstractTypeDeclaration) {
               atd = (AbstractTypeDeclaration) p;
               break;
             }
          }
         if (atd != null) {
            for (Object o : atd.bodyDeclarations()) {
               if (o instanceof MethodDeclaration) {
                  MethodDeclaration md = (MethodDeclaration) o;
                  if (md.getName().getIdentifier().equals(nm)) {
                     fnd = true;
                     if (done_methods.contains(md)) continue;
                     else todo_methods.add(md);
                   }
                }
             }
          }
       }
      if (!fnd) ++num_calls;
    }
   
}       // end of inner class LoopVisitor



/********************************************************************************/
/*                                                                              */
/*      File dependency measures                                                */
/*                                                                              */
/********************************************************************************/

private void handleFileMeasures(ASTNode obj)
{
   CompilationUnit cu = (CompilationUnit) obj.getRoot();
   int numimport = 0;
   int numdemandimport = 0;
   int numstdimport = 0;
   
   String pkg = null;
   if (cu.getPackage() != null) {
      pkg = cu.getPackage().getName().getFullyQualifiedName();
    }
   
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      String nm = id.getName().getFullyQualifiedName();
      boolean isstd = false;
      for (String s : standard_imports) {
         if (nm.startsWith(s)) isstd = true;
       }
      if (isstd) numstdimport++;
      else if (id.isOnDemand()) numdemandimport++;
      else numimport++;
    }
   
   value_map.put("PACKAGE",(pkg != null));
   value_map.put("IMPORTS",numimport);
   value_map.put("DEMANDIMPORTS",numdemandimport);
   value_map.put("STDIMPORT",numstdimport);
}



/********************************************************************************/
/*                                                                              */
/*      Property methods                                                        */
/*                                                                              */
/********************************************************************************/

private void handlePropertyMeasures(ASTNode obj)
{
   List<?> mods = null;
   switch (obj.getNodeType()) {
      case ASTNode.ENUM_DECLARATION :
      case ASTNode.ANNOTATION_TYPE_DECLARATION :
      case ASTNode.TYPE_DECLARATION :
         mods = ((AbstractTypeDeclaration) obj).modifiers();
         break;
      case ASTNode.COMPILATION_UNIT :
         for (Object o : ((CompilationUnit) obj).types()) {
            AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
            mods = atd.modifiers();
            break;
          }
         break;
      case ASTNode.METHOD_DECLARATION :
         mods = ((MethodDeclaration) obj).modifiers();
         break;
    }  
   if (mods != null) {
      for (Object o : mods) {
         if (o instanceof Modifier) {
            Modifier md = (Modifier) o;
            value_map.put("ABSTRACT",md.isAbstract());
            value_map.put("DEFAULT",md.isDefault());
            value_map.put("FINAL",md.isFinal());
            value_map.put("PRIVATE",md.isPrivate());
            value_map.put("PROTECTED",md.isProtected());
            value_map.put("PUBLIC",md.isPublic());
            value_map.put("PKGPROT",!md.isPrivate() && !md.isProtected()&& !md.isPublic());
            value_map.put("STATIC",md.isStatic());
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle keyword matching                                                 */
/*                                                                              */
/********************************************************************************/

private void handleKeywordMatch(String txt,ASTNode obj)
{
   int maxmatch = 10000;
   switch (result_type) {
      case FILE :
      case CLASS :
         maxmatch = 100;
         break;
      case METHOD :
         maxmatch = 10;
         break;
      case PACKAGE :
         maxmatch = 5000;
         break;
    }
   
   String ttl = "";
   switch (obj.getNodeType()) {
      case ASTNode.METHOD_DECLARATION :
         MethodDeclaration md = (MethodDeclaration) obj;
         ttl = md.getName().getIdentifier();
         break;
      case ASTNode.TYPE_DECLARATION :
      case ASTNode.ANNOTATION_TYPE_DECLARATION :
      case ASTNode.ENUM_DECLARATION :
         AbstractTypeDeclaration atd = (AbstractTypeDeclaration) obj;
         ttl = atd.getName().getFullyQualifiedName();
         break;
      case ASTNode.COMPILATION_UNIT :
         CompilationUnit cu = (CompilationUnit) obj;
         if (cu.getPackage() != null) {
            ttl = cu.getPackage().getName().getFullyQualifiedName();
          }
         break;
      default :
         break;
    }
   if (ttl != null) ttl = ttl.toLowerCase();
   
   txt = txt.toLowerCase();
   
   int matches = 0;
   int nkey = 0;
   int keyused = 0;
   int ntitle = 0;
   
   for (CoseKeywordSet cks : getKeywordSets()) {
      for (String s : cks.getWords()) {
         int mat = countUses(txt,s);
         matches += mat;
         int kmat = countUses(ttl,s);
         ntitle += kmat;
         ++nkey;
         if (mat > 0) ++keyused;
       }
    }
   if (matches > maxmatch) matches = maxmatch;
   value_map.put("MATCHES",matches);
   value_map.put("TITLEMATCH",ntitle);
   double v = keyused;
   v /= nkey;
   value_map.put("KEYMATCH",v);
   
   int nterm = 0;
   int ttlterm = 0;
   int keyterm = 0;      
   int termused = 0;
   for (String s : getKeyTerms()) {
      int mat = countUses(txt,s);
      keyterm += mat;
      int kmat = countUses(ttl,s);
      ttlterm += kmat;
      ++nterm;
      if (mat > 0) ++termused;
    }
   if (keyterm > maxmatch) keyterm = maxmatch;
   value_map.put("TERMMATCHES",keyterm);
   v = termused;
   v /= nterm;
   value_map.put("TERMMATCH",v);
   value_map.put("TERMTITLEMATCH",ttlterm);
}



private int countUses(String txt,String key)
{
   if (txt == null) return 0;
   
   int matches = 0;
   key = key.toLowerCase();
   
   int idx0 = 0;
   while (idx0 >= 0) {
      int idx = txt.indexOf(key,idx0);
      if (idx < 0) break;
      ++matches;
      idx0 = idx + key.length();
    }
   
   return matches;
}



/********************************************************************************/
/*                                                                              */
/*      Handle target matching                                                  */
/*                                                                              */
/********************************************************************************/

private void handleTargetMatch(ASTNode obj)
{
   CoseSignature sgn = getSignature();
   if (sgn == null) return;
   
   switch (getSearchType()) {
      case METHOD :
         handleTargetMethodMatch((CoseSignature.CoseMethodSignature) sgn,(MethodDeclaration) obj);
         break;
      case CLASS :
      case FILE :
         break;
      case PACKAGE :
         break;
      case TESTCLASS :
      case ANDROIDUI :
         break;
    }
}



private void handleTargetMethodMatch(CoseSignature.CoseMethodSignature sgn,MethodDeclaration md)
{
   if (sgn == null) return;
   
   String nm0 = sgn.getName();
   String nm1 = md.getName().getIdentifier();
   double c = IvyStringDiff.normalizedStringDiff(nm0,nm1);
   value_map.put("NAMEMATCH",c);
   
   int argc0 = sgn.getParameterTypeNames().size();
   int argc1 = md.parameters().size();
   int diff = argc0 - argc1;
   if (diff > 3) diff = 3;
   if (diff < -3) diff = -3;
   value_map.put("ARGCDIFF",diff);
   
   String rt0 = sgn.getReturnTypeName();
   String rt1 = getTypeName(md,md.getReturnType2());
   value_map.put("RETURNMATCH",compareTypes(rt0,rt1));
   
   double cval = 100;
   for (int i = 0; i < sgn.getParameterTypeNames().size(); ++i) {
      double best = 100;
      String pnam = sgn.getParameterNames().get(i);
      String ptyp = sgn.getParameterTypeNames().get(i);
      for (int j = 0; j < md.parameters().size(); ++j) {
         SingleVariableDeclaration svd = (SingleVariableDeclaration) md.parameters().get(j);
         
         double c0 = 1.0 - IvyStringDiff.normalizedStringDiff(svd.getName().getIdentifier(),pnam);
         double c1 = compareTypes(getTypeName(md,svd.getType()),ptyp);
         double c2 = (c0 + c1) / 2.0;
         if (c2 < best) best = c2;
       }
      if (best != 100) {
         if (cval == 100) cval = 0;
         cval += best;
       }
    }
   if (cval != 100) {
      cval /= argc0;
      value_map.put("ARGSMATCH",cval);
    }
}




private String getTypeName(ASTNode base,Type t0)
{
   String tnm = null;
   
   if (t0 == null) return null;
   
   switch (t0.getNodeType()) {
      case ASTNode.PRIMITIVE_TYPE :
         break;
      case ASTNode.ARRAY_TYPE :
         ArrayType atyp = (ArrayType) t0;
         String elttyp = getTypeName(base,atyp.getElementType());
         for (int i = 0; i < atyp.getDimensions(); ++i) {
            elttyp += "[]";
          }
         return elttyp;
      case ASTNode.PARAMETERIZED_TYPE :
         ParameterizedType ptyp = (ParameterizedType) t0;
         return getTypeName(base,ptyp.getType()); 
      case ASTNode.SIMPLE_TYPE :
         SimpleType styp = (SimpleType) t0;
         tnm = styp.getName().getFullyQualifiedName();
         break;
      case ASTNode.NAME_QUALIFIED_TYPE :
         NameQualifiedType nqt = (NameQualifiedType) t0;
         tnm = nqt.getQualifier().getFullyQualifiedName();
         tnm += ".";
         tnm += nqt.getName().getIdentifier();
         break;
      case ASTNode.QUALIFIED_TYPE :
         QualifiedType qt = (QualifiedType) t0;
         tnm = getTypeName(base,qt.getQualifier()); 
         tnm += ".";
         tnm += qt.getName().getIdentifier();
         break;
      case ASTNode.INTERSECTION_TYPE :
      case ASTNode.UNION_TYPE :
      case ASTNode.WILDCARD_TYPE :
         break;
    }
   
   if (tnm != null) {
      // handle normalizing type names
    }
   
   return t0.toString();
}



private double compareTypes(String t0,String t1)
{
   // generate a comparison between two types
   // value between 0 and 1
   // 0 means identical, 1 means incompatible
   if (t0 == null) t0 = "void";
   if (t1 == null) t1 = "void";
   
   if (t0.equals(t1)) return 0;
   
   if (primitive_types.contains(t0) && primitive_types.contains(t1)) {
      if (t0.equals("void") || t1.equals("void")) return 1;
      if (t0.equals("boolean") || t1.equals("boolean")) return 1;
      if (t0.equals("float") || t0.equals("double")) {
         if (t1.equals("float") || t1.equals("double")) {
            return 0.10;
          }
         else return 0.5;
       }
      else if (t1.equals("float") || t1.equals("double")) {
         return 0.5;
       }
      else {
         // compatible integer types
         return 0.25;
       }
    }
   
   String lt0 = t0;
   String lt1 = t1;
   int idx0 = t0.lastIndexOf(".");
   if (idx0 > 0) lt0 = t0.substring(idx0+1);
   int idx1 = t1.lastIndexOf(".");
   if (idx1 > 0) lt1 = t1.substring(idx1+1);
   
   if (primitive_types.contains(t0)) {
      if (lt1.toLowerCase().startsWith(t0)) return 0.5;
      else return 1;
    }
   if (primitive_types.contains(t1)) {
      if (lt0.toLowerCase().startsWith(t1)) return 0.5;
      else return 1;
    } 
      
   if (lt0.equals(lt1)) return 0.25;
   
   // if idx0 < 0, try finding actual type for t0 & recurse
   // if idx1 < 0, try finding actual type for t1 & recurse
   
   // if t0 or t1 is java.lang.Object, return 0.25
   
   // if t0 or t1 is java.lang.String, return 0.5
   
   return 1.0 - IvyStringDiff.normalizedStringDiff(t0,t1);
}


}       // end of class ScorerAnalyzerJava




/* end of ScorerAnalyzerJava.java */

