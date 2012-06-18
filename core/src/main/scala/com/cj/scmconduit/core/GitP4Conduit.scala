package com.cj.scmconduit.core;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.cj.scmconduit.core.git.GitRevisionInfo;
import com.cj.scmconduit.core.git.GitStatus;
import com.cj.scmconduit.core.p4.P4Changelist;
import com.cj.scmconduit.core.p4.P4ClientId;
import com.cj.scmconduit.core.p4.P4Credentials;
import com.cj.scmconduit.core.p4.P4DepotAddress;
import com.cj.scmconduit.core.p4.P4Impl;
import com.cj.scmconduit.core.p4.P4;
import com.cj.scmconduit.core.p4.P4RevRangeSpec;
import com.cj.scmconduit.core.p4.P4RevSpec;
import com.cj.scmconduit.core.p4.P4SyncOutputParser.Change;
import com.cj.scmconduit.core.util.CommandRunner;

import scala.collection.JavaConversions._

class GitP4Conduit(private val conduitPath:File, private val shell:CommandRunner) extends Conduit {
//	private static final String TEMP_FILE_NAME=".scm-conduit-temp";
	private val META_FILE_NAME=".scm-conduit";

	private val p4:P4 = {
			  val state = readState();
			  new P4Impl(
						new P4DepotAddress(state.p4Port), 
						new P4ClientId(state.p4ClientId),
						state.p4ReadUser,
						conduitPath, 
						shell
				)
		  }

	override def commit(using:P4Credentials) {}
	
	override def rollback() {}
	
	override def push() {
		val p4TimeZoneOffset = findP4TimeZoneOffset();

		var keepPumping = true;
		while(keepPumping){
			val lastSync = findLastSyncRevision();
			val newStuff = findDepotChangesSince(lastSync);

			if(newStuff.isEmpty()){
				keepPumping = false;
			}else{
				val nextChange = newStuff.get(0);
				assertNoGitChanges();
				val changes = p4.syncTo(P4RevSpec.forChangelist(nextChange.id()));
				
				val gitCommands = new P42GitTranslator(conduitPath).translate(nextChange, changes, p4TimeZoneOffset);
				gitCommands.foreach{command=>
					runGit(command:_*);
				}
				
				assertNoGitChanges();
				recordLastSuccessfulSync(nextChange.id());
			}
		}
	}
	
	private def findP4TimeZoneOffset():Int = {
		return -7;
	}


	private def findDepotChangesSince(lastSync:Long):List[P4Changelist] = {
		return p4.changesBetween(P4RevRangeSpec.everythingAfter(lastSync));
	}

	private def recordLastSuccessfulSync(id:Long) {
		val state = readState();
		state.lastSyncedP4Changelist = id;
		writeState(state);
	}


	private def readState():ConduitState = {
		return ConduitState.read(new File(conduitPath, META_FILE_NAME));
	}

	private def writeState(state:ConduitState){
		ConduitState.write(state, new File(conduitPath, META_FILE_NAME));
	}

	private def findLastSyncRevision():Long =  {
		return readState().lastSyncedP4Changelist;
	}

	
	private def runGit(args:String*):String = {
		val a = new java.util.ArrayList[String](args);
		a.add(0, "--git-dir=" + new File(this.conduitPath, ".git").getAbsolutePath());
		a.add(0, "--work-tree=" + this.conduitPath.getAbsolutePath());
		return shell.run("git", a:_*);
	}

	
	private def getGitStatus():GitStatus = {
		return new GitStatus(runGit("status", "-s", "-uno").trim());
	}
	
	private def assertNoGitChanges(){
		val status = runGit("status", "-s", "-uno").trim();
 
		if(!getGitStatus().isUnchanged()){
			throw new RuntimeException("I was expecting there to be no local git changes, but I found some:\n" + status);
		}
	}
	
	override def pull(source:String, using:P4Credentials):Boolean = {
		val currentRev = runGit("log", "-1", "--format=%H").trim();
		runGit("remote", "add", "temp", source);
		runGit("fetch", "temp");
		System.out.println("Remotes are " + runGit("remote"));
		runGit("branch", "incoming", "temp/master");
		runGit("checkout", "incoming");
		val missing = runGit("cherry", "master");
		runGit("checkout", "master");
		System.out.println("Missing is " + missing);
		if(missing.isEmpty()){
			return false;
		}else{
			val lines = IOUtils.readLines(new StringReader(missing)).asInstanceOf[List[String]];
			
			lines.foreach{line=>
				System.out.println("Need to fetch " + line);
				val rev = line.replaceAll(Pattern.quote("+"), "").trim();
				runGit("merge", "incoming", rev);
				val log = runGit("log", "--name-status", currentRev + ".." + rev);
				System.out.println(log);
				
				val changes = new GitRevisionInfo(log);
				
				val changeListNum = new Translator(p4).translate(changes); 
				
				p4.doCommand("submit", "-c", changeListNum.toString());
			}
			
			return true;
		}
	}

}
