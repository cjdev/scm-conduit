package com.cj.scmconduit.server.jetty;

import java.util.regex.Pattern;

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
	
	public ConduitHandler(String name, ConduitController controller) {
		super(name + "/{remainder*}");
		this.name = name;
		log = LogFactory.getLog(getClass()+":" + name);
		this.controller = controller;
	}

	@Override
	public Response get(Request req) {
		final String remainder = req.pathVars().valueFor("remainder");
		final Integer sessionId = parseSessionId(remainder);
		if(sessionId==null) return NOT_FOUND();
		final PushSession session = controller.sessions().get(sessionId);
		
		final Response r;
		
		if(session!=null){
			final State state = session.state();
			
			if(state == PushSession.State.WAITING_FOR_INPUT){
				session.inputReceived(controller);
				r = OK(Text("WORKING"));
			}else if(state == PushSession.State.WORKING){
				r = OK(Text("WORKING"));
			}else if(state == PushSession.State.FINISHED){
				if(session.hadErrors()){
					r = OK(Text("ERROR:" + session.explanation()));
				}else{
					r = OK(Text("OK:" + session.explanation()));
				}
			}else{
				r = INTERNAL_SERVER_ERROR(Text("Unknown state: " + state));
			}
		}else{
			r = NOT_FOUND();
		}
		
		return r;
	}

    private Integer parseSessionId(String remainder) {
        try {
            String path = name + (remainder==null?"":("/" + remainder));	
            
            String[] parts = path.split(Pattern.quote("/"));
            String p = parts[parts.length-1];
            
            final Integer sessionId = Integer.parseInt(p.replaceAll(Pattern.quote(".scm-conduit-push-session-"), ""));
            return sessionId;
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
