package com.cj.scmconduit.server.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SelfResettingFileOutputStream extends OutputStream {
	private final File pathToFile;
	private final Long maxSizeInBytes;
	private FileOutputStream fOut;
	
	public SelfResettingFileOutputStream(File pathToFile, Long maxSizeInBytes) {
		super();
		this.pathToFile = pathToFile;
		this.maxSizeInBytes = maxSizeInBytes;
		reset();
	}

	@Override
	public void flush() throws IOException {
		fOut.flush();
		if(pathToFile.length()>maxSizeInBytes){
			reset();
		}
	}
	
	private void reset(){
		try {
			if(fOut!=null) fOut.close();
			fOut = new FileOutputStream(pathToFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void write(int b) throws IOException {
		fOut.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		fOut.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len)throws IOException {
		fOut.write(b, off, len);
	}
	
}