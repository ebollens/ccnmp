package org.ccnx.ccnmp.mobilenode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.ccnx.ccn.CCNContentHandler;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.CCNInterestHandler;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.CCNFileOutputStream;
import org.ccnx.ccn.profiles.SegmentationProfile;
import org.ccnx.ccn.profiles.metadata.MetadataProfile;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.KeyLocator;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;
import org.ccnx.ccn.protocol.SignedInfo;
import org.ccnx.ccn.protocol.SignedInfo.ContentType;
import org.ccnx.ccnmp.CCNMP;

public class MobileNode implements Runnable, CCNInterestHandler, CCNContentHandler {
	
	private static int BUF_SIZE = 4096;
	
	protected ContentName _homePrefix;
	protected ContentName _remotePrefix;
	protected long _refreshRate;
	protected String _filePrefix;
	protected File _rootDirectory;
	protected CCNHandle _handle;
	
	private final Thread _thd;
	private boolean _finished;
	
	public static void usage() {
		System.err.println("usage: MobileNode <root directory> <home namespace> <refresh rate in sec> [<foreign namespace> default:<home namespace>]");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length < 3) {
			usage();
			return;
		}
		
		String filePrefix = args[0];
		String homePrefix = args[1];
		long refreshRate = Integer.parseInt(args[2]) * 1000;
		String remotePrefix = (args.length > 3)? args[3]: homePrefix;
		
		try {
			MobileNode mn;
			
			try {
				mn = new MobileNode(filePrefix, homePrefix, remotePrefix, refreshRate);
			} catch (MalformedContentNameStringException e) {
				System.err.println("Invalid Name String" + e);
				return;
			} 
			
			mn.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String inputline = null;

			while((inputline = br.readLine()) != null) {
				inputline = inputline.trim();
				if (inputline.compareTo("exit") == 0) {
					break;
				}

				String[] tokens = inputline.split(" ");
				if (tokens.length < 2) {
					System.err.println("Invalid command" + inputline);
					continue;
				}
				
				if (tokens[0].compareTo("move") == 0) {
					try{
						mn.move(tokens[1]);
					} catch (MalformedContentNameStringException e) {
						System.err.println("Invalid namespace" + tokens[1]);
						continue;
					}
				} else {
					System.err.println("Invalid command" + inputline);
					continue;
				}
			}
			
			mn.shutdown();
			
		} catch (ConfigurationException e) {
			Log.warning("Exception in MobileNode: type: " + e.getClass().getName() + ", message:  "+ e.getMessage());
			Log.warningStackTrace(e);
			System.err.println("Exception in MobileNode: type: " + e.getClass().getName() + ", message:  "+ e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.warning("Exception in MobileNode: type: " + e.getClass().getName() + ", message:  "+ e.getMessage());
			Log.warningStackTrace(e);
			System.err.println("Exception in MobileNode: type: " + e.getClass().getName() + ", message:  "+ e.getMessage());
			e.printStackTrace();
		} 
	}
	
	public MobileNode(String filePrefix, String homePrefix, String remotePrefix, long refreshRate) throws MalformedContentNameStringException, ConfigurationException, IOException {

		_homePrefix = ContentName.fromURI(homePrefix);
		_remotePrefix = ContentName.fromURI(remotePrefix);
		_refreshRate = refreshRate;
		_filePrefix = filePrefix;
		_rootDirectory = new File(filePrefix);
		if (!_rootDirectory.exists() || !_rootDirectory.isDirectory()) {
			Log.severe("Cannot serve files from directory {0}: directory does not exist!", filePrefix);
			throw new IOException("Cannot serve files from directory " + filePrefix + ": directory does not exist!");
		}
		_handle = CCNHandle.open();

		_thd = new Thread(this, "MobileNode");
		_finished = false;
		
	}
	
	public void start() throws IOException {
		_handle.registerFilter(_remotePrefix, this);
		
		if (!register()) {
			shutdown();
			System.exit(1);
		}
		
		_thd.start();
	}
	
	public void run() {
		while (!_finished) {
			try {
				redirect();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			try {
				Thread.sleep(_refreshRate);
			} catch (InterruptedException e) {
				continue;
			} 
		}
		Log.info("Thread ends");
	}
	
    /**
     * Turn off everything.
     * @throws IOException 
     */
	public void shutdown() throws IOException {
		if (null != _handle) {
			_handle.unregisterFilter(_remotePrefix, this);
			Log.info("Shutting down file proxy for " + _filePrefix + " on CCNx namespace " + _remotePrefix + "...");
		}
		_finished = true;
		_thd.interrupt();
	}

	@Override
	public Interest handleContent(ContentObject data, Interest interest) {
		Log.info("MobileNode: get content {0} for interest {1}.",data, interest);
		return null;
	}

	@Override
	public boolean handleInterest(Interest interest) {
		// Alright, we've gotten an interest. Either it's an interest for a stream we're
		// already reading, or it's a request for a new stream.
		Log.info("MobileNode main responder: got new interest: {0}", interest);

		// Test to see if we need to respond to it.
		if (!_remotePrefix.isPrefixOf(interest.name())) {
			Log.info("Unexpected: got an interest not matching our prefix (which is {0})", _remotePrefix);
			return false;
		}

//		ContentObject content = generateContent(interest);
//		
//		if (content != null) {
//			Log.info("Satisfying interest: {0} with content {1}", interest, content);
//			try {
//				_handle.put(content);
//			} catch (IOException e) {
//				Log.warning("IOException {0}", e);
//				return false;
//			}
//		} else {
//			Log.info("Cannot satify interest {0}", interest.name());
//			return false;
//		}
//		
//		return true;
		

		// We see interests for all our segments, and the header. We want to only
		// handle interests for the first segment of a file, and not the first segment
		// of the header. Order tests so most common one (segments other than first, non-header)
		// fails first.
		if (SegmentationProfile.isSegment(interest.name()) && !SegmentationProfile.isFirstSegment(interest.name())) {
			Log.info("Got an interest for something other than a first segment, ignoring {0}.", interest.name());
			return false;
		} else if (MetadataProfile.isHeader(interest.name())) {
			Log.info("Got an interest for the first segment of the header, ignoring {0}.", interest.name());
			return false;
		} 

		// Write the file
		try {
			return writeFile(interest);
		} catch (IOException e) {
			Log.warning("IOException writing file {0}: {1}: {2}", interest.name(), e.getClass().getName(), e.getMessage());
			return false;
		}
	}

	public boolean move(String newRemoteName) throws MalformedContentNameStringException, IOException {
		_handle.unregisterFilter(_remotePrefix, this);
		
		_remotePrefix = ContentName.fromURI(newRemoteName);

		_handle.registerFilter(_remotePrefix, this);
		
		return redirect();
	}
	
	protected File ccnNameToFilePath(ContentName name) {
		
		ContentName fileNamePostfix = name.postfix(_remotePrefix);
		assert (fileNamePostfix != null);	// _remotePrefix is not a prefix of name

		File fileToWrite = new File(_rootDirectory, fileNamePostfix.toString());
		Log.info("MobileNode: file postfix {0}, resulting path name {1}", fileNamePostfix, fileToWrite.getAbsolutePath());
		return fileToWrite;
	}
	
	protected ContentObject generateContent(Interest interest) {
		
		File fileToWrite = ccnNameToFilePath(interest.name());
		Log.finest("MobileNode: extracted request for file: " + fileToWrite.getAbsolutePath() + " exists? {0}", fileToWrite.exists());
		if (!fileToWrite.exists()) {
			Log.warning("MobileNode: File {0} does not exist. Ignoring request.", fileToWrite.getAbsoluteFile());
			return null;
		}
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileToWrite);
		} catch (FileNotFoundException fnf) {
			Log.warning("MobileNode: file we expected to exist doesn't exist: {0}.", fileToWrite.getAbsolutePath());
			return null;
		}

		long fileLength = fileToWrite.length();
		
		if (fileLength > Integer.MAX_VALUE) {
			Log.warning("File is too large.");
			return null;
		}

		byte [] fileContent = new byte[(int) fileLength];
		try {
			fis.read(fileContent);
			fis.close();
		} catch (IOException e) {
			Log.warning("File reading error: {0}", e);
			return null;
		}
		Log.finest("fileContent: {0}", fileContent.toString());

		ContentName name = new ContentName(interest.name());
		KeyManager keyManager = KeyManager.getDefaultKeyManager();
		PrivateKey signingKey = keyManager.getDefaultSigningKey();
		PublisherPublicKeyDigest publisher = keyManager.getPublisherKeyID(signingKey);
		KeyLocator locator = keyManager.getKeyLocator(signingKey);
		SignedInfo signedInfo = new SignedInfo(publisher, ContentType.DATA, locator);
		
		ContentObject co;
		try {
			co = new ContentObject(name, signedInfo, fileContent, signingKey);
		} catch (InvalidKeyException e) {
			Log.warning("Content generation failed: {0}", e);
			return null;
		} catch (SignatureException e) {
			Log.warning("Content generation failed: {0}", e);
			return null;
		}
		
		return co;
	}
	
	
	/**
	 * Actually write the file; should probably run in a separate thread.
	 * Copied from CCNFileProxy
	 * @param fileNamePostfix
	 * @throws IOException 
	 */
	protected boolean writeFile(Interest outstandingInterest) throws IOException {
		
		File fileToWrite = ccnNameToFilePath(outstandingInterest.name());
		Log.info("MobileNode: extracted request for file: " + fileToWrite.getAbsolutePath() + " exists? ", fileToWrite.exists());
		if (!fileToWrite.exists()) {
			Log.warning("MobileNode: File {0} does not exist. Ignoring request.", fileToWrite.getAbsoluteFile());
			return false;
		}
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileToWrite);
		} catch (FileNotFoundException fnf) {
			Log.warning("MobileNode: file we expected to exist doesn't exist: {0}.", fileToWrite.getAbsolutePath());
			return false;
		}
		
		// Set the version of the CCN content to be the last modification time of the file.
		CCNTime modificationTime = new CCNTime(fileToWrite.lastModified());
		ContentName versionedName = new ContentName(outstandingInterest.name(), modificationTime);

		// CCNFileOutputStream will use the version on a name you hand it (or if the name
		// is unversioned, it will version it).
		CCNFileOutputStream ccnout = new CCNFileOutputStream(versionedName, _handle);
		
		// We have an interest already, register it so we can write immediately.
		ccnout.addOutstandingInterest(outstandingInterest);
		
		byte [] buffer = new byte[BUF_SIZE];
		
		int read = fis.read(buffer);
		while (read >= 0) {
			ccnout.write(buffer, 0, read);
			read = fis.read(buffer);
		} 
		fis.close();
		ccnout.close(); // will flush
		
		return true;
	}
	
	protected boolean register() throws IOException {
		ContentName name;
		try {
			name = _homePrefix.append(CCNMP.COMMAND_ROOT + "/"+ CCNMP.COMMAND_REGISTER);
		} catch (MalformedContentNameStringException e) {
			Log.warning("MobileNode register commnad generation failed");
			return false;
		}
		CCNTime timestamp = new CCNTime();
		ContentName versionedName = new ContentName(name, timestamp);
		Interest interest = new Interest(versionedName);
		_handle.expressInterest(interest, this);
		Log.info("MobileNode: sending interest {0}.", interest);
		return true;
	}
	
	protected boolean redirect() throws IOException {
		ContentName name;
		try {
			name = _homePrefix.append(CCNMP.COMMAND_ROOT + "/"+ CCNMP.COMMAND_REDIRECT +  _remotePrefix.toString());
		} catch (MalformedContentNameStringException e) {
			Log.warning("MobileNode register commnad generation failed");
			return false;
		}
		CCNTime timestamp = new CCNTime();
		ContentName versionedName = new ContentName(name, timestamp);
		Interest interest = new Interest(versionedName);
		_handle.expressInterest(interest, this);
		Log.info("MobileNode: requesting redirection for {0}.", _remotePrefix);
		return true;
	}

}
