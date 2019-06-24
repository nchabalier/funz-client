package org.funz.parameter;

import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;

/** Used to describe the parsing syntax rules for variables and formulas.
 * Every Variable or formula type must have a starting symbol and delimiters
 * Example:
 * <pre>$VAR, $(VAR), !{ 10 + TAN(3) }</pre>
 */
public class SyntaxRules {
    
    public final static SyntaxRules NoSyntax = new SyntaxRules();

    public static class Limit {

        public final char LEFT, RIGHT;
        public final String NAME;

        private Limit(char left, char right, String n) {
            LEFT = left;
            RIGHT = right;
            NAME = n;
        }

        @Override
        public String toString() {
            return LEFT + "..." + RIGHT;//+ " - " + NAME;
        }
    }

    public static class StartSymbol {

        public final char CHAR;
        public final String NAME;

        private StartSymbol(char c, String n) {
            CHAR = c;
            NAME = n;
        }

        @Override
        public String toString() {
            return CHAR + "" /*+ " - " + NAME*/;
        }
    }
    public static final Limit LIMITS[] = {new Limit('(', ')', "parenthesis"),
     /*=> do not allow because of formula variables which need it (and may be used in default variable value after "~"*/
                                          new Limit('{', '}', "brackets"),
                                          new Limit('[', ']', "square brackets"),
                                          new Limit('<', '>', "less/greater")
    };
    public static final int START_SYMBOL_DOLLAR = 0,
            START_SYMBOL_HASH = 1,
            START_SYMBOL_STAR = 2,
            START_SYMBOL_AMPERSAND = 3,
            START_SYMBOL_AT = 4,
            START_SYMBOL_EXCLAMATION = 5,
            START_SYMBOL_QUESTION = 6,
            START_SYMBOL_TWOPOINTS = 7,
            START_SYMBOL_PERCENT = 8,
            START_SYMBOL_EURO = 9,
            START_SYMBOL_POUND = 10,
            START_SYMBOL_YEN = 11,
            LIMIT_SYMBOL_PARENTHESIS = 0,
            LIMIT_SYMBOL_BRACKETS = 1,
            LIMIT_SYMBOL_SQ_BRACKETS = 2,
            LIMIT_SYMBOL_LT_GT = 3;
    public static final String START_SYMBOL_STRINGS[], LIMIT_STRINGS[];
    public static final StartSymbol START_SYMBOLS[] = {new StartSymbol('$', "dollar"),
                                                       new StartSymbol('#', "hash"),
                                                       new StartSymbol('*', "star"),
                                                       new StartSymbol('&', "ampersand"),
                                                       new StartSymbol('@', "comercial at"),
                                                       new StartSymbol('!', "exlamation point"),
                                                       new StartSymbol('?', "question point"),
                                                       new StartSymbol(':', "two points"),
                                                       new StartSymbol('%', "percent"),
                                                       new StartSymbol('\u20AC', "euro"),
                                                       new StartSymbol('\u00A3', "pound"),
                                                       new StartSymbol('\u00A5', "yen")
    };

    static {
        START_SYMBOL_STRINGS = new String[START_SYMBOLS.length];
        for (int i = 0; i < START_SYMBOLS.length; i++) {
            START_SYMBOL_STRINGS[i] = START_SYMBOLS[i].toString();
        }
        LIMIT_STRINGS = new String[LIMITS.length];
        for (int i = 0; i < LIMITS.length; i++) {
            LIMIT_STRINGS[i] = LIMITS[i].toString();
        }
    }

    public static int getLimitsIdxByName(String name) {
        for (int i = 0; i < LIMITS.length; i++) {
            if (LIMITS[i].NAME.equals(name)) {
                return i;
            }
        }

        return -1;
    }

    public static int readLimits(String c) {
        for (int i = 0; i < LIMITS.length; i++) {
            if (LIMITS[i].toString().equals(c)) {
                return i;
            }
        }
        Log.logMessage(null, SeverityLevel.PANIC, true, "Impossible to read Limits: " + c);
        return -1;
    }

    public static int findLimitSymbol(char c) {
        for (int i = 0; i < LIMITS.length; i++) {
            if ((LIMITS[i].LEFT == c) || (LIMITS[i].RIGHT == c)) {
                return i;
            }
        }
        Log.logMessage(null, SeverityLevel.PANIC, true, "Impossible to read Limits: " + c);
        return -1;
    }

    public static int getStartSymbolIdxByName(String name) {
        for (int i = 0; i < START_SYMBOLS.length; i++) {
            if (START_SYMBOLS[i].NAME.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public static int readStartSymbol(String c) {
        for (int i = 0; i < START_SYMBOLS.length; i++) {
            if (START_SYMBOLS[i].toString().equals(c)) {
                return i;
            }
        }
        Log.logMessage(null, SeverityLevel.PANIC, true, "Impossible to read StartSymbol: " + c);
        return -1;
    }

    public static int findStartSymbol(char c) {
        for (int i = 0; i < START_SYMBOLS.length; i++) {
            if (START_SYMBOLS[i].CHAR == c) {
                return i;
            }
        }
        Log.logMessage(null, SeverityLevel.PANIC, true, "Impossible to read StartSymbol: " + c);
        return -1;
    }
    private int _startSymbolIdx = 0, _limitIdx = 0;

    public SyntaxRules() {
    }

    /** Commonly used constructor.
     * Example:
     * <pre> 
     * new SyntaxRule( SyntaxRule.START_SYMBOL_DOLLAR, SyntaxRules.LIMIT_SYMBOL_PARENTHESIS );
     * </pre>
     */
    public SyntaxRules(int startSymbol, int limitSymbol) {
        _startSymbolIdx = startSymbol;
        _limitIdx = limitSymbol;
    }

    public char getLeftLimitSymbol() {
        return LIMITS[_limitIdx].LEFT;
    }

    public int getLimitsIdx() {
        return _limitIdx;
    }

    public char getRightLimitSymbol() {
        return LIMITS[_limitIdx].RIGHT;
    }

    public char getStartSymbol() {
        return START_SYMBOLS[_startSymbolIdx].CHAR;
    }

    public int getStartSymbolIdx() {
        return _startSymbolIdx;
    }

    public void setLimits(int idx) {
        _limitIdx = idx;
    }

    public void setStartSymbol(int idx) {
        _startSymbolIdx = idx;
    }

    public String toString() {
        return START_SYMBOLS[_startSymbolIdx].toString() + LIMITS[_limitIdx].toString();
    }
}
