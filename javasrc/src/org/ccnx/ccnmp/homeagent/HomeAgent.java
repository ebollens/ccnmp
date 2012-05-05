package org.ccnx.ccnmp.homeagent;

import java.util.logging.Level;
import org.ccnx.ccn.impl.repo.RepositoryInterestHandler;
import org.ccnx.ccn.impl.repo.RepositoryServer;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccnmp.CCNMP;

/**
 * The HomeAgent thread that manages all CCNMP Home Agent functionality.
 * 
 * @author ebollens
 */
public class HomeAgent implements Runnable {
	
	/**
	 * Set true if the CCNMP home agent should be enabled. This requires that
	 * CCNMP is also enabled.
	 */
	public final static boolean ENABLED = CCNMP.ENABLED && true;
	
	/**
	 * Reference to the RepositoryServer object that initialized the
	 * RepositoryInterestHandler which instantiated this HomeAgent.
	 */
	protected RepositoryServer _repositoryServer;
	
	/**
	 * Reference to the RepositoryInterestHandler which instantiated this
	 * HomeAgent and will pass interests that it cannot handle itself.
	 */
	protected RepositoryInterestHandler _repositoryInterestHandler;
	
	/**
	 * HomeAgent passes Interests intended for mobile nodes off to this object,
	 * which becomes responsible for handling these Interests once the mobile 
	 * node requests Interests outstanding for it.
	 */
	protected MobileInterestHandler _mobileInterestHandler;
	
	/**
	 * Constructor for HomeAgent.
	 * 
	 * @param server object that initialized the interestHandler
	 * @param interestHandler object that instantiated this object
	 */
	public HomeAgent(RepositoryServer server, RepositoryInterestHandler interestHandler) {
		if (Log.isLoggable(Log.FAC_REPO, Level.FINER))
			Log.finer(Log.FAC_REPO, "HomeAgent constructing");
		_repositoryServer = server;
		_repositoryInterestHandler = interestHandler;
		_mobileInterestHandler = new MobileInterestHandler(this);
	}
	
	/**
	 * Call-forward to CCNMP interest handler(s) depending on the type of 
	 * Interest. 
	 * 
	 * In the event that the Interest is from some remote node for content in a 
	 * namespace bounded by CCNMP, then we forward to the RemoteInterestHandler 
	 * member. 
	 * 
	 * @todo MobileNodeInterestHandler on CCNMP mobile interest
	 * In the event that the Interest is from a mobile node and for the CCNMP
	 * application, then we forward to the MobileNodeInterestHandler member.
	 * 
	 * @param interest
	 * @return 
	 */
	public boolean handleInterest(Interest interest) {
		if (Log.isLoggable(Log.FAC_REPO, Level.FINER))
			Log.finer(Log.FAC_REPO, "HomeAgent handling: {0}", interest.name());
		_mobileInterestHandler.handleInterest(interest);
		return false;
	}

	/**
	 * @todo
	 */
	@Override
	public void run() {
		
	}

}
