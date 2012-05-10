package com.cj.scmconduit.server.jetty;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.handler.AbstractHandler;

import com.cj.scmconduit.server.conduit.ConduitController;
import com.cj.scmconduit.server.conduit.PushSession;

public class ConduitHandler extends AbstractHandler {
	
	private final Log log;
	private final String path;
	private final ConduitController controller;
	private final Map<String, SessionResource> sessions = new HashMap<String, ConduitHandler.SessionResource>();
	
	private class SessionResource {
		final String restPath;
		final PushSession session;
		
		public SessionResource(PushSession session) {
			super();
			this.restPath = statusPathFor(session);;
			this.session = session;
		}
		private String statusPathFor(PushSession session){
			if(path.endsWith("/")){
				return path + ".scm-conduit-push-session-" + session.id();
			}else{
				return path + "/.scm-conduit-push-session-" + session.id();
			}
		}
	}
	
	public ConduitHandler(String path, ConduitController controller) {
		super();
		log = LogFactory.getLog(getClass()+":" + path);
		this.path = path;
		this.controller = controller;
	}

	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {
		try{
			
			if(!target.startsWith(path)) return;
			log.info("Request for " + target);
			
			final String method = request.getMethod();
			
			if(target.equals(path)){
				if(method.equals("POST")){
					log.info("Creating session");
					
					SessionResource session = new SessionResource(controller.newSession());
					log.info("created session: " + session);
					
					sessions.put(session.restPath, session);
					
					
					send("{\n" + 
							"\"pushLocation\":\"" + session.session.sftpUrl() + "\",\n" + 
							"\"resultLocation\":\"" + session.restPath + "\"\n" + 
							"}", response);
				}
				
			}else{
				
				final SessionResource resource = sessions.get(target);
				if(resource!=null){
					if(resource.session.state() == PushSession.State.WAITING_FOR_INPUT){
						resource.session.inputReceived(controller);
						send("WORKING", response);
					}else if(resource.session.state() == PushSession.State.WORKING){
						send("WORKING", response);
					}else if(resource.session.state() == PushSession.State.FINISHED){
						if(resource.session.hadErrors()){
							send("ERROR:" + resource.session.explanation(), response);
						}else{
							send("OK:" + resource.session.explanation(), response);
						}
					}
				}
				
			}
		} catch (Throwable t){
			log.error("There was an error in the conduit handler", t);
			throw new RuntimeException(t);
		}
	}

	private void send(String text, HttpServletResponse response) throws IOException{
		PrintWriter w = response.getWriter();
		w.write(text);
		w.close();
	}

}
