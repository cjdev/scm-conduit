package com.cj.scmconduit.server;

import com.cj.scmconduit.core.ScmPump;
import com.cj.scmconduit.core.bzr.BzrToP4Pump;
import com.cj.scmconduit.core.git.GitToP4Pump;
import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.core.util.CommandRunnerImpl;
import com.cj.scmconduit.server.api.ConduitType;
import com.cj.scmconduit.server.data.Database;
import com.cj.scmconduit.server.fs.TempDirAllocator;
import com.cj.scmconduit.server.session.BzrSessionPrepStrategy;
import com.cj.scmconduit.server.session.CodeSubmissionSession;
import com.cj.scmconduit.server.session.ConduitSshDaemon;
import com.cj.scmconduit.server.session.ConduitState;
import com.cj.scmconduit.server.session.GitSessionPrepStrategy;
import com.cj.scmconduit.server.session.Pusher;
import com.cj.scmconduit.server.session.SessionPrepStrategy;
import com.cj.scmconduit.server.ssh.ShellCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.common.Session;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ConduitOrchestrator implements Pusher {
    public static final int FIVE_SECONDS = 5000;
    private final Log log = LogFactory.getLog(getClass());
    private final PrintStream out;
    private final URI publicUri;
    private final File pathOnDisk;
    private final List<ConduitOperation> requestQueue = new LinkedList<ConduitOperation>();
    private final ScmPump scmPump;
    private final CommandRunner shell;
    private final SessionPrepStrategy prepStrategy;
    private final Map<Integer, CodeSubmissionSession> pushes = new HashMap<Integer, CodeSubmissionSession>();
    private final TempDirAllocator temps;
    private final ConduitType type;
    private final String name;

    private ConduitState state = ConduitState.IDLE;
    private String error;

    public ConduitOrchestrator(String name, PrintStream out, URI publicUri, final Integer sshPort, final File pathOnDisk, TempDirAllocator temps, final Database database) {
        super();
        this.name = name;
        this.out = out;
        this.shell = new CommandRunnerImpl(out, out);
        this.publicUri = publicUri;
        this.pathOnDisk = pathOnDisk;
        this.temps = temps;

        if (new File(pathOnDisk, ".bzr").exists()) {
            type = ConduitType.BZR;
            prepStrategy = new BzrSessionPrepStrategy();
            scmPump = new BzrToP4Pump(pathOnDisk, shell, out);
        } else if (new File(pathOnDisk, ".git").exists()) {
            type = ConduitType.GIT;
            prepStrategy = new GitSessionPrepStrategy(name);
            scmPump = new GitToP4Pump(pathOnDisk, shell, out);
        } else {
            throw new RuntimeException("Not sure what kind of conduit this is: " + pathOnDisk);
        }

        final ConduitSshDaemon.SessionHandler sshSessionHandler = new ConduitSshDaemon.SessionHandler() {
            @Override
            public CodeSubmissionSession prepareSessionFor(P4Credentials credentials, Session session) {
                CodeSubmissionSession psession = newSession(credentials);
                log.debug("THE SESSION IS: " + session.getClass().getName());
                session.setAttribute(ShellCommand.LOCAL_PATH, psession.localPath());

                return psession;
            }
        };


        final Runnable doNothing = new Runnable() {
            public void run() {
            }

            ;
        };

        new ConduitSshDaemon(
                sshPort,
                sshSessionHandler,
                prepStrategy,
                doNothing,
                database.trustedKeysByUsername(),
                database.passwordsByUsername());
    }

    public ConduitType type() {
        return type;
    }

    public String error() {
        return error;
    }

    public void delete() {
        scmPump.delete();
    }

    public ConduitState state() {
        return state;
    }

    public int queueLength() {
        return requestQueue.size();
    }

    public int backlogSize() {
        return scmPump.backlogSize();
    }

    public Long currentP4Changelist() {
        return scmPump.currentP4Changelist();
    }

    public String p4Path() {
        return scmPump.p4Path();
    }

    public synchronized CodeSubmissionSession newSession(P4Credentials creds) {
        final Integer id = findAvailableId();
        out.println("id: " + id);

        File somethingThatShouldBeDeletedEventually = temps.newTempDir();
        CodeSubmissionSession session = new CodeSubmissionSession(name, id, publicUri, pathOnDisk, somethingThatShouldBeDeletedEventually, prepStrategy, shell, creds, this);
        pushes.put(session.id(), session);
        return session;
    }

    public synchronized void endSession(final CodeSubmissionSession removeMe) {
        removeMe.trash();
        new Thread(new Runnable() {
            @Override
            public void run() {
                temps.dispose(removeMe.localPath());
            }
        }).start();
    }

    private static final int MAX_PRIVATE_PUSH_ID = 65000;
    private static final int MIN_PRIVATE_PUSH_ID = 1000;

    private Integer findAvailableId() {
        Integer id = null;
        while (id == null || (pushes.get(id) != null && !pushes.get(id).isDone())) {
            id = new Random().nextInt(MAX_PRIVATE_PUSH_ID - MIN_PRIVATE_PUSH_ID) + MIN_PRIVATE_PUSH_ID;
        }
        return id;
    }

    public Map<Integer, CodeSubmissionSession> sessions() {
        return Collections.unmodifiableMap(pushes);
    }

    CodeSubmissionSession getSessionWithId(Integer sessionId) {
        return pushes.get(sessionId);
    }

    private interface ConduitOperation {
        void exec(ScmPump scmPump, PrintStream out);
    }

    private static class PushRequest implements ConduitOperation {
        final File location;
        final PushListener listener;
        final P4Credentials credentials;

        public PushRequest(File location, PushListener listener, P4Credentials credentials) {
            super();
            this.location = location;
            this.listener = listener;
            this.credentials = credentials;
        }

        public void exec(ScmPump scmPump, PrintStream out) {
            try {
                String source = location.getAbsolutePath();
                out.println("Pulling from " + source);

                boolean changesWerePulled = scmPump.pushChangesToPerforce(source, credentials);
                if (changesWerePulled) {
                    out.println("Committing");
                    scmPump.commit(credentials);
                    listener.pushSucceeded();
                } else {
                    listener.nothingToPush();
                }
            } catch (Exception e) {
                out.println("There was an error: " + e.getMessage());
                e.printStackTrace(System.out);

                List<Throwable> errors = new ArrayList<Throwable>();
                errors.add(e);

                try {
                    scmPump.rollback(credentials);
                } catch (Throwable e2) {
                    e2.printStackTrace();
                    errors.add(e2);
                }

                StringBuilder text = new StringBuilder("Error:");
                for (Throwable t : errors) {
                    text.append('\n');
                    text.append(stackTrace(t));
                }
                listener.pushFailed(text.toString());
                return;
            }

        }
    }

    public void submitPush(File location, final P4Credentials credentials, PushListener listener) {
        synchronized (requestQueue) {
            requestQueue.add(new PushRequest(location, listener, credentials));
            requestQueue.notifyAll();
        }
    }

    private ConduitOperation popNextRequest() {
        synchronized (requestQueue) {
            if (requestQueue.size() > 0) {
                ConduitOperation r = requestQueue.get(0);
                requestQueue.remove(0);
                return r;
            } else {
                return null;
            }
        }
    }

    private boolean keepRunning = true;
    private Thread t;

    public void start() {
        t = new Thread() {
            public void run() {
                while (keepRunning) {
                    try {
                        state = ConduitState.POLLING;
                        readFromPerforce();
                        ConduitOperation request = popNextRequest();
                        if (request != null) {
                            state = ConduitState.SENDING;
                            out.println("Handling request: " + request);
                            request.exec(scmPump, out);
                        } else {
                            out.println("Sleeping");
                            state = ConduitState.IDLE;
                            synchronized (requestQueue) {
                                requestQueue.wait(FIVE_SECONDS);
                            }
                            out.println("Waking-up");
                        }
                        error = null;

                    } catch (Exception e) {
                        error = stackTrace(e);
                        out.println("ERROR IN CONDUIT " + pathOnDisk + "\n" + error);
                        e.printStackTrace(out);
                        sleepMillis(FIVE_SECONDS);
                    }
                }
            }

            private void readFromPerforce() throws Exception {
                out.println("Pumping conduit " + pathOnDisk.getAbsolutePath());
                scmPump.pullChangesFromPerforce();
            }

            private void sleepMillis(long millis) {
                try {
                    Thread.sleep(millis);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        };
        t.start();
    }


    private static String stackTrace(Throwable t) {
        try {
            StringWriter s = new StringWriter();
            PrintWriter w = new PrintWriter(s);
            t.printStackTrace(w);
            s.flush();
            s.close();
            return s.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            keepRunning = false;
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
