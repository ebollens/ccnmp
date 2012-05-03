package org.ccnx.ccnmp.homeagent;

import java.util.logging.Level;
import org.ccnx.ccn.CCNInterestHandler;
import org.ccnx.ccn.impl.QueuedContentHandler;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.protocol.Interest;

/**
 * RemoteInterestHandler is responsible for queuing Interest that could not be
 * satisfied through standard CCN mechanisms but that some mobile node may be
 * able to fulfill via CCNMP.
 * 
 * @author ebollens
 */
public class RemoteInterestHandler extends QueuedContentHandler<Interest> implements Runnable, CCNInterestHandler {

	/**
	 * Reference to the HomeAgent that instantiated this object.
	 */
	protected HomeAgent _homeAgent;
	
	/**
	 * Constructor for RemoteInterestHandler.
	 * 
	 * @param homeAgent object that instantiated this object
	 */
	RemoteInterestHandler(HomeAgent homeAgent) {
		_homeAgent = homeAgent;
	}

	/**
	 * Processes pending interests that a mobile node may satisfy via CCNMP.
	 * 
	 * @todo
	 * 
	 * @param interest 
	 */
	@Override
	protected void process(Interest interest) {
		if (Log.isLoggable(Log.FAC_REPO, Level.FINER))
			Log.finer(Log.FAC_REPO, "RemoteInterestHandler processing: {0}", interest.name());
	}

	/**
	 * Handles an Interest delegated to itself, queuing it for later processing.
	 * 
	 * @todo
	 * 
	 * @param interest
	 * @return 
	 */
	@Override
	public boolean handleInterest(Interest interest) {
		if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
			Log.finest(Log.FAC_REPO, "RemoteInterestHandler handling: {0}", interest.name());
		add(interest);
		return true;
	}
}
