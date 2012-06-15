package com.cj.scmconduit.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.cj.scmconduit.core.bzr.BzrFile;
import com.cj.scmconduit.core.bzr.BzrStatus;
import com.cj.scmconduit.core.bzr.LogEntry;
import com.cj.scmconduit.core.git.GitFileStatus;
import com.cj.scmconduit.core.git.GitStatus;
import com.cj.scmconduit.core.p4.P4;
import com.cj.scmconduit.core.p4.P4Changelist;
import com.cj.scmconduit.core.p4.P4ClientId;
import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.p4.P4DepotAddress;
import com.cj.scmconduit.core.p4.P4RevRangeSpec;
import com.cj.scmconduit.core.p4.P4RevSpec;
import com.cj.scmconduit.core.p4.P4SyncOutputParser;
import com.cj.scmconduit.core.p4.P4SyncOutputParser.Change;
import com.cj.scmconduit.core.p4.P4SyncOutputParser.ChangeType;
import com.cj.scmconduit.core.p4.P4Time;
import com.cj.scmconduit.core.util.CommandRunner;
import com.cj.scmconduit.core.util.CommandRunnerImpl;

public class GitP4Conduit { 
	private static final String TEMP_FILE_NAME=".scm-conduit-temp";
	private static final String META_FILE_NAME=".scm-conduit";


	private final File conduitPath;
	private final P4 p4;
	private final CommandRunner shell;

	public GitP4Conduit(File conduitPath, CommandRunner shell) {
		super();
		this.conduitPath = conduitPath;
		this.shell = shell;
		
		ConduitState state = readState();
		
		this.p4 = new P4(
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
				
				List<Change> adds = new LinkedList<P4SyncOutputParser.Change>();
				
				for(Change next : changes){
					ChangeType type = next.type;
					
					switch(type){
					case ADD:
						adds.add(next);
						break;
					case DELETE:
//						runGit("rm", branchPath);
						break;
					case UPDATE:
//						runGit("add", branchPath);
						break;
					default: throw new RuntimeException("Not sure what to do with " + type);
					}
				}
				
				if(adds.size()>0){
					List<String> args = new ArrayList<String>(adds.size()+1);
					args.add("add");
					
					for(Change add : adds){
						
						final String branchPath = add.workspacePath;
						assertIsConduitFile(branchPath);
						args.add(branchPath);
					}
					
					System.out.println("adding " + adds.size());
					shell.run("bzr", args.toArray(new String[]{}));
				}
				//runGit("add");

				runGit("commit", 
						"--author=Joe Schmo <" + nextChange.whoString() + ">",
						"--date=" + toBzrCommitDateFormat(nextChange.getWhen(), p4TimeZoneOffset),
						"--allow-empty",
						"-m", "[P4 CHANGELIST " + nextChange.id() + "]\n" + nextChange.description());
				assertNoGitChanges();
				recordLastSuccessfulSync(nextChange.id());
			}
		}
	}

	
	private void assertIsConduitFile(String actual) {
			String expectation = this.conduitPath.getAbsolutePath();
			if(!actual.startsWith(expectation)){
				throw new RuntimeException("I was expecting " + actual + " to start with " + expectation);
			}
	}

	public static String toBzrCommitDateFormat(P4Time when, int p4TimeZoneOffsetInHours) {
		int p4TimeZoneOffsetInMinutes = p4TimeZoneOffsetInHours * 100;
		return String.format(
				"%02d-%02d-%02d %02d:%02d:%02d %+05d", 
				when.year(), when.monthOfYear(), when.dayOfMonth(), 
				when.hourOfDay(), when.minuteOfHour(), when.secondOfMinute(), 
				p4TimeZoneOffsetInMinutes
		);

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

	private File tempFile(){
		return new File(conduitPath, TEMP_FILE_NAME);
	}

	public void commit(P4Credentials using) throws Exception {

//		File tempFile = tempFile(); 
//		if(!tempFile.exists())
//			throw new RuntimeException("Cannot find" + tempFile.getAbsolutePath());
//
//		Long p4ChangelistId = new Long(FileUtils.readFileToString(tempFile));
//		
//		P4 p4 = p4ForUser(using);
//		
//		p4.doCommand("submit", "-c", p4ChangelistId.toString());
//		runGit("commit", "-m", "Pushed to p4 as changelist " + p4ChangelistId);
//
//		if(!tempFile.delete())
//			throw new IOException("Cannot delete file: " + tempFile.getAbsolutePath());
//
//
//		assertNoGitChanges();
	}


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
	
//	public void shelveADiff(String changelistDescription, File pathToDiff) throws Exception {
//
//		File tempFile = tempFile(); 
//		if(!tempFile.exists())
//			throw new RuntimeException("Cannot find" + tempFile.getAbsolutePath());
//
//		Long p4ChangelistId = new Long(FileUtils.readFileToString(tempFile));
//		
//		shell.run("patch", "-d", this.conduitPath.getAbsolutePath(), "-p0", "-i", pathToDiff.getAbsolutePath());
//		
//		p4.doCommand("shelve", "-c", p4ChangelistId.toString());
//		p4.doCommand("revert", "./...");
//		runGit("revert");
//		
//		if(!tempFile.delete())
//			throw new IOException("Cannot delete file: " + tempFile.getAbsolutePath());
//
//		assertNoGitChanges();
//	}
	

//	private void shelve2Diff(String string) {
//		 p4 unshelve
//		 bzr add
//		 patch = `bzr diff`
//		 revert()
//		 stdout << patch
//	}
	
	String runGit(String ... args){
		List<String> a = new ArrayList<String>(Arrays.asList(args));
//		a.add(0, "--exec-path=" + this.conduitPath.getAbsolutePath());
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
	private enum GitOp {
		A
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
//			runGit("remote", "rm", "temp");
			System.out.println("Missing is " + missing);
			if(missing.isEmpty()){
				return false;
			}else{
				List<String> lines = IOUtils.readLines(new StringReader(missing));
				for(String line : lines){
					System.out.println("Need to fetch " + line);
					String rev = line.replaceAll(Pattern.quote("+"), "").trim();
					runGit("merge", "incoming", rev);
					String log = runGit("log", "--name-status", currentRev + ".." + rev);
					log = stripStuff(log);
					System.out.println("Log is " + log);
					
					final Integer changeListNum = createP4ChangelistWithMessage("revision " + rev, p4);
					for(String changeLine : (List<String>) IOUtils.readLines(new StringReader(log))){ 
						GitOp gitOp = GitOp.valueOf(changeLine.substring(0, 1));
						String file = changeLine.substring(2);
						System.out.println("  NEXT: " + changeLine);
						System.out.println("   CHANGE: " + gitOp);
						System.out.println("     FILE: " + file);
//						changeLine.charAt(0)
						
						switch(gitOp){
						case A: {
							p4.doCommand("add", "-c", changeListNum.toString(), file);
							break;
						}
						default: throw new RuntimeException("I don't know how to handle " + gitOp);
						}
					}
					p4.doCommand("submit", "-c", changeListNum.toString());
				}
				
				return true;
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		
		

//		if(!getGitStatus().isUnchanged()){
//			throw new RuntimeException("There are unsaved changes.  You need to roll back.");
//		}
//		
//		String revBefore = runGit("log", "-1", "--format=%H");
//		System.out.println("Revision before was " + revBefore);
//
//		runGit("remote", "add", "temp", source);
//		runGit("fetch", "temp");
//		String missing = runGit("cherry", "remotes/temp/master");
//		runGit("remote", "rm", "temp");
//		String revAfter = runGit("log", "-1", "--format=%H");
//		System.out.println("missing " + missing);
//		if(revBefore.equals(revAfter)){
//			System.out.println("There are no new changes");
//			return false;
//		}else{
//			final P4 p4 = p4ForUser(using);
			
//			runGit("remote", "add", "temp", source);
			
			// for each pulled revision, submit to p4
			
//			final Integer changeListNum = createP4ChangelistFromBzrStatus(s, p4);
//
//			writeTempFile(changeListNum);
			
//			return true;
//			throw new RuntimeException("Not sure how to translate git commits to p4 yet");
//		}
	}
	
//	private Integer createP4ChangelistFromBzrStatus(final GitStatus s, final P4 p4) {
//		final String message = createP4MessageFromBzrStatus(s);
//		
//		final Integer changeListNum = createP4ChangelistWithMessage(message, p4);
//
//		translateBzrStatusToP4Changelist(s, changeListNum, p4);
//		return changeListNum;
//	}

	private String stripStuff(String log) {
		try{
			StringBuilder text = new StringBuilder();
			BufferedReader r = new BufferedReader(new StringReader(log));
			for(String line = r.readLine(); line!=null; line = r.readLine()){
				if(line.startsWith("commit ") || line.startsWith("Author") || line.startsWith("Date:") || line.trim().isEmpty() || line.startsWith("    ")){
					// do nothing
				}else{
					text.append(line);
					text.append("\n");
				}
			}
			return text.toString();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}


	private void writeTempFile(final Integer changeListNum) {
		try {
			FileWriter f = new FileWriter(tempFile());
			f.write(Integer.toString(changeListNum));
			f.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private Integer createP4ChangelistWithMessage(final String message,  final P4 p4) {
		System.out.println("Changes:\n" + message);

		System.out.println("Creating changelist");

		final String changelistText = p4.doCommand("changelist", "-o").replaceAll(Pattern.quote("<enter description here>"), message);

		final Integer changeListNum = createChangelist(changelistText, p4);
		return changeListNum;
	}


	private void translateBzrStatusToP4Changelist(BzrStatus s, final Integer changeListNum,  final P4 p4) {
		System.out.println("Processing changes");
		final Set<String> filesMoved = new HashSet<String>();

		if(s.renames.numDirectories()>0){
			// TODO: Add a check here to make sure we're 'synced' with perforce?
			//   the reason is that you might be moving a directory into which someone else has places something
			//   since your last sync, and you probably want that to move as well.
			// REALLY, SUCH A CHECK SHOULD PROBABLY BE MANDATORY REGARDLESS
			for(BzrFile next : s.renames.directories){
				System.out.println("[MOV DIR] " + next.oldPath + " --> " +  next.file);
				File pathOnDisk = new File(conduitPath, next.file);
				if(pathOnDisk.isDirectory()){
					recursiveMove(pathOnDisk, next.oldPath, next.file, changeListNum, filesMoved);
				}else {
					throw new RuntimeException("This shouldn't be a file.  Something has happened that I don't understand.");
				}
			}
		}

		System.out.println(s.renames.numFiles() + " file renames");
		for(BzrFile next : s.renames.files){
			System.out.println("[MOV] " + next.oldPath + " --> " +  next.file);
			if(!new File(next.file).isDirectory()){
				p4.doCommand("edit", "-c", changeListNum.toString(), "-k", next.oldPath);
				p4.doCommand("move", "-c", changeListNum.toString(), "-k", next.oldPath, next.file);
				filesMoved.add(next.file);
			}else{
				throw new RuntimeException("This shouldn't be a directory.  Something has happened that I don't understand.");
			}
		}

		for(BzrFile next : s.additions.files){
			System.out.println("[ADD] " + next.file);
			p4.doCommand("add", "-c", changeListNum.toString(), next.file);
		}
		for(BzrFile next : s.deletions.files){
			System.out.println("[DEL] " + next.file);
			p4.doCommand("delete", "-c", changeListNum.toString(), next.file);
		}

		for(BzrFile next : s.modifications.files){
			System.out.println("[MOD] " + next.file);
			if(filesMoved.contains(next.file)){
				// NO NEED TO DO ANYTHING ... WE'VE ALREADY OPENED THIS FILE FOR EDITS
			}else{
				p4.doCommand("edit", "-c", changeListNum.toString(), next.file);
			}
		}
		System.out.println("Your bzr changes have been saved to " + changeListNum);
	}


//	private String createP4MessageFromBzrStatus(BzrStatus s) {
//		String message;
//		{
//			StringBuilder changeLog = new StringBuilder();
//			for(int x=s.pendingMerges.size()-1;x>=0;x--){
//				if(x>0){
//					changeLog.append("\n");
//				}
//				LogEntry next = s.pendingMerges.get(x);
//				changeLog.append(next.message);
//			}
//			message = changeLog.toString().replaceAll(Pattern.quote("\n"), "\n	");
//		}
//		return message;
//	}


	private Integer createChangelist(final String changelistText,  final P4 p4) {
		final Integer changeListNum;
		
		final String output = p4.doCommand(new ByteArrayInputStream(changelistText.getBytes()), "changelist", "-i");
		System.out.println(output);

		try {
			String txt = output
			.replaceAll(("created."), "")
			.replaceAll(("Change"), "")
			.trim();
			System.out.println("Txt: " + txt);
			changeListNum = Integer.parseInt(txt);
			System.out.println("Found number " + changeListNum);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return changeListNum;
	}


	private void recursiveMove(File currentOnDiskLocation, String oldRelPath, String newRelPath, Integer changeListNum, Set<String> filesMoved){
		for(File childPath : currentOnDiskLocation.listFiles()){
			String childOldRelPath = oldRelPath + "/" + childPath.getName();
			String childNewRelPath = newRelPath + "/" + childPath.getName();

			if(!childPath.exists()){
				throw new RuntimeException(childPath.getAbsolutePath() + " doesn't exist!");
			}else if(childPath.isDirectory()){
				recursiveMove(childPath, childOldRelPath, childNewRelPath, changeListNum, filesMoved);
			}else{
				System.out.println("Moving child file at " + childPath.getAbsolutePath());
				p4.doCommand("edit", "-c", changeListNum.toString(), "-k", childOldRelPath);
				p4.doCommand("move", "-c", changeListNum.toString(), "-k", childOldRelPath, childNewRelPath);
				filesMoved.add(childNewRelPath);
			}
		}

		// Tell perforce to delete the directory (not that it really tracks them, but this at least keeps them from showing-up on other workspaces)
		if(new File(conduitPath, oldRelPath).exists()){
			p4.doCommand("delete", "-c", changeListNum.toString(), "-k", oldRelPath);
		}
	}

}
