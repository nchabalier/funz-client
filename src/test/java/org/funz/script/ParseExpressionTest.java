package org.funz.script;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

import org.funz.util.Data;
import org.funz.util.Format;
import static org.funz.util.Format.repeat;
import org.funz.util.Parser;
import org.junit.Test;
import org.math.io.parser.ArrayString;

public class ParseExpressionTest {

    public static void main(String[] args) {
        org.junit.runner.JUnitCore.main(ParseExpressionTest.class.getName());
    }

    @Test
    public void testParseExpression() throws Exception {
        System.err.println("testParseExpression");

        HashMap<String, Object> params;
        LinkedList<String> expressions;
        LinkedList<Object> results;

        params = new HashMap<>();
        params.put(ParseExpression.FILES, new File("./src/test/samples/").listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        }));

        expressions = new LinkedList<>();
        results = new LinkedList<>();

        expressions.add("CSV(\"toto.csv\",\",\")>>asString()");
        results.add("{a=[1.0,2.0,3.0],b=[4.0,5.0,6.0],c=[7.0,8.0,9.0]}");        
        
        expressions.add("grep(\"(.*)Rmd\",\"mean\")>>get(0)");
        results.add("Mean speed: `r mean(cars$speed)`");

        expressions.add("grep(\"(.*)Rmd\",\": `r \")");
        results.add("Mean speed: `r mean(cars$speed)`");  

        expressions.add("`grep(\"(.*)Rmd\",\"\\: `r \")>>cut(\"\\: `r \",1)>>get(0)>>split(\" \")>>get(0)`");
        results.add("speed");        
        
        // now, space char is not a split arg, if following a " char
        expressions.add("`grep(\"(.*)Rmd\",\": `r \")>>cut(\": `r \",1)>>get(0)>>split(\" \")>>get(0)`");
        results.add("speed");

        expressions.add("contains(\"(.*)vbs\",\"WriteLine\")");
        results.add("true");

        expressions.add("`grep(\"(.*)vbs\",\"WScript\\.StdOut\\.WriteLine\\(\\\"(.*)=\")>>before(\"=\")>>after(\"\\\"\")`");
        results.add("z");

        expressions.add("`grep(\"branin\\.R\",\"cat\\(\\'(.*)=\")>>before(\"=\")>>after(\"'\")>>trim()>>get(0)`");
        results.add("z");

        expressions.add("`grep(\"branin\\.R\",\"cat\\(\\'(.*)=\")>>after(\"'\")>>before(\"=\")>>trim()>>get(0)`");
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
                throw new Exception("Parsing returned null !: "+ex);
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

        Parser p = new Parser();

        expressions = new LinkedList<>();
        results = new LinkedList<>();

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
            Object o;
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
    public void testBugFailedContains() {
        //contains("(.*)","DEBUT_APOLLO") && !contains("(.*)","DEBUT_MORET") && !contains("(.*)","SN KEFF") && !contains("(.*)","RECHERCHE_DIM_S") && contains("(.*)","CIGALES version 3(.)2")
        // on files [Y:\Promethee\samples\branin.R] 
        // org.funz.util.Parser.contains(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String) 
        // Failed to evaluate expression contains("(.*)","DEBUT_APOLLO") && !contains("(.*)","DEBUT_MORET") && !contains("(.*)","SN KEFF") && !contains("(.*)","RECHERCHE_DIM_S") && contains("(.*)","CIGALES version 3(.)2") on files [Y:\Promethee\samples\branin.R] org.funz.util.Parser.contains(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String) java.lang.Exception null at org.funz.script.ParseExpression.eval(ParseExpression.java:422) 

        System.err.println("===============>"
                + ParseExpression.eval("contains(\"(.*)\",\"DEBUT_APOLLO\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println("===============>"
                + ParseExpression.eval("contains(\"(.*)\",\"DEBUT_APOLLO\") & contains(\"(.*)\",\"DEBUT_MORET\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println("===============>"
                + ParseExpression.eval("contains(\"(.*)\",\"DEBUT_APOLLO\") & !contains(\"(.*)\",\"DEBUT_MORET\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println("===============>"
                + ParseExpression.eval("contains(\"(.*)\",\"DEBUT_APOLLO\") & !contains(\"(.*)\",\"DEBUT_MORET\") & !contains(\"(.*)\",\"SN KEFF\") & !contains(\"(.*)\",\"RECHERCHE_DIM_S\") & contains(\"(.*)\",\"CIGALES version 3(.)2\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));
    }

    @Test
    public void testBraces() {
        System.err.println("===============>"
                + ParseExpression.eval("containsIn(\"(abc)\",\"a\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println("===============>"
                + ParseExpression.eval("containsIn(\"(abc)\",\"a\") & containsIn(\"(abc)\",\"b\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println("===============>"
                + ParseExpression.eval("containsIn(\"(abc)\",\"a\") & !containsIn(\"(abc)\",\"d\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println("===============>"
                + ParseExpression.eval("containsIn(\"(abc)\",\"a\") & containsIn(\"(abc)\",\"b\") & containsIn(\"(abc)\",\"c\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

    }

    @Test
    public void testSerialLogic() {
        System.err.println(
                ParseExpression.eval("containsIn(\"abc\",\"a\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println(
                ParseExpression.eval("containsIn(\"abc\",\"a\") & containsIn(\"abc\",\"b\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

        System.err.println(
                ParseExpression.eval("containsIn(\"abc\",\"a\") & containsIn(\"abc\",\"b\") & containsIn(\"abc\",\"c\")",
                        Data.newMap("files", "src/test/samples/branin.R")
                ));

    }

    @Test
    public void testConcatString() {
        System.err.println("testStringConcat");
        testEvalEquality("concatString('1','1')", "11");
        testEvalInequality("concatString('1','1')", 2);
    }

    @Test
    public void testDoubleToInt() {
        System.err.println("testDoubleToInt");
        testEvalEquality("doubleToInt(2.3)", 2);
        testEvalEquality("doubleToInt(2.0)", 2);
        testEvalEquality("doubleToInt(1.9)", 1);
        testEvalInequality("doubleToInt(1.9)", 2);
    }

    @Test
    public void testBooleanComparison() {
        System.err.println("testBooleanComparison");
        testEvalEquality("1 > 2", false);
        testEvalEquality("1 < 2", true);
        testEvalInequality("1 > 2", true);
        testEvalEquality("asNumeric(\"1.2\")", 1.2);
        testEvalEquality("asNumeric(\"1.2\") < 2", true);
        testEvalEquality("asNumeric(\"1.2\") > asNumeric(\"1.2\")", false);
    }

    @Test
    public void testBooleanConversion() {
        System.err.println("testBooleanConversion");
        testEvalEquality("asNumeric(1>2)", 0.0);
        testEvalEquality("asNumeric(1<2)", 1.0);
        testEvalInequality("asNumeric(1>2)", 1.0);
        testEvalEquality("asNumeric(asNumeric(\"1.2\") > asNumeric(\"1.2\"))", 0.0);
    }

    @Test
    public void testBetween() {
        System.err.println("testBetween");
        testEvalEquality("between(\"1abc2\", \"1\", \"2\")", "abc");
        testEvalInequality("between(\"1abc2\", \"1\", \"2\")", "1abc2");
        testEvalEquality("between(\"1a<bc2\", \"1\", \"2\")", "a<bc");
        testEvalEquality("between(\"1a>bc2\", \"1\", \"2\")", "a>bc");
        testEvalEquality("between(\"1a+bc2\", \"1\", \"2\")", "a+bc");
        testEvalEquality("between(\"1a-bc2\", \"1\", \"2\")", "a-bc");
        testEvalEquality("between(\"1a*bc2\", \"1\", \"2\")", "a*bc");
        testEvalEquality("between(\"1a/bc2\", \"1\", \"2\")", "a/bc");
        testEvalEquality("between(\"+1a/bc-2\", \"+1\", \"-2\")", "a/bc");
        testEvalEquality("between(\"<1a/bc>2\", \"<1\", \">2\")", "a/bc");
        testEvalEquality("between(1abc2, 1a, c2)", "b");
        testEvalEquality("between(\"<mean>.10000000E+31 .20000000E+31 .30000000E+31 <\\mean>\", \"<mean>\", \" \")", ".10000000E+31");
        testEvalEquality("between(\"<?xml version=\"1.0\" encoding=\"UTF-16\"?><mean>.10000000E+31 .20000000E+31 .30000000E+31 <\\mean>\", \"<mean>\", \" \")", ".10000000E+31");
    }

    @Test
    public void testReturnIf() {
        System.err.println("returnIf");
        testEvalEquality("returnIf(1>2, \"a\", \"b\")", "b");
        testEvalEquality("returnIf(1<2, \"a\", \"b\")", "a");
        testEvalInequality("returnIf(1>2, \"a\", \"b\")", "a");
        testEvalEquality("returnIf(asNumeric(\"1.2\") > asNumeric(\"1.2\"), \"a\", \"b\")", "b");
    }

    /**
     * Test if the evaluation of an expression is equals to the expected result
     *
     * @param expr expression to evaluate
     * @param expected expected result
     */
    private static void testEvalEquality(String expr, Object expected) {
        testEvalEquality(expr, expected, null);
    }

    /**
     * Test if the evaluation of an expression is equals to the expected result
     *
     * @param expr expression to evaluate
     * @param expected expected result
     */
    private static void testEvalEquality(String expr, Object expected, File file) {
        Map fileMap = Data.newMap("files", new File[]{file});
        Object evalRes = ParseExpression.eval(expr, fileMap);
        assert expected.equals(evalRes) : "Result not matching when eval " + expr + ": [" + evalRes + "] != [" + expected + "] ";
    }


    @Test
    public void testXpath() {
        File xmlFile = loadTestXmlFile();
        assert Parser.XPath(xmlFile, "/calculation/keff/esti[@name=\"SOURCE-COLLISION\"]/mean").equals("<?xml version=\"1.0\" encoding=\"UTF-16\"?><mean>.99580215 .99584567 .99587032 .99586262 </mean>") : "XPath evaluation failed";
    }

    private static File loadTestXmlFile() {
        // Load xpath-test.xml from resources
        String testFileName = "xpath-test.xml";
        URL resource = ParseExpressionTest.class.getClassLoader().getResource(testFileName);

        File xmlFile;
        if (resource != null) {
            xmlFile = new File(resource.getFile());
            System.out.println("Found file: " + xmlFile.getAbsolutePath());
        } else {
            System.out.println("File not found!");
            throw new RuntimeException("XML file not found: " + testFileName);

        }
        return xmlFile;
    }

    @Test
    public void testExtractMinStdValues() {
        File xmlFile = loadTestXmlFile();
        String expectedMean = ".19580215";
        String expectedStd = ".56892243E-03";
        List<String> meanAndStdList = Parser.extractMinValues(
                xmlFile,
                "/calculation/keff/esti",
                "mean",
                "std");
        assert meanAndStdList.size() == 2 : "Expected 2 values, got: " + meanAndStdList.size();
        assert meanAndStdList.get(0).equals(expectedMean) : "Expected " + expectedMean +", got: " + meanAndStdList.get(0);
        assert meanAndStdList.get(1).equals(expectedStd) : "Expected " + expectedStd + ", got: " + meanAndStdList.get(1);

        List<String> expectResult = new ArrayList<>();
        expectResult.add(expectedMean);
        expectResult.add(expectedStd);

        testEvalEquality("extractMinValues(\"" + xmlFile.getAbsolutePath() + "\", \"/calculation/keff/esti\", \"mean\", \"std\")", expectResult, xmlFile);
        testEvalEquality("extractMinValues(\"" + xmlFile.getAbsolutePath() + "\", \"/calculation/keff/esti\", \"mean\", \"std\") >> get(1)", expectedMean, xmlFile);
        testEvalEquality("extractMinValues(\"" + xmlFile.getAbsolutePath() + "\", \"/calculation/keff/esti\", \"mean\", \"std\") >> get(0)", expectedStd, xmlFile);
    }

    /**
     * Test the evaluation of an expression is not equals to the expected result
     *
     * @param expr expression to evaluate
     * @param expected not expected result
     */
    private static void testEvalInequality(String expr, Object expected) {
        Object evalRes = ParseExpression.eval(expr, new HashMap<>());
        assert !expected.equals(evalRes) : "Result SHOULD NOT match when eval " + expr + ": [" + evalRes + "] == [" + expected + "] ";

    }

    @Test
    public void testCallMethod() throws Exception {
        System.err.println("testCallMethod");

        LinkedList<String> expressions;
        LinkedList<Object> results;

        Parser p = new Parser();

        expressions = new LinkedList<>();
        results = new LinkedList<>();

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
            Object o;
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
                if (o != null) {
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
