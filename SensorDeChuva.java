/* An extension of Locator with a graphical rendition of the transmit range.

 Copyright (c) 2003-2005 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 */
package ptolemy.myactors.MaximumEntropy;

import ptolemy.actor.Director;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.vergil.icon.EditorIcon;
import ptolemy.vergil.kernel.attributes.EllipseAttribute;
import ptolemy.vergil.kernel.attributes.ResizablePolygonAttribute;

//////////////////////////////////////////////////////////////////////////
//// GraphicalLocator

/**
 This class is an extension of the Locator actor that adds an
 <i>outputRange</i> parameter and an icon that is a circle with
 a radius defined by the value of this parameter. In addition,
 this class provides the output channel with a transmit property named
 "range" with value equal to the value of its <i>outputRange</i>
 parameter.  Thus, if the output channel is an instance of
 LimitedRangeChannel, then the <i>outputRange</i> parameter
 will define the range of transmission.

 @author Philip Baldwin, Xiaojun Liu, and Edward A. Lee
 @version $Id: GraphicalLocator.java,v 1.24 2005/07/08 19:58:55 cxh Exp $
 @since Ptolemy II 4.0
 @Pt.ProposedRating Green (cxh)
 @Pt.AcceptedRating Yellow (cxh)
 @see LimitedRangeChannel
 */
public class SensorDeChuva extends Sensor {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Construct an actor with the specified container and name.
     *  @param container The container.
     *  @param name The name.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public SensorDeChuva(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        outputRange = new Parameter(this, "outputRange");
        outputRange.setToken("100.0");
        outputRange.setTypeEquals(BaseType.DOUBLE);

        output.outsideTransmitProperties.setExpression("{range = outputRange}");

        // Hide the ports in Vergil.
        SingletonParameter hide = new SingletonParameter(output, "_hide");
        hide.setToken(BooleanToken.TRUE);
        hide = new SingletonParameter(input, "_hide");
        hide.setToken(BooleanToken.TRUE);

        // Create an icon for this sensor node.
        EditorIcon node_icon = new EditorIcon(this, "_icon");

        // The icon has two parts: a circle and an antenna.
        // Create a circle that indicates the signal radius.
        _circle = new EllipseAttribute(node_icon, "_circle");
        _circle.centered.setToken("true");
        _circle.width.setToken("outputRange*2");
        _circle.height.setToken("outputRange*2");
        _circle.fillColor.setToken("{0.0, 0.0, 1.0, 0.08}");
        _circle.lineColor.setToken("{0.0, 0.5, 0.5, 1.0}");

        // Create the green antenna shape.
        ResizablePolygonAttribute antenna = new ResizablePolygonAttribute(
                node_icon, "antenna2");
        antenna.vertices.setToken("{0, -5, -5, -15, 5, -15, 0, -5, 0, 15}");
        antenna.width.setToken("10");
        antenna.height.setToken("30");

        // Set the color to green.
        antenna.fillColor.setToken("{0.0, 1.0, 0.0, 1.0}");

        node_icon.setPersistent(false);

        // Hide the name of this sensor node.
        hide = new SingletonParameter(this, "_hideName");
        hide.setToken(BooleanToken.TRUE);
        hide.setVisibility(Settable.EXPERT);
    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The output signal range. This is a double that
     *  defaults to 100.0.  The icon for this sensor node includes
     *  a circle with this as its radius.
     */
    public Parameter outputRange;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Generate an event on the <i>output</i> port that indicates the
     *  current position and time of the last input on the <i>input</i>
     *  port.  Also, change the color of the icon to red and schedule
     *  another firing after 1.0 time unit to change it back to blue.
     */
    public void fire() throws IllegalActionException {
    	super.fire();
    }

    /** Initialize the sensor node by setting its icon color to blue.
     *  @exception IllegalActionException If thrown by the base class.
     */
    public void initialize() throws IllegalActionException {
        super.initialize();

        // Set initial icon color to blue.
        _circle.fillColor.setToken("{0.0, 0.0, 1.0, 0.05}");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** Icon indicating the communication region. */
    private EllipseAttribute _circle;
}
