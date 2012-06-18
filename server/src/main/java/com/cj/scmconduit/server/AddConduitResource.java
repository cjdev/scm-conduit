package com.cj.scmconduit.server;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;

public class AddConduitResource extends HttpObject {
	public interface Listener {
		void addConduit(String name, String p4Path);
	}
	
	private Listener listener;
	
	public AddConduitResource(Listener listener) {
		super("/admin");
		this.listener = listener;
	}
	
	@Override
	public Response get(Request req) {
		return OK(Html(
				"<html><body>" +
				"	<form action=\"/admin\" method=\"post\" >" +
				"		<div>Name: <input type=\"text\" name=\"name\"/></div>" +
				"		<div>Perforce Path: <input type=\"text\" name=\"p4path\"/></div>" +
				"		<input type=\"submit\" value=\"Add\">" +
				"	</form>" +
				"</body></html>"));
	}
	
	@Override
	public Response post(Request req) {
		String name = req.getParameter("name");
		String p4path = req.getParameter("p4path");
		
		listener.addConduit(name, p4path);
		
		return OK(Text("You wanted me to create a conduit named \"" + name + "\" to " + p4path));
	}
}
