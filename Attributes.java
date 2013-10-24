package ptolemy.myactors.MaximumEntropy;

import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.ReflectedAttributes;


public class Attributes {
	
	 private LogicalTime receivedTime 			= null;
	 private ReflectedAttributes receivedData 	= null;
	 
	 
	 public Attributes(){
		 
	 }
	 
	 
	public Attributes(LogicalTime receivedTime, ReflectedAttributes receivedData) {
		super();
		this.receivedTime = receivedTime;
		this.receivedData = receivedData;
	}
	
	public LogicalTime getReceivedTime() {
		return receivedTime;
	}
	
	public void setReceivedTime(LogicalTime receivedTime) {
		this.receivedTime = receivedTime;
	}
	
	public ReflectedAttributes getReceivedData() {
		return receivedData;
	}
	
	public void setReceivedData(ReflectedAttributes receivedData) {
		this.receivedData = receivedData;
	}
	   
	
	

}
