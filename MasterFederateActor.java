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

import java.io.BufferedWriter;
import java.util.HashSet;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.jlc.EncodingHelpers;
import ptolemy.actor.TypeAttribute;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
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
 This is a wireless sensor node that reacts to an input event by
 transmitting an output with the current location of this node and
 the time of the input.  The output is a record token with type
 {location={double}, time=double}.  The location is an array with
 two doubles representing the X and Y positions of the sensor.
 The location of the sensor is determined by the _getLocation()
 protected method, which in this base class returns the location
 of the icon in the visual editor, which is determined from the
 _location attribute of the actor.  If there is no _location
 attribute, then an exception is thrown.  Derived classes may
 override this protected method to specify the location in some
 other way (or in more dimensions).

 @author Philip Baldwin, Xiaojun Liu and Edward A. Lee
 @version $Id: Locator.java,v 1.22 2005/10/27 15:36:09 cxh Exp $
 @since Ptolemy II 4.0
 @Pt.ProposedRating Yellow (eal)
 @Pt.AcceptedRating Red (pjb2e)
 */
public class MasterFederateActor extends TypedAtomicActor implements PtolemyFederateActor {
    
	private static final long serialVersionUID = 1L;

	//angelo - mudei
	//private IntToken myValue;
	private StringToken myValue;

	
	private double myTime;
	
	//angelo - mudando de Interaction para Attributes
	//private Interaction interactionToSend = null; //comentei
	
	private Attributes attributesToSend = null;
	
	private boolean hasDataToSend = false;
	
	//angelo
	private boolean hasDataToReceive = false;
	
	/** Construct an actor with the specified container and name.
     *  @param container The container.
     *  @param name The name.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public MasterFederateActor(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        //rtiFederation = new SlaveFederate();
        
        // Create and configure the parameters.
       
        // federateName = new StringParameter(this, "federateName");
        //federateName.setExpression("PtolemyFederate");

        
        // Create and configure the ports.
        input = new TypedIOPort(this, "input", true, false);
        input2 = new TypedIOPort(this, "inputChannel2", true, false);
        input3 = new TypedIOPort(this, "inputChannel3", true, false);

        output = new TypedIOPort(this, "output", false, true);
//        TypeAttribute outputType = new TypeAttribute(output, "type");
//        outputType.setExpression("String");
        
        myValue = new StringToken("");
        myTime = 0;

        
    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    public boolean hasDataToSend(){
    	return hasDataToSend;
    }
    
    public boolean hasDataToReceive(){
    	return hasDataToReceive;
    }
    
    /**
	 * @return the myValue
	 */
	public StringToken getValue() {
		return myValue;
	}
	
	public StringToken getDataToSend() {
		hasDataToSend = false;
		return myValue;
	}

	/**
	 * @param myValue the myValue to set
	 */
	public void setValue(StringToken myValue) {
		this.myValue = myValue;
	}
	
	
	/**
	 * @return the myTime
	 */
	public double getTime() {
		return myTime;
	}

	/**
	 * @param myTime the myTime to set
	 */
	public void setTime(double myTime) {
		this.myTime = myTime;
	}

	/** Port that receives a trigger input that causes transmission
     *  of location and time information on the <i>output</i> port.
     */
    public TypedIOPort input;
    public TypedIOPort input2;
    public TypedIOPort input3;

    
    /** Name of the input channel. This is a string that defaults to
     *  "InputChannel".
     */
   // public StringParameter federateName;

    /** Port that transmits the current location and the time
     *  of the event on the <i>input</i> port.  This has
     *  type {location={double}, time=double}, a record token.
     */
    public TypedIOPort output;

    //private SlaveFederate rtiFederation;
    
    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////
    
    public String capturarEstatisticas(){
    	try {
			Sigar sigar = new Sigar();
    		
    		BufferedWriter arquivo;								
			String str = "";
			CpuPerc cpu;
			cpu = sigar.getCpuPerc();
			Mem memory = sigar.getMem();

			CpuPerc cpus[] = sigar.getCpuPercList();
			
			double totalMemory = (double)memory.getTotal() / (1024*1024*1024);
			double usedMemory = (double)memory.getUsed() / (1024*1024*1024); //vira GB - origianl eh bytes
	        double freeMemory = (double)memory.getFree() / (1024*1024*1024);
	        double usedMemoryPercent = (double)memory.getUsedPercent();
			
	        Runtime runtime = Runtime.getRuntime();
	        double totalMemoryJVM = (double)runtime.totalMemory() / (1024*1024); //valor utilizado pela JVM - em bytes
	        double freeMemoryJVM = (double)runtime.freeMemory() / (1024*1024);
	        double maxMemory = (double)runtime.maxMemory() / 1024;
	        
	        String cpu_pc = CpuPerc.format(cpu.getUser() + cpu.getSys());
	        String cpu_jvm = CpuPerc.format(cpu.getSys());
	        
	        String cpu_pc_0 = CpuPerc.format(cpus[0].getUser() + cpus[0].getSys());
	        String cpu_jvm_0 = CpuPerc.format(cpus[0].getSys());

	        
	        String cpu_pc_1 = CpuPerc.format(cpus[1].getUser() + cpus[1].getSys());
	        String cpu_jvm_1 = CpuPerc.format(cpus[1].getSys());	        
	        
	        str = usedMemoryPercent+"% - "+ ((totalMemoryJVM - freeMemoryJVM)/totalMemoryJVM)*100
	        +"% - "+cpu_pc+" - "+cpu_jvm+"% - "+cpu_pc_0+" - "+cpu_jvm_0+"% - "+cpu_pc_1+" - "+cpu_jvm_1;
	        
//			arquivo = new BufferedWriter(new FileWriter("config/experimentos_mem_used.txt", true));
//			arquivo.write(str);
//			arquivo.newLine();
//	        arquivo.close();
	        
	        return str;
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}		
    }

    /** Generate an event on the <i>output</i> port that indicates the
     *  current position and time of the last input on the <i>input</i>
     *  port.  The value of the input is ignored.
     */
    int aux = 0, aux1= 0 ;
    public void fire() throws IllegalActionException {
        super.fire();
        
      //  System.out.println("MasterFederateActor - fire() at " + this.getDirector().getModelTime());
      //dados recebidos do slave
    /*    if(attributesToSend != null){
        	
        	String string, time, pessoa;
			try {						        
//				Parameter finalTime = (Parameter)getAttribute("timeWindow");
//		        double finalT = Double.parseDouble(finalTime.getValueAsString());
		 
//		        System.out.println("--------------:"+finalT);
//	            if(aux < finalT){
//	            	System.out.println("akiiii 11111 MF REC");
//	            	aux++;
//	            }else{    
//	            	aux = 0;
	            	
	                
	            string = EncodingHelpers.decodeString(attributesToSend.getReceivedData().getValue(0));
						       
	            	
				StringToken s = new StringToken(string);
	            						
			//	output.send(0,s); //joga na saída do master - era exibido no display
	            	
//	            }
//				System.out.println(idSensor + " - " + time + " - " + pessoa + " - "+capturarEstatisticas());
				
				//System.out.println(string );
				//syso
//				System.out.println("Data received by MasterFederateActor.fire() at " + this.getDirector().getModelTime() + ": "
//						+ idSensor + " - " + time + " - " + pessoa);
	            
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
			attributesToSend = null;
        }*/
        
        //If there is anything in the input to send
        //angelo - estava comentado - novo modelo
        if (input.hasToken(0)) {
            Token inputValue = input.get(0);
                    
            Token val =inputValue;
            StringToken value = StringToken.convert(val);

           double timeValue = getDirector().getModelTime().getDoubleValue();
            
            Parameter finalTime = (Parameter)getAttribute("timeWindow");
	        double finalT = Double.parseDouble(finalTime.getValueAsString());
	 
//	        System.out.println("--------------:"+finalT);
            if(aux1 < finalT){
//            	System.out.println("akiiii 11111 MF ENV");
            	aux1++;
            	hasDataToSend = false;
            }else{    
            	aux1 = 0;
//            	System.out.println("master enviou");
            	
            	this.setValue(new StringToken("channel1" + " - " + value));
                this.setTime(timeValue);           
                hasDataToSend = true;
            }            
          
            output.send(0, value);
            //System.out.println("MasterFederateActor - message sent: " + value.toString());
        }else
        
        
        if (input2.hasToken(0)) {
            Token inputValue = input2.get(0);
                    
            Token val =inputValue;
            StringToken value = StringToken.convert(val);

           double timeValue = getDirector().getModelTime().getDoubleValue();
            
            Parameter finalTime = (Parameter)getAttribute("timeWindow");
	        double finalT = Double.parseDouble(finalTime.getValueAsString());
	 
//	        System.out.println("--------------:"+finalT);
            if(aux1 < finalT){
//            	System.out.println("akiiii 11111 MF ENV");
            	aux1++;
            	hasDataToSend = false;
            }else{    
            	aux1 = 0;
//            	System.out.println("master enviou");
            	
            	this.setValue(new StringToken("channel2" + " - " + value));
                this.setTime(timeValue);           
                hasDataToSend = true;
            }            
          
            output.send(0, value);
            //System.out.println("MasterFederateActor - message sent: " + value.toString());
        }else
        
        if (input3.hasToken(0)) {
            Token inputValue = input3.get(0);
                    
            Token val =inputValue;
            StringToken value = StringToken.convert(val);

           double timeValue = getDirector().getModelTime().getDoubleValue();
            
            Parameter finalTime = (Parameter)getAttribute("timeWindow");
	        double finalT = Double.parseDouble(finalTime.getValueAsString());
	 
//	        System.out.println("--------------:"+finalT);
            if(aux1 < finalT){
//            	System.out.println("akiiii 11111 MF ENV");
            	aux1++;
            	hasDataToSend = false;
            }else{    
            	aux1 = 0;
//            	System.out.println("master enviou");
            	
            	this.setValue(new StringToken("channel3" + " - " + value));
                this.setTime(timeValue);           
                hasDataToSend = true;
            }            
          
            output.send(0, value);
            //System.out.println("MasterFederateActor - message sent: " + value.toString());
        }
        
        //angelo - estava comentado - novo modelo
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Return the location of this sensor. In this base class,
     *  this is determined by looking for an attribute with name
     *  "_location" and class Location.  Normally, a visual editor
     *  such as Vergil will create this icon, so the location will
     *  be determined by the visual editor.  Derived classes can
     *  override this method to specify the location in some other way.
     *  @return An array identifying the location.
     *  @exception IllegalActionException If the location attribute does
     *   not exist or cannot be evaluated.
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

	/* (non-Javadoc)
	 * @see ptolemy.actor.AtomicActor#initialize()
	 */
	@Override
	public void initialize() throws IllegalActionException {
		// TODO Auto-generated method stub
		super.initialize();
	}

	/* (non-Javadoc)
	 * @see ptolemy.actor.AtomicActor#terminate()
	 */
	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		super.terminate();
	}

	public void addInteractionToSend(Interaction inter) {
		//System.out.println("Data received by MasterFederateActor " + inter.getReceivedData() + " at "+ inter.getReceivedTime());
		//interactionToSend = inter; //comentei
	}

	@Override
	public void updateAtributesToSend(Attributes attr) {
		//System.out.println("Data received by MasterFederateActor " + inter.getReceivedData() + " at "+ inter.getReceivedTime());
		attributesToSend = attr;
		
	}

	
	
	
    
    
    
}
