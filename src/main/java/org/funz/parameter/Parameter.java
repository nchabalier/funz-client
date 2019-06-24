package org.funz.parameter;

import java.util.List;

/** Superclass for variables and groups. */
public interface Parameter   {

    public final static String PARAM_TYPE_NAMES[] = {"var.", "group", "empty"};

    /** Returns the value array. */
    public Variable.Value[] getValueArray();

    /** Returns the pos-th value. */
    public String getValueAsPathAt(int pos);
    public String getValueNode(int pos);

    public int getIndex();

    public void setIndex(int idx);
    
      /** Parameter type. */
    public static final int PARAM_TYPE_VARIABLE = 0,  PARAM_TYPE_VARGROUP = 1,  PARAM_TYPE_VOID = 2;

    @Override
    public boolean equals(Object other);

    /** Returns the parameter unique name. */
    public String getName();

    /** Return the parameter type. */
    public int getParameterType();

    /** Returns variables composing this group.
     * If the parameter is not a group returns null.
     */
    public List<Parameter> getGroupComponents();

    /** Returns the number of values. */
    public int getNmOfValues();

    /** Returns the smallest variable value.
     * If the parameter is a group of variables it returns 0 or parameter-variable minimal value.
     */
    public double getMinValue();

    /** Returns the highest variable value.
     * If the parameter is a group of variables it returns 1 or parameter-variable maximal value.
     */
    public double getMaxValue();

        /** Returns the smallest possible value.
     * If the parameter is not continous variable, throws exception.
     */
    public double getLowerBound();

    /** Returns the highest variable value.
     * If the parameter is not continous variable, throws exception.
     */
    public double getUpperBound();
    
    /** Says whether the parameter is a group of variables or not. */
    public boolean isGroup();

    /** Says whether the parameter is continuous */
    public boolean isContinuous();

    /** Says whether the parameter is real */
    public boolean isReal();

    /** Says whether the parameter is integer */
    public boolean isInteger();
}
