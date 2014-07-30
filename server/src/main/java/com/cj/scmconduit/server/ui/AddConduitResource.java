package com.cj.scmconduit.server.ui;

import org.apache.commons.io.IOUtils;
import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.header.response.LocationField;

import com.cj.scmconduit.server.api.ConduitType;

import java.io.*;
import java.nio.ByteBuffer;

public class AddConduitResource extends HttpObject {
	public interface Listener {
		void addConduit(ConduitType type, String name, String p4Path, Integer p4FirstCL);
	}
	private final Listener listener;
	
	public AddConduitResource(Listener listener) {
		super("/admin");
		this.listener = listener;
	}

	@Override
	public Response get(Request req) {
        return OK(FromClasspath("text/html", "/com/cj/scmconduit/server/admin.html", this.getClass()));
	}
	
	@Override
	public Response post(Request req) {
		try{
			ConduitType type = ConduitType.valueOf(req.getParameter("type"));
			String name = req.getParameter("name");
			String p4path = req.getParameter("p4path");
			Integer p4FirstCL = Integer.parseInt(req.getParameter("p4FirstCL"));
			
			listener.addConduit(type, name, p4path, p4FirstCL);
			return SEE_OTHER(new LocationField("/"));
			
		}catch(Exception e){
			return OK(Text("Sorry, there was an error: " + toString(e)));
		}
	}
	

	private static String toString(Throwable t) {
		final StringBuffer text = new StringBuffer(t.getClass().getName());
		final String message = t.getMessage();
		if(message!=null){
			text.append(": ");
			text.append(message);
		}
		
		for(StackTraceElement next : t.getStackTrace()){
			text.append("\n    at " + next.getClassName() + "." + next.getMethodName() + "(" + next.getFileName() + ":" +  next.getLineNumber() + ")");
		}
		return text.toString();
	}
}
