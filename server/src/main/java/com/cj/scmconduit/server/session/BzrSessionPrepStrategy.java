package com.cj.scmconduit.server.session;

import java.io.File;
import java.net.URI;
import java.util.LinkedList;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.sftp.SftpSubsystem;

import com.cj.scmconduit.core.bzr.Bzr;
import com.cj.scmconduit.core.util.CommandRunner;

public class BzrSessionPrepStrategy implements SessionPrepStrategy {

	@Override
	public void prepareDestinationDirectory(Integer sessionId, URI publicUri, File conduitLocation, File codePath, CommandRunner shell) {
		new Bzr(shell).createStackedBranch(publicUri.toString(), codePath.getAbsolutePath());
	}
	
	@Override
	public void configureSshDaemon(SshServer sshd, File path, int port) {
		sshd.setSubsystemFactories(new LinkedList<NamedFactory<Command>>());
		sshd.getSubsystemFactories().add(new SftpSubsystem.Factory());
	}
}
