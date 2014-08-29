package com.cj.scmconduit.server;

import static org.httpobjects.jackson.JacksonDSL.JacksonJson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.httpobjects.HttpObject;
import org.httpobjects.Representation;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.jetty.HttpObjectsJettyHandler;
import org.httpobjects.util.ClasspathResourceObject;
import org.httpobjects.util.ClasspathResourcesObject;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;

import scala.Function1;
import scala.Tuple2;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

import com.cj.scmconduit.core.ScmPump;
import com.cj.scmconduit.core.bzr.BzrToP4Pump;
import com.cj.scmconduit.core.git.GitToP4Pump;
import com.cj.scmconduit.core.p4.ClientSpec;
import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.p4.P4DepotAddress;
import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.core.util.CommandRunnerImpl;
import com.cj.scmconduit.server.api.ConduitInfoDto;
import com.cj.scmconduit.server.api.ConduitType;
import com.cj.scmconduit.server.api.ConduitsDto;
import com.cj.scmconduit.server.api.UserSecrets;
import com.cj.scmconduit.server.data.Database;
import com.cj.scmconduit.server.data.FilesOnDiskKeyValueStore;
import com.cj.scmconduit.server.data.KeyValueStore;
import com.cj.scmconduit.server.fs.TempDirAllocator;
import com.cj.scmconduit.server.fs.TempDirAllocatorImpl;
import com.cj.scmconduit.server.jetty.VFSResource;
import com.cj.scmconduit.server.session.ConduitState;
import com.cj.scmconduit.server.ui.AddConduitResource;
import com.cj.scmconduit.server.util.SelfResettingFileOutputStream;

public class ConduitServerMain {

    public static final long FIVE_MEG = 1024 * 1024 * 5;
    public static final int LOW_PORT = 6000;
    public static final int HIGH_PORT_LIMIT = 7000;

    public static void main(String[] args) throws Exception {
        try {
            setupLogging();
            doBzrSafetyCheck();

            Config config = Config.fromArgs(args);

            new ConduitServerMain(config);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void setupLogging() {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    private static void doBzrSafetyCheck() {
        File f = new File(System.getProperty("user.dir"));
        while (f != null) {
            File dotBzrDir = new File(f, ".bzr");
            if (dotBzrDir.exists()) {
                throw new RuntimeException("Looks like we're being run from under bzr (" + f.getAbsolutePath() + ").  This can be dangerous.  Run me from somewhere else instead.");
            }
            f = f.getParentFile();
        }
    }

    private final File tempDirPath;
    private final File path;
    private final Server jetty;
    private final String basePublicUrl;
    private final P4DepotAddress p4Address;
    private final TempDirAllocator allocator;
    private final List<HostableConduit> conduits = new ArrayList<ConduitServerMain.HostableConduit>();
    private final List<ConduitCreationThread> creationThreads = new ArrayList<ConduitServerMain.ConduitCreationThread>();
    private final Log log;
    private final Config config;
    private final String publicHostname;
    private final Database database;

    public ConduitServerMain(final Config config) throws Exception {
        this.log = LogFactory.getLog(getClass());
        this.config = config;
        this.path = config.path;
        this.publicHostname = config.publicHostname;
        this.tempDirPath = new File(this.path, "tmp");

        this.database = new Database() {
            KeyValueStore keysByUsername = new FilesOnDiskKeyValueStore(new File(config.path, "keys"));
            KeyValueStore passwordsByUsername = new FilesOnDiskKeyValueStore(new File(config.path, "credentials"));

            @Override
            public KeyValueStore trustedKeysByUsername() {
                return keysByUsername;
            }

            @Override
            public KeyValueStore passwordsByUsername() {
                return passwordsByUsername;
            }
        };

        mkDirs(this.tempDirPath);

        basePublicUrl = "http://" + config.publicHostname + ":" + config.port;
        log.info("My public url is " + basePublicUrl);

        p4Address = new P4DepotAddress(config.p4Address);

        jetty = new Server(config.port);
        ResourceHandler defaultHandler = new ResourceHandler();
        final VFSResource root = new VFSResource("/");
        defaultHandler.setBaseResource(root);
        defaultHandler.setWelcomeFiles(new String[]{"hi.txt"});
        final List<Handler> handlers = new ArrayList<Handler>();
        handlers.add(defaultHandler);

        allocator = new TempDirAllocatorImpl(tempDirPath);

        for (ConduitDiscoveryResult conduit : findConduits()) {
            if (conduit.localPath.listFiles().length == 0) {
                FileUtils.deleteDirectory(conduit.localPath);
            } else {
                conduits.add(startConduit(basePublicUrl, root, allocator, conduit));
            }
        }

        // Add shared bzr repository
        root.addVResource("/.bzr", new File(config.path, ".bzr"));


        HttpObject addConduitPage = new AddConduitResource(new AddConduitResource.Listener() {
            @Override
            public void addConduit(final ConduitType type, final String name, final String p4Path, final Integer p4FirstCL) {
                ConduitCreationThread thread = new ConduitCreationThread(config, type, name, p4Path, p4FirstCL, root);
                creationThreads.add(thread);
                thread.start();
            }
        });

        HttpObject depotHandler = new HttpObject("/{conduitName}/{remainder*}", null) {

            public HostableConduit findConduitForPath(String conduitName) {
                HostableConduit foundConduit = null;
                for (HostableConduit conduit : conduits) {
                    final String next = conduit.httpResource.name;
                    log.debug("name: " + next);
                    if (conduitName.equals(next)) {
                        foundConduit = conduit;
                    }
                }
                return foundConduit;
            }

            public Response relay(Request req, final Method m) {
                String conduitName = "/" + req.pathVars().valueFor("conduitName");

                HostableConduit match = findConduitForPath(conduitName);
                if (match != null) {
                    return HttpObjectUtil.invokeMethod(match.httpResource, m, req);
                } else {
                    log.debug("There is no conduit at " + conduitName);
                    return null;
                }

            }

            @Override
            public Response get(Request req) {
                return relay(req, Method.GET);
            }

            @Override
            public Response post(Request req) {
                return relay(req, Method.POST);
            }
        };

        final HttpObject conduitLogResource = new HttpObject("/api/conduits/{conduitName}/log") {
            @Override
            public Response get(Request req) {
                final String conduitName = req.pathVars().valueFor("conduitName");
                final HostableConduit conduit = conduitNamed(conduitName);
                if (conduit == null) {
                    return NOT_FOUND();
                } else {
                    try {
                        final FileInputStream in = new FileInputStream(conduit.log.location);
                        return OK(Bytes("text/plain", in));
                    } catch (FileNotFoundException e) {
                        return INTERNAL_SERVER_ERROR(e);
                    }
                }
            }
        };


        final HttpObject conduitApiResource = new HttpObject("/api/conduits/{conduitName}") {


            @Override
            public Response get(Request req) {
                final String conduitName = req.pathVars().valueFor("conduitName");
                final HostableConduit conduit = conduitNamed(conduitName);
                if (conduit == null) {
                    return NOT_FOUND();
                } else {
                    return OK(JacksonJson(conduit.toDto()));
                }
            }

            @Override
            public Response delete(Request req) {
                final String conduitName = req.pathVars().valueFor("conduitName");
                final HostableConduit conduit = conduitNamed(conduitName);
                if (conduit == null) {
                    return NOT_FOUND();
                } else {
                    try {
                        log.info("Deleting " + conduitName);
                        conduits.remove(conduit);
                        conduit.orchestrator.stop();
                        conduit.orchestrator.delete();
                        return OK(Text("deleted"));
                    } catch (Exception e) {
                        return INTERNAL_SERVER_ERROR(e);
                    }

                }
            }

        };

        final HttpObject conduitsApiResource = new HttpObject("/api/conduits") {
            @Override
            public Response get(Request req) {
                ConduitsDto conduitsDto = new ConduitsDto();

                for (HostableConduit conduit : conduits) {
                    conduitsDto.conduits.add(conduit.toDto());
                }

                for (ConduitCreationThread next : creationThreads) {
                    ConduitInfoDto dto = new ConduitInfoDto();
                    dto.name = next.name;
                    if (next.pump == null) {
                        dto.status = ConduitState.STARTING;
                    } else {
                        dto.status = ConduitState.BUILDING;
                        dto.backlogSize = next.pump.backlogSize();
                        dto.currentP4Changelist = next.pump.currentP4Changelist();
                    }

                    conduitsDto.conduits.add(dto);
                }

                return OK(JacksonJson(conduitsDto));
            }
        };


        final HttpObject userDataApiResource = new HttpObject("/api/users/{user}/secrets") {
            @Override
            public Response put(Request req) {
                final String username = req.pathVars().valueFor("user");
                final UserSecrets secrets = readJackson(req.representation(), UserSecrets.class);

                database.passwordsByUsername().put(username, secrets.password);
                database.trustedKeysByUsername().put(username, secrets.key);

                return CREATED(Location(""));
            }
        };

        handlers.add(new HttpObjectsJettyHandler(
                new ClasspathResourceObject("/", "index.html", getClass()),
                new ClasspathResourceObject("/submit.py", "submit.py", getClass()),
                new ClasspathResourcesObject("/{resource*}", getClass()),
                addConduitPage,
                depotHandler,
                conduitsApiResource,
                conduitApiResource,
                userDataApiResource,
                conduitLogResource
        ));

        jetty.setHandlers(handlers.toArray(new Handler[handlers.size()]));
        jetty.start();

    }


    private static <T> T readJackson(Representation representation, Class<? extends T> clazz) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            representation.write(bytes);
            return new ObjectMapper().readValue(bytes.toByteArray(), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void mkDirs(File path) {
        if (!path.isDirectory() && !path.mkdirs()) {
            throw new RuntimeException("Directory does not exist and I could not create it: " + path.getAbsolutePath());
        }
    }

    private HostableConduit conduitNamed(String name) {
        for (HostableConduit next : conduits) {
            if (next.isNamed(name)) {
                return next;
            }
        }

        return null;
    }

    static class ConduitLog {
        final File location;
        final PrintStream stream;

        public ConduitLog(File location, PrintStream stream) {
            super();
            this.location = location;
            this.stream = stream;
        }
    }

    private ConduitLog logStreamForConduit(String name) {
        try {
            final File f = new File(this.config.path, name + ".log");
            log.info("Conduit log is at " + f.getAbsolutePath());
            if (!f.exists() && !f.createNewFile()) {
                throw new RuntimeException("Could not create file at " + f.getAbsolutePath());
            }

            final Long maxLogSizeInBytes = FIVE_MEG;
            final boolean autoFlush = true;
            final PrintStream out = new PrintStream(
                    new SelfResettingFileOutputStream(f, maxLogSizeInBytes),
                    autoFlush);

            return new ConduitLog(f, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class ConduitCreationThread extends Thread {
        final Config config;
        final ConduitType type;
        final String name;
        final String p4Path;
        final Integer p4FirstCL;
        final VFSResource root;
        ScmPump pump;

        private ConduitCreationThread(Config config, ConduitType type,
                String name, String p4Path, Integer p4FirstCL, VFSResource root) {
            super();
            this.config = config;
            this.type = type;
            this.name = name;
            this.p4Path = p4Path;
            this.p4FirstCL = p4FirstCL;
            this.root = root;
        }

        public void run() {
            PrintStream logStream = null;
            try {
                final ConduitLog conduitLog = logStreamForConduit(name);
                logStream = conduitLog.stream;
                final CommandRunner shell = new CommandRunnerImpl(logStream, logStream);
                File localPath = new File(config.basePathForNewConduits, name);
                log.info("I am going to create something at " + localPath);
                if (localPath.exists()) {
                    throw new RuntimeException("There is already a conduit called \"" + name + "\" " +
                            "(given that there is already a directory at " + localPath.getAbsolutePath() + ")");
                }

                String clientId = config.clientIdPrefix + name;

                String serverLocation = p4Path + "/...";
                String clientLocation = "/...";
                scala.collection.immutable.List<Tuple2<String, String>> view = scala.collection.immutable.List.<Tuple2<String,String>>fromArray(new Tuple2[]{
                        new Tuple2<String, String>(serverLocation, clientLocation)
                });

                ClientSpec spec = new ClientSpec(
                        localPath,
                        config.p4User,
                        clientId,
                        config.publicHostname,
                        view);

                P4Credentials credentials = new P4Credentials(config.p4User, "");

                Function1<ScmPump, BoxedUnit> observerFunction = new AbstractFunction1<ScmPump, BoxedUnit>() {
                    @Override
                    public BoxedUnit apply(ScmPump arg0) {
                        pump = arg0;
                        return BoxedUnit.UNIT;
                    }
                };

                if (type == ConduitType.GIT) {
                    GitToP4Pump.create(p4Address, spec, p4FirstCL, shell, credentials, logStream, observerFunction);
                } else if (type == ConduitType.BZR) {
                    BzrToP4Pump.create(p4Address, spec, p4FirstCL, shell, credentials, logStream, observerFunction);
                } else {
                    throw new RuntimeException("not sure how to create a \"" + type + "\" conduit");
                }

                ConduitDiscoveryResult conduit = new ConduitDiscoveryResult("/" + name, localPath);
                HostableConduit stuff = startConduit(basePublicUrl, root, allocator, conduit);
                conduits.add(stuff);
            } catch (Exception e) {
                e.printStackTrace(logStream == null ? System.out : logStream);
                log.error(e);
                if (pump != null) {
                    pump.delete();
                }
            } finally {
                creationThreads.remove(this);
            }
        }
    }

    class ConduitDiscoveryResult {
        public final String name;
        public final String hostingPath;
        public final File localPath;

        private ConduitDiscoveryResult(String hostingPath, File localPath) {
            super();
            this.hostingPath = hostingPath;
            this.localPath = localPath;
            this.name = localPath.getName();
        }

    }

    private List<ConduitDiscoveryResult> findConduits() {
        List<ConduitDiscoveryResult> conduits = new ArrayList<ConduitDiscoveryResult>();

        File conduitsDir = new File(path, "conduits");

        if (!conduitsDir.isDirectory() && !conduitsDir.mkdirs()) {
            throw new RuntimeException("Could not create directory at " + conduitsDir);
        }

        for (File localPath : conduitsDir.listFiles()) {
            if (localPath.isDirectory() && !localPath.getName().toLowerCase().endsWith(".bak")) {
                String httpPath = "/" + localPath.getName();//.replaceAll(Pattern.quote("-"), "/");

                conduits.add(new ConduitDiscoveryResult(httpPath, localPath));
            }
        }

        return conduits;
    }

    class HostableConduit {
        final String p4path;
        final String name;
        final String hostingPath;
        final File localPath;
        final ConduitHttpResource httpResource;
        final ConduitOrchestrator orchestrator;
        final ConduitLog log;

        private HostableConduit(ConduitDiscoveryResult config, ConduitHttpResource handler,
                ConduitOrchestrator controller, ConduitLog log) {
            super();
            this.name = config.name;
            this.hostingPath = config.hostingPath;
            this.localPath = config.localPath;
            this.httpResource = handler;
            this.orchestrator = controller;
            this.p4path = controller.p4Path();
            this.log = log;
        }


        public boolean isNamed(String name) {
            return httpResource.name.replaceAll(Pattern.quote("/"), "").equals(name);
        }

        ConduitInfoDto toDto() {
            final HostableConduit conduit = this;
            final ConduitInfoDto dto = new ConduitInfoDto();
            dto.readOnlyUrl = basePublicUrl + conduit.hostingPath + (new File(conduit.localPath, ".git").exists() ? "/.git" : "");
            dto.apiUrl = basePublicUrl + "/api/conduits" + conduit.hostingPath;
            dto.p4path = conduit.p4path;
            dto.name = conduit.localPath.getName();
            dto.queueLength = conduit.orchestrator.queueLength();
            dto.status = conduit.orchestrator.state();
            dto.backlogSize = conduit.orchestrator.backlogSize();
            dto.currentP4Changelist = conduit.orchestrator.currentP4Changelist();
            dto.error = conduit.orchestrator.error();
            dto.type = conduit.orchestrator.type();
            dto.logUrl = basePublicUrl + "/api/conduits" + conduit.hostingPath + "/log";
            dto.sshUrl = "ssh://" + publicHostname + ":" + sshPortForConduitNamed(conduit.name) + "/" + conduit.name;
            return dto;
        }
    }

    private synchronized Map<Integer, String> portAssignments() {
        try {
            final Map<Integer, String> result = new HashMap<Integer, String>();
            final File path = portAssignmentsFile();
            if (path.exists()) {
                final BufferedReader reader = new BufferedReader(new FileReader(path));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String[] parts = line.split(Pattern.quote("="));
                    result.put(Integer.parseInt(parts[0]), parts[1]);
                }
                reader.close();
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File portAssignmentsFile() {
        return new File(path, "ports.txt");
    }

    private synchronized Integer sshPortForConduitNamed(final String name) {
        Map<Integer, String> pas = portAssignments();

        for (Map.Entry<Integer, String> entry : pas.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }

        Integer port = LOW_PORT;
        while (port < HIGH_PORT_LIMIT) {
            if (!pas.containsKey(port)) {
                break;
            }
            port++;
        }
        if (port == HIGH_PORT_LIMIT) {
            throw new RuntimeException("No free ports!?");
        }

        log.info("Assigned port " + port + " to " + name);
        pas.put(port, name);

        try {
            Writer writer = new FileWriter(portAssignmentsFile());
            for (Map.Entry<Integer, String> entry : pas.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return port;
    }

    private HostableConduit startConduit(final String basePublicUrl, VFSResource root, TempDirAllocator allocator, ConduitDiscoveryResult conduit) {
        final URI publicUri = URI(basePublicUrl + conduit.hostingPath);
        final ConduitLog log = logStreamForConduit(conduit.localPath.getName());
        ConduitOrchestrator orchestrator = new ConduitOrchestrator(conduit.name, log.stream, publicUri, sshPortForConduitNamed(conduit.name), conduit.localPath, allocator, database);
        orchestrator.start();

        ConduitHttpResource httpResource = new ConduitHttpResource(conduit.hostingPath, orchestrator);

        // For basic read-only "GET" access
        this.log.info("Serving " + conduit.localPath + " at " + conduit.hostingPath);
        root.addVResource(conduit.hostingPath, conduit.localPath);
        return new HostableConduit(conduit, httpResource, orchestrator, log);
    }

    URI URI(String uri) {
        try {
            return new URI(uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
