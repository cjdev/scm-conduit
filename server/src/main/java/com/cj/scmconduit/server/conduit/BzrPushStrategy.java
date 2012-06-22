package com.cj.scmconduit.server.conduit;

import java.io.File;
import java.net.URI;
import java.util.LinkedList;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.sftp.SftpSubsystem;

import com.cj.scmconduit.core.bzr.Bzr;
import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.server.conduit.PushSession.PushStrategy;

public class BzrPushStrategy implements PushStrategy {

	@Override
	public void prepareDestinationDirectory(URI publicUri, File conduitLocation, File codePath, CommandRunner shell) {
		new Bzr(shell).createStackedBranch(publicUri.toString(), codePath.getAbsolutePath());
	}
	
	@Override
	public String constructPushUrl(String hostname, Integer port, String path) {
		return "sftp://" + hostname + ":" + port + "/code";
	}
	
	@Override
	public void configureSshDaemon(SshServer sshd, File path, int port) {
		sshd.setSubsystemFactories(new LinkedList<NamedFactory<Command>>());
		sshd.getSubsystemFactories().add(new SftpSubsystem.Factory());
	}
}
