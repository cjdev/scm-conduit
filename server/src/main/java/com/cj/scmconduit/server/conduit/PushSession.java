package com.cj.scmconduit.server.conduit;

import java.io.File;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.SshServer;

import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.util.CommandRunner;

public class PushSession {
	public enum State {WAITING_FOR_INPUT, WORKING, FINISHED}
	
	public interface PushStrategy {
		void prepareDestinationDirectory(Integer sessionId, URI publicUri, File conduitLocation, File codePath, CommandRunner shell);
		void configureSshDaemon(SshServer sshd, final File path, int port);
	}
	
	private PushSession.State state = State.WAITING_FOR_INPUT;

	private final Log log = LogFactory.getLog(getClass());
	private final Integer pushId;
	private final P4Credentials credentials;
	
	private final File onDisk;
	private final String conduitName;
	
	private boolean hadErrors = false;
	private String explanation;

	PushSession(String conduitName, Integer id, URI publicUri, File conduitLocation, File onDisk, PushStrategy strategy, CommandRunner shell, final P4Credentials credentials) {
	    this.conduitName = conduitName;
		this.pushId = id;
		this.onDisk = onDisk;
		this.credentials = credentials;
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
	
	public PushSession.State state() {
		return state;
	}
	
	private void markAsFinished(boolean hasErrors, String explanation){
		state = State.FINISHED;
		this.hadErrors = hasErrors;
		this.explanation = explanation;
		log.debug(getClass().getSimpleName() + " is finished: " + state + "  " + explanation);
	}
	
	public void inputReceived(final Pusher pusher){
		File codeLocation = codePath();
		state = PushSession.State.WORKING;
		log.info("Input received at " + codeLocation);
		
		pusher.submitPush(codeLocation, credentials, new Pusher.PushListener() {
			public void pushSucceeded() {
				log.info("Push succeeded: " + explanation);
				close();
				markAsFinished(false, "IT WORKED");
			}
			public void nothingToPush() {
				log.info("There was nothing to push");
				close();
				markAsFinished(false, "There was nothing to push");
			}
			public void pushFailed(String explanation) {
				log.info("Push failed: " + explanation);
				close();
				markAsFinished(true, "THE PUSH FAILED: " + explanation);
			}
		});
	}
	
	private File codePath() {
		return new File(localPath(), conduitName);
	}

	synchronized void close(){
	}

    public File localPath() {
        return onDisk;
    }
	
}