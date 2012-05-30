/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ccnx.ccnmp.homeagent;

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Interest;

/**
 * Handles ContentObject when delegated.
 * 
 * This handler accepts registration for interests to be mapped back to their
 * original name upon reception of the content object in response for the
 * re-issued interest.
 * 
 * @author ebollens
 */
public class MobileDataHandler implements Runnable {

	/**
	 * Reference to the HomeAgent that instantiated this object.
	 */
	protected HomeAgent _agent;
	
	/**
	 * Mapping with key equal to the content name of a remote agent and a set
	 * of values equal to the names of all interests that should be fulfilled
	 * by the content name used as key.
	 */
	protected HashMap<String, Vector<String>> _map;
	
	/**
	 * Constructor for MobileDataHandler.
	 * 
	 * @param agent 
	 */
	MobileDataHandler(HomeAgent agent){
		_agent = agent;
		_map = new HashMap<String, Vector<String>>();
	}
	
	public boolean registerRemoteInterest(Interest interest, Interest originalInterest){
		
		if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
			Log.finest(Log.FAC_REPO, "Registering {0} within the interest {0} bucket", interest.name(), originalInterest.name());
		
		String interestName = interest.getContentName().toString(),
			   originalInterestName = originalInterest.getContentName().toString();
		
		Vector<String> v = _map.containsKey(interestName) 
								? _map.get(interestName)
								: new Vector<String>();
		
		if(!v.contains(originalInterestName)){
			v.addElement(originalInterestName);
			_map.put(interestName, v);
			return true;
		}
		
		return true;
		
	}
	
	public boolean unregisterRemoteInterest(Interest interest){
		
		_map.put(interest.getContentName().toString(), new Vector<String>());
		
		return true;
		
	}
	
	public boolean handleContent(Interest interest, ContentObject content){
		
		String interestName = interest.getContentName().toString();
		
		if(!_map.containsKey(interestName)){
			return false;
		}
		
		Vector<String> originalInterestNames = _map.get(interestName);
		
		for(String originalInterestName :  originalInterestNames){
			
			if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
				Log.finest(Log.FAC_REPO, "Handling remote data {0} for original interest {1}", interest.name(), originalInterestName);
			
			/**
			 * @todo actually do something when we get a ContentObject
			 */
		}
		
		
		return true;
		
	}
	
	@Override
	public void run() {
		
	}
	
}
