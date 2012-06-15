package org.ccnx.ccnmp.homeagent;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import org.ccnx.ccn.CCNInterestHandler;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccnmp.CCNMP;

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
		//this.registerNamespace(new ContentName("ndn", "local"), false);
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
		
		Log.finest(Log.FAC_REPO, "RemoteInterestHandler handling: {0}", interest.name());
		
		int idx = interest.name().containsWhere(CCNMP.COMMAND_ROOT.getBytes());
		if(idx > -1) {
			/*
			 * This is a CCNMP command, handle it 
			 * Command format: {A}/ccnmp/<command>[/{args}]/<timestamp>
			 * the component after ccnmp(idx) is the actual command
			 * {A} is the namespace
			 * CCNMP command always ends with a timestamp to ensure no caching, 
			 * need to be stripped 
			 */
			if (idx + 2 > interest.name().count() - 1) {
				// idx starts at 0, so count need to -1, a valid command need at
				// least 3 components: CCNMP.COMMAND_ROOT, command, timestamp
				// TODO against certain attack?
				Log.warning(Log.FAC_REPO, "RemoteInterestHandler::handleInterest has encountered invalid command interest: {0}", interest.name());
				return false;
			}
			ContentName namespace = interest.name().cut(idx);
			String command = interest.name().stringComponent(idx+1);
			ContentName arguments = interest.name().right(idx+2);
			arguments = arguments.cut(arguments.count()-1); // cut timestamp
			
			Log.finest(Log.FAC_REPO, "RemoteInterestHandler::handleInterest will process: {0} -> {1} ; {2} ; {3}", interest.name(), namespace, command, arguments);
			
			if (command.compareTo(CCNMP.COMMAND_REGISTER) == 0) {
				// {A}/ccnmp/rg/{true/false}, argument is for isForwardASAPEnabled. If not recognized, assume false
				boolean isForwardASAPEnabled =  arguments.toString().compareTo("/true") == 0;
				
				/** @todo the argument can also be isStoringEnabled */
				if (!_interestStore.containsNamespace(namespace)) {
					this.registerNamespace(namespace, isForwardASAPEnabled);
					
					try {
						ContentObject object = ContentObject.buildContentObject(interest.name(), CCNMP.RESPONSE_SUCCESS.getBytes());
						
						_homeAgent.getHandle().put(object);
						
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler is sending back acknowledgement.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			} else if (command.compareTo(CCNMP.COMMAND_REDIRECT) == 0){
				// {A}/ccnmp/rd/{B}, {B} is the remote namespace
				ContentName remoteName = arguments;
				
				if(_interestStore.containsNamespace(namespace)){
										
					_interestStore.setRemoteName(namespace, remoteName);
					
					/** 
					 * given {A}/ccnmp/{B}, issue all interests in {A} stored
					 * in the _interestStore to the namespace defined as {B} such
					 * that {A}/{C} is remapped to {B}/{C}
					 */
					Interest redirectedInterest;
					Vector<Interest> interests = _interestStore.getInterests(namespace);
					for (Interest originalInterest : interests)
					{
						ContentName newName = remoteName.append(originalInterest.name().postfix(namespace));
						
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler translating {0} into {1}", originalInterest.name(), newName);
						
						redirectedInterest = Interest.constructInterest(newName, originalInterest.exclude(), originalInterest.childSelector(), originalInterest.maxSuffixComponents(), originalInterest.minSuffixComponents(), null);
						
						try {
							_homeAgent.redirectInterest(redirectedInterest, originalInterest);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// TODO may want to add back removed interests if no response
					_interestStore.removeInterests(namespace);
					Log.finest(Log.FAC_REPO, "Interest storage cleared");
					
					try {
						ContentObject object = ContentObject.buildContentObject(interest.name(), CCNMP.RESPONSE_SUCCESS.getBytes());
						
						_homeAgent.getHandle().put(object);
						
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler is sending back acknowledgement.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				}else{
					// Do nothing if the namespace is not registered
					
					try {
						ContentObject object = ContentObject.buildContentObject(interest.name(), CCNMP.RESPONSE_FAILURE.getBytes());
						
						_homeAgent.getHandle().put(object);
						
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler is sending back error report.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler does not support CCNMP for namespace: {0}", namespace);
				
				}
				
			}
			else if (command.compareTo(CCNMP.COMMAND_REMOVE) == 0) {
				// {A}/ccnmp/rm, no arguments
				
				ContentObject object;
				if (_interestStore.containsNamespace(namespace)) {
					this.removeNamespace(namespace);
					object = ContentObject.buildContentObject(interest.name(), CCNMP.RESPONSE_SUCCESS.getBytes());
				} else {
					object = ContentObject.buildContentObject(interest.name(), CCNMP.RESPONSE_FAILURE.getBytes());
				}
					
				try {
					_homeAgent.getHandle().put(object);
					
					Log.finest(Log.FAC_REPO, "RemoteInterestHandler is sending back acknowledgement.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else {
				if (Log.isLoggable(Log.FAC_REPO, Level.FINE))
					Log.warning(Log.FAC_REPO, "RemoteInterestHandler::handleInterest has encountered invalid command: {0}", command);
			}
			
			
		}else{

			/** 
			 * @todo we should do a longest prefix matching here, not shortest
			 */
			for(int i = 0; i < interest.getContentName().count(); i++){
				
				ContentName namespace = interest.getContentName().cut(i);
				
				if(_interestStore.containsNamespace(namespace)) {
					if (_interestStore.isForwardASAPEnabled(namespace)) {
						// If forwarding ASAP enabled, forward is  immediately
						ContentName remoteName = _interestStore.getRemoteName(namespace);
						ContentName newName = remoteName.append(interest.name().postfix(namespace));
						
						Log.finest(Log.FAC_REPO, "RemoteInterestHandler translating {0} into {1}", interest.name(), newName);
						
						Interest redirectedInterest = Interest.constructInterest(newName, interest.exclude(), interest.childSelector(), interest.maxSuffixComponents(), interest.minSuffixComponents(), null);
						
						try {
							_homeAgent.redirectInterest(redirectedInterest, interest);
						} catch (IOException e) {
							// Exception mean the redirect has failed. We should store it for future redirect command
							Log.finest(Log.FAC_REPO, "RemoteInterestHandler active forrwarding failed: {0}", redirectedInterest.name());
							if(_interestStore.addInterest(namespace, interest)){
								Log.finest(Log.FAC_REPO, "RemoteInterestHandler storing interest for retrieval: {0} -> key {1}", interest.name(), namespace);
							}
						}
					} else {
						// Else, store it and wait for polling
						if(_interestStore.addInterest(namespace, interest)){
							Log.finest(Log.FAC_REPO, "RemoteInterestHandler storing interest for retrieval: {0} -> key {1}", interest.name(), namespace);
						}
					}
				}
			}
			
		}
		return true;
	}
	
	/**
	 * Any namespace with CCNMP support must be registered with this method.
	 * 
	 * @param namespace 
	 * @param isForwardASAPEnabled 
	 */
	private void registerNamespace(ContentName namespace, boolean isForwardASAPEnabled){
		
		if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
			Log.finest(Log.FAC_REPO, "RemoteInterestHandler added listener for: {0}", namespace);
		
		_interestStore.addNamespace(namespace, isForwardASAPEnabled);
	}
	
	/**
	 * Unregister a CCNMP namespace.
	 * 
	 * @param namespace 
	 */
	private void removeNamespace(ContentName namespace){
		
		if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
			Log.finest(Log.FAC_REPO, "RemoteInterestHandler remove listener for: {0}", namespace);

		_interestStore.removeNamespace(namespace);
	}

	@Override
	public void run() {
		
	}
}
