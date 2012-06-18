package com.cj.scmconduit.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.jetty.HttpObjectsJettyHandler;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;

import scala.Tuple2;

import com.cj.scmconduit.core.BzrP4Conduit;
import com.cj.scmconduit.core.p4.ClientSpec;
import com.cj.scmconduit.core.p4.P4DepotAddress;
import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.core.util.CommandRunnerImpl;
import com.cj.scmconduit.server.conduit.ConduitController;
import com.cj.scmconduit.server.config.Config;
import com.cj.scmconduit.server.fs.TempDirAllocator;
import com.cj.scmconduit.server.jetty.ConduitHandler;
import com.cj.scmconduit.server.jetty.VFSResource;

public class ConduitServerMain {
	public static void main(String[] args) throws Exception {
		try {
			setupLogging();
			doBzrSafetyCheck();
			
			File path = new File(args[0]);
			String p4Address = args[1];
			String p4User = args[2];
			String publicHostname = args.length>3?args[3]:InetAddress.getLocalHost().getCanonicalHostName();
			
			Config config = new Config(publicHostname, path, p4Address, p4User);
			
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
		while(f!=null){
			File dotBzrDir = new File(f, ".bzr"); 
			if(dotBzrDir.exists()){
				throw new RuntimeException("Looks like we're being run from under bzr (" + f.getAbsolutePath() + ").  This can be dangerous.  Run me from somewhere else instead.");
			}
			f = f.getParentFile();
		}
	}
	
	private final File tempDirPath;
	private final File path;
	private final Server jetty;
	private final Map<String, String> credentials = new HashMap<String, String>();
	{
		credentials.put("someuser", "test");
	}
	
	public ConduitServerMain(final Config config) throws Exception {
		this.path = config.path;
		this.tempDirPath = new File(this.path, "tmp");
		if(!this.tempDirPath.exists() || !this.tempDirPath.isDirectory()){
			throw new IOException("Directory does not exist: " + this.tempDirPath);
		}
		
		final String basePublicUrl = "http://" + config.publicHostname + ":8034";
		System.out.println("My public url is " + basePublicUrl);
		
		final P4DepotAddress p4Address = new P4DepotAddress(config.p4Address);
		
		jetty = new Server(8034);
		ResourceHandler defaultHandler = new ResourceHandler();
		final VFSResource root = new VFSResource("/");
		defaultHandler.setBaseResource(root);
		defaultHandler.setWelcomeFiles(new String[]{"hi.txt"});
		final List<Handler> handlers = new ArrayList<Handler>();
		handlers.add(defaultHandler);
		
		final TempDirAllocator allocator = new TempDirAllocator() {
			private Set<File> allocatedPaths = new HashSet<File>();
			public File newTempDir() {
				try {
					File path = File.createTempFile("conduit-server", ".dir", tempDirPath);
					if(!path.delete() & !path.mkdirs()){
						throw new RuntimeException("Could not create directory at path " + tempDirPath.getAbsolutePath());
					}
					return path;
				} catch (IOException e) {
					throw new RuntimeException("Error creating temp dir at " + tempDirPath, e);
				}
			}
			public void dispose(File tempDir) {
				if(!allocatedPaths.contains(tempDir))
					throw new RuntimeException("This is not something I allocated");
				
				try {
					FileUtils.deleteDirectory(tempDir);
				} catch (IOException e) {
					throw new RuntimeException("Could not delete directory:" + tempDir.getAbsolutePath());
				}
			}
		};
		
		for(ConduitConfig conduit: findConduits()){
			ConduitHandler handler = prepareConduit(basePublicUrl, root, allocator, conduit);
			handlers.add(handler);
		}
		
		// Add shared bzr repository
		root.addVResource("/.bzr", new File(config.path, ".bzr"));
		
		handlers.add(new HttpObjectsJettyHandler(new HttpObject("/message"){
			@Override
			public Response get(Request req) {
				return OK(Html("<html><body>Hello, World!</body></html"));
			}
		},
		new AddConduitResource(new AddConduitResource.Listener() {
			
			@Override
			public void addConduit(String name, String p4Path) {
				final CommandRunner shell = new CommandRunnerImpl();
				File localPath = new File(config.basePathForNewConduits, name);
				if(localPath.exists()) throw new RuntimeException("There is already a conduit by that name");
				
				String clientId = config.clientIdPrefix + name;
				
				@SuppressWarnings("deprecation")
				scala.collection.immutable.List<Tuple2<String, String>> view = scala.collection.immutable.List.fromArray(new Tuple2[]{
						new Tuple2<String, String>(p4Path + "/...", "/...")
				});
				
				ClientSpec spec = new ClientSpec(
									localPath, 
									config.p4User, 
									clientId, 
									config.publicHostname, 
									view);
				
				BzrP4Conduit.create(p4Address, spec, shell);
				
				
				ConduitConfig conduit = new ConduitConfig("name", localPath);
				ConduitHandler handler = prepareConduit(basePublicUrl, root, allocator, conduit);
				jetty.addHandler(handler);
			}
		}
		)));
		
		jetty.setHandlers(handlers.toArray(new Handler[]{}));
		jetty.start();
		
	}

	
	class ConduitConfig {
		
		public final String hostingPath;
		
		public final File localPath;
		
		public ConduitConfig(String hostingPath, File localPath) {
			this.hostingPath = hostingPath;
			this.localPath = localPath;
		}
		
	}

	private List<ConduitConfig> findConduits(){
		List<ConduitConfig> conduits = new ArrayList<ConduitConfig>();
		
		File conduitsDir = new File(path, "conduits");
		
		if(!conduitsDir.isDirectory() && !conduitsDir.mkdirs()) throw new RuntimeException("Could not create directory at " + conduitsDir);
		
		for(File localPath : conduitsDir.listFiles()){
			if(localPath.isDirectory() && !localPath.getName().toLowerCase().endsWith(".bak")){
				String httpPath = "/" + localPath.getName().replaceAll(Pattern.quote("-"), "/");
				System.out.println("Autodiscovered " + httpPath);
				conduits.add(new ConduitConfig(httpPath, localPath));
			}
		}
		
		return conduits;
	}
	
	private ConduitHandler prepareConduit(final String basePublicUrl,
			VFSResource root, TempDirAllocator allocator, ConduitConfig conduit) {
		URI publicUri = URI(basePublicUrl + conduit.hostingPath);
		ConduitController controller = new ConduitController(publicUri, conduit.localPath, allocator);
		controller.start();
		
		ConduitHandler handler = new ConduitHandler(conduit.hostingPath, controller);
		
		// For basic read-only "GET" access
		root.addVResource(conduit.hostingPath, conduit.localPath);
		return handler;
	}
	
	URI URI(String uri){
		try {
			return new URI(uri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
