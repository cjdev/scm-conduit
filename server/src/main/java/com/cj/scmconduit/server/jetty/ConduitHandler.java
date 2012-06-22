package com.cj.scmconduit.server.jetty;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;

import com.cj.scmconduit.server.conduit.ConduitController;
import com.cj.scmconduit.server.conduit.PushSession;
import com.cj.scmconduit.server.conduit.PushSession.State;

public class ConduitHandler extends HttpObject {
	
	private final Log log;
	public final String name;
	private final ConduitController controller;
	private final Map<String, SessionResource> sessions = new HashMap<String, ConduitHandler.SessionResource>();
	
	private class SessionResource {
		final String restPath;
		final PushSession session;
		
		private SessionResource(PushSession session) {
			super();
			this.restPath = name + "/" + ".scm-conduit-push-session-" + session.id();
			this.session = session;
		}
		
	}
	
	public ConduitHandler(String name, ConduitController controller) {
		super(name + "/{remainder*}");
		this.name = name;
		log = LogFactory.getLog(getClass()+":" + name);
		this.controller = controller;
	}

	@Override
	public Response post(Request req) {
		String remainder = req.pathVars().valueFor("remainder");
		if(remainder==null || remainder.equals("")){
			log.info("Creating session");
			
			SessionResource session = new SessionResource(controller.newSession());
			log.info("created session: " + session);
			
			sessions.put(session.restPath, session);
			
			return OK(Json("{\n" + 
					"\"pushLocation\":\"" + session.session.sftpUrl() + "\",\n" + 
					"\"resultLocation\":\"" + session.restPath + "\"\n" + 
					"}"));
		}else{
			return BAD_REQUEST();
		}
	}
	
	
	@Override
	public Response get(Request req) {
		String remainder = req.pathVars().valueFor("remainder");
		String path = name + (remainder==null?"":("/" + remainder));	
		
		final SessionResource resource = sessions.get(path);
		
		final Response r;
		
		if(resource!=null){
			final State state = resource.session.state();
			
			if(state == PushSession.State.WAITING_FOR_INPUT){
				resource.session.inputReceived(controller);
				r = OK(Text("WORKING"));
			}else if(state == PushSession.State.WORKING){
				r = OK(Text("WORKING"));
			}else if(state == PushSession.State.FINISHED){
				if(resource.session.hadErrors()){
					r = OK(Text("ERROR:" + resource.session.explanation()));
				}else{
					r = OK(Text("OK:" + resource.session.explanation()));
				}
			}else{
				r = INTERNAL_SERVER_ERROR(Text("Unknown state: " + state));
			}
		}else{
			r = NOT_FOUND();
		}
		
		return r;
	}

}
