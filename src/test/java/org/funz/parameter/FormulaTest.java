package org.funz.parameter;

import org.funz.conf.Configuration;
import org.funz.script.MathExpression;
import org.funz.script.RMathExpression;
import org.funz.util.Data;
import org.junit.Before;
import org.junit.Test;

public class FormulaTest {

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(FormulaTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        Configuration.readProperties(null);
        Configuration.writeUserProperty = false;
        MathExpression.SetDefaultInstance(RMathExpression.class);
        System.out.println(MathExpression.Eval("(1.23)+pi", null));
    }

    @Test
    public void testValid() throws Exception {
        //System.out.println("#123.4650".replaceAll("[1-9]", "0"));

        //Configuration.readProperties(null);
        //MathExpression.SetDefaultInstance(RMathExpression.class);
        //System.out.println(MathExpression.Eval("(1.23)+pi", null));
        SyntaxRules varSyntax = new SyntaxRules(SyntaxRules.START_SYMBOL_DOLLAR, SyntaxRules.LIMIT_SYMBOL_BRACKETS);

        String[] testValid = {"if(1>0){'a'}else{'b'}",
            "3*(1.0/2.0)",
            "$a*(1.0/2.0)",
            "${a~1.0}*(1.0/$b)"};

        for (String string : testValid) {
            System.err.println("valid ? " + string + " " + Formula.isValid(string, varSyntax, null));

            assert Formula.isValid(string, varSyntax, null) : "error in " + string;
        }

        String[] testNotValid = {"if(1>0){'a'}else'b'}",
            "3*(1.0/2.0",
            "${a~1.0}(1.0/$b)"};

        for (String string : testNotValid) {
            System.err.println("not valid ? " + string + " " + Formula.isValid(string, varSyntax, null));
            assert !Formula.isValid(string, varSyntax, null) : "no error in " + string;
        }
    }

    @Test
    public void testPrint() throws Exception {
        String s = Formula.Eval("print('*')", null);
        assert s.equals("*") : "Bad print: " + s;
    }

    @Test
    public void testSource() throws Exception {
        String s = Formula.Eval("source('https://raw.githubusercontent.com/IRSN/flood.R/main/pline.R'); 1+1", null);
        assert s.equals("[1] 2") : "Bad print: " + s;
    }

    @Test
    public void testInsideXML() throws Exception {
        SyntaxRules varSyntax = new SyntaxRules(SyntaxRules.START_SYMBOL_DOLLAR, SyntaxRules.LIMIT_SYMBOL_BRACKETS);

        String[] testValid = {"if(1&gt;0){'a'}else{'b'}",
            "if(1&lt;0){'a'}else{'b'}",
            "1 &amp; 1"};

        Formula.AUTO_REPLACE_IN_EVAL.put("&quot;", "\"");
        Formula.AUTO_REPLACE_IN_EVAL.put("&apos;", "'");
        Formula.AUTO_REPLACE_IN_EVAL.put("&amp;", "&");
        Formula.AUTO_REPLACE_IN_EVAL.put("&lt;", "<");
        Formula.AUTO_REPLACE_IN_EVAL.put("&gt;", ">");

        for (String string : testValid) {
            System.err.println("valid ? " + string + " " + Formula.isValid(string, varSyntax, null));

            assert Formula.isValid(string, varSyntax, null) : "error in " + string;
        }

    }

    @Test
    public void testEval() throws Exception {
        String[] testEval = {
//            "if(1>0){'a'}else{'b'}",
            "if(1>0) print('a') else print('b')",
            //!!! not working in R2js "( if(1>0) print('a') else print('b') )",
//            "3*(1.0/2.0)",
//            "((5E-2)+(0.4e+3)^(1.0*exp(1))*pi*atan((1/3)))",
//            "(1.1*(2-0.5)+(0.0439*(0.3162717071827501)^(2/3))*cos(2*pi*(0.4590657262597233)))",
//            "(1.1*(2-0.5)+0.0439*(0.8921983358450234)*sin(2*pi*(0.9606179972179234)))",
//            "(1.1*(2-0.5))",
//            "(10000/2/2)",
//            "(1.1*(1-0.5)+(0.0439*(0.4785452482756227)^(2/3))*cos(2*pi*(0.08424691436812282)))",
//            "(1.1*(1-0.5)+0.0439*(0.4785452482756227)^(2/3))*cos(2*pi*(0.08424691436812282))"
        };

        for (String string : testEval) {
            System.err.println(Formula.Eval(string, null));
            assert Formula.Eval(string, null) != null && Formula.Eval(string, null).length() > 0 : "error in " + string;
        }
    }
}
