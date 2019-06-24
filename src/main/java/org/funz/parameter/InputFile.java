package org.funz.parameter;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import org.funz.XMLConstants;
import org.funz.log.Log;
import org.funz.log.LogCollector;
import org.funz.util.ASCII;
import org.funz.util.Digest;
import org.funz.util.Disk;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Associates an input file with the variable list found within this file.
 */
public class InputFile implements XMLConstants {

    private File _file/*, _source*/;
    public boolean hasParameters = true;
    String[] _path;
    private LinkedList<String> _varnames = new LinkedList<String>();
    public LinkedList<Variable> _vars = new LinkedList<Variable>();

    public InputFile(File file, String... path/*, File source*/) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("Could not create an InputFile with " + file);
        }
        if (isBinary(file)) {
            hasParameters = false;
            Log.logMessage("InputFile", LogCollector.SeverityLevel.INFO, false, "File " + file + " is binary.");
        }
        _file = file;
        _path = path;
        //_source = source;
    }

    @Override
    public String toString() {
        return "Input file: " + _file.getAbsolutePath() + " with" + (hasParameters ? " " : "out ") + "parameters " + _varnames;
    }

    public boolean isBinary(File file) {
        return Disk.isBinary(file);
    }

    public InputFile(File dir, Element e) throws Exception {
        _path = (String[]) StringToPath(e.getAttribute(ATTR_PATH));
        if (_path == null || _path.length == 0 || (_path.length == 1 && _path[0].length() == 0)) {
            _path = null;
        }
        if (_path != null) {
            _file = new File(dir + File.separator + ASCII.cat(File.separator, _path) + File.separator + e.getAttribute(ATTR_NAME));
        } else {
            _file = new File(dir + File.separator + e.getAttribute(ATTR_NAME));
        }

        if (e.hasAttribute(ATTR_PARAM)) {
            hasParameters = Boolean.parseBoolean(e.getAttribute(ATTR_PARAM));
        } else {
            if (isBinary(_file)) {
                hasParameters = false;
                Log.logMessage("InputFile", LogCollector.SeverityLevel.INFO, false, "File " + _file + " is binary.");
            }
        }

        NodeList vars = e.getElementsByTagName(ELEM_VAR);
        for (int i = 0; i < vars.getLength(); i++) {
            _varnames.add(((Element) vars.item(i)).getAttribute(ATTR_NAME));
        }
    }

    public void addVariable(Variable var) {
        if (!_vars.contains(var)) {
            _vars.add(var);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + (this._file != null ? this._file.hashCode() : 0);
        hash = 11 * hash + Arrays.deepHashCode(this._path);
        hash = 11 * hash + (this._vars != null ? this._vars.hashCode() : 0);
        System.err.println(this.toString()+": "+hash);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InputFile other = (InputFile) obj;
        if (this._file != other._file && (this._file == null || !this._file.equals(other._file))) {
            return false;
        }
        if (!Arrays.deepEquals(this._path, other._path)) {
            return false;
        }
        if (this._vars != other._vars && (this._vars == null || !this._vars.equals(other._vars))) {
            return false;
        }
        return true;
    }

    /**
     * Returns the file object.
     */
    public File getFile() {
        return _file;
    }

    /**
     * Returns the variables found inside the file.
     */
    public LinkedList<Variable> getVariables() {
        return _vars;
    }

    public LinkedList<String> getVarNames() {
        return _varnames;
    }

    public String[] getParentPath() {
        return _path;
    }

    public String getPath() {
        StringBuilder sb = new StringBuilder();
        if (_path != null) {
            for (String d : _path) {
                sb.append(d);
                sb.append(File.separatorChar);
            }
        }
        sb.append(_file.getName());
        return sb.toString();
    }

    public void releaseVarNames() {
        _varnames = null;
    }
    static char sep = ':';

    static String PathToString(String[] path) {
        if (path == null || path.length == 0 || (path.length == 1 && path[0].length() == 0)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : path) {
            sb.append(p);
            sb.append(sep);
        }
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    static String[] StringToPath(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        String[] p = s.split("" + sep);
        return p;
    }

    public void save(PrintStream ps) {
        ps.println("\t<" + ELEM_FILE + " " + ATTR_NAME + "=\"" + _file.getName() + "\""
                + " " + ATTR_PARAM + "=\"" + hasParameters + "\""
                + " " + ATTR_PATH + "=\"" + PathToString(_path) + "\">");

        for (Iterator<Variable> it = _vars.iterator(); it.hasNext();) {
            ps.println("\t\t<" + ELEM_VAR + " " + ATTR_NAME + "=\"" + ((Variable) it.next()).getName() + "\"/>");
        }

        ps.println("\t</" + ELEM_FILE + ">");
    }

    public void setParentPath(String... path) {
        _path = path;
    }

    String sum;

    public String getSum() {
        if (sum == null) {
            sum = new String(Digest.getSum(_file), StandardCharsets.UTF_8);
        }
        return sum;
    }
}
