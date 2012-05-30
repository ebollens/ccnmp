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
import org.ccnx.ccn.protocol.MalformedContentNameStringException;

/**
 * RemoteInterestHandler is responsible for queuing Interest that could not be
 * satisfied through standard CCN mechanisms but that some mobile node may be
 * able to fulfill via CCNMP.
 * 
 * @author ebollens
 */
public class MobileInterestHandler implements Runnable, CCNInterestHandler {
	
	public static final String COMMAND_ROOT = "ccnmp";
	public static final String COMMAND_REGISTER = "rg";
	public static final String COMMAND_REDIRECT = "rd";
	public static final String COMMAND_REGISTERREDIRECT = "rr";
	public static final String COMMAND_REMOVE = "rm";

	/**
	 * Reference to the HomeAgent that instantiated this object.
	 */
	protected HomeAgent _homeAgent;
	
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
		
		Component component = new Component(COMMAND_ROOT);
		int idx;
		
		if((idx = interest.name().containsWhere(component)) > -1 &&
			idx + 2 < interest.name().count()){
			
			// For {A}/ccnmp/<command>/{B}, namespace is {A}, remoteName is {B}
			ContentName namespace = interest.name().cut(idx);
			String command = interest.name().stringComponent(idx+1);
			ContentName remoteName = interest.name().right(idx+2);
			
			if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
				Log.finest(Log.FAC_REPO, "RemoteInterestHandler::handleInterest will process: {0} -> {1} ; {2} ; {3}", interest.name(), namespace, command, remoteName);
			
			if (command.compareTo(COMMAND_REGISTER) == 0) {
				
				/** @todo the argument can also be isStoringEnabled */
				if (!_interestStore.containsNamespace(namespace)) {
					this.registerNamespace(namespace);
				}
				
			}else if (command.compareTo(COMMAND_REDIRECT) == 0){
				
				if(_interestStore.containsNamespace(namespace)){
					
					//_interestStore.setRemoteName(namespace, remoteName);
					
					/** 
					 * given {A}/ccnmp/{B}, issue all interests in {A} stored
					 * in the _interestStore to the namespace defined as {B} such
					 * that {A}/{C} is remapped to {B}/{C}
					 */
					Interest redirectedInterest;
					Vector<Interest> interests = _interestStore.getInterest(namespace);
					for (Interest originalInterest : interests)
					{
						ContentName newName = remoteName.append(originalInterest.name().postfix(namespace));
						
						if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
							Log.finest(Log.FAC_REPO, "RemoteInterestHandler translating {0} into {1}", originalInterest.name(), newName);
						
						redirectedInterest = Interest.constructInterest(newName, originalInterest.exclude(), originalInterest.childSelector(), originalInterest.maxSuffixComponents(), originalInterest.minSuffixComponents(), null);
						
						_homeAgent.redirectInterest(redirectedInterest, originalInterest);
					}
				
				}else{
					
					// Do nothing if the namespace is not registered
					
					if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler does not support CCNMP for namespace: {0}", namespace);
				
				}
				
			}
			else if (command.compareTo(COMMAND_REMOVE) == 0) {
				
			}
			else {
				if (Log.isLoggable(Log.FAC_REPO, Level.FINE))
					Log.finest(Log.FAC_REPO, "RemoteInterestHandler has encountered invalid command: {0}", command);
			}
			
			
		}else{

			/** 
			 * @todo we should do a longest prefix matching here, not shortest
			 */
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
