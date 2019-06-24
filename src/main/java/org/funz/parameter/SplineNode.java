package org.funz.parameter;

import java.io.PrintStream;
import org.funz.XMLConstants;
import org.w3c.dom.Element;

public class SplineNode implements XMLConstants
{

    public SplineNode( double x, double y )
    {
        _x = x;
        _y = y;
    }
    
    public SplineNode()
    {
    }

	SplineNode( Element e ) throws Exception 
    {
		_x = Double.parseDouble( e.getAttribute( ATTR_X ) );
        _y = Double.parseDouble( e.getAttribute( ATTR_Y ) );
	}

	public void save( PrintStream ps )
    {
		ps.println("\t<" + ELEM_SPLINE_NODE + " " + 
                           ATTR_X + "=\"" + _x + "\" " + 
                           ATTR_Y + "=\"" + _y + "\"/>\n" );
	}

    public double getX(){ return _x; }

    public void setX( double x ) { _x = x; }

    public double getY(){ return _y; }
    
    public void setY( double y ) { _y = y; }
    
    private double _x;
    
    private double _y;
}
