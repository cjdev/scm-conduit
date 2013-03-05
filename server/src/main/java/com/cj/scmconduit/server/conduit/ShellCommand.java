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
        Command c = new ShellCommand("cat", "whatever");
        c.setOutputStream(System.out);
        c.setErrorStream(System.err);
        c.setInputStream(System.in);
        c.setExitCallback(new ExitCallback() {

            @Override
            public void onExit(int exitValue, String exitMessage) {
            }

            @Override
            public void onExit(int exitValue) {
                onExit(exitValue, null);
            }
        });
        c.start(null);
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
        for(String key : env.getEnv().keySet()){
           log.debug(key + ":" + env.getEnv().get(key));
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
            
            stdOut = new StreamConduit("stdout", p.getInputStream(), outputSink);
            stdErr = new StreamConduit("stderr", p.getErrorStream(), errOutputSink);
            if(input!=null){
                stdIn = new StreamConduit("stdin", input, p.getOutputStream());
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
        
        public StreamConduit(String name, InputStream in, OutputStream out) {
            super();
            this.in = in;
            this.out = out;
            this.name = name;
            start();
        }
        
        @Override
        public void run() {
            
            try {
                while(true){
                    if(name.equals("stdin")){
                            int bite = in.read();
                            if(bite==-1){
                                break;
                            }else{
                                out.write(bite);
                                out.flush();
                            }
                    }else{
                        int bite = in.read();
                        if(bite==-1){
                            break;
                        }else{
                            out.write(bite);
                            out.flush();
                        }
                        
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


