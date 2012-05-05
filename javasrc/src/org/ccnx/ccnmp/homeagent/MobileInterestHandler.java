package org.ccnx.ccnmp.homeagent;

import java.util.*;
import java.util.logging.Level;
import org.ccnx.ccn.CCNInterestHandler;
import org.ccnx.ccn.impl.InterestTable;
import org.ccnx.ccn.impl.InterestTable.Entry;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.protocol.Component;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.Interest;

/**
 * RemoteInterestHandler is responsible for queuing Interest that could not be
 * satisfied through standard CCN mechanisms but that some mobile node may be
 * able to fulfill via CCNMP.
 * 
 * @author ebollens
 */
public class MobileInterestHandler implements Runnable, CCNInterestHandler {

	/**
	 * Reference to the HomeAgent that instantiated this object.
	 */
	protected HomeAgent _homeAgent;
	
	protected HashMap _interestMap;
	
	protected MobileInterestStore _interestStore;
	
	/**
	 * Constructor for RemoteInterestHandler.
	 * 
	 * @param homeAgent object that instantiated this object
	 */
	MobileInterestHandler(HomeAgent homeAgent) {
		
		// Save reference to instantiating home agent
		_homeAgent = homeAgent;
		
		// Instantiate an interest store for CCNMP-related interests
		_interestStore = new MobileInterestStore();
		
		// Register an example namespace for testing
		this.registerNamespace(new ContentName("ndn", "local"));
	}

	/**
	 * Handles Interests when delegated. 
	 * 
	 * If an Interest is of the form {A}/ccnmp/{B}, then if {A} is a namespace
	 * registered with CCNMP, it will remap interests to {B} that it has
	 * stored under the namespace {A}.
	 * 
	 * If an Interest references a namespace that is registered with CCNMP, then
	 * this method will store the Interest for later retrieval.
	 * 
	 * @param interest
	 * @return 
	 */
	@Override
	public boolean handleInterest(Interest interest) {
		
		if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
			Log.finest(Log.FAC_REPO, "RemoteInterestHandler handling: {0}", interest.name());
		
		Component component = new Component("ccnmp");
		int idx;
		
		if((idx = interest.name().containsWhere(component)) > -1){
			
			// For {A}/ccnmp/{B}, namespace is {A}
			ContentName namespace = interest.name().cut(idx);
			
			if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
				Log.finest(Log.FAC_REPO, "RemoteInterestHandler::handleInterest will process: {0} -> {1}", interest.name(), namespace);
			
			if(_interestStore.containsNamespace(namespace)){
				
				/** 
				 * @todo given {A}/ccnmp/{B}, issue all interests in {A} stored
				 * in the _interestStore to the namespace defined as {B} such
				 * that {A}/{C} is remapped to {B}/{C}
				 */
				
				if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
					Log.finest(Log.FAC_REPO, "RemoteInterestHandler supports CCNMP for namespace: {0}", namespace);
			
			}else{
				
				// Do nothing if the namespace is not registered
				
				if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
					Log.finest(Log.FAC_REPO, "RemoteInterestHandler does not support CCNMP for namespace: {0}", namespace);
			
			}
			
		}else{
			
			for(int i = 0; i < interest.getContentName().count(); i++){
				
				ContentName namespace = interest.getContentName().cut(i);
				
				if(_interestStore.addInterest(namespace, interest)){
					if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler storing interest for retrieval: {0} -> key {1}", interest.name(), namespace);
				}
				
				
			}
			
		}
		return true;
	}
	
	/**
	 * Any namespace with CCNMP support must be registered with this method.
	 * 
	 * @param target 
	 */
	public void registerNamespace(ContentName target){
		
		if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
			Log.finest(Log.FAC_REPO, "RemoteInterestHandler added listener for: {0}", target);
		
		_interestStore.addNamespace(target);
	}

	@Override
	public void run() {
		
	}
}
