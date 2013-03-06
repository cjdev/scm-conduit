package com.cj.scmconduit.server.conduit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.common.Session.AttributeKey;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

public class ShellCommand implements Command, SessionAware{
    public static final AttributeKey<File> LOCAL_PATH = new AttributeKey<File>();
    
    public static void main(String[] args) throws Exception {

        Process p = Runtime.getRuntime().exec("cat");
        
        System.out.println("Exited with " + new ShellProcess(p, System.out, System.err, System.in).waitFor());
    }
    
    private final Log log = LogFactory.getLog(getClass());
    OutputStream errorStream;
    OutputStream outputStream;
    InputStream inputStream;
    ExitCallback callback;
    ServerSession session;
    final String command;
    final String conduitName;
    
    public ShellCommand(String command, String conduitName) {
        super();
        this.command = command;
        this.conduitName = conduitName;
    }
    
    @Override
    public void setSession(ServerSession session) {
        this.session = session;
    }
    @Override
    public void destroy() {
        log.debug("DESTROY!!!");
    }
    
    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }
    
    @Override
    public void start(Environment env) throws IOException {
        log.debug("Starting for " + toString(env));
        new Thread(){
            public void run() {
                try {
                    File path = session.getAttribute(LOCAL_PATH);
                    log.debug("Executing in " + path);
                    
                    log.debug("YO: I was asked to create this command: " + command);
                    final String realCommand = command.replaceAll(Pattern.quote("'/" + conduitName + "'"), new File(path, conduitName).getAbsolutePath());
                    log.debug("I changed the command to " + realCommand);
                    String[] cmd = realCommand.split(Pattern.quote(" "));
                    
                    ProcessBuilder pbuilder = new ProcessBuilder().command(cmd);
                    pbuilder.directory(path);
                    Process p = pbuilder.start();
                    int result = new ShellProcess(p, outputStream, errorStream, inputStream).waitFor();
                    callback.onExit(result);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }
    private String toString(Environment env) {
        if(env!=null && log.isDebugEnabled()){
            for(String key : env.getEnv().keySet()){
                log.debug(key + ":" + env.getEnv().get(key));
            }
        }
        return null;
    }

    @Override
    public void setErrorStream(OutputStream errorStream) {
        this.errorStream = errorStream;
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private static class ShellProcess {
        private final Process p;
        private StreamConduit stdOut;
        private StreamConduit stdErr;
        private StreamConduit stdIn;
        
        public ShellProcess(Process p, OutputStream outputSink, OutputStream errOutputSink, InputStream input) {
            super();
            this.p = p;
            
            stdOut = new StreamConduit("stdout", p.getInputStream(), outputSink, 1024);
            stdErr = new StreamConduit("stderr", p.getErrorStream(), errOutputSink, 1024);
            if(input!=null){
                stdIn = new StreamConduit("stdin", input, p.getOutputStream(), 1);
            }
        }
        
        public int waitFor(){
            try {
                stdOut.join();
                stdErr.join();
                if(stdIn!=null){
                    stdIn.join();
                }
                
                return p.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static class StreamConduit extends Thread {
        private final InputStream in;
        private final OutputStream out;
        private final String name;
        private final int bufferSize;
        
        public StreamConduit(String name, InputStream in, OutputStream out, int bufferSize) {
            super();
            this.in = in;
            this.out = out;
            this.name = name;
            this.bufferSize = bufferSize;
            start();
        }
        
        @Override
        public void run() {
            
            try {
                byte[] buffer = new byte[bufferSize];
                while(true){
                        int avail = in.available();
                            final int n;;
                            if(avail==0){
                                n = bufferSize;
                            }else {
                                n = Math.min(avail, bufferSize);
                            }
                            int numRead = in.read(buffer, 0, n);
                            
                            if(numRead==-1){
                                break;
                            }else{
                                out.write(buffer, 0, numRead);
                                out.flush();
                            }
                }
                if(in!=System.in) in.close();
                if(out!=System.out && out!=System.err) out.close();
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
}


