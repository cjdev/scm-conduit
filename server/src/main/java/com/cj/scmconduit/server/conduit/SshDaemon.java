package com.cj.scmconduit.server.conduit;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.Session.AttributeKey;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.FileSystemFactory;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.SessionFactory;

import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.server.conduit.PushSession.PushStrategy;
import com.cj.scmconduit.server.ssh.SshFsView;

public class SshDaemon {
    public interface SessionHandler{
        PushSession prepareSessionFor(P4Credentials credentials, Session session);
    }
    private final SshServer sshd;
    private final Log log = LogFactory.getLog(getClass());
    private final AttributeKey<P4Credentials> CREDENTIALS_KEY = new AttributeKey<P4Credentials>();
    public final AttributeKey<PushSession> PUSH_SESSION_KEY = new AttributeKey<PushSession>();

    public SshDaemon(int port, final SessionHandler handler, final PushStrategy strategy, final Runnable onPushComplete) { 


        log.info("Serving at port " + port);
        try {
            sshd = SshServer.setUpDefaultServer();

            sshd.setFileSystemFactory(new FileSystemFactory() {
                @Override
                public FileSystemView createFileSystemView(Session session)
                        throws IOException {
                    PushSession push = session.getAttribute(PUSH_SESSION_KEY);
                    return new SshFsView(
                                    session.getUsername(), 
                                    false, 
                                    push.localPath());
                }
            });

            sshd.setPort(port);
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
            sshd.setPasswordAuthenticator(new PasswordAuthenticator() {

                public boolean authenticate(String username, String password, ServerSession session) {
                    P4Credentials credentials = new P4Credentials(username, password);
                    session.setAttribute(CREDENTIALS_KEY, credentials);
                    PushSession push = handler.prepareSessionFor(credentials, session);
                    session.setAttribute(PUSH_SESSION_KEY, push);
                    return true;
                }
            });
            
            strategy.configureSshDaemon(sshd, null, port);
            sshd.setSessionFactory(new SessionFactory(){
                @Override
                public void sessionOpened(IoSession session) throws Exception {
                    super.sessionOpened(session);
                }
                @Override
                public void sessionClosed(IoSession ioSession) throws Exception {
                    onPushComplete.run();

                }
            });
            sshd.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void stop() throws InterruptedException {
        sshd.stop();
    }


}
