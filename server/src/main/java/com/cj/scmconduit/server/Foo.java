package com.cj.scmconduit.server;

import java.io.File;
import java.net.URI;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;

import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.server.conduit.PushSession.PushStrategy;
import com.cj.scmconduit.server.conduit.PushSession;
import com.cj.scmconduit.server.conduit.SshDaemon;
import com.cj.scmconduit.server.conduit.SshDaemon.SessionHandler;
import com.cj.scmconduit.server.data.FilesOnDiskKeyValueStore;

public class Foo {
    public static void main(String[] args) {
        SessionHandler handler = new SessionHandler(){
            @Override
            public PushSession prepareSessionFor(P4Credentials credentials, Session session) {
                return null;
            }
        };
        
        PushStrategy strategy = new PushStrategy(){
            @Override
            public void configureSshDaemon(SshServer sshd, File path, int port) {
            }
            @Override
            public void prepareDestinationDirectory(Integer sessionId,
                    URI publicUri, File conduitLocation, File codePath,
                    CommandRunner shell) {
            }
        };
        
        Runnable action = new Runnable() {
            @Override
            public void run() {
            }
        };
        
        new SshDaemon(3929, handler, strategy, action, new FilesOnDiskKeyValueStore(new File("keys")), new FilesOnDiskKeyValueStore(new File("credentials")));
    }
}
