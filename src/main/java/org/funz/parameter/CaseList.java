package org.funz.parameter;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.funz.Project;
import static org.funz.XMLConstants.*;
import org.funz.util.ASCII;
import static org.funz.util.Format.repeat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CaseList extends ArrayList<Case> {

    public CaseList(int initialCapacity) {
        ensureCapacity(initialCapacity);
    }

    public CaseList() {
    }

    public void save(File out) {

        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(out));
            save(ps);
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
                ee.printStackTrace(System.err);
            }
        }
    }

    public void save(PrintStream ps) {
        ps.println("<?xml version=\"1.0\" encoding=\"" + ASCII.CHARSET + "\"?>");
        ps.println("<" + ELEM_CASE_LIST + ">");
        for (Case c : this) {
            c.save(ps);
        }
        ps.println("</" + ELEM_CASE_LIST + ">");
    }

    public void load(File file, Project prj) throws ParserConfigurationException, SAXException, IOException {
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        Element e = d.getDocumentElement();
        if (!e.getTagName().equals(ELEM_CASE_LIST)) {
            throw new IllegalArgumentException("wrong XML element " + e.getTagName() + " in file " + file);
        }
        clear();
        NodeList cases = e.getElementsByTagName(ELEM_CASE);
        for (int i = 0; i < cases.getLength(); i++) {
            add(new Case((Element) cases.item(i), prj));
        }

    }

    @Override
    public String toString() {
        StringBuilder outexps = new StringBuilder(this.size()*5);
        int l = 0;
        for (Case c : this) {
            String c_str = c.toString();
            l = Math.max(l, c_str.length());
            outexps.append(c_str).append("\n");
        }
        outexps.insert(0, repeat(l, "", "=") + "\n");
        outexps.append(repeat(l, "", "=")).append("\n");
        return outexps.toString();
    }

    public String toCSV(char sep, String... columns) {
        StringWriter s_writer = new StringWriter();
        CSVWriter writer = new CSVWriter(s_writer, sep);
        try {
            writer.writeNext(columns);
            String[] values;
            for (Case c : this) {
                values = new String[columns.length];
                Properties info = c.getInfo();
                for (int i = 0; i < columns.length; i++) {
                    if (info!=null)
                        values[i] = info.getProperty(columns[i], "-");
                    else 
                        values[i] = "?";
                }
                writer.writeNext(values);
            }
            writer.flush();
        } catch (IOException ex) {
            return ex.getLocalizedMessage();
        }
        return s_writer.toString();
    }

    public Case find(Case test) {
        for (Iterator<Case> it = this.iterator(); it.hasNext();) {
            Case c = it.next();
            if (c.equals(test)) {
                return c;
            }
        }
        return null;
    }
}
