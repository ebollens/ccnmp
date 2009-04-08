package test.ccn.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

import org.junit.BeforeClass;
import org.junit.Test;

import com.parc.ccn.Library;
import com.parc.ccn.config.ConfigurationException;
import com.parc.ccn.data.ContentName;
import com.parc.ccn.data.ContentObject;
import com.parc.ccn.data.content.Collection;
import com.parc.ccn.data.content.CollectionData;
import com.parc.ccn.data.content.LinkReference;
import com.parc.ccn.data.query.BasicNameEnumeratorListener;
import com.parc.ccn.data.query.Interest;
import com.parc.ccn.library.CCNLibrary;
import com.parc.ccn.library.CCNNameEnumerator;

import junit.framework.Assert;

//public class NameEnumeratorTest extends BasePutGetTest implements BasicNameEnumeratorListener{
public class NameEnumeratorTest implements BasicNameEnumeratorListener{
	
	//static CCNLibrary _library;
	static CCNLibrary putLibrary;
	static CCNLibrary getLibrary;
	static CCNNameEnumerator putne;
	static CCNNameEnumerator getne;
	static NameEnumeratorTest net;
	
	public static Random rand = new Random();

	String namespaceString = "/parc.com";
	ContentName namespace;
	String name1String = "/parc.com/registerTest/name1";
	ContentName name1;
	String name2String = "/parc.com/registerTest/name2";
	ContentName name2;
	String name2aString = "/parc.com/registerTest/name2/namea";
	ContentName name2a;
	
	String prefix1String = "/parc.com/registerTest";
	String prefix1StringError = "/park.com/registerTest";
	ArrayList<LinkReference> names;
	ContentName prefix1;
	ContentName c1;
	ContentName c2;
	
	
	@Test
	public void testCreateEnumerator(){
		Assert.assertNotNull(putLibrary);
		Assert.assertNotNull(getLibrary);
		System.out.println("checking if we created a name enumerator");

		Assert.assertNotNull(putne);
		Assert.assertNotNull(getne);
	}
	
	@Test
	public void testRegisterName(){
		Assert.assertNotNull(putLibrary);
		Assert.assertNotNull(getLibrary);
		Assert.assertNotNull(putne);
		
		try{
			net.namespace = ContentName.fromNative(namespaceString);
			net.name1 = ContentName.fromNative(name1String);
			net.name2 = ContentName.fromNative(name2String);
			net.name2a = ContentName.fromNative(name2aString);
		}
		catch(Exception e){
			Assert.fail("Could not create ContentName from "+name1String +" or "+name2String);
		}
		
		putne.registerNameSpace(net.namespace);
		putne.registerNameForResponses(net.name1);
		putne.registerNameForResponses(net.name2);
		putne.registerNameForResponses(net.name2a);
		ContentName nullName = null;
		putne.registerNameForResponses(nullName);
		
		try{
			while(!putne.containsRegisteredName(net.name2a)){
				Thread.sleep(rand.nextInt(50));
			}
			
			//the names are registered...
			System.out.println("the names are now registered");
		}
		catch(InterruptedException e){
			System.err.println("error waiting for names to be registered by name enumeration responder");
			Assert.fail();
		}
		
	}
	
	@Test
	public void testRegisterPrefix(){
		Assert.assertNotNull(putLibrary);
		Assert.assertNotNull(getLibrary);
		Assert.assertNotNull(getne);
		
		try{
			net.prefix1 = ContentName.fromNative(prefix1String);
		}
		catch(Exception e){
			Assert.fail("Could not create ContentName from "+prefix1String);
		}
		
		System.out.println("registering prefix: "+net.prefix1.toString());
		
		try{
			getne.registerPrefix(net.prefix1);
		}
		catch(IOException e){
			System.err.println("error registering prefix");
			e.printStackTrace();
			Assert.fail();
		}
		
	}
	
	@Test
	public void testGetCallback(){

		Assert.assertNotNull(putLibrary);
		Assert.assertNotNull(getLibrary);
		int attempts = 0;
		try{
			while(net.names==null && attempts < 50){
				Thread.sleep(rand.nextInt(50));
				attempts++;
			}
			
			//the names are registered...
			System.out.println("response has been generated");
		}
		catch(InterruptedException e){
			System.err.println("error waiting for names to be registered by name enumeration responder");
			Assert.fail();
		}
		
		
		for(LinkReference lr: net.names){
			System.out.println("got name: "+lr.targetName());
			Assert.assertTrue(lr.targetName().toString().equals("/name1") || lr.targetName().toString().equals("/name2"));
		}
		
	}
	
	
		
	@Test
	public void testCancelPrefix(){
		Assert.assertNotNull(putLibrary);
		Assert.assertNotNull(getLibrary);
		Assert.assertNotNull(getne);
		
		//ContentName prefix1 = null;
		ContentName prefix1Error = null;
		
		try{
			//prefix1 = ContentName.fromNative(prefix1String);
			prefix1Error = ContentName.fromNative(prefix1StringError);
		}
		catch(Exception e){
			e.printStackTrace();
			Assert.fail("Could not create ContentName from "+prefix1String);
		}
		
		//try to remove a prefix not registered
		Assert.assertFalse(getne.cancelPrefix(prefix1Error));
		//remove the registered name
		Assert.assertTrue(getne.cancelPrefix(net.prefix1));
		//try to remove the registered name again
		Assert.assertFalse(getne.cancelPrefix(prefix1));
		
	}
	
	@Test
	public void testGetCallbackAfterCancel(){
		Assert.assertNotNull(putLibrary);
		Assert.assertNotNull(getLibrary);
		net.names = null;
		
		//getne.handleContent(createNameList(), net.prefix1);
		
		Assert.assertNull(net.names);
		
	}
	
	/*
	public static void nameEnumeratorSetup(){
		net.ne = new CCNNameEnumerator(_library, net);
	}
	*/
	
	public void setLibraries(CCNLibrary l1, CCNLibrary l2){
		putLibrary = l1;
		getLibrary = l2;
		putne = new CCNNameEnumerator(l1, net);
		getne = new CCNNameEnumerator(l2, net);
	}
    
	
	
	@BeforeClass
	public static void setup(){
		System.out.println("Starting CCNNameEnumerator Test");
		net = new NameEnumeratorTest();
		try {
			net.setLibraries(CCNLibrary.open(), CCNLibrary.open());
			//net.setLibrary(CCNLibrary.open());
			Library.logger().setLevel(Level.FINEST);
			//net.nameEnumeratorSetup();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int handleNameEnumerator(ContentName p, ArrayList<LinkReference> n) {
		
		System.out.println("got a callback!");
		
		net.names = n;
		System.out.println("here are the returned names: ");
		for(LinkReference l: net.names)
			System.out.println(l.toString()+" ("+p.toString()+l.toString()+")");
		
		return 0;
	}
	
	public ArrayList<LinkReference> createNameList(){
		ArrayList<LinkReference> n = new ArrayList<LinkReference>();
		
		try{
			ContentName c1 = ContentName.fromNative("/name1");
			ContentName c2 = ContentName.fromNative("/name2");
			
			n.add(new LinkReference(c1));
			n.add(new LinkReference(c2));
		}
		catch(Exception e){
			e.printStackTrace();
			Assert.fail("Could not create ContentName from "+prefix1String);
		}
		return n;
	}
	
}
