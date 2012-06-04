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
	 * Data structure that a namespace to mapped to
	 */
	protected static class MobileInterestData {
		/**
		 * Whether the HomeAgent should store interest
		 */
		protected boolean _isStoringEnabled;
		/**
		 * The remote namespace (on foreign agent) of the namespace
		 */
		protected ContentName _remoteName;
		/**
		 * The stored interests for the namespace
		 */
		protected Vector<Interest> _interests;
		
		public MobileInterestData() {
			this(true);
		}
		
		/** @todo  may replace null with empty name */
		public MobileInterestData(boolean isStoringEnabled) {
			_isStoringEnabled = isStoringEnabled;
			_remoteName = null;
			_interests = new Vector<Interest>();
		}
		
		public MobileInterestData(boolean isStoringEnabled, ContentName remoteName) {
			_isStoringEnabled = isStoringEnabled;
			_remoteName = new ContentName(remoteName);
			_interests = new Vector<Interest>();
		}
		
		public boolean isStoringEnabled() { return _isStoringEnabled; }
		public void isStoringEnabled(boolean isStoringEnabled) { _isStoringEnabled = isStoringEnabled; }
		
		public ContentName remoteName() { return _remoteName; }
		public void remoteName(ContentName remoteName) { _remoteName = new ContentName(remoteName); }
		
		public Vector<Interest> interests() { return _interests; }
		public void removeInterests() {_interests.clear();}
	}
	
	/**
	 * HashMap containing Vector<Interest> objects mapped to namespaces as keys
	 */
	protected HashMap<String, MobileInterestData> _map;
	
	/**
	 * Constructor for MobileInterestStore.
	 */
	public MobileInterestStore(){
		_map = new HashMap<String, MobileInterestData>();
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
		_map.put(key, new MobileInterestData());
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
	 * the namespace provided is one that has been defined for the store, plus
	 * the storing for that namespace is enabled
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
	 * the namespace provided is one that has been defined for the store, plus
	 * the storing for that namespace is enabled
	 * 
	 * @param namespace
	 * @param interest
	 * @return true if successful or false otherwise
	 */
	public boolean addInterest(String namespace, Interest interest){
		if(!this.containsNamespace(namespace))
			return false;
		
		MobileInterestData dataObject = (MobileInterestData)_map.get(namespace);
		
		if (dataObject._isStoringEnabled == false)
			return false;
		
		dataObject.interests().add(interest);
		return true;
	}
	
	/**
	 * Get all Interests stored for the namespace provided
	 * 
	 * @param namespace
	 * @return the interest stored for the given namespace
	 */
	public Vector<Interest> getInterests(ContentName namespace) {
		return this.getInterests(namespace.toString());
	}
	
	/**
	 * Get all Interests stored for the namespace provided
	 * 
	 * @param namespace
	 * @return the interest stored for the given namespace
	 */
	public Vector<Interest> getInterests(String namespace) {
		if(!this.containsNamespace(namespace))
			return null;
		
		return ((MobileInterestData)_map.get(namespace)).interests();
	}

	/**
	 * Set the remote namespace for a namespace
	 * 
	 * @param namespace
	 * @param remoteName
	 * @return true if successful or false otherwise
	 */
	public boolean setRemoteName(ContentName namespace, ContentName remoteName) {
		return this.setRemoteName(namespace.toString(), remoteName);
	}
	/**
	 * Set the remote namespace for a namespace
	 * 
	 * @param namespace
	 * @param remoteName
	 * @return true if successful or false otherwise
	 */
	public boolean setRemoteName(String namespace, ContentName remoteName) {
		if(!this.containsNamespace(namespace))
			return false;
		
		((MobileInterestData)_map.get(namespace)).remoteName(remoteName);
		return true;
	}

	/**
	 * Get the remote namespace for a namespace
	 * 
	 * @param namespace
	 * @return remoteName
	 */
	public ContentName getRemoteName(ContentName namespace) {
		return this.getRemoteName(namespace.toString());
	}
	
	/**
	 * Get the remote namespace for a namespace
	 * 
	 * @param namespace
	 * @return remoteName
	 */
	public ContentName getRemoteName(String namespace) {
		return ((MobileInterestData)_map.get(namespace)).remoteName();
	}

	public boolean removeInterests(ContentName namespace) {
		return this.removeInterests(namespace.toString());
		
	}
	public boolean removeInterests(String namespace) {
		if(!this.containsNamespace(namespace))
			return false;
		
		((MobileInterestData)_map.get(namespace)).removeInterests();
		return true;
	}
}
