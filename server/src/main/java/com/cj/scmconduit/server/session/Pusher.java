package com.cj.scmconduit.server.session;

import java.io.File;

import com.cj.scmconduit.core.p4.P4Credentials;

public interface Pusher {
	public static interface PushListener {
		void pushFailed(String explanation);
		void pushSucceeded();
		void nothingToPush();
	}
	
	void submitPush(File location, P4Credentials credentials, PushListener listener);
}
