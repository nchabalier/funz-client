/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.funz;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import static org.funz.Project.makeDiscreteCaseList;
import org.funz.parameter.Case;
import org.funz.parameter.CaseList;
import org.funz.parameter.Parameter;
import org.funz.parameter.VarGroup;
import org.funz.parameter.Variable;
import org.funz.parameter.VariableMethods;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class ProjectTest {

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(ProjectTest.class.getName());
    }

    @Test
    public void testNoParam1Case() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ testNoParam1Case");
        Project p = new Project("testNoParam");

        p.resetDiscreteCases(null);
        assert p.getDiscreteCases() != null : "No disc cases built !";
        p.setCases(p.getDiscreteCases(), null);
        assert p.getCases() != null : "No cases built !";
        assert p.getCases().size() == 1 : "Not 1 case built !";
        assert p.getDiscreteCases() != null : "Null case list !";
        assert p.getDiscreteCases().size() == 1 : "Empty case list !";

    }

    @Test
    public void test1Param1Case() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ test1Param1Case");
        Project p = new Project("testNoParam");

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0"));
        p._params.add(a);

        p.resetDiscreteCases(null);
        assert p.getDiscreteCases() != null : "No disc cases built !";
        p.setCases(p.getDiscreteCases(), null);
        assert p.getCases() != null : "No cases built !";
        assert p.getCases().size() == 1 : "Not 1 case built !";
        assert p.getDiscreteCases() != null : "Null case list !";
        assert p.getDiscreteCases().size() == 1 : "Empty case list !";

    }

    void checkCaseParamValue(Case c, int node, String expected_param, String expected_value) {
        assert node < c.getNmOfNodes() : "Try to access " + node + "th node but only " + c.getNmOfNodes() + " available.";
        assert c.getParamName(node).equals(expected_param) : "Bad parameter at posisiton " + node + ": '" + c.getParamName(node) + "' instead of '" + expected_param + "' expected";
        assert c.getParamValue(node).equals(expected_value) : "Bad parameter value of parameter '" + expected_param + "' (case " + c.getName() + ") at posisiton " + node + ": '" + c.getParamValue(node) + "' instead of '" + expected_value + "' expected";
    }

    @Test
    public void test2ParamOrder() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ test2ParamOrder");
        Project p = new Project("test2ParamOrder");

        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0", "1", "2", "3"));
        params.add(a);

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1", "2"));
        params.add(b);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "a", "0");
        checkCaseParamValue(list.get(1), 1, "b", "1");
        checkCaseParamValue(list.get(5), 0, "a", "1");
        checkCaseParamValue(list.get(5), 1, "b", "2");
    }

    @Test
    public void testParamOrder() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ testParamOrder");
        Project p = new Project("testParamOrder");

        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0", "1", "2", "3"));
        params.add(a);

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1", "2"));
        params.add(b);

        Variable c = new Variable(p, "c");
        c.setValues(makeValues("0", "1"));
        params.add(c);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "a", "0");
        checkCaseParamValue(list.get(1), 1, "b", "0");
        checkCaseParamValue(list.get(1), 2, "c", "1");
        checkCaseParamValue(list.get(5), 0, "a", "0");
        checkCaseParamValue(list.get(5), 1, "b", "2");
        checkCaseParamValue(list.get(5), 2, "c", "1");
    }

    @Test
    public void testParamOrder3Vars() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ testParamOrder3Vars");
        Project p = new Project("testParamOrder3Vars");

        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0", "1", "2"));
        params.add(a);

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1"));
        params.add(b);

        Variable c = new Variable(p, "c");
        c.setValues(makeValues("0"));
        params.add(c);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "a", "0");
        checkCaseParamValue(list.get(1), 1, "b", "1");
        checkCaseParamValue(list.get(1), 2, "c", "0");
        checkCaseParamValue(list.get(5), 0, "a", "2");
        checkCaseParamValue(list.get(5), 1, "b", "1");
        checkCaseParamValue(list.get(5), 2, "c", "0");

    }

    @Test
    public void testParamOrder3Vars2() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ testParamOrder3Vars2");
        Project p = new Project("testParamOrder3Vars2");

        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0"));
        params.add(a);

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1"));
        params.add(b);

        Variable c = new Variable(p, "c");
        c.setValues(makeValues("0", "1", "2"));
        params.add(c);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "c", "0");
        checkCaseParamValue(list.get(1), 1, "b", "1");
        checkCaseParamValue(list.get(1), 2, "a", "0");
        checkCaseParamValue(list.get(5), 0, "c", "2");
        checkCaseParamValue(list.get(5), 1, "b", "1");
        checkCaseParamValue(list.get(5), 2, "a", "0");

    }

    @Test
    public void testParamOrder1Grp1Var() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ testParamOrder1Grp1Var");
        Project p = new Project("testParamOrder1Grp1Var");

        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0", "1"));

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1"));

        VarGroup ab = new VarGroup("ab");
        ab.addVariable(a);
        ab.addVariable(b);
        params.add(ab);

        Variable c = new Variable(p, "c");
        c.setValues(makeValues("0", "1"));
        params.add(c);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "a,b", "0,0");
        checkCaseParamValue(list.get(1), 1, "c", "1");
        checkCaseParamValue(list.get(3), 0, "a,b", "1,1");
        checkCaseParamValue(list.get(3), 1, "c", "1");
    }

    @Test
    public void test2ParamOrderBIGBRANCHESAFTER() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ test2ParamOrderBIGBRANCHESAFTER");
        Project p = new Project("test2ParamOrder");
        p.setParametersStorageOrder(Project.ParametersStorageOrder.BIG_BRANCHES_AFTER);
        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0", "1", "2", "3"));
        params.add(a);

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1", "2"));
        params.add(b);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "b", "0");
        checkCaseParamValue(list.get(1), 1, "a", "1");
        checkCaseParamValue(list.get(5), 0, "b", "1");
        checkCaseParamValue(list.get(6), 1, "a", "2");
    }

    @Test
    public void test2ParamOrderALPHA() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ test2ParamOrderALPHA");
        Project p = new Project("test2ParamOrder");
        p.setParametersStorageOrder(Project.ParametersStorageOrder.ALPHA);
        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "z");
        a.setValues(makeValues("0", "1", "2", "3"));
        params.add(a);

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1", "2"));
        params.add(b);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "b", "0");
        checkCaseParamValue(list.get(1), 1, "z", "1");
        checkCaseParamValue(list.get(5), 0, "b", "1");
        checkCaseParamValue(list.get(6), 1, "z", "2");
    }

    @Test
    public void test2ParamOrderARBITRARY() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++ test2ParamOrderARBITRARY");
        Project p = new Project("test2ParamOrder");
        p.setParametersStorageOrder(Project.ParametersStorageOrder.ARBITRARY);
        p.setArbitraryParametersStorageOrder("b", "a");

        List<Parameter> params = new LinkedList<Parameter>();

        Variable a = new Variable(p, "a");
        a.setValues(makeValues("0", "1", "2", "3"));
        params.add(a);

        Variable b = new Variable(p, "b");
        b.setValues(makeValues("0", "1", "2"));
        params.add(b);

        printParamList(params);
        CaseList list = makeDiscreteCaseList(params, new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
            }
        }, p, p.getParametersStorageOrder());
        printCaseList(list);

        checkCaseParamValue(list.get(0), 0, "b", "0");
        checkCaseParamValue(list.get(1), 1, "a", "1");
        checkCaseParamValue(list.get(5), 0, "b", "1");
        checkCaseParamValue(list.get(6), 1, "a", "2");
    }

    void printParamList(List params) {
        for (Iterator it = params.iterator(); it.hasNext();) {
            Parameter p = (Parameter) it.next();
            System.err.print(p.getName() + " [" + p.getNmOfValues() + "]");
            if (p instanceof Variable) {
                System.err.println(Arrays.asList(p.getValueArray()));
            } else {
                System.err.print("  ");
                printParamList(((VarGroup) p).getVariables());
            }
        }
    }

    List<VariableMethods.Value> makeValues(Object... values) {
        List<VariableMethods.Value> l = new LinkedList<>();
        for (Object o : values) {
            l.add(new VariableMethods.Value(o.toString()));
        }
        return l;
    }

    void printCaseList(CaseList cases) {
        System.err.println(cases.toString());
    }

}
