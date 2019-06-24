package org.funz.parameter;

/** Replaceable text fragment */
public class Replaceable {
	public static final int TYPE_VAR = 0, TYPE_FORMULA = 1;

	public final int line, lastLine, colStart, colEnd;

	public final String name;

	public final int type;

	/** Constructor for variables */
	public Replaceable(final String n, final int l, final int start, final int end) {
		name = n;
		line = l;
		colStart = start;
		colEnd = end;
		lastLine = line;
		type = TYPE_VAR;
	}

	/** Constructor for formulas */
	public Replaceable(final String n, final int l1, final int l2, final int start, final int end) {
		name = n;
		line = l1;
		lastLine = l2;
		colStart = start;
		colEnd = end;
		type = TYPE_FORMULA;
	}
}
