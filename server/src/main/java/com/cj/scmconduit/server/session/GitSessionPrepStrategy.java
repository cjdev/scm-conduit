package com.cj.scmconduit.server.session;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;

import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.server.ssh.ShellCommand;

public class GitSessionPrepStrategy implements SessionPrepStrategy {
	private final Log log = LogFactory.getLog(getClass());
	private final String conduitName;
	
	public GitSessionPrepStrategy(String conduitName) {
        super();
        this.conduitName = conduitName;
    }

    @Override
	public void prepareDestinationDirectory(Integer sessionId, URI publicUri, File conduitLocation, File codePath, CommandRunner shell) {
		shell.run("git", "clone", "--bare", conduitLocation.getAbsolutePath() + "/.git", codePath.getAbsolutePath());
		  try {
            File pathToHook = new File(codePath, "hooks/post-receive");
              log.debug("WRiting post-receive hook to " + pathToHook.getAbsolutePath());
              String script = IOUtils.toString(getClass().getResourceAsStream("/com/cj/scmconduit/post-receive.py"));
              script = script.replaceAll("THE_REPO", publicUri.toASCIIString() + "/.scm-conduit-push-session-" + sessionId);
              FileUtils.writeStringToFile(pathToHook, script);
              pathToHook.setExecutable(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}
	
	@Override
	public void configureSshDaemon(SshServer sshd, final File path, int port) {
	    
	    
		sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" }));
		sshd.setCommandFactory(new ScpCommandFactory(new CommandFactory() {
		    
			@Override
			public Command createCommand(String command) {

               
                    return new ShellCommand(command, conduitName);
			}
		}));
		
	}
}
