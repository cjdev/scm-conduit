package com.cj.scmconduit.core.p4;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.cj.scmconduit.core.p4.P4SyncOutputParser.Change;
import com.cj.scmconduit.core.util.CommandRunner;

public class P4Impl implements P4 {
	private final CommandRunner shell;
	private final File workspace;
	private final P4DepotAddress depot;
	private final P4ClientId client;
	private final P4Credentials credentials;
	private final String P4_SHELL_COMMAND = "p4";
	
	public P4Impl(P4DepotAddress depot, P4ClientId client, P4Credentials cred, File workspace, CommandRunner shell) {
		super();
		this.depot = depot;
		this.client = client;
		this.credentials = cred;
		this.workspace = workspace;
		this.shell = shell;
	}	
	
	@Override
	public String doCommand(InputStream in, String ... args){
		return shell.runWithHiddenKey(in, P4_SHELL_COMMAND, credentials.password, withArgs(args));
	}
	
	@Override
	public String doCommand(String ... parts){
		return shell.runWithHiddenKey(P4_SHELL_COMMAND, credentials.password, withArgs(parts));
	}
	
	@SuppressWarnings("serial")
    @Override
    public List<Change> syncTo(final P4RevSpec rev, final boolean force){
	    
	    List<String> args = new ArrayList<String>(){{
	        add("sync");
	        if(force) add("-f");
	        add("..." + rev);
	    }};
	    
        String output = shell.runWithHiddenKey(P4_SHELL_COMMAND, credentials.password, withArgs(args.toArray(new String[]{})));
        P4SyncOutputParser parser = new P4SyncOutputParser();
        return parser.parse(new StringReader(output));
    }
	
	@Override
	public List<Change> syncTo(P4RevSpec rev){
		return syncTo(rev, false);
	}
	
	@Override
	public List<P4Changelist> changesBetween(P4RevRangeSpec range){
		String output = shell.runWithHiddenKey(P4_SHELL_COMMAND, credentials.password, withArgs("changelists", "-lt", "..." + range));
		
		List<P4Changelist> changes = new ArrayList<P4Changelist>(P4Util.parseChangesFromLongPlusTimeFormat(output));
		
		Collections.reverse(changes);
		
		return changes;
	}
	
	
	private String[] withArgs(String ... args){
		String[] baseArgs =
			{"-c", client.toString(), "-d", workspace.getAbsolutePath(), "-p", depot.toString(), "-u", credentials.user};

		List<String> allArgs = new LinkedList<String>(Arrays.asList(baseArgs));		
		if (credentials.password != null) {
			String[] passArgs = {"-P", credentials.password};
			allArgs.addAll(Arrays.asList(passArgs));
		}
		
		if(args!=null){
			allArgs.addAll(Arrays.asList(args));
		}
		
		return allArgs.toArray(new String[allArgs.size()]);
	}
}
