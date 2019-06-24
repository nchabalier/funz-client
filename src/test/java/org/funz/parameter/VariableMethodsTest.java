package org.funz.parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import org.codehaus.plexus.util.FileUtils;
import org.funz.Project;
import org.funz.conf.Configuration;
import static org.funz.parameter.VariableMethods.MATHENGINE_SET_MARKER;
import org.funz.parameter.VariableMethods.ParseEvalException;
import org.funz.script.MathExpression;
import org.funz.script.MathExpression.MathException;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import org.funz.util.ParserUtils;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VariableMethodsTest {

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(VariableMethodsTest.class.getName());
    }
    char fs, fl, fr, vs, vl, vr;
    static File fin, fout;
    static SyntaxRules varSyntax, formSyntax;
    StringBuffer test;

    @Before
    public void setUp() throws IOException {
        Configuration.readProperties(null);
        Configuration.writeUserProperty = false;
        MathExpression.SetDefaultInstance(RMathExpression.class);

        FileUtils.mkdir("tmp");
        fin = File.createTempFile("VariableMethods", "input", new File("tmp"));
        fout = File.createTempFile("VariableMethods", "target", new File("tmp"));

        varSyntax = new SyntaxRules(0, 3);
        formSyntax = new SyntaxRules(3, 2);
        fs = formSyntax.getStartSymbol();
        fl = formSyntax.getLeftLimitSymbol();
        fr = formSyntax.getRightLimitSymbol();
        vs = varSyntax.getStartSymbol();
        vl = varSyntax.getLeftLimitSymbol();
        vr = varSyntax.getRightLimitSymbol();

        System.err.println(varSyntax.toString());
        System.err.println(formSyntax.toString());

        //test = new LinkedList<String>();
        test = new StringBuffer();
        test.append("#" + fs + ": toto <- " + vs + "titi\n");
        test.append("#" + fs + ": ftoto <- function(x){\n#" + fs + ":if(x>0) {\n#" + fs + ": x\n#" + fs + ":} else {-x}\n#" + fs + ":}\n");
        //test.append("#" + fs + ": ftoto <- function(x){\n#" + fs + ":if(x>0) {\n#" + fs + ": x\n#" + fs + ":} else {-x}\n");
        //test.append("# dfgd \n");
        //test.append("#" + fs + ": ftoto <- function(x){\n#" + fs + ":if(x>0) {\n#" + fs + ": x\n#" + fs + ":} else {-x}\n#" + fs + ":}\n");

        test.append("ok_var " + vs + "a" + " \n");
        test.append("no_var_1 " + vs + "a" + VariableMethods.DEFAULT_VALUE_STR + "0.123" + "\n");
        test.append("no_var_2 " + vs + "a" + VariableMethods.DEFAULT_VALUE_STR + "0.123E-1" + "\n");
        test.append("no_var_3 " + vs + "a" + VariableMethods.DEFAULT_VALUE_STR + "[0.123E-1,0.456E-1]" + "\n");

        test.append("ok_var_1 " + vs + vl + "a" + VariableMethods.DEFAULT_VALUE_STR + "0.123" + vr + "\n");
        test.append("ok_var_2 " + vs + vl + "a" + VariableMethods.DEFAULT_VALUE_STR + "0.123E-1" + vr + "\n");
        test.append("ok_var_3 " + vs + vl + "a" + VariableMethods.DEFAULT_VALUE_STR + "[0.123E-1,0.456E-1]" + vr + "\n");

        test.append("ok_form " + fs + fl + vs + "a" + fr + "\n");
        test.append("no_form " + fs + "anything " + fr + "\n");
        test.append("#" + fs + MATHENGINE_SET_MARKER + "c <- rnorm(10)" + "\n");
        test.append("ok_form_1 " + fs + fl + vs + vl + "a" + VariableMethods.DEFAULT_VALUE_STR + "0.123" + vr + "*1.235" + fr + "\n");
        test.append("ok_form_2 " + fs + fl + vs + vl + "a" + VariableMethods.DEFAULT_VALUE_STR + "0.123E-1" + vr + "*1.235" + fr + "\n");
        test.append("ok_var_4 " + vs + "" + vl + "b" + VariableMethods.DEFAULT_VALUE_STR + "0.456" + vr + "\n");
        test.append("ok_form_3 " + fs + fl + vs + "" + vl + "b" + VariableMethods.DEFAULT_VALUE_STR + "0.567" + vr + fr + "\n");
        test.append("ok_form_4 " + fs + fl + vs + "a" + "|00.000" + fr + "\n");
        test.append("ok_form_5 " + fs + fl + vs + vl + "c" + VariableMethods.DEFAULT_VALUE_STR + "0.890" + vr + "|00.0000E0" + fr + "\n");
        test.append("ok_form_60 " + fs + fl + vs + "d" + "" + fr + "\n");
        test.append("ok_form_6 " + fs + fl + vs + "d" + "|0.00000000E0" + fr + "\n");
        test.append("ok_form_7 " + fs + fl + vs + "e" + "|0.0000E0" + fr + "\n");
        test.append("ok_form_8 " + fs + fl + vs + vl + "e" + vr + "|0.0000E0" + fr + "\n");
        test.append("ok_var_5 " + vs + vl + "s" + vr + "\n");
        test.append("ok_form_9 " + fs + fl + "paste('\"','" + vs + vl + "s" + vr + "','\"')" + fr + "\n");
    }

    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(2000);
    }

    @Test
    public void testFail() throws Exception {
        boolean errorfound = false;
        try {
            HashMap values = new HashMap();
            values.put("titi", -1.1);
            values.put("a", 1.1);
            //THIS IS THE ERROR TO DETECT: b is not set
            //values.put("b", 2.0);
            values.put("c", 0.000000123456789);
            values.put("d", 123456789);
            values.put("e", "  1.234560000E-06");
            values.put("s", "abcdef");

            ASCII.saveFile(fin, test.toString());
            HashMap<String, String> defval = parse(values);
        } catch (Exception e) {
            if (e.getMessage().contains("b" + VariableMethods.DEFAULT_VALUE_STR)) {
                errorfound = true;
            } else {
                System.err.println("Bad error detected:" + e.getMessage());
            }
        }

        assert errorfound : "Error in syntax not well detected !";
    }

    @Test
    public void testOk() throws Exception {
        HashMap values = new HashMap();
        values.put("titi", -1.1);
        values.put("a", 1.1);
        values.put("b", 2.0);
        values.put("c", 0.000000123456789);
        values.put("d", 123456789);
        values.put("e", "  1.234560000E-06");
        values.put("s", "abcdef");

        ASCII.saveFile(fin, test.toString());
        try {
            HashMap<String, String> defval = parse(values);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(test.toString());
            throw e;
        }
        
        String out = printFilesIO();
        System.err.println(out);
        assert grep("ok_var")[1].equals("1.1 ") : "Failed to parse ok_var:" + Arrays.asList(grep("ok_var"));
        assert !grep("no_var_1")[1].equals("1.1") : "Failed to parse no_var_1";
        assert grep("ok_var_1")[1].equals("1.1") : "Failed to parse ok_var_1";
        assert grep("ok_form")[1].equals("1.1") : "Failed to parse ok_form";
        assert grep("ok_form_1")[1].equals("1.3585000000000003") : "Failed to parse ok_form_1";
        assert grep("ok_form_2")[1].equals("1.3585000000000003") : "Failed to parse ok_form_2";
        assert grep("ok_form_5")[1].equals("12.3457E-8") : "Failed to parse ok_form_5";
        assert grep("ok_var_5")[1].equals("abcdef") : "Failed to parse ok_var_5";
        assert grep("ok_form_9")[1].equals("\" abcdef \"") : "Failed to parse ok_form_9:" + Arrays.asList(grep("ok_form_9"));
    }

    @Test
    public void testFormVarOk() throws Exception {
        String fvtest = "#" + fs + MATHENGINE_SET_MARKER + " a_123 <- 456\n" + fs + fl + "a_" + vs + vl + "tag" + vr + fr;
        String vtest = "#" + fs + MATHENGINE_SET_MARKER + " a_123 <- 456\n" + "a_" + vs + vl + "tag" + vr;
        String vbtest = "#" + fs + MATHENGINE_SET_MARKER + " a_123 <- 456\n(" + "a_" + vs + vl + "tag" + vr + ")";
        HashMap values = new HashMap();

        values.put("tag", 123);

        ASCII.saveFile(fin, vtest);
        parse(values);

        ASCII.saveFile(fin, vbtest);
        parse(values);

        ASCII.saveFile(fin, fvtest);
        parse(values);
    } 
    
    static Variable readVar(String txt, boolean buildParameters) throws Exception {
        System.err.println(""+txt);
        ASCII.saveFile(fin, txt);
        HashMap<String, String> default_models = new HashMap<String, String>();
        VariableMethods.parseFileVars(varSyntax, fin, fout, new HashMap(), new LinkedList<Replaceable>(), default_models);
        Project p = new Project("testModel");
        Variable v = new Variable(p, "v");
        p.getVariables().add(v);
        v.setDefaultModel(default_models.get("v"), true);
        if (buildParameters) buildParameters_FromAbstractShell(p);
        return v;
    }
    
    static void buildParameters_FromAbstractShell(Project prj) {
        // first, complete undefined variables
        LinkedList<Variable> vars = prj.getVariables();
        for (Variable v : vars) {
            if (v.getNmOfValues() == 0) {
                if (v.getDefaultModel() != null && v.getDefaultModel().contains("{")) {
                    v.setDefaultModel(v.getDefaultModel(), true);
                    continue;
                } else if (v.getDefaultValue() != null) {
                    LinkedList<VariableMethods.Value> vals = new LinkedList<>();
                    vals.add(new VariableMethods.Value(v.getDefaultValue()));
                    v.setValues(vals);
                } else {
                    LinkedList<VariableMethods.Value> vals = new LinkedList<>();
                    vals.add(new VariableMethods.Value(""));
                    v.setValues(vals);
                }
            }

            if (v.getName().contains(".") && v.getGroup() == null) {
                String prefix = v.getName().substring(0, v.getName().indexOf("."));
                VarGroup g = prj.getGroupByName(prefix);
                if (g == null) {
                    g = new VarGroup(prefix);
                    prj.addGroup(g);
                }
                g.addVariable(v);
                v.setGroup(g);
            }
        }

        prj.buildParameterList();
    }

    @Test
    public void testModel() throws Exception {
        assert readVar("" + vs + vl + "v" + vr,false).getDefaultValue().length() == 0 : "Failed to read var without model";
        assert readVar("" + vs + vl + "v" + vr,false).getNmOfValues() == 0 : "Failed to read var without model";
        assert readVar("" + vs + vl + "v" + vr,true).getNmOfValues() == 1 : "Failed to read var without model";
        assert readVar("" + vs + vl + "v~123" + vr,false).getDefaultValue().equals("123") : "Failed to read var with model";
        assert readVar("" + vs + vl + "v~123" + vr,false).getNmOfValues()==0 : "Failed to read var with model without values";
        assert readVar("" + vs + vl + "v~123" + vr,true).getNmOfValues()==1 : "Failed to read var with model without values";
        assert readVar("" + vs + vl + "v~123;{1,2,3}" + vr,false).getNmOfValues() == 3 : "Failed to read var values";
        assert readVar("" + vs + vl + "v~123;{1,2,3}" + vr,true).getNmOfValues() == 3 : "Failed to read var values";
        assert readVar("" + vs + vl + "v~123;[1,2]" + vr,false).getNmOfValues() == 0 : "Failed to read var values";
        assert readVar("" + vs + vl + "v~123;[1,2]" + vr,true).getNmOfValues() == 1 : "Failed to read var values";
        assert readVar("" + vs + vl + "v~123;{1,2,3};'comment'" + vr,false).getComment().equals("comment") : "Failed to read var comment";
        assert readVar("" + vs + vl + "v~123;{1,2,3};'comment';[10,20]" + vr,false).getLowerBound() == 10 : "Failed to read var bound";
        assert readVar("" + vs + vl + "v~123;{1,2,3};'comment';[10,20]" + vr,false).getUpperBound() == 20 : "Failed to read var bound";
    }

    @Test
    public void testFunctionnalComment() throws VariableMethods.BadSyntaxException, UnsupportedEncodingException, FileNotFoundException, IOException, ParseEvalException, MathException {
        String atest = "#" + fs + MATHENGINE_SET_MARKER + " a =" +  " 1";
        ASCII.saveFile(fin, atest);
        parse(new HashMap());
        //System.err.println(((Rsession)((RMathExpression) MathExpression.GetDefaultInstance()).R).eval("a"));
        assert Arrays.asList(((RMathExpression) MathExpression.GetDefaultInstance()).R.ls()).contains("a") : "a not found in "+ Arrays.asList(((RMathExpression) MathExpression.GetDefaultInstance()).R.ls());

        String ftest = "#" + fs + MATHENGINE_SET_MARKER + " f <- function(x) {x}";
        ASCII.saveFile(fin, ftest);
        parse(new HashMap());
        assert Arrays.asList(((RMathExpression) MathExpression.GetDefaultInstance()).R.ls()).contains("f") : "f not found";

        String gtest = "#" + fs + MATHENGINE_SET_MARKER + " g <- function(x) {\n" + "#" + fs + MATHENGINE_SET_MARKER + "x\n" + "#" + fs + MATHENGINE_SET_MARKER + "}";
        ASCII.saveFile(fin, gtest);
        parse(new HashMap());
        assert Arrays.asList(((RMathExpression) MathExpression.GetDefaultInstance()).R.ls()).contains("g") : "g not found";

        String old_gtest = "#" + fs + MATHENGINE_SET_MARKER + " old_g <- function(x) {\\\n" + "#" + fs + MATHENGINE_SET_MARKER + "x \\\n" + "#" + fs + MATHENGINE_SET_MARKER + "}";
        ASCII.saveFile(fin, old_gtest);
        parse(new HashMap());
        assert Arrays.asList(((RMathExpression) MathExpression.GetDefaultInstance()).R.ls()).contains("old_g") : "old_g not found";
    }

    public HashMap<String, String> parse(HashMap values) throws VariableMethods.BadSyntaxException, UnsupportedEncodingException, FileNotFoundException, IOException, ParseEvalException, MathException {
        HashMap<String, String> defval = new HashMap<String, String>();
        HashSet h = VariableMethods.parseFile("#", varSyntax, formSyntax, fin, fout, values, null, null, null, null, null, defval, MathExpression.GetDefaultInstance());
        return defval;
    }

    public String printFilesIO() {
        StringBuffer b = new StringBuffer();
        b.append("=======================\n");
        b.append(getASCIIFileContent(fin));
        b.append("\n>>>>>>>>>>>>>>>>>>>>>>>>\n");
        b.append(getASCIIFileContent(fout));
        b.append("\n=======================\n");
        return b.toString();
    }

    public String[] grep(String what) {
        String[] in = ParserUtils.getAllLinesStarting(fin, what + " ");
        assert in.length == 1 : "Not one (" + in.length + ") test of type: " + what + ": " + Arrays.asList(in);
        String[] out = ParserUtils.getAllLinesStarting(fout, what + " ");
        assert out.length == 1 : "Not one (" + out.length + ") result of type: " + what + ": " + Arrays.asList(out);
        return new String[]{in[0].replace(what + " ", ""), out[0].replace(what + " ", "")};
    }
}
