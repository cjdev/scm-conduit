package com.cj.scmconduit.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.cj.scmconduit.core.git.GitRevisionInfo;
import com.cj.scmconduit.core.git.GitStatus;
import com.cj.scmconduit.core.p4.P4Changelist;
import com.cj.scmconduit.core.p4.P4ClientId;
import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.p4.P4DepotAddress;
import com.cj.scmconduit.core.p4.P4Impl;
import com.cj.scmconduit.core.p4.P4RevRangeSpec;
import com.cj.scmconduit.core.p4.P4RevSpec;
import com.cj.scmconduit.core.p4.P4SyncOutputParser.Change;
import com.cj.scmconduit.core.util.CommandRunner;

public class GitP4Conduit {
//	private static final String TEMP_FILE_NAME=".scm-conduit-temp";
	private static final String META_FILE_NAME=".scm-conduit";

	private final File conduitPath;
	private final P4Impl p4;
	private final CommandRunner shell;

	public GitP4Conduit(File conduitPath, CommandRunner shell) {
		super();
		this.conduitPath = conduitPath;
		this.shell = shell;
		
		ConduitState state = readState();
		
		this.p4 = new P4Impl(
				new P4DepotAddress(state.p4Port), 
				new P4ClientId(state.p4ClientId),
				state.p4ReadUser,
				conduitPath, 
				shell
		);
	}


	public void p42git() throws Exception {
		final int p4TimeZoneOffset = findP4TimeZoneOffset();

		boolean keepPumping = true;
		while(keepPumping){
			long lastSync = findLastSyncRevision();
			List<P4Changelist> newStuff = findDepotChangesSince(lastSync);

			if(newStuff.isEmpty()){
				keepPumping = false;
			}else{
				P4Changelist nextChange = newStuff.get(0);
				assertNoGitChanges();
				List<Change> changes = p4.syncTo(P4RevSpec.forChangelist(nextChange.id()));
				
				List<List<String>> gitCommands = new P42GitTranslator(conduitPath).translate(nextChange, changes, p4TimeZoneOffset);
				for(List<String> command: gitCommands){
					runGit(command.toArray(new String[]{}));
				}
				assertNoGitChanges();
				recordLastSuccessfulSync(nextChange.id());
			}
		}
	}
	
	private int findP4TimeZoneOffset() {
		return -7;
	}


	private List<P4Changelist> findDepotChangesSince(long lastSync) {
		return p4.changesBetween(P4RevRangeSpec.everythingAfter(lastSync));
	}

	private void recordLastSuccessfulSync(long id) {
		ConduitState state = readState();
		state.lastSyncedP4Changelist = id;
		writeState(state);
	}


	private ConduitState readState(){
		return ConduitState.read(new File(conduitPath, META_FILE_NAME));
	}

	private void writeState(ConduitState state){
		ConduitState.write(state, new File(conduitPath, META_FILE_NAME));
	}

	private long findLastSyncRevision() {
		ConduitState state = readState();
		return state.lastSyncedP4Changelist;
	}

//	public void rollback() throws Exception {
//		//		BzrStatus s = BzrStatus.read(runGit("xmlstatus"));
//		p4.doCommand("revert", "//...");
//		runGit("revert");
//
//		Integer changelistNum = new Integer(new BufferedReader(new FileReader(tempFile())).readLine().trim());
//
//		p4.doCommand("changelist", "-d", changelistNum.toString());
//
//		if(!tempFile().delete())
//			throw new IOException("Cannot delete file: " + TEMP_FILE_NAME);
//	}

//	private P4 p4ForUser(P4Credentials using) {
//		ConduitState state = readState();
//		
//		P4 p4 = new P4(
//				new P4DepotAddress(state.p4Port), 
//				new P4ClientId(state.p4ClientId),
//				using.user,
//				conduitPath, 
//				shell);
//		return p4;
//	}
	
	String runGit(String ... args){
		List<String> a = new ArrayList<String>(Arrays.asList(args));
		a.add(0, "--git-dir=" + new File(this.conduitPath, ".git").getAbsolutePath());
		a.add(0, "--work-tree=" + this.conduitPath.getAbsolutePath());
		return shell.run("git", a.toArray(new String[]{}));
	}

	
	private GitStatus getGitStatus(){
		return new GitStatus(runGit("status", "-s", "-uno").trim());
	}
	
	private void assertNoGitChanges(){
		String status = runGit("status", "-s", "-uno").trim();
 
		if(!getGitStatus().isUnchanged()){
			throw new RuntimeException("I was expecting there to be no local git changes, but I found some:\n" + status);
		}
	}
	
	public boolean pull(String source, P4Credentials using){
		try{
			String currentRev = runGit("log", "-1", "--format=%H").trim();
			runGit("remote", "add", "temp", source);
			runGit("fetch", "temp");
			System.out.println("Remotes are " + runGit("remote"));
			runGit("branch", "incoming", "temp/master");
			runGit("checkout", "incoming");
			final String missing = runGit("cherry", "master");
			runGit("checkout", "master");
			System.out.println("Missing is " + missing);
			if(missing.isEmpty()){
				return false;
			}else{
				@SuppressWarnings("unchecked")
				List<String> lines = IOUtils.readLines(new StringReader(missing));
				for(String line : lines){
					System.out.println("Need to fetch " + line);
					String rev = line.replaceAll(Pattern.quote("+"), "").trim();
					runGit("merge", "incoming", rev);
					String log = runGit("log", "--name-status", currentRev + ".." + rev);
					System.out.println(log);
					
					GitRevisionInfo changes = new GitRevisionInfo(log);
					
					final Integer changeListNum = new Translator(p4).translate(changes); 
					
					p4.doCommand("submit", "-c", changeListNum.toString());
				}
				
				return true;
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

//	private void writeTempFile(final Integer changeListNum) {
//		try {
//			FileWriter f = new FileWriter(tempFile());
//			f.write(Integer.toString(changeListNum));
//			f.close();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	private void recursiveMove(File currentOnDiskLocation, String oldRelPath, String newRelPath, Integer changeListNum, Set<String> filesMoved){
//		for(File childPath : currentOnDiskLocation.listFiles()){
//			String childOldRelPath = oldRelPath + "/" + childPath.getName();
//			String childNewRelPath = newRelPath + "/" + childPath.getName();
//
//			if(!childPath.exists()){
//				throw new RuntimeException(childPath.getAbsolutePath() + " doesn't exist!");
//			}else if(childPath.isDirectory()){
//				recursiveMove(childPath, childOldRelPath, childNewRelPath, changeListNum, filesMoved);
//			}else{
//				System.out.println("Moving child file at " + childPath.getAbsolutePath());
//				p4.doCommand("edit", "-c", changeListNum.toString(), "-k", childOldRelPath);
//				p4.doCommand("move", "-c", changeListNum.toString(), "-k", childOldRelPath, childNewRelPath);
//				filesMoved.add(childNewRelPath);
//			}
//		}
//
//		// Tell perforce to delete the directory (not that it really tracks them, but this at least keeps them from showing-up on other workspaces)
//		if(new File(conduitPath, oldRelPath).exists()){
//			p4.doCommand("delete", "-c", changeListNum.toString(), "-k", oldRelPath);
//		}
//	}

}
