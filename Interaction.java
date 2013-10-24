package ptolemy.myactors.MaximumEntropy;

import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;


public class Interaction {
	
	 private LogicalTime receivedTime 			= null;
	 private ReceivedInteraction receivedData 	= null;
	 
	 
	 public Interaction(){
		 
	 }
	 
	 
	public Interaction(LogicalTime receivedTime, ReceivedInteraction receivedData) {
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
	
	public ReceivedInteraction getReceivedData() {
		return receivedData;
	}
	
	public void setReceivedData(ReceivedInteraction receivedData) {
		this.receivedData = receivedData;
	}
	   
	
	

}
