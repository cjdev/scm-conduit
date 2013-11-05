package com.cj.scmconduit.server.session;

import java.io.File;
import java.net.URI;

import org.apache.sshd.SshServer;

import com.cj.scmconduit.core.util.CommandRunner;

public interface SessionPrepStrategy {
	void prepareDestinationDirectory(Integer sessionId, URI publicUri, File conduitLocation, File codePath, CommandRunner shell);
	void configureSshDaemon(SshServer sshd, final File path, int port);
}