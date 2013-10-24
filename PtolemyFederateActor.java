package ptolemy.myactors.MaximumEntropy;

import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.kernel.util.IllegalActionException;

public interface PtolemyFederateActor {
	
	public void addInteractionToSend(Interaction inter);
	public void fire() throws IllegalActionException;	
	public boolean hasDataToSend();
	//angelo - mudei
//	public IntToken getValue();
//	public IntToken getDataToSend();
	public StringToken getValue();
	public StringToken getDataToSend();
	public void updateAtributesToSend(Attributes attr);

}
