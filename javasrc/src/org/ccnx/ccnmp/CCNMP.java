package org.ccnx.ccnmp;

/**
 *
 * @author ebollens
 */
public class CCNMP {
	
	/**
	 * Set true if CCNMP functionality should be enabled.
	 */
	public final static boolean ENABLED = true;

	/**
	 * CCNMP home-mobile node communicate commands
	 */
	public final static String COMMAND_ROOT = "ccnmp";
	public final static String COMMAND_REGISTER = "rg";
	public final static String COMMAND_REDIRECT = "rd";
	public final static String COMMAND_REGISTERREDIRECT = "rr";
	public final static String COMMAND_REMOVE = "rm";
	
	/**
	 * CCNMP home-mobile node communicate response
	 */
	public final static String RESPONSE_SUCCESS = "ACK";
	public final static String RESPONSE_FAILURE = "ERR";
	
}
