/* Triangulate to identify the origin of a signal.

 Copyright (c) 2003-2008 The Regents of the University of California.
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

import ptolemy.actor.TypeAttribute;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import ptolemy.vergil.fsm.FSMGraphController.NewStateAction;
import sun.security.krb5.internal.Ticket;

import hla.rti.RTIambassador;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.MultiProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.cmd.CpuInfo;

//////////////////////////////////////////////////////////////////////////
//// Triangulator

/**
 Given inputs that represent the location of a sensor and the time at
 which those sensors detect an event, this actor outputs the location
 of the source of the event.  It uses the specified
 <i>signalPropagationSpeed</i> and triangulates.
 <p>
 The input is a record with two fields named "location" and "time".
 The location field is an array of doubles, and the time is a double.
 In the current implementation, the actor assumes a two-dimensional space,
 so the location field is assumed to be an array with two doubles,
 which represent the horizontal (east-west) and vertical (north-south)
 location of the sensor.  The time field is assumed to be
 a double representing the time at which the event was detected by
 the sensor.
 <p>
 The triangulation algorithm requires three distinct sensor inputs
 for the same event in order to be able to calculate the location
 of the origin of the event.  Suppose that the event occurred at
 time <i>t</i> and location <i>x</i>.  Suppose that three sensors
 at locations <i>y1</i>, <i>y2</i>, and <i>y3</i> have each detected
 events at times <i>t1</i>, <i>t2</i>, and <i>t3</i>, respectively.
 Then this actor looks for a solution for <i>x</i> and <i>t</i>
 in the following equations:
 <quote>
 distance(<i>x</i>, <i>y1</i>)/<i>v</i> = <i>t1</i> - <i>t</i>,
 <br>
 distance(<i>x</i>, <i>y2</i>)/<i>v</i> = <i>t2</i> - <i>t</i>,
 <br>
 distance(<i>x</i>, <i>y3</i>)/<i>v</i> = <i>t3</i> - <i>t</i>,
 <br>
 </quote>
 where <i>v</i> is the value of <i>propagationSpeed</i>.
 If such a solution is found, then the output <i>x</i> is produced.
 <p>
 Since three distinct observations are required, this actor will
 produce no output until it has received three distinct observations.
 The observations are distinct if the sensor locations are distinct.
 If the three observations yield no solution, then this actor
 will produce no output.  However, it is possible for the three
 observations to come from distinct events, in which case the output
 may be erroneous.  To guard against this, this actor provides a
 <i>timeWindow</i> parameter.  The times of the three observations
 must be within the value of <i>timeWindow</i>, or no output will
 be produced. The output is an array of doubles, which in this
 implementation represent the X and Y locations of the event.

 @author Xiaojun Liu, Edward A. Lee
 @version $Id: Triangulator.java,v 1.19.8.4 2008/04/01 14:32:28 cxh Exp $
 @since Ptolemy II 4.0
 @Pt.ProposedRating Yellow (eal)
 @Pt.AcceptedRating Red (ptolemy)
 */
public class SensorDataForecasting extends TypedAtomicActor {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	 ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The input port for an event detection, which is a record
     *  containing the location of a sensor that has detected the
     *  event and the time at which the sensor detected the event.
     *  This has type {location = {double}, time = double} The location
     *  is assumed to have two entries.
     */
    public TypedIOPort input;
    
    /** The output producing the calculated event location.
     *  This has type {double}, a double array with two entries.
     */
    public TypedIOPort output_location;
    
    public TypedIOPort output_time;
    
    public TypedIOPort output_value;
    
    public TypedIOPort output_sensor;


    /** Time window within which observations are assumed to come from
     *  the same sound event.
     *  This is a double that defaults to 0.5 (in seconds).
     */
    public Parameter timeWindow;
    
    /** How late the forecast will be targeted
     * 
     */
    public Parameter timeForecasting;
	
    /** Number of amounts used to do the forecasting
     * 
     */
    public Parameter amounts;
		
	 ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
    // Buffer of four readings.
    
    private int numberOfAmounts = 0;
    
    private double[] _locationsX = new double[2];

    private double[] _locationsY = new double[2];

    private double[] _times = new double[2];
    
    private int[] _values = new int[2];
    
    private int storedData = 0;
    
    private double sumOfLocationsX = 0;
    private double sumOfLocationsY = 0;
    private int sumOfValues = 0;
    private double firstDataTime = 0;
    
    private ArrayList<String> ids = new ArrayList<String>();
    
    private double proximoTempo = 0;
    
    private int periodo = 2;
    

	
	/** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public SensorDataForecasting(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        input = new TypedIOPort(this, "input", true, false);
        TypeAttribute inputType = new TypeAttribute(input, "type");
        inputType.setExpression("{location = {double,2}, time = double, value = int}");        
        
        output_location = new TypedIOPort(this, "output_location", false, true);        
        TypeAttribute outputType = new TypeAttribute(output_location, "type");
        outputType.setExpression("{double, 2}");

        output_time = new TypedIOPort(this, "output_time", false, true);
        TypeAttribute outputType2 = new TypeAttribute(output_time, "type");
        outputType2.setExpression("double");

        output_value = new TypedIOPort(this, "output_value", false, true);
        TypeAttribute outputType3 = new TypeAttribute(output_value, "type");
        outputType3.setExpression("{time = double, pessoa = string, idSensorCentral = int }");
        
        output_sensor = new TypedIOPort(this, "output_sensor", false, true);
        TypeAttribute outputType4 = new TypeAttribute(output_sensor, "type");
        outputType4.setExpression("int");
        
        // Create parameters.
        timeWindow = new Parameter(this, "timeWindow");
        timeWindow.setToken("0.5");
        timeWindow.setTypeEquals(BaseType.DOUBLE);
        
        timeForecasting = new Parameter(this, "timeForecasting");
        timeForecasting.setToken("10.0");
        timeForecasting.setTypeEquals(BaseType.DOUBLE);
        
        amounts = new Parameter(this, "amounts");
        amounts.setToken("5");
        amounts.setTypeEquals(BaseType.INT);
        
        
        
        _times[0] = -1.0;
        _times[1] = -1.0;
    }

   

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Clone this actor into the specified workspace. This overrides the
     *  base class to handle private variables.
     *  @param workspace The workspace for the cloned object.
     *  @exception CloneNotSupportedException If cloned ports cannot have
     *   as their container the cloned entity (this should not occur), or
     *   if one of the attributes cannot be cloned.
     *  @return A new ComponentEntity.
     */
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        SensorDataForecasting newObject = (SensorDataForecasting)super.clone(workspace);
        newObject._locationsX = new double[2];
        newObject._locationsY = new double[2];
        newObject._times = new double[2];
        newObject._values = new int[2];
        return newObject;
    }

    /** Read all available input tokens and attempt to use them to triangulate
     *  the signal source. If the attempt is successful, then output
     *  the location of the signal source. Otherwise, output nothing.
     *  This method keeps a buffer of the three most recently seen
     *  inputs from distinct sensors (as determined by their locations).
     *  If a new input token has a location field that matches
     *  a location already in the buffer, then the new input simply
     *  replaces that previous observation.  Otherwise, it replaces
     *  the oldest observation in the buffer.  If there are three
     *  observations in the buffer with time stamps within the
     *  <i>timeWindow</i> parameter value, then triangulation is
     *  performed, and if the the three observations are consistent,
     *  then an output is produced.
     *  @exception IllegalActionException If there is no director,
     *   or if the parameters cannot be evaluated.
     */
    
    int aux = 0, cont = 0;
    HashSet<Integer> ps = new HashSet<Integer>();
    public void fire() throws IllegalActionException {
        super.fire();

        if (input.hasToken(0)) {
            RecordToken recordToken = (RecordToken) input.get(0);

            ArrayToken locationArray = (ArrayToken) recordToken.get("location");

            if (locationArray.length() < 2) {
                throw new IllegalActionException(this,
                        "Input is malformed: location field does not "
                                + "have two entries.");
            }

            double locationX = ((DoubleToken) locationArray.getElement(0))
                    .doubleValue();
            double locationY = ((DoubleToken) locationArray.getElement(1))
                    .doubleValue();
            
            double time = ((DoubleToken) recordToken.get("time")).doubleValue();
            int pessoa = ((IntToken) recordToken.get("value")).intValue();
            
            // First check whether the location matches one already in the
            // buffer.  At the same time, identify the entry with the
            // oldest time and the newest time.
                                  
            
            //Deixe isso aqui, por enquanto, mas ele esta sendo inutil                       
            
            Parameter sens = (Parameter)getAttribute("idSensor");
            String idSensor = sens.getValueAsString();
           
            Parameter finalTime = (Parameter)getAttribute("timeWindow");
            double finalT = Double.parseDouble(finalTime.getValueAsString());
                                    
            String[] labels = { "time", "pessoa", "idSensorCentral" };
            Token[] values = { new DoubleToken(time), new StringToken(ps.toString()) , new IntToken(idSensor)};
            Token result = new RecordToken(labels, values);
            
                        
//            System.out.println("--------------:"+finalT);
//            if(aux < finalT){
            	ps.add(pessoa);
//            	System.out.println("akiiii 11111 SF");
//            	aux++;
//            }else{          
//            	System.out.println("akiiii 2222222222 SF");
//            	cont++;                                
//                System.out.println(result.toString()+" - "+capturarEstatisticas());
//                if (ps.size() == 10)
//                	ps.clear();
               
//                aux = 0;
                
                output_value.send(0, result); //important               
                output_time.send(0, new DoubleToken(time));                       
                output_location.send(0, locationArray); 
                
                System.out.println(idSensor + " - " + time + " - " + ps.toString());
//            }
            
           // gravarArquivoDeteccao(result, idSensor);
          //  System.out.println("Forecasting - time: " + timeResult + " Location: " + targetLocationArray.toString() + " Value: " + valueResult);                                               
        }
    }
    
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

    public void gravarArquivoDeteccao(Token token, String idSensor){
    	BufferedWriter arquivo;								
		String str = token.toString();
    	try {
			arquivo = new BufferedWriter(new FileWriter("config/sensorCentral"+idSensor+".txt", true));
			arquivo.write(str);
            arquivo.newLine();
			arquivo.close();
		} catch (IOException e1) {
			//e1.printStackTrace();
		}
    }
    
    
    // Ao inves de estimar deslocamento, agora precisamos armazenar os IDs e posicoes de cada pessoa!!!
	private double[] doLocationForecasting(double[] locationsX, double[] locationsY, double[] times, 
			 double elapsedTime) {
		
		int timeSpan = (int)(_times[1] - _times[0]);
		
		double result[] = new double[2];
		
		if(timeSpan == 0){
			result[0] = locationsX[1];
			result[1] = locationsY[1];
			return result;
			
		}
		
		double locationXVariation = locationsX[1] - locationsX[0];
		double locationYVariation = locationsY[1] - locationsY[0];
		
		
		
		result[0] = locationsX[1] + (int)(locationXVariation * (elapsedTime/timeSpan));
	/*	
		System.out.println("LocationX: " + locationsX[1]);
		System.out.println("LocationXVariation: " + locationXVariation);
		System.out.println("LocationY: " + locationsY[1]);
		System.out.println("LocationYVariation: " + locationYVariation);
		
		
		System.out.println("elapsedTime: " + elapsedTime);
		System.out.println("timeSpan: " + timeSpan);
		System.out.println("Previsao: " + result[0] + "\n");
		
		*/
		
		result[1] = locationsY[1] + (int)(locationYVariation * (elapsedTime/timeSpan));
		
		
		return result;
	}



	private int doValueForecasting(int[] values, double[] times, double elapsedTime) {
		int timeSpan = (int)(_times[1] - _times[0]);
		
		int result = 0;
		if(timeSpan == 0){
			result = values[1];
			return result;
			
		}
		
		//System.out.println("timeSpan: " + timeSpan);
		
		int valueVariation = _values[1] - _values[0];
		
		//System.out.println("valueVariation: "+ valueVariation);
		
		result = _values[1] + ((int)(valueVariation * (int)(elapsedTime/timeSpan)));
		
		//System.out.println("Value: " + result);
		
		return result;
	}
    
	private void logReceivedData(double locationX, double locationY,
			double time, int value) throws IllegalActionException {
		
		
			if(ids.contains(value+"") == false)
				ids.add(value+"");
			
			if (time > proximoTempo){				
				System.out.println("location: (" + locationX + ", " + locationY + ")");
				System.out.println("time: " + time);				
				System.out.println("ID: " + ids.toString());
				//System.out.println("sensor that detected:"+sensor+ "\n");
				proximoTempo = proximoTempo + periodo;
				
				//zerar ids:
				ids = new ArrayList<String>();
				
			}
	}

	private void switchDataPosition() {
		
		double tempTime = _times[1];
		int tempValue = _values[1];
		double tempLocationX = _locationsX[1];
		double tempLocationY = _locationsY[1];
		
		_times[1] = _times[0];
		_values[1] = _values[0];  
		_locationsX[1] = _locationsX[0];  
		_locationsY[1] = _locationsY[0];
		
		_times[0] = tempTime;
		_values[0] = tempValue;  
		_locationsX[0] = tempLocationX;  
		_locationsY[0] = tempLocationY;
		
		
	}


	/** Override the base class to initialize the signal count.
     *  @exception IllegalActionException If the base class throws it.
     */
    public void initialize() throws IllegalActionException {
        super.initialize();

        numberOfAmounts = 0;
        storedData = 0;
        sumOfLocationsX = 0;
        sumOfLocationsY = 0;
        sumOfValues = 0;
        firstDataTime = 0;
        
        
        for (int i = 0; i < 2; i++) {
            _locationsX[i] = 0;
            _locationsY[i] = 0;
            _times[i] = -1;
            _values[i] = 0;
        }
    }

    

   

   
}








