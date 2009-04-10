package com.parc.ccn.network.daemons;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DaemonOutput extends Thread {
	private InputStream _is;
	private OutputStream _os;
	
	public DaemonOutput(InputStream is, String outputFile) throws FileNotFoundException {
		_is = is;
		_os = new FileOutputStream(outputFile);
		this.start();
	}
	
	public void run() {
		while (true) {
			try {
				while (_is.available() > 0) {
					byte[] b = new byte[_is.available()];
					_is.read(b, 0, _is.available());
					_os.write(b);
					_os.flush();
				}
				Thread.sleep(1000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
