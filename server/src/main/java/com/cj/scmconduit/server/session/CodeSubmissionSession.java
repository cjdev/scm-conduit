package com.cj.scmconduit.server.session;

import java.io.File;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.util.CommandRunner;

public class CodeSubmissionSession {
    public enum State {WAITING_FOR_INPUT, WORKING, FINISHED, TRASHY}
	
	private CodeSubmissionSession.State state = State.WAITING_FOR_INPUT;

	private final Log log = LogFactory.getLog(getClass());
	private final Integer pushId;
	private final P4Credentials credentials;
	
	private final File onDisk;
	private final String conduitName;
	
	private boolean hadErrors = false;
	private String explanation;
	private final Pusher pusher;

    public void trash() {
        state = State.TRASHY;
    }

    public boolean isDone() {
        return state == State.TRASHY;
    }

    public CodeSubmissionSession(String conduitName, Integer id, URI publicUri, File conduitLocation, File onDisk, SessionPrepStrategy strategy, CommandRunner shell, final P4Credentials credentials, final Pusher pusher) {
	    this.conduitName = conduitName;
		this.pushId = id;
		this.onDisk = onDisk;
		this.credentials = credentials;
		this.pusher = pusher;
		strategy.prepareDestinationDirectory(id, publicUri, conduitLocation, codePath(), shell);
	}
	
	public boolean hadErrors(){
		return hadErrors;
	}
	
	public String explanation(){
		return explanation;
	}
	
	public Integer id() {
		return pushId;
	}
	
	public CodeSubmissionSession.State state() {
		return state;
	}
	
	private void markAsFinished(boolean hasErrors, String explanation){
		state = State.FINISHED;
		this.hadErrors = hasErrors;
		this.explanation = explanation;
		log.debug(getClass().getSimpleName() + " is finished: " + state + "  " + explanation);
	}
	
	public void inputReceived(){
		File codeLocation = codePath();
		state = CodeSubmissionSession.State.WORKING;
		log.info("Input received at " + codeLocation);
		
		pusher.submitPush(codeLocation, credentials, new Pusher.PushListener() {
			public void pushSucceeded() {
				log.info("Push succeeded: " + explanation);
				markAsFinished(false, "IT WORKED");
			}
			public void nothingToPush() {
				log.info("There was nothing to push");
				markAsFinished(false, "There was nothing to push");
			}
			public void pushFailed(String explanation) {
				log.info("Push failed: " + explanation);
				markAsFinished(true, "THE PUSH FAILED: " + explanation);
			}
		});
	}
	
	private File codePath() {
		return new File(localPath(), conduitName);
	}

    public File localPath() {
        return onDisk;
    }
	
}