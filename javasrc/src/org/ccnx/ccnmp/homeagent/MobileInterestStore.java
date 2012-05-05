/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ccnx.ccnmp.homeagent;

import java.util.HashMap;
import java.util.Vector;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.Interest;

/**
 * Stores sets of Interests by namespaces that have been explicitly defined. 
 * This structure is used by MobileInterestHandler to store interests for
 * namespaces registered with CCNMP and to retrieve interests under a CCNMP
 * namespace so that they can be remapped and issued back out.
 * 
 * @author ebollens
 */
public class MobileInterestStore {
	
	/**
	 * HashMap containing Vector<Interest> objects mspped to namespaces as keys
	 */
	protected HashMap _map;
	
	/**
	 * Constructor for MobileInterestStore.
	 */
	public MobileInterestStore(){
		_map = new HashMap();
	}
	
	/**
	 * Add a namespace that the MobileInterestStore will accept Interests for.
	 * 
	 * @param key 
	 */
	public void addNamespace(ContentName key){
		this.addNamespace(key.toString());
	}
	
	/**
	 * Add a namespace that the MobileInterestStore will accept Interests for.
	 * 
	 * @param key 
	 */
	public void addNamespace(String key){
		_map.put(key, new Vector<Interest>());
	}
	
	/**
	 * Determine if a namespace is one that this object accepts Interests for.
	 * 
	 * @param key 
	 */
	public boolean containsNamespace(ContentName key){
		return this.containsNamespace(key.toString());
	}
	
	/**
	 * Determine if a namespace is one that this object accepts Interests for.
	 * 
	 * @param key 
	 */
	public boolean containsNamespace(String key){
		return _map.containsKey(key);
	}
	
	/**
	 * Add an Interest to the Vector<Interest> for the namespace provided, iff
	 * the namespace provided is one that has been defined for the store.
	 * 
	 * @param namespace
	 * @param interest
	 * @return true if successful or false otherwise
	 */
	public boolean addInterest(ContentName namespace, Interest interest){
		return this.addInterest(namespace.toString(), interest);
	}
	
	/**
	 * Add an Interest to the Vector<Interest> for the namespace provided, iff
	 * the namespace provided is one that has been defined for the store.
	 * 
	 * @param namespace
	 * @param interest
	 * @return true if successful or false otherwise
	 */
	public boolean addInterest(String namespace, Interest interest){
		if(!this.containsNamespace(namespace))
			return false;
		
		((Vector<Interest>)_map.get(namespace)).add(interest);
		return true;
	}
}
