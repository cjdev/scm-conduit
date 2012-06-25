package com.cj.scmconduit.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CommandRunnerImpl implements CommandRunner {

	public String run(String command, String ... args){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream errOut = new ByteArrayOutputStream();
		try {
			run(out, errOut, null, command, args);
			return new String(out.toByteArray());
		} catch (Exception e) {
			throw makeErrorMessage(e, out, errOut);
		}
	}
		
	private RuntimeException makeErrorMessage(Throwable e, ByteArrayOutputStream out, ByteArrayOutputStream errOut){

		return new RuntimeException("Error.  " + e.getMessage() + "\n" + 
				"=============== StdOut ============= \n" + 
				new String(out.toByteArray()) + "\n" +
				"=============== StdErr ============= \n" + 
				 new String(errOut.toByteArray()));
	}
	
	public String run(InputStream in, String command, String ... args){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream errOut = new ByteArrayOutputStream();
		try {
			run(out, errOut, in, command, args);
		} catch (Exception e) {
			throw makeErrorMessage(e, out, errOut);
		}
		return new String(out.toByteArray());
	}
	
	public void runPassThrough(String command, String ... args){
		run(null, System.err, System.in, command, args);
	}
	
	public void run(OutputStream sink, OutputStream errSink, InputStream input, String command, String ... args){
		try {
			List<String> parts = new LinkedList<String>();
			parts.add(command);
			if(args!=null){
				parts.addAll(Arrays.asList(args));
			}
			
			System.out.print("[COMMAND]");
			for(String next : parts){
				System.out.print(" " + next);
			}
			System.out.println();
			
			int returnCode = new ShellProcess(
									Runtime.getRuntime().exec(parts.toArray(new String[parts.size()])),
									sink,
									errSink,
									input
								).waitFor();
			
			if(returnCode!=0){
				throw new RuntimeException("Error: command returned " + returnCode);
			}else{
				System.out.println("[COMMAND-RETURN] " + returnCode);
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private static class ShellProcess {
		private final Process p;
		private StreamConduit stdOut;
		private StreamConduit stdErr;
		private StreamConduit stdIn;
		
		public ShellProcess(Process p, OutputStream outputSink, OutputStream errOutputSink, InputStream input) {
			super();
			this.p = p;
			
			stdOut = new StreamConduit(p.getInputStream(), outputSink!=null?outputSink:System.out);
			stdErr = new StreamConduit(p.getErrorStream(), errOutputSink);
			if(input!=null){
				stdIn = new StreamConduit(input, p.getOutputStream());
			}
		}
		
		public int waitFor(){
			try {
				stdOut.join();
				stdErr.join();
				if(stdIn!=null){
					stdIn.join();
				}
				return p.waitFor();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class StreamConduit extends Thread {
		private final InputStream in;
		private final OutputStream out;
		
		public StreamConduit(InputStream in, OutputStream out) {
			super();
			this.in = in;
			this.out = out;
			start();
		}
		
		@Override
		public void run() {
			
			try {
				for(int bite = in.read();bite!=-1;bite = in.read()){
					out.write(bite);
				}
				if(in!=System.in) in.close();
				if(out!=System.out && out!=System.err) out.close();
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
