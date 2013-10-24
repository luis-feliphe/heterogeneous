package ptolemy.myactors.MaximumEntropy;

import hla.rti.RTIexception;

public interface PtolemyFederate {
	
	//angelo - mudei
//	public void sendData(int data) throws RTIexception;
	public void sendData(String data) throws RTIexception;

	 //angelo - mudando de Interaction para Attributes
//	 public Interaction receivedData(double time);	    
//	 public Interaction consumeReceivedData(double time);
	
	 public Attributes receivedData(double time);	    
	 
	 public Attributes consumeReceivedData(double time);
	 
	 public void advanceTime( double timestep ) throws RTIexception;
	    
	 public void advanceTimeTo( double nextStep ) throws RTIexception;
	 
	 public void createFederate( String federateName, String fedFileName ) throws RTIexception;
	 
	 public void finalizeFederate() throws RTIexception;

	 public double getRTINextTime();

	

}
