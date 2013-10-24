package ptolemy.myactors.MaximumEntropy;

import javax.swing.JOptionPane;

/*
 * This class Manage the values of federateName variable in 
 * SlaveFederate class. Now its possible choice automatically the value of this variable
 * 
 * Essa classe é responsavel por mudar os valores da variavel federatename na classe SlaveFederate
 * Agora é possivel mudar os valores da variavel automaticamente
 * 
 */




public class SlaveManager {
	
	
	
	private static SlaveManager instance;


	private SlaveManager() {

	}


	public static SlaveManager getInstance(){

	      if (instance == null) {
				JOptionPane.showMessageDialog(null, "Creating a new Instance of SlaveManager ... ");
	          instance = new SlaveManager();
	      }

	      return instance;
	}


	
	
	
	
	
/*
	private static SlaveManager instance = null;

	private static int contador;// = 0;
	private static int max;// = 3;

	private SlaveManager() {
		contador = 0;
		max = 3;

	}

	public static SlaveManager getInstance() {
		
		if (instance == null) {
			JOptionPane.showMessageDialog(null, "Creating a new Instance of SlaveManager ... "+ contador);
			instance = new SlaveManager();
		}
		
		
		return instance;

	}
*/


	/*
	 * return the string that contains the value of actual SlaveFederate and
	 * return "readytoRun" if the counter is equals to the max of slaves (the
	 * last slave).
	 */

/*
	public String getSlaveNumber() {
		incrementCount();
		if (contador == max) {
			return "ReadyToRun";
		}
		return contador + "";
	}


 public int getCounterValue(){

	return contador;
	 
 }
	
	public void incrementCount() {
		contador++;
		//contador = contador % max;

	}

	*/
	
}
