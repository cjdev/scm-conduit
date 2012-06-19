package com.cj.scmconduit.server.conduit;

import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;

import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.server.conduit.PushSession.PushStrategy;

public class GitPushStrategy implements PushStrategy {
	
	@Override
	public void prepareDestinationDirectory(URI publicUri, File codePath, CommandRunner shell) {
		shell.run("git", "clone", "--bare", publicUri.toString() + "/.git", codePath.getAbsolutePath());
	}
	
	@Override
	public String constructPushUrl(String hostname, Integer port, String path) {
		return "ssh://" + hostname + ":" + port + "/code";
	}
	
	@Override
	public void configureSshDaemon(SshServer sshd, final File path, int port) {
		sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" }));
		
		sshd.setCommandFactory(new ScpCommandFactory(new CommandFactory() {
			@Override
			public Command createCommand(String command) {
				
				System.out.println("YO: I was asked to create this command: " + command);
				command = command.replaceAll(Pattern.quote("'/code'"), new File(path, "code").getAbsolutePath());
				System.out.println("I changed the command to " + command);
				return new ProcessShellFactory(command.split(Pattern.quote(" "))).create();
			}
		}));
		
	}
}
