/* 
 *   Copyright 2007 The Portico Project 
 * 
 *   This file is part of portico. 
 * 
 *   portico is free software; you can redistribute it and/or modify 
 *   it under the terms of the Common Developer and Distribution License (CDDL)  
 *   as published by Sun Microsystems. For more information see the LICENSE file. 
 *    
 *   Use of this software is strictly AT YOUR OWN RISK!!! 
 *   If something bad happens you do not have permission to come crying to me. 
 *   (that goes for your lawyer as well) 
 * 
 */
package ptolemy.myactors.MaximumEntropy;

import hla.rti.AttributeHandleSet;
import hla.rti.FederatesCurrentlyJoined;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.FederationExecutionDoesNotExist;
import hla.rti.LogicalTime;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.ResignAction;
import hla.rti.SuppliedAttributes;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.swing.JOptionPane;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.cmd.SigarCommandBase;

import certi.rti.impl.CertiLogicalTime;
import certi.rti.impl.CertiLogicalTimeInterval;
import certi.rti.impl.CertiRtiAmbassador;

import com.sun.management.OperatingSystemMXBean;

/**
 * This is an example federate demonstrating how to properly use the HLA 1.3
 * Java interface supplied with Portico.
 * 
 * As it is intended for example purposes, this is a rather simple federate. The
 * process is goes through is as follows:
 * 
 * 1. Create the RTIambassador 2. Try to create the federation (nofail) 3. Join
 * the federation 4. Announce a Synchronization Point (nofail) 5. Wait for the
 * federation to Synchronized on the point 6. Enable Time Regulation and
 * Constrained 7. Publish and Subscribe 8. Register an Object Instance 9. Main
 * Simulation Loop (executes 20 times) 9.1 Update attributes of registered
 * object 9.2 Send an Interaction 9.3 Advance time by 1.0 10. Delete the Object
 * Instance 11. Resign from Federation 12. Try to destroy the federation
 * (nofail)
 * 
 * NOTE: Those items marked with (nofail) deal with situations where multiple
 * federates may be working in the federation. In this sitaution, the federate
 * will attempt to carry out the tasks defined, but it won't stop or exit if
 * they fail. For example, if another federate has already created the
 * federation, the call to create it again will result in an exception. The
 * example federate expects this and will not fail. NOTE: Between actions 4. and
 * 5., the federate will pause until the uses presses the enter key. This will
 * give other federates a chance to enter the federation and prevent other
 * federates from racing ahead.
 * 
 * 
 * The main method to take notice of is {@link #runFederate(String)}. It
 * controls the main simulation loop and triggers most of the important
 * behaviour. To make the code simpler to read and navigate, many of the
 * important HLA activities are broken down into separate methods. For example,
 * if you want to know how to send an interaction, see the
 * {@link #sendInteraction()} method.
 * 
 * With regard to the FederateAmbassador, it will log all incoming information.
 * Thus, if it receives any reflects or interactions etc... you will be notified
 * of them.
 * 
 * Note that all of the methods throw an RTIexception. This class is the parent
 * of all HLA exceptions. The HLA Java interface is full of exceptions, with
 * only a handful being actually useful. To make matters worse, they're all
 * checked exceptions, so unlike C++, we are forced to handle them by the
 * compiler. This is unnecessary in this small example, so we'll just throw all
 * exceptions out to the main method and handle them there, rather than handling
 * each exception independently as they arise.
 */
public class MasterFederate extends SigarCommandBase implements PtolemyFederate {
	// ----------------------------------------------------------
	// STATIC VARIABLES
	// ----------------------------------------------------------
	/**
	 * The number of times we will update our attributes and send an interaction
	 */
	public static final int ITERATIONS = 20;

	/** The sync point all federates will sync up on before starting */
	public static final String READY_TO_RUN = "ReadyToRun";

	// ----------------------------------------------------------
	// INSTANCE VARIABLES
	// ----------------------------------------------------------
	private RTIambassador rtiamb;
	private FederateAmbassador fedamb;

	private int objectHandle;

	// ----------------------------------------------------------
	// CONSTRUCTORS
	// ----------------------------------------------------------

	public MasterFederate() {
		fedamb = new FederateAmbassador();
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Main Simulation Method /////////////////////////
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * This is the main simulation loop. It can be thought of as the main method
	 * of the federate. For a description of the basic flow of this federate,
	 * see the class level comments
	 */
	private OperatingSystemMXBean osMBean = null;
	private long nanoBefore, cpuBefore;

	public void createFederate(String federateName, String federateFile)
			throws RTIexception {
		// ///////////////////////////////
		// 1. create the RTIambassador //
		// ///////////////////////////////
		createRTIAmbassador();

		// ////////////////////////////
		// 2. create the federation //
		// ////////////////////////////
		// create
		// NOTE: some other federate may have already created the federation,
		// in that case, we'll just try and join it

		try {
			createFederation(federateFile);
			log("Created Federation");

			// //////////////////////////
			// 3. join the federation //
			// //////////////////////////
			joinFederation(federateName);

		} catch (FederationExecutionAlreadyExists exists) {
			log("Didn't create federation, it already existed");
		} catch (MalformedURLException urle) {
			log("Exception processing fom: " + urle.getMessage());
			urle.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// //////////////////////////////
		// 4. announce the sync point //
		// //////////////////////////////
		// announce a sync point to get everyone on the same page. if the point
		// has already been registered, we'll get a callback saying it failed,
		// but we don't care about that, as long as someone registered it

		// Just present in SlaveFederate
		// announceSynchronizationPoint();

		// WAIT FOR USER TO KICK US OFF
		// So that there is time to add other federates, we will wait until the
		// user hits enter before proceeding. That was, you have time to start
		// other federates.
		waitForUser();

		/*
		 * MBeanServerConnection mbsc =
		 * ManagementFactory.getPlatformMBeanServer();
		 * 
		 * try { osMBean = ManagementFactory.newPlatformMXBeanProxy( mbsc,
		 * ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
		 * OperatingSystemMXBean.class); } catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 * 
		 * nanoBefore = System.nanoTime(); cpuBefore =
		 * osMBean.getProcessCpuTime();
		 */

		// /////////////////////////////////////////////////////
		// 5. achieve the point and wait for synchronization //
		// /////////////////////////////////////////////////////
		// tell the RTI we are ready to move past the sync point and then wait
		// until the federation has synchronized on
		log("calling achieveSynchronizationPoint()");
		achieveSynchronizationPoint();

		// ///////////////////////////
		// 6. enable time policies //
		// ///////////////////////////
		// in this section we enable/disable all time policies
		// note that this step is optional!
		enableTimePolicy();
		log("Time Policy Enabled");

		// ////////////////////////////
		// 7. publish and subscribe //
		// ////////////////////////////
		// in this section we tell the RTI of all the data we are going to
		// produce, and all the data we want to know about
		publishAndSubscribe();
		log("Published and Subscribed");

		// ///////////////////////////////////
		// 8. register an object to update //
		// ///////////////////////////////////
		objectHandle = registerObject();
		log("Registered Object, handle=" + objectHandle);
	}

	public void sendData(String data) throws RTIexception {
		// 9.1 update the attribute values of the instance //
		// System.out.println("bbbbbbbbbb");
		updateAttributeValues(data); // objecthandle

		/*
		 * try { BufferedWriter arquivo; String str = ""; CpuPerc cpu; cpu =
		 * sigar.getCpuPerc(); Mem memory = sigar.getMem();
		 * 
		 * //str = CpuPerc.format(cpu.getSys());
		 * 
		 * double totalMemory = (double)memory.getTotal() / (1024*1024*1024);
		 * double usedMemory = (double)memory.getUsed() / (1024*1024*1024);
		 * //vira GB - origianl eh bytes double freeMemory =
		 * (double)memory.getFree() / (1024*1024*1024); double usedMemoryPercent
		 * = (double)memory.getUsedPercent();
		 * 
		 * Runtime runtime = Runtime.getRuntime(); double totalMemoryJVM =
		 * (double)runtime.totalMemory() / (1024*1024); //valor utilizado pela
		 * JVM - em bytes double freeMemoryJVM = (double)runtime.freeMemory() /
		 * (1024*1024); double maxMemory = (double)runtime.maxMemory() / 1024;
		 * 
		 * // str = ""+memory.getUsedPercent();
		 * 
		 * String cpu_pc = CpuPerc.format(cpu.getCombined()); String cpu_jvm =
		 * CpuPerc.format(cpu.getSys());
		 * 
		 * str = usedMemoryPercent+"GB - "+ (totalMemoryJVM -
		 * freeMemoryJVM)+"MB - "+cpu_pc+" - "+cpu_jvm;
		 * 
		 * arquivo = new BufferedWriter(new
		 * FileWriter("config/experimentos_mem_used.txt", true));
		 * arquivo.write(str); arquivo.newLine(); arquivo.close(); } catch
		 * (Exception e1) { e1.printStackTrace(); }
		 */

		// 9.2 send an interaction
		// sendInteraction(data);//angelo
	}

	public void finalizeFederate() throws RTIexception {
		// ////////////////////////////////////
		// 10. delete the object we created //
		// ////////////////////////////////////
		deleteObject(objectHandle);
		log("Deleted Object, handle=" + objectHandle);

		// //////////////////////////////////
		// 11. resign from the federation //
		// //////////////////////////////////
		rtiamb.resignFederationExecution(ResignAction.NO_ACTION);
		log("Resigned from Federation");

		// //////////////////////////////////////
		// 12. try and destroy the federation //
		// //////////////////////////////////////
		// NOTE: we won't die if we can't do this because other federates
		// remain. in that case we'll leave it for them to clean up
		try {
			rtiamb.destroyFederationExecution("ExampleFederation");
			log("Destroyed Federation");
		} catch (FederationExecutionDoesNotExist dne) {
			log("No need to destroy federation, it doesn't exist");
		} catch (FederatesCurrentlyJoined fcj) {
			log("Didn't destroy federation, federates still joined");
		}

	}

	/*
	 * 
	 * angelo - mudando de Interaction para Attributes
	 * 
	 * public Interaction receivedData(double time){ LogicalTime t = new
	 * CertiLogicalTime(time); return fedamb.receivedData(t); }
	 * 
	 * public Interaction consumeReceivedData(double time){ LogicalTime t = new
	 * CertiLogicalTime(time); return fedamb.consumeReceivedData(t); }
	 */

	public Attributes receivedData(double time) {
		LogicalTime t = new CertiLogicalTime(time);
		return fedamb.receivedData(t);
	}

	public Attributes consumeReceivedData(double time) {
		LogicalTime t = new CertiLogicalTime(time);
		return fedamb.consumeReceivedData(t);
	}

	private void createRTIAmbassador() throws RTIexception {
		log(" - createAmbassador()");
		rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
	}

	private void createFederation(String federateFile) throws RTIexception,
			MalformedURLException {
		log(" - createFederation()");

		File fom = new File(federateFile);
		rtiamb.createFederationExecution("ExampleFederation", fom.toURI()
				.toURL());
	}

	private void joinFederation(String federateName) throws RTIexception {
		// create the federate ambassador and join the federation
		fedamb = new FederateAmbassador();
		rtiamb.joinFederationExecution(federateName, "ExampleFederation",
				fedamb);
		log("Joined Federation as " + federateName);
	}

	private void announceSynchronizationPoint() throws RTIexception {

		log("MasterFederate - announceSynPoint()");

		byte[] tag = EncodingHelpers.encodeString("hi!");
		rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, tag);
		// wait until the point is announced
		while (fedamb.isRegistered == false || fedamb.isAnnounced == false) {
			log(" tick () !!");
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	private void achieveSynchronizationPoint() throws RTIexception {
		// Not present in SlaveFederate
		while (fedamb.isAnnounced == false) {
			// log("isAnnounced == false");
			((CertiRtiAmbassador) rtiamb).tick2();
		}

		rtiamb.synchronizationPointAchieved(READY_TO_RUN);
		log("Achieved sync point: " + READY_TO_RUN
				+ ", waiting for federation...");

		while (fedamb.isReadyToRun == false) {
			// log("isReadyToRun == false");
			System.out.println("Master 1: tick () !!");
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	// ----------------------------------------------------------
	// INSTANCE METHODS
	// ----------------------------------------------------------
	/**
	 * This is just a helper method to make sure all logging it output in the
	 * same form
	 */
	private void log(String message) {
		 System.out.println( "MasterFederate   : " + message );
	}

	/**
	 * This method will block until the user presses enter
	 */
	private void waitForUser() {
		JOptionPane.showMessageDialog(null,
				"Wait for User... Start aAll slaves, then press OK!");
	}

	// //////////////////////////////////////////////////////////////////////////
	// //////////////////////////// Helper Methods
	// //////////////////////////////
	// //////////////////////////////////////////////////////////////////////////
	/**
	 * This method will attempt to enable the various time related properties
	 * for the federate
	 */
	private void enableTimePolicy() throws RTIexception {
		// NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code
		// is
		// Portico specific. You will have to alter this if you move to a
		// different RTI implementation. As such, we've isolated it into a
		// method so that any change only needs to happen in a couple of spots
		CertiLogicalTime currentTime = new CertiLogicalTime(fedamb.federateTime);
		CertiLogicalTimeInterval lookahead = new CertiLogicalTimeInterval(
				fedamb.federateLookahead);

		// //////////////////////////
		// enable time regulation //
		// //////////////////////////
		this.rtiamb.enableTimeRegulation(currentTime, lookahead);

		// tick until we get the callback
		while (fedamb.isRegulating == false) {
			((CertiRtiAmbassador) rtiamb).tick2();
		}

		// ///////////////////////////
		// enable time constrained //
		// ///////////////////////////
		this.rtiamb.enableTimeConstrained();

		// tick until we get the callback
		while (fedamb.isConstrained == false) {
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	/**
	 * This method will inform the RTI about the types of data that the federate
	 * will be creating, and the types of data we are interested in hearing
	 * about as other federates produce it.
	 */
	private void publishAndSubscribe() throws RTIexception {
		// //////////////////////////////////////////
		// publish all attributes of ObjectRoot.A //
		// //////////////////////////////////////////
		// before we can register instance of the object class ObjectRoot.A and
		// update the values of the various attributes, we need to tell the RTI
		// that we intend to publish this information

		// get all the handle information for the attributes of ObjectRoot.A

		int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.aes");
		int aaHandle = rtiamb.getAttributeHandle("canal1", classHandle);
		int abHandle = rtiamb.getAttributeHandle("canal2", classHandle);
		int acHandle = rtiamb.getAttributeHandle("canal3", classHandle);
		int adHandle = rtiamb.getAttributeHandle("canal4", classHandle);
		int aeHandle = rtiamb.getAttributeHandle("canal5", classHandle);
		int afHandle = rtiamb.getAttributeHandle("canal6", classHandle);
		int agHandle = rtiamb.getAttributeHandle("canal7", classHandle);
		int ahHandle = rtiamb.getAttributeHandle("canal8", classHandle);
		int aiHandle = rtiamb.getAttributeHandle("canal9", classHandle);
		int ajHandle = rtiamb.getAttributeHandle("canal10", classHandle);

		// int classHandle = rtiamb.getObjectClassHandle("InteractionRoot.X");
		// int aaHandle = rtiamb.getAttributeHandle("xa", classHandle);

		// package the information into a handle set
		AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory()
				.createAttributeHandleSet();
		attributes.add(aaHandle);
		attributes.add(abHandle);
		attributes.add(acHandle);
		attributes.add(adHandle);
		attributes.add(aeHandle);
		attributes.add(afHandle);
		attributes.add(agHandle);
		attributes.add(ahHandle);
		attributes.add(aiHandle);
		attributes.add(ajHandle);
		// do the actual publication
		rtiamb.publishObjectClass(classHandle, attributes);

		// ///////////////////////////////////////////////
		// subscribe to all attributes of ObjectRoot.A //
		// ///////////////////////////////////////////////
		// we also want to hear about the same sort of information as it is
		// created and altered in other federates, so we need to subscribe to it

		rtiamb.subscribeObjectClassAttributes(classHandle, attributes);

		// ///////////////////////////////////////////////////
		// publish the interaction class InteractionRoot.X //
		// ///////////////////////////////////////////////////
		// we want to send interactions of type InteractionRoot.X, so we need
		// to tell the RTI that we're publishing it first. We don't need to
		// inform it of the parameters, only the class, making it much simpler
		int interactionHandle = rtiamb
				.getInteractionClassHandle("InteractionRoot.X");

		// do the publication
		rtiamb.publishInteractionClass(interactionHandle);

		// //////////////////////////////////////////////////
		// subscribe to the InteractionRoot.X interaction //
		// //////////////////////////////////////////////////
		// we also want to receive other interaction of the same type that are
		// sent out by other federates, so we have to subscribe to it first
		rtiamb.subscribeInteractionClass(interactionHandle);
	}

	/**
	 * This method will register an instance of the class ObjectRoot.A and will
	 * return the federation-wide unique handle for that instance. Later in the
	 * simulation, we will update the attribute values for this instance
	 */
	private int registerObject() throws RTIexception {
		int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.aes");
		// int classHandle = rtiamb.getObjectClassHandle("InteractionRoot.X");
		return rtiamb.registerObjectInstance(classHandle);
	}

	
	
	/**
	 * 
	 * This method will update all the values of the given object instance. It
	 * will set each of the values to be a string which is equal to the name of
	 * the attribute plus the current time. eg "aa:10.0" if the time is 10.0.
	 * <p/>
	 * Note that we don't actually have to update all the attributes at once, we
	 * could update them individually, in groups or not at all!
	 */
	private void updateAttributeValues(String data) throws RTIexception {
		// /////////////////////////////////////////////
		// create the necessary container and values //
		// /////////////////////////////////////////////
		// create the collection to store the values in, as you can see
		// this is quite a lot of work
		
		//SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

		// generate the new values
		// we use EncodingHelpers to make things nice friendly for both Java and
		// C++
		
		//System.out.println("Ultimo valor Processado no master Federate => "+data);
		
		String[] tokens = data.split(" ; ");
		
		for (String token : tokens){
			
				
			SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
			//Removing special characters 
			token  = token.replace("\n", "");
			token  = token.replace("\"", "");
			token  = token.replace("\\", "");
			
			String[] valores = token.split(" - ");
			
			//O erro foi aqui 

			int classHandle = rtiamb.getObjectClass(1);//objectHandle);

			if (valores[0].equalsIgnoreCase("channel1")) {

				byte[] aaValue = EncodingHelpers.encodeString("canal1:"+ valores[1]);
				int aaHandle = rtiamb.getAttributeHandle("canal1", classHandle);
				attributes.add(aaHandle, aaValue);
				
			} else if (valores[0].equalsIgnoreCase("channel2")) {
				
				byte[] abValue = EncodingHelpers.encodeString("canal2:"+ valores[1]);
				int abHandle = rtiamb.getAttributeHandle("canal2", classHandle);
				attributes.add(abHandle, abValue);
				
			} else if (valores[0].equalsIgnoreCase("channel3")) {
				
				byte[] acValue = EncodingHelpers.encodeString("canal3:"+ valores[1]);
				int acHandle = rtiamb.getAttributeHandle("canal3", classHandle);
				attributes.add(acHandle, acValue);
			} else if (valores[0].equalsIgnoreCase("channel4")) {
				
				byte[] adValue = EncodingHelpers.encodeString("canal4:"+ valores[1]);
				int adHandle = rtiamb.getAttributeHandle("canal4", classHandle);
				attributes.add(adHandle, adValue);
			} else if (valores[0].equalsIgnoreCase("channel5")) {
				
				byte[] aeValue = EncodingHelpers.encodeString("canal5:"+ valores[1]);
				int aeHandle = rtiamb.getAttributeHandle("canal5", classHandle);
				attributes.add(aeHandle, aeValue);
			} else if (valores[0].equalsIgnoreCase("channel6")) {
				
				byte[] afValue = EncodingHelpers.encodeString("canal6:"+ valores[1]);
				int afHandle = rtiamb.getAttributeHandle("canal6", classHandle);
				attributes.add(afHandle, afValue);

			} else if (valores[0].equalsIgnoreCase("channel7")) {
				
				byte[] agValue = EncodingHelpers.encodeString("canal7:"+ valores[1]);
				int agHandle = rtiamb.getAttributeHandle("canal7", classHandle);
				attributes.add(agHandle, agValue);
				
			} else if (valores[0].equalsIgnoreCase("channel8")) {
				
				byte[] ahValue = EncodingHelpers.encodeString("canal8:"+ valores[1]);
				int ahHandle = rtiamb.getAttributeHandle("canal8", classHandle);
				attributes.add(ahHandle, ahValue);

			} else if (valores[0].equalsIgnoreCase("channel9")) {
				
				byte[] aiValue = EncodingHelpers.encodeString("canal9:"+ valores[1]);
				int aiHandle = rtiamb.getAttributeHandle("canal9", classHandle);
				attributes.add(aiHandle, aiValue);

			} else if (valores[0].equalsIgnoreCase("channel10")) {
				
				byte[] ajValue = EncodingHelpers.encodeString("canal10:"+ valores[1]);
				int ajHandle = rtiamb.getAttributeHandle("canal10", classHandle);
				attributes.add(ajHandle, ajValue);
				
			}else {
				System.out.println("O Valor não foi adicionado no Attributes => " + valores[1]);

			}
			
			byte[] tag = EncodingHelpers.encodeString("hi!");
			rtiamb.updateAttributeValues(objectHandle, attributes, tag);
			CertiLogicalTime time = new CertiLogicalTime(fedamb.federateTime + fedamb.federateLookahead);
			//System.out.println("Valor adicionado no Attributes => " + valores[1]);
			rtiamb.updateAttributeValues(objectHandle, attributes, tag, time);


			}
				

	}

	/**
	 * This method will send out an interaction of the type InteractionRoot.X.
	 * Any federates which are subscribed to it will receive a notification the
	 * next time they tick(). Here we are passing only two of the three
	 * parameters we could be passing, but we don't actually have to pass any at
	 * all!
	 */
	private void sendInteraction(int data) throws RTIexception {
		// /////////////////////////////////////////////
		// create the necessary container and values //
		// /////////////////////////////////////////////
		// create the collection to store the values in
		SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory()
				.createSuppliedParameters();

		// generate the new values
		// we use EncodingHelpers to make things nice friendly for both Java and
		// C++
		byte[] xaValue = EncodingHelpers.encodeString("xa:" + data);
		byte[] xbValue = EncodingHelpers.encodeString("xb:" + (data + 1));

		// get the handles
		int classHandle = rtiamb.getInteractionClassHandle("InteractionRoot.X");
		int xaHandle = rtiamb.getParameterHandle("xa", classHandle);
		int xbHandle = rtiamb.getParameterHandle("xb", classHandle);

		// put the values into the collection
		parameters.add(xaHandle, xaValue);
		parameters.add(xbHandle, xbValue);

		// ////////////////////////
		// send the interaction //
		// ////////////////////////
		byte[] tag = EncodingHelpers.encodeString("hi!");
		rtiamb.sendInteraction(classHandle, parameters, tag);

		// if you want to associate a particular timestamp with the
		// interaction, you will have to supply it to the RTI. Here
		// we send another interaction, this time with a timestamp:
		CertiLogicalTime time = new CertiLogicalTime(fedamb.federateTime
				+ fedamb.federateLookahead);
		rtiamb.sendInteraction(classHandle, parameters, tag, time);
	}

	/**
	 * This method will request a time advance to the current time, plus the
	 * given timestep. It will then wait until a notification of the time
	 * advance grant has been received.
	 */
	public void advanceTime(double timestep) throws RTIexception {
		// request the advance
		fedamb.isAdvancing = true;
		LogicalTime newTime = new CertiLogicalTime(fedamb.federateTime
				+ timestep);
		rtiamb.timeAdvanceRequest(newTime);

		//log("Time Advanced to " + fedamb.federateTime);

		// wait for the time advance to be granted. ticking will tell the
		// LRC to start delivering callbacks to the federate
		while (fedamb.isAdvancing) {
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	public void advanceTimeTo(double nextStep) throws RTIexception {
		// request the advance
		fedamb.isAdvancing = true;
		LogicalTime newTime = new CertiLogicalTime(nextStep);
		rtiamb.timeAdvanceRequest(newTime);

		//log("Time Advanced to " + newTime);

		// wait for the time advance to be granted. ticking will tell the
		// LRC to start delivering callbacks to the federate
		while (fedamb.isAdvancing) {
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	/**
	 * This method will attempt to delete the object instance of the given
	 * handle. We can only delete objects we created, or for which we own the
	 * privilegeToDelete attribute.
	 */
	private void deleteObject(int handle) throws RTIexception {
		byte[] tag = EncodingHelpers.encodeString("hi!");
		rtiamb.deleteObjectInstance(handle, tag); // no tag, we're lazy
	}

	// ----------------------------------------------------------
	// STATIC METHODS
	// ----------------------------------------------------------
	public static void main(String[] args) {
		// get a federate name, use "exampleFederate" as default
		String federateName = "exampleFederate";
		if (args.length != 0) {
			federateName = args[0];
		}

		try {
			// run the example federate
			new MasterFederate().createFederate(federateName, "testfom.fed");
		} catch (RTIexception rtie) {
			// an exception occurred, just log the information and exit
			rtie.printStackTrace();
		}
	}

	public int getObjectHandle() {
		// TODO Auto-generated method stub
		return this.objectHandle;
	}

	@Override
	public double getRTINextTime() {
		// TODO Auto-generated method stub
		return fedamb.federateTime + fedamb.federateLookahead;
	}

	@Override
	public void output(String[] arg0) throws SigarException {
		// TODO Auto-generated method stub

	}

}