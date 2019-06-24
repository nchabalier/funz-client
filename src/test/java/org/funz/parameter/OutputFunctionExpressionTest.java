package org.funz.parameter;

import java.util.Arrays;
import org.funz.conf.Configuration;
import org.funz.parameter.OutputFunctionExpression.Anything;
import org.funz.parameter.OutputFunctionExpression.AnythingND;
import org.funz.parameter.OutputFunctionExpression.GaussianDensity;
import org.funz.parameter.OutputFunctionExpression.Numeric;
import org.funz.parameter.OutputFunctionExpression.Numeric2D;
import org.funz.parameter.OutputFunctionExpression.Numeric3D;
import org.funz.parameter.OutputFunctionExpression.NumericArray;
import org.funz.parameter.OutputFunctionExpression.Sequence;
import org.funz.parameter.OutputFunctionExpression.Text;
import org.funz.util.ASCII;
import org.junit.Before;
import org.junit.Test;

public class OutputFunctionExpressionTest {

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(OutputFunctionExpressionTest.class.getName());
    }

    @Test
    public void testSplitArgs() {
        //String tosplit="((0.13849207099992783,0.4356983,0.547504397429293));((0.2248849621042609,15.87809,0.8471806526649743));((0.2858809254132211,4.094367,0.9918381538882386));((0.19959530474152415,13.4864,0.6515178151603322));((0.1,4.030505,0.5));((0.3,5.154316,0.5));((0.1,3.504775,1.0));((0.3,5.006853,1.0));((0.21924483103063186,2.240749,0.5599487775084904));((0.17660272646539696,21.83196,0.7317179985930358));((0.1961439084515299,16.73588,0.8311464477513845));((0.2741073890858154,29.25211,0.7471573440574986));((0.13135331976791959,16.88609,0.6936083961202636));((0.13473498908044595,11.09686,0.853028411760461));((0.28512459108014343,22.85456,0.6709819718594578));((0.28616820388279596,29.49166,0.7797755832516404));((0.10201550673420542,6.924802,0.6257183866990411));((0.27559986766366446,25.53914,0.8093067601302707));((0.1959913421522186,22.67943,0.7763341608984099));((0.26974464992703495,27.60829,0.7200712891853759));((0.29534812918355935,12.98118,0.8946843853559615));((0.2987640440888213,29.02577,0.798974814303931));((0.2559586494954592,8.665298,0.601306665352598));((0.24652861406824192,26.64713,0.738425229034504));((0.2920891381049961,28.29833,0.7049747937661632));((0.10189869842954437,19.72728,0.7640094172783568))";
        String tosplit = "((0.13849207099992783,0.4356983,0.547504397429293))";
        assert OutputFunctionExpression.splitArgs(tosplit).length == 3 : "Erroro interface splitting " + tosplit + "\n returned:" + ASCII.cat("\n", OutputFunctionExpression.splitArgs(tosplit));
    }

    /*@Test
     public void testUI() {
     MathExpression.SetDefaultInstance(RMathExpression.class);
     JPanel panel = new JPanel(new BorderLayout());
     final OutputFunctionExpressionEditor p = new OutputFunctionExpressionEditor() {

     @Override
     public void selectedField(JTextField field) {
     System.err.println("Selected " + field + " : " + field.getText());
     }
     };
     panel.add(p, BorderLayout.CENTER);
     panel.add(new JButton(new AbstractAction("do") {

     public void actionPerformed(ActionEvent e) {
     System.err.println(p.getOutputFunctionExpression());
     }
     }), BorderLayout.SOUTH);
     FrameView fv = new FrameView(panel);
     fv.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     fv.setVisible(true);
     }*/
    @Test
    public void testCast() {
        /*String[] s = splitArgs("expand.grid(MeshTally_4_meshY,MeshTally_4_meshX)[,1],expand.grid(MeshTally_4_meshY,MeshTally_4_meshX)[,2],array(MeshTally_4_average)");
         for (String string : s) {
         System.err.println(string);
         }*/

        Text t = new Text();
        String t_str = t.toNiceString("abcd");
        assert t.fromNiceString(t_str)[0].equals("abcd") : t_str + "!=" + t.toNiceString(t.fromNiceString(t_str));

        Numeric n = new Numeric();
        String n_str = n.toNiceNumericString(1.0);
        assert n.fromNiceNumericString(n_str)[0] == 1.0 : n_str + "!=" + n.toNiceNumericString(n.fromNiceNumericString(n_str));

        GaussianDensity g = new GaussianDensity();
        String g_str = g.toNiceNumericString(new double[]{1.0, 2.0});
        assert Arrays.equals(g.fromNiceNumericString(g_str), new double[]{1.0, 2.0}) : g_str + "!=" + g.toNiceNumericString(n.fromNiceNumericString(g_str));

        Anything a = new Anything();
        String a_str = a.toNiceNumericString("sdfgdg");
        assert a.fromNiceString(a_str)[0].equals("sdfgdg") : a_str + "!=" + a.toNiceString(a.fromNiceString(a_str));

        AnythingND an = new AnythingND();
        Object[] v = new Object[]{"sdfgdg", "1.0"};
        String an_str = an.toNiceString(v);
        assert Arrays.deepEquals(an.fromNiceString(an_str), v) : an_str + "!=" + an.toNiceString(an.fromNiceString(an_str));

        NumericArray na = new NumericArray();
        String na_str = na.toNiceNumericString(new double[]{1.0, 2.0});
        assert Arrays.equals(na.fromNiceNumericString(na_str), new double[]{1.0, 2.0}) : na_str + "!=" + na.toNiceNumericString(na.fromNiceNumericString(na_str));

        Sequence s = new Sequence();
        String s_str = s.toNiceNumericString(new double[]{1.0, 2.0, 3.0});
        assert Arrays.equals(s.fromNiceNumericString(s_str), new double[]{1.0, 2.0, 3.0}) : s_str + "!=" + s.toNiceNumericString(s.fromNiceNumericString(s_str));

        Numeric2D n2 = new Numeric2D();
        String n2_str = n2.toNiceNumericString(new double[]{1.0, 2.0});
        assert Arrays.equals(n2.fromNiceNumericString(n2_str), new double[]{1.0, 2.0}) : n2_str + "!=" + n2.toNiceNumericString(n2.fromNiceNumericString(n2_str));

        //Numeric2DArray n2a = new Numeric2DArray();
        //String n2a_str = n2a.toNiceNumericString(new double[][]{{1.0, 2.0, 3}, {10.0, 20.0, 30}});
        //assert Arrays.deepEquals(n2a.fromNiceNumericString(n2a_str), new double[][]{{1.0, 2.0, 3}, {10.0, 20.0, 30}}) : n2a_str + "!=" + n2a.toNiceNumericString(n2a.fromNiceNumericString(n2a_str));
        Numeric3D n3 = new Numeric3D();
        String n3_str = n3.toNiceNumericString(new double[]{1.0, 2.0, 3.0});
        assert Arrays.equals(n3.fromNiceNumericString(n3_str), new double[]{1.0, 2.0, 3.0}) : n3_str + "!=" + n3.toNiceNumericString(n3.fromNiceNumericString(n3_str));

        //System.out.println("" + new double[]{1, 2}.length);
        //System.out.println("" + new double[]{1, 2, 3}.length);
        //System.out.println("" + new double[][]{{1, 2, 3}, {1, 2, 3}}.length);
        //System.out.println("" + new double[][]{{1, 2, 3}, {1, 2, 3}, {1, 2, 3}}.length);
        //System.out.println("" + new double[][]{{1, 2, 3}, {1, 2, 3}, {1, 2, 3}, {1, 2, 3}}.length);
        //Numeric3DArray n3a = new Numeric3DArray();
        //String n3a_str = n3a.toNiceNumericString(new double[][]{{1.0, 2.0}, {10.0, 20.0}, {100.0, 200.0}});
        //assert Arrays.deepEquals(Numeric3DArray.fromNiceNumericString(n3a_str), new double[][]{{1.0, 2.0}, {10.0, 20.0}, {100.0, 200.0}}) : n3a_str + "!=" + n3a.toNiceNumericString(Numeric3DArray.fromNiceNumericString(n3a_str));
    }

    @Before
    public void setUp() {
        OutputFunctionExpression.initOutputFunctionTypes();
        Configuration.writeUserProperty = false;
    }
}
