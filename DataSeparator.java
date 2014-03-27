/* A class modeling a sensor that transmits location information.

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

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JOptionPane;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;

import ptolemy.data.StringToken;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// Locator

/**
 * This is a wireless sensor node that reacts to an input event by transmitting
 * an output with the current location of this node and the time of the input.
 * The output is a record token with type {location={double}, time=double}. The
 * location is an array with two doubles representing the X and Y positions of
 * the sensor. The location of the sensor is determined by the _getLocation()
 * protected method, which in this base class returns the location of the icon
 * in the visual editor, which is determined from the _location attribute of the
 * actor. If there is no _location attribute, then an exception is thrown.
 * Derived classes may override this protected method to specify the location in
 * some other way (or in more dimensions).
 * 
 * @author Philip Baldwin, Xiaojun Liu and Edward A. Lee
 * @version $Id: Locator.java,v 1.22 2005/10/27 15:36:09 cxh Exp $
 * @since Ptolemy II 4.0
 * @Pt.ProposedRating Yellow (eal)
 * @Pt.AcceptedRating Red (pjb2e)
 */
public class DataSeparator extends TypedAtomicActor {

	private static final long serialVersionUID = 1L;

	//private StringToken myValue;
	Queue <StringToken>myValue=  new  <StringToken>LinkedList() ;
	private double myTime;

	/**
	 * Construct an actor with the specified container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public DataSeparator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Create and configure the ports.
		input = new TypedIOPort(this, "input", true, false);

		outputChannel1 = new TypedIOPort(this, "outputChannel1", false, true);
		outputChannel1.setMultiport(true);
		myValue.add(new StringToken(""));
		myTime = 0;

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * @return the myValue
	 */
	public StringToken getValue() {
		return myValue.poll();
	}

	/**
	 * @param myValue
	 *            the myValue to set
	 */
	public void setValue(StringToken myValue) {
		this.myValue.add( myValue);
	}

	/**
	 * @return the myTime
	 */
	public double getTime() {
		return myTime;
	}

	/**
	 * @param myTime
	 *            the myTime to set
	 */
	public void setTime(double myTime) {
		this.myTime = myTime;
	}

	/**
	 * Port that receives a trigger input that causes transmission of location
	 * and time information on the <i>output</i> port.
	 */
	public TypedIOPort input;

	/**
	 * Name of the input channel. This is a string that defaults to
	 * "InputChannel".
	 */
	// public StringParameter federateName;

	/**
	 * Port that transmits the current location and the time of the event on the
	 * <i>input</i> port. This has type {location={double}, time=double}, a
	 * record token.
	 */
	public TypedIOPort outputChannel1;


	// private SlaveFederate rtiFederation;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Generate an event on the <i>output</i> port that indicates the current
	 * position and time of the last input on the <i>input</i> port. The value
	 * of the input is ignored.
	 */
	
	
	private final int DATA_SIZE = 300;
	private int countInteractions = 0;
	
	public void fire() throws IllegalActionException {
		super.fire();

		// System.out.println("MasterFederateActor - fire() at " +
		// this.getDirector().getModelTime());
		// dados recebidos do slave
		if (input.hasToken(0)) {

			Token inputValue = input.get(0);

			IntToken val = (IntToken) inputValue;
			//int value = val.intValue();
			StringToken s =new StringToken (val.intValue()+"");// StringToken.convert(val);
			// StringToken s = new StringToken(string);

		
			int faixa = DATA_SIZE/3;
            //Parameter finalTime = (Parameter)getAttribute("timeWindow");

			
			if (countInteractions <= faixa ) { 
				outputChannel1.send(0, s);
				countInteractions++;
			} else if (countInteractions > faixa && countInteractions <= (faixa*2)) {
				outputChannel1.send(1, s);
				countInteractions++;
			}else{
				outputChannel1.send(2, s);
				countInteractions++;
			}

		}

	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	/**
	 * Return the location of this sensor. In this base class, this is
	 * determined by looking for an attribute with name "_location" and class
	 * Location. Normally, a visual editor such as Vergil will create this icon,
	 * so the location will be determined by the visual editor. Derived classes
	 * can override this method to specify the location in some other way.
	 * 
	 * @return An array identifying the location.
	 * @exception IllegalActionException
	 *                If the location attribute does not exist or cannot be
	 *                evaluated.
	 */
	protected double[] _getLocation() throws IllegalActionException {
		Location locationAttribute = (Location) getAttribute("_location",
				Location.class);

		if (locationAttribute == null) {
			throw new IllegalActionException(this,
					"Cannot find a _location attribute of class Location.");
		}

		return locationAttribute.getLocation();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ptolemy.actor.AtomicActor#initialize()
	 */
	@Override
	public void initialize() throws IllegalActionException {
		// TODO Auto-generated method stub
		super.initialize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ptolemy.actor.AtomicActor#terminate()
	 */
	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		super.terminate();
	}

}
