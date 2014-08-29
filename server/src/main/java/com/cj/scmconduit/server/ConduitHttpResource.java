package com.cj.scmconduit.server;

import java.util.regex.Pattern;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;

import com.cj.scmconduit.server.session.CodeSubmissionSession;
import com.cj.scmconduit.server.session.CodeSubmissionSession.State;

public class ConduitHttpResource extends HttpObject {

    public final String name;
    private final ConduitOrchestrator orchestrator;

    public ConduitHttpResource(String name, ConduitOrchestrator orchestrator) {
        super(name + "/{remainder*}");
        this.name = name;
        this.orchestrator = orchestrator;
    }

    @Override
    public Response get(Request req) {
        final String remainder = req.pathVars().valueFor("remainder");
        final Integer sessionId = parseSessionId(remainder);
        if (sessionId == null) {
            return NOT_FOUND();
        }
        final CodeSubmissionSession session = orchestrator.getSessionWithId(sessionId);

        final Response r;

        if (session != null) {
            final State state = session.state();

            if (state == CodeSubmissionSession.State.WAITING_FOR_INPUT) {
                session.inputReceived();
                r = OK(Text("WORKING"));
            } else if (state == CodeSubmissionSession.State.WORKING) {
                r = OK(Text("WORKING"));
            } else if (state == CodeSubmissionSession.State.FINISHED || state == State.TRASHY) {
                if (session.hadErrors()) {
                    r = OK(Text("ERROR:" + session.explanation()));
                } else {
                    r = OK(Text("OK:" + session.explanation()));
                }
                if (state != State.TRASHY) {
                    session.trash();
                }
            } else {
                r = INTERNAL_SERVER_ERROR(Text("Unknown state: " + state));
            }
        } else {
            r = NOT_FOUND();
        }

        return r;
    }

    private Integer parseSessionId(String remainder) {
        try {
            String path = name + (remainder == null ? "" : ("/" + remainder));

            String[] parts = path.split(Pattern.quote("/"));
            String p = parts[parts.length - 1];

            final Integer sessionId = Integer.parseInt(p.replaceAll(Pattern.quote(".scm-conduit-push-session-"), ""));
            return sessionId;
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
