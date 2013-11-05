package com.cj.scmconduit.server.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.Session.AttributeKey;
import org.apache.sshd.server.FileSystemFactory;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.SessionFactory;

import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.server.data.KeyValueStore;
import com.cj.scmconduit.server.ssh.SshFsView;

public class ConduitSshDaemon {
    public interface SessionHandler{
        CodeSubmissionSession prepareSessionFor(P4Credentials credentials, Session session);
    }
    private final SshServer sshd;
    private final Log log = LogFactory.getLog(getClass());
    private final AttributeKey<P4Credentials> CREDENTIALS_KEY = new AttributeKey<P4Credentials>();
    public final AttributeKey<CodeSubmissionSession> PUSH_SESSION_KEY = new AttributeKey<CodeSubmissionSession>();
    
    public ConduitSshDaemon(int port, final SessionHandler handler, final SessionPrepStrategy strategy, final Runnable onPushComplete, final KeyValueStore sshKeys, final KeyValueStore perforcePasswords) { 

        log.info("Serving at port " + port);
        try {
            sshd = SshServer.setUpDefaultServer();

            sshd.setFileSystemFactory(new FileSystemFactory() {
                @Override
                public FileSystemView createFileSystemView(Session session)
                        throws IOException {
                    CodeSubmissionSession push = session.getAttribute(PUSH_SESSION_KEY);
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
                    setCredentials(handler, session, new P4Credentials(username, password));
                    return true;
                }
            });
            
            sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
                @Override
                public boolean authenticate(String username, PublicKey key, ServerSession session) {
                    try {
                        if(isCorrectKeyForUser(username, key)){
                            final String passwordOnFile = perforcePasswords.get(username);
                            if(passwordOnFile!=null){
                                setCredentials(handler, session, new P4Credentials(username, passwordOnFile.trim()));
                                return true;
                            }else{
                                log.debug("There was no password!");
                                return false;
                            }
                        }else {
                            return false;
                        }
                    } catch (Exception e) {
                        log.debug("There was an error");
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }

                private Boolean isCorrectKeyForUser(String username, PublicKey key) throws IOException {
                    log.debug("############### Received ##############\n" + 
                               key + 
                              "#######################################\n");
                    log.debug(key.getClass());
                    
                    RSAPublicKey rsaKey = cast(key);
                    
                    String actual = new String(Base64.encodeBase64(rfc4253Encode(rsaKey)));
                    String keyOnFile = sshKeys.get(username);
                    
                    if(keyOnFile==null){
                        log.debug("No key on file for " + username);
                        return false;
                    }else{
                        String expected = keyOnFile.trim().split(" ")[1];
                        log.debug(actual + "\n    vs\n" + expected);
                        Boolean result = actual.equals(expected);
                        log.debug(result);
                        return result;
                    }
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


    @SuppressWarnings("unchecked")
    private static <O, T extends O> T cast(O object){
        return (T) object;
    }
    
    private void setCredentials(final SessionHandler handler, ServerSession session, P4Credentials credentials) {
        session.setAttribute(CREDENTIALS_KEY, credentials);
        CodeSubmissionSession push = handler.prepareSessionFor(credentials, session);
        session.setAttribute(PUSH_SESSION_KEY, push);
    }
    

    public byte[] rfc4253Encode(RSAPublicKey key) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        /* encode the "ssh-rsa" string */
        byte[] sshrsa = new byte[] {0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a'};
        out.write(sshrsa);
        /* Encode the public exponent */
        BigInteger e = key.getPublicExponent();
        byte[] data = e.toByteArray();
        encodeUInt32(data.length, out);
        out.write(data);
        /* Encode the modulus */
        BigInteger m = key.getModulus();
        data = m.toByteArray();
        encodeUInt32(data.length, out);
        out.write(data);
        return out.toByteArray();
    }

    public void encodeUInt32(int value, OutputStream out) throws IOException {
        byte[] tmp = new byte[4];
        tmp[0] = (byte)((value >>> 24) & 0xff);
        tmp[1] = (byte)((value >>> 16) & 0xff);
        tmp[2] = (byte)((value >>> 8) & 0xff);
        tmp[3] = (byte)(value & 0xff);
        out.write(tmp);
    }
     
}
