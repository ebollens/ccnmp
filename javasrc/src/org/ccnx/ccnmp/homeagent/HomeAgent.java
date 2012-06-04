package org.ccnx.ccnmp.homeagent;

import java.io.IOException;
import java.util.logging.Level;

import org.ccnx.ccn.CCNContentHandler;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.impl.repo.RepositoryInterestHandler;
import org.ccnx.ccn.impl.repo.RepositoryServer;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.CCNFileInputStream;
import org.ccnx.ccn.io.CCNFileOutputStream;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccnmp.CCNMP;

/**
 * The HomeAgent thread that manages all CCNMP Home Agent functionality.
 * 
 * @author ebollens
 */
public class HomeAgent implements Runnable, CCNContentHandler {
	
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
	
	protected MobileDataHandler _mobileDataHandler;
	
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
		_mobileDataHandler = new MobileDataHandler(this);
	}
	
	public RepositoryInterestHandler getRepositoryInterestHandler(){
		return _repositoryInterestHandler;
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
		return true;
	}
	
	public boolean redirectInterest(Interest interest, Interest originalInterest) throws IOException{
						
		if (Log.isLoggable(Log.FAC_REPO, Level.FINEST))
			Log.finest(Log.FAC_REPO, "HomeAgent reissuing {0} for {1}", interest.name(), originalInterest.name());
			
		/**
		 * @todo add support for preventing interest generation loops
		 */
		//_repositoryInterestHandler.handleInterest(interest);
		
		//_repositoryServer.getHandle().expressInterest(interest, this);
		
		//_mobileDataHandler.registerRemoteInterest(interest, originalInterest);

		ContentName originalName = new ContentName(originalInterest.name(), new CCNTime());

		CCNFileInputStream ccnin = new CCNFileInputStream(interest.name(), _repositoryServer.getHandle());
		CCNFileOutputStream ccnout = new CCNFileOutputStream(originalName, _repositoryServer.getHandle());
		
		// ccnout.addOutstandingInterest(originalInterest);
		
		int BUF_SIZE = 1024;
		byte [] buffer = new byte[BUF_SIZE];

		int readcount = 0;
		while ((readcount = ccnin.read(buffer)) != -1){
			ccnout.write(buffer, 0, readcount);
			ccnout.flush();
		}
		//int read = ccnin.read(buffer);
		//while (read >= 0) {
		//	ccnout.write(buffer, 0, read);
		//	read = ccnin.read(buffer);
		//} 
		ccnin.close();
		ccnout.close(); // will flush
		
		return true;
	}

	@Override
	public Interest handleContent(ContentObject data, Interest interest) {
		
		Log.finest(Log.FAC_REPO, "HomeAgent considering if {0} is remote mapping", interest.name());
			
		if (!_mobileDataHandler.handleContent(data, interest))
			Log.warning(Log.FAC_REPO, "HomeAgent::handleContent cannot find mapping for {0}", interest.name());
		
		return null;
	}

	/**
	 * @todo
	 */
	@Override
	public void run() {
		
	}
	
	public CCNHandle getHandle(){
		return _repositoryServer.getHandle();
	}

}
