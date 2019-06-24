package org.funz.script;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import org.funz.util.Format;
import static org.funz.util.Format.repeat;
import org.funz.util.Parser;
import org.junit.Test;
import org.math.io.parser.ArrayString;

public class ParseExpressionTest {

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(ParseExpressionTest.class.getName());
    }

    @Test
    public void testParseExpression() throws Exception {
        System.err.println("testParseExpression");

        HashMap<String, Object> params;
        LinkedList<String> expressions;
        LinkedList<Object> results;

        params = new HashMap<String, Object>();
        params.put(ParseExpression.FILES, new File("./src/test/resources/").listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        }));
        /*for (File f : (File[]) params.get(JavaParseExpression.FILES)) {
        System.out.println(f.getName());
        }*/

        expressions = new LinkedList<String>();
        results = new LinkedList<Object>();

        expressions.add("contains(\"(.*)vbs\",\"WriteLine\")");
        results.add("true");

        expressions.add("`grep(\"(.*)vbs\",\"WScript\\.StdOut\\.WriteLine\\(\\\"(.*)=\")>>before(\"=\")>>after(\"\"\")`");
        results.add("z");
        
        expressions.add("`grep(\"(.*)R\",\"cat\\(\\'(.*)=\")>>before(\"=\")>>after(\"'\")`");
        results.add("z");

        expressions.add("`grep(\"(.*)R\",\"cat\\(\\'(.*)=\")>>after(\"'\")>>before(\"=\")`");
        results.add("z");

        expressions.add("grep(\"(.*).listing\",\"         Le test du Khi2\") >> get(1) >> between(\"dans\",\", teste\") >> trim()");
        results.add("MORET");

        expressions.add("Integer.parseInt(\"301\")");
        results.add(301);

        expressions.add("grep(\"(.*).listing\",\"FAIBLE SIGMA\") >> get(1) >> cut(\"SIGMA\",2) >>  substring(\"ZZZ\",\"+/-\") >> asNumeric()");
        results.add(0.71477);

        expressions.add("grep(\"(.*).listing\",\"FAIBLE SIGMA\") >> get(1) >> cut(\"SIGMA\",2) >>  substring(\"ZZZ\",\"+/-\") >> asNumeric() + 1");
        results.add(1.7147700000000001);

        expressions.add("grep(\"(.*).listing\",\"FAIBLE SIGMA\") >> get(1) >> cut(\"SIGMA\",2) >>  before(\"+/-\") >> asNumeric()");
        results.add(0.71477);

        expressions.add("grep(\"(.*).listing\",\"FAIBLE SIGMA\") >> get(1) >> cut(\"SIGMA\",2) >>  after(\"+/-\") >>  before(\":\") >> asNumeric()");
        results.add(0.00098);

        expressions.add("contains(\"(.*)\",\"FAIBLE SIGMA\") & contains(\"(.*)\",\"CIGALES version 3(.)1\")");
        results.add("true");

        expressions.add("!contains(\"(.*)\",\"FAIBLE SIGMA\") & contains(\"(.*)\",\"CIGALES version 3(.)1\")");
        results.add("false");

        expressions.add("asNumeric1DArray( between(grep(\"(.*)resume\",\"(( ABANDONNEE )|( VAUT ))\") ,\")  \",\"|\") )");
        results.add(new double[]{10.9909, 9.43223});

        for (int i = 0; i < expressions.size(); i++) {
            String ex = expressions.get(i);
            System.err.println(ex);
            Object res = results.get(i);

            long tic = Calendar.getInstance().getTimeInMillis();
            Object o = ParseExpression.eval(ex, params);
            System.err.println(o);
            long toc = Calendar.getInstance().getTimeInMillis();
            System.err.println("time elapsed: " + ((toc - tic) / 1000.0));

            if (o == null) {
                throw new Exception("Parsing returned null !");
            } else {
                String ostr = o.toString();
                if (o instanceof double[]) {
                    ostr = ArrayString.printDoubleArray((double[]) o);
                }
                if (o instanceof double[][]) {
                    ostr = ArrayString.printDoubleArray((double[][]) o);
                }
                String resstr = res.toString();
                if (res instanceof double[]) {
                    resstr = ArrayString.printDoubleArray((double[]) res);
                }
                if (res instanceof double[][]) {
                    resstr = ArrayString.printDoubleArray((double[][]) res);
                }
                assert ostr.equals(resstr) : "Result not matching: [" + o.getClass().getSimpleName() + "] " + ostr + " != [" + res.getClass().getSimpleName() + "] " + resstr;
            }
        }
    }

    @Test
    public void testParseExpressionConsistency() throws Exception {
        assert repeat(5, "a", ";").equals(ParseExpression.Eval(new Format(), "repeat", 5, "a", ";")) : "failed Format.repeat(5, \"a\", \";\")";

        assert Parser.between("111 sfdgfg 555", "111", "555").equals(ParseExpression.Eval(new Parser(), "between", "111 sfdgfg 555", "111", "555")) : "failed between(\"111 sfdgfg 555\", \"111\", \"555\")";
        String expr = "between(\"111 sfdgfg 555\", \"111\", \"555\")";
        assert Parser.between("111 sfdgfg 555", "111", "555").equals(ParseExpression.CallAlgebra(new Parser(), expr)) : "failed " + expr;

        String rec_expr = "between(between(\"111 sfdgfg 555\", \"111\", \"555\"),\"sf\",\"fg\")";
        assert (Parser.between(Parser.between("111 sfdgfg 555", "111", "555"), "sf", "fg")).equals(ParseExpression.CallAlgebra(new Parser(), rec_expr)) : "failed " + rec_expr;
    }

    @Test
    public void testCall() throws Exception {
        System.err.println("testCall");

        LinkedList<String> expressions;
        LinkedList<Object> results;

        Parser p = new Parser(new File[0]);
        expressions = new LinkedList<String>();
        results = new LinkedList<Object>();

        expressions = new LinkedList<String>();
        results = new LinkedList<Object>();

        expressions.add("1+1");
        results.add(2.0);

        expressions.add("1/2");
        results.add(0.5);

        expressions.add("-1");
        results.add(-1.0);

        expressions.add("- 1");
        results.add(-1.0);

        expressions.add("(1+1)*3");
        results.add(6.0);

        //Do NOT yet support operator priority !!! mandatory bracketing
        expressions.add("1+(2*3)");
        results.add(7.0);

        expressions.add("(2*3)+1");
        results.add(7.0);

        for (int i = 0; i < expressions.size(); i++) {
            String ex = expressions.get(i);
            System.err.println(ex);
            Object res = results.get(i);

            long tic = Calendar.getInstance().getTimeInMillis();
            Object o = null;
            try {
                o = ParseExpression.CallAlgebra(p, ex);
                System.err.println(o);
            } catch (Exception ex1) {
                ex1.printStackTrace();
                throw new Exception("Exception calling " + ex + ": " + ex1);
            }
            long toc = Calendar.getInstance().getTimeInMillis();
            System.err.println("time elapsed: " + ((toc - tic) / 1000.0));

            if (o == null) {
                throw new Exception("Parsing returned null !");
            } else {
                String ostr = o.toString();
                if (o instanceof double[]) {
                    ostr = ArrayString.printDoubleArray((double[]) o);
                }
                if (o instanceof double[][]) {
                    ostr = ArrayString.printDoubleArray((double[][]) o);
                }
                String resstr = res.toString();
                if (res instanceof double[]) {
                    resstr = ArrayString.printDoubleArray((double[]) res);
                }
                if (res instanceof double[][]) {
                    resstr = ArrayString.printDoubleArray((double[][]) res);
                }
                assert ostr.equals(resstr) : "Result not matching when eval " + ex + ": [" + o.getClass().getSimpleName() + "] " + ostr + " != [" + res.getClass().getSimpleName() + "] " + resstr;
            }
        }

    }

    @Test
    public void testCallMethod() throws Exception {
        System.err.println("testCallMethod");

        LinkedList<String> expressions;
        LinkedList<Object> results;

        Parser p = new Parser(new File[0]);
        expressions = new LinkedList<String>();
        results = new LinkedList<Object>();

        expressions = new LinkedList<String>();
        results = new LinkedList<Object>();

        // test basic algebra
        expressions.add("1+1");
        results.add(2.0);

        expressions.add("1/2");
        results.add(0.5);

        expressions.add("-1");
        results.add(-1.0);

        expressions.add("- 1");
        results.add(-1.0);

        expressions.add("(1+1)*3");
        results.add(6.0);

        //Do NOT yet support operator priority !!! mandatory bracketing
        expressions.add("1+(2*3)");
        results.add(7.0);

        expressions.add("(2*3)+1");
        results.add(7.0);

        // test with methods & algebraic op
        expressions.add("length(\"2*3\")");
        results.add(3);

        expressions.add("length(\"2*3\")+1");
        results.add(4.0);

        expressions.add("1+(length(\"2*3\"))");
        results.add(4.0);

        expressions.add("(1+(length(\"2*3\")))+1");
        results.add(5.0);

        expressions.add("1+((length(\"2*3\"))+1)");
        results.add(5.0);

        // test with escaping chars
        expressions.add("length(\"\\(3\")");
        results.add(3);

        expressions.add("length(\"\\(3\")+1");
        results.add(4.0);

        expressions.add("1+(length(\"\\(3\"))");
        results.add(4.0);

        expressions.add("(1+(length(\"\\(3\")))+1");
        results.add(5.0);

        expressions.add("1+((length(\"\\(3\"))+1)");
        results.add(5.0);

        expressions.add("1+((length(\",\"))+1)");
        results.add(3.0);

        expressions.add("z");
        results.add("z");

        for (int i = 0; i < expressions.size(); i++) {
            String ex = expressions.get(i);
            System.err.println(ex);
            Object res = results.get(i);

            long tic = Calendar.getInstance().getTimeInMillis();
            Object o = null;
            try {
                o = ParseExpression.CallMethod(p, ex);
                System.err.println(o);
            } catch (Exception ex1) {
                ex1.printStackTrace();
                throw new Exception("Exception calling " + ex + ": " + ex1);
            }
            long toc = Calendar.getInstance().getTimeInMillis();
            System.err.println("time elapsed: " + ((toc - tic) / 1000.0));

            if (o == null && res != null) {
                throw new Exception("Parsing returned null !");
            } else {
                if (o != null || res != null) {
                    String ostr = o.toString();
                    if (o instanceof double[]) {
                        ostr = ArrayString.printDoubleArray((double[]) o);
                    }
                    if (o instanceof double[][]) {
                        ostr = ArrayString.printDoubleArray((double[][]) o);
                    }
                    String resstr = res.toString();
                    if (res instanceof double[]) {
                        resstr = ArrayString.printDoubleArray((double[]) res);
                    }
                    if (res instanceof double[][]) {
                        resstr = ArrayString.printDoubleArray((double[][]) res);
                    }
                    assert ostr.equals(resstr) : "Result not matching when eval " + ex + ": [" + o.getClass().getSimpleName() + "] " + ostr + " != [" + res.getClass().getSimpleName() + "] " + resstr;
                }
            }
        }

    }
}
