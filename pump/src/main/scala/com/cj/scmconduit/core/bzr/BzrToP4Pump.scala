package com.cj.scmconduit.core.bzr

import java.io.{BufferedReader, ByteArrayInputStream, File, FileReader, StringReader, FileWriter, IOException, PrintStream}
import java.util.regex.Pattern
import org.apache.commons.io.IOUtils
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils
import com.cj.scmconduit.core.p4.{P4, P4Impl, P4ClientId, P4Credentials, P4DepotAddress, P4RevRangeSpec, P4RevSpec, P4Time, ClientSpec, createDummyInitialP4Commit}
import com.cj.scmconduit.core.p4.P4SyncOutputParser.ChangeType
import com.cj.scmconduit.core.util.CommandRunner
import com.cj.scmconduit.core.RichFile._
import com.cj.scmconduit.core.PumpState
import com.cj.scmconduit.core.ScmPump
import javax.xml.bind.annotation.XmlRootElement

object BzrToP4Pump {

	def toBzrCommitDateFormat(when:P4Time, p4TimeZoneOffsetInHours:Int):String = {
		val p4TimeZoneOffsetInMinutes = p4TimeZoneOffsetInHours * 100;
				
		return "%02d-%02d-%02d %02d:%02d:%02d %+05d".format( 
				when.year(), when.monthOfYear(), when.dayOfMonth(), 
				when.hourOfDay(), when.minuteOfHour(), when.secondOfMinute(), 
				p4TimeZoneOffsetInMinutes
		);
	}
	
	def create(p4Address:P4DepotAddress, spec:ClientSpec, p4FirstCL:Integer, shell:CommandRunner, credentials:P4Credentials, out:PrintStream, observer:(ScmPump)=>Unit = {c=>}) {
			val p4:P4 = new P4Impl(
					p4Address, 
					new P4ClientId(spec.clientId),
					new P4Credentials("", ""),
					spec.localPath, 
					shell)
	  
			out.println(spec)
			val changes = p4.doCommand(new ByteArrayInputStream(spec.toString().getBytes()), "client", "-i")
			out.println(changes)
			
			shell.run("bzr", "init", spec.localPath.toString())
					
			// start conduit
			
			if(!spec.localPath.isDirectory) throw new Exception("No such directory: " + spec.localPath)
			
			val file = spec.localPath/".scm-conduit"
			file.createNewFile()
			file.write(
				<scm-conduit-state>
					<last-synced-p4-changelist>0</last-synced-p4-changelist>
					<p4-port>{p4Address}</p4-port>
					<p4-read-user>{spec.owner}</p4-read-user>
					<p4-client-id>{spec.clientId}</p4-client-id>
				</scm-conduit-state>
			    )
			    
           val conduit = new BzrToP4Pump(spec.localPath, shell, out)
           observer(conduit);
           createDummyInitialP4Commit(spec.localPath, p4, conduit)

	}
}

class BzrToP4Pump(private val conduitPath:File, private val shell:CommandRunner, private val out:PrintStream) extends ScmPump {
	private val TEMP_FILE_NAME = ".scm-conduit-temp"
	private val META_FILE_NAME = ".scm-conduit"
 
	
	private val p4Address:P4DepotAddress = new P4DepotAddress(state().p4Port)
	private val p4:P4 = {
		val s = state();
	    out.println(FileUtils.readFileToString(new File(conduitPath, META_FILE_NAME)))
	    out.println("The port is " + s.p4Port)
		  new P4Impl(
					p4Address, 
					new P4ClientId(s.p4ClientId),
					new P4Credentials(state.p4ReadUser, null),
					conduitPath, 
					shell)
	}

	override def backlogSize():Int = 0
	
	override def currentP4Changelist() = state().getLastSyncedP4Changelist();

	override def delete(){
		val s = state();
		p4.doCommand("workspace", "-d", s.getP4ClientId())
		try{
			FileUtils.deleteDirectory(conduitPath);
		}catch{
		  case _:Throwable => shell.run("rm", "-rf", conduitPath.getAbsolutePath())
		}
	}
		
	override def pullChangesFromPerforce() { 
		val p4TimeZoneOffset = findP4TimeZoneOffset();

		var keepPumping = true;
		while(keepPumping){
			val lastSync = findLastSyncRevision();
			val newStuff = findDepotChangesSince(lastSync);

			if(newStuff.isEmpty()){
				keepPumping = false;
			}else{
				val nextChange = newStuff.get(0);
				assertNoBzrChanges();
				val changes = p4.syncTo(P4RevSpec.forChangelist(nextChange.id()));
				
				val adds = changes.filter{next=>
				    val changeType = next.`type`
					changeType match {
					case ChangeType.ADD => true
					case ChangeType.DELETE => false
					case ChangeType.UPDATE => false
					case _=> throw new RuntimeException("Not sure what to do with " + changeType);
					}
				}
				
				if(adds.size()>0){
					val args = new java.util.ArrayList[String](adds.size()+1);
					args.add("add");
					
					adds.foreach{add=>
						val branchPath = add.workspacePath;
						assertIsConduitFile(branchPath);
						args.add(branchPath);
					}
					
					out.println("adding " + adds.size());
					shell.run("bzr", args:_*);
				}
				//runBzr("add");

				runBzr("commit", 
						"--author=" + nextChange.whoString(),
						"--commit-time=" + BzrToP4Pump.toBzrCommitDateFormat(nextChange.getWhen(), p4TimeZoneOffset),
						"--unchanged",
						"-m", "[P4 CHANGELIST " + nextChange.id() + "]\n" + nextChange.description());
				assertNoBzrChanges();
				recordLastSuccessfulSync(nextChange.id());
			}
		}
	}


	private def assertIsConduitFile(actual:String) {
			val expectation = this.conduitPath.getAbsolutePath();
			if(!actual.startsWith(expectation)){
				throw new RuntimeException("I was expecting " + actual + " to start with " + expectation);
			}
	}

	private def findP4TimeZoneOffset() = -7


	private def findDepotChangesSince(lastSync:Long) = p4.changesBetween(P4RevRangeSpec.everythingAfter(lastSync));

	private def recordLastSuccessfulSync(id:Long ) {
		val s = state();
		s.lastSyncedP4Changelist = id;
		writeState(s);
	}
	
	def p4Path():String = {
		val clientSpec = p4.doCommand("client", "-o")
		
		var path = ""
		var inViewSection = false;
		IOUtils.readLines(new StringReader(clientSpec)).asInstanceOf[java.util.List[String]].foreach{line=>
		  if(inViewSection){
		    path += line.trim().split(" ")(0).trim()
		  }else if(line.trim().startsWith("View:")){
		    inViewSection = true
		  }
		}
		
		path
	}

	private def state() = PumpState.read(new File(conduitPath, META_FILE_NAME));

	private def writeState(state:PumpState ){
		PumpState.write(state, new File(conduitPath, META_FILE_NAME));
	}

	private def findLastSyncRevision() = state().lastSyncedP4Changelist

	override def rollback(using:P4Credentials) {
		//		BzrStatus s = BzrStatus.read(runBzr("xmlstatus"));
		p4.doCommand("revert", "//...");
		runBzr("revert");

		val changelistNum = new Integer(new BufferedReader(new FileReader(tempFile())).readLine().trim());

		p4.doCommand("changelist", "-d", changelistNum.toString());

		if(!tempFile().delete())
			throw new IOException("Cannot delete file: " + TEMP_FILE_NAME);
	}
    
	private def tempFile() = {new File(conduitPath, TEMP_FILE_NAME)}
	
	override def forceSync(){
	    throw new Exception("NOT IMPLEMENTED")
	}
	
	override def commit(using:P4Credentials) {
		
		val tempFile = this.tempFile(); 
		if(!tempFile.exists())
			throw new RuntimeException("Cannot find" + tempFile.getAbsolutePath());

		val p4ChangelistId = java.lang.Long.parseLong(FileUtils.readFileToString(tempFile));
		
		val p4 = p4ForUser(using);
		
		p4.doCommand("submit", "-c", p4ChangelistId.toString());
		runBzr("commit", "-m", "Pushed to p4 as changelist " + p4ChangelistId);

		if(!tempFile.delete())
			throw new IOException("Cannot delete file: " + tempFile.getAbsolutePath());


		assertNoBzrChanges();
	}


	private def p4ForUser(using:P4Credentials):P4 = {
		val s = state();
		
		new P4Impl(
				new P4DepotAddress(s.p4Port), 
				new P4ClientId(s.p4ClientId),
				using,
				conduitPath, 
				shell);
	}
	
	def shelveADiff(changelistDescription:String, pathToDiff:File) {

		val tempFile = this.tempFile(); 
		if(!tempFile.exists())
			throw new RuntimeException("Cannot find" + tempFile.getAbsolutePath());

		val p4ChangelistId = java.lang.Long.parseLong(FileUtils.readFileToString(tempFile));
		
		shell.run("patch", "-d", this.conduitPath.getAbsolutePath(), "-p0", "-i", pathToDiff.getAbsolutePath());
		
		p4.doCommand("shelve", "-c", p4ChangelistId.toString());
		p4.doCommand("revert", "./...");
		runBzr("revert");
		
		if(!tempFile.delete())
			throw new IOException("Cannot delete file: " + tempFile.getAbsolutePath());

		assertNoBzrChanges();
	}
	

	private def shelve2Diff(string:String) {
		// p4 unshelve
		// bzr add
		// patch = `bzr diff`
		// revert()
		// stdout << patch
	}
	
	private def runBzr(args:String*):String = { 
		val a = new java.util.ArrayList[String](args);
		a.add("-D");
		a.add(this.conduitPath.getAbsolutePath());
		return shell.run("bzr", a:_*);
	}


	def assertNoBzrChanges(){
		val s = BzrStatus.read(runBzr("xmlstatus"));
 
		if(!s.isUnchanged()){
			throw new RuntimeException("I was expecting there to be no local bzr changes, but I found some.\n" + s.toString());
		}
	}

	override def pushChangesToPerforce(source:String, using:P4Credentials):Boolean = {

		if(!BzrStatus.read(runBzr("xmlstatus")).isUnchanged()){
			throw new RuntimeException("There are unsaved changes.  You need to roll back.");
		}

		shell.run("bzr", "merge", source, "-d", this.conduitPath.getAbsolutePath());

		
		sendPendingBzrChangesToPerforce(using)
	}
	
	private def sendPendingBzrChangesToPerforce(using:P4Credentials):Boolean = {
	  val s = BzrStatus.read(runBzr("xmlstatus"));

	  if(s.isUnchanged()){
			out.println("There are no new changes");
			return false;
		}else{
			val p4 = p4ForUser(using);
			val changeListNum = createP4ChangelistFromBzrStatus(s, p4);

			writeTempFile(changeListNum);
			
			return true;
		}
	}


	private def createP4ChangelistFromBzrStatus(s:BzrStatus, p4:P4):Int = {
		val message = createP4MessageFromBzrStatus(s);
		
		val changeListNum = createP4ChangelistWithMessage(message, p4);

		translateBzrStatusToP4Changelist(s, changeListNum, p4);
		return changeListNum;
	}
	
	private def writeTempFile(changeListNum:Int) {
		val f = new FileWriter(tempFile());
		f.write(Integer.toString(changeListNum));
		f.close();
	}


	private def createP4ChangelistWithMessage(message:String,  p4:P4):Int = {
		out.println("Changes:\n" + message);

		out.println("Creating changelist");

		val changelistText = p4.doCommand("changelist", "-o").replaceAll(Pattern.quote("<enter description here>"), message);

		val changeListNum = createChangelist(changelistText, p4);
		return changeListNum;
	}


	private def translateBzrStatusToP4Changelist(s:BzrStatus, changeListNum:Integer, p4:P4) {
		out.println("Processing changes");
		val filesMoved = new java.util.HashSet[String]();

		if(s.renames.numDirectories()>0){
			// TODO: Add a check here to make sure we're 'synced' with perforce?
			//   the reason is that you might be moving a directory into which someone else has places something
			//   since your last sync, and you probably want that to move as well.
			// REALLY, SUCH A CHECK SHOULD PROBABLY BE MANDATORY REGARDLESS
		    s.renames.directories.foreach{next=>
				out.println("[MOV DIR] " + next.oldPath + " --> " +  next.file);
				val pathOnDisk = new File(conduitPath, next.file);
				if(pathOnDisk.isDirectory()){
					recursiveMove(pathOnDisk, next.oldPath, next.file, changeListNum, filesMoved);
				}else {
					throw new RuntimeException("This shouldn't be a file.  Something has happened that I don't understand.");
				}
			}
		}

		out.println(s.renames.numFiles() + " file renames");
		s.renames.files.foreach{next=>
			out.println("[MOV] " + next.oldPath + " --> " +  next.file);
			if(!new File(next.file).isDirectory()){
				p4.doCommand("edit", "-c", changeListNum.toString(), "-k", next.oldPath);
				p4.doCommand("move", "-c", changeListNum.toString(), "-k", next.oldPath, next.file);
				filesMoved.add(next.file);
			}else{
				throw new RuntimeException("This shouldn't be a directory.  Something has happened that I don't understand.");
			}
		}

		s.additions.files.foreach{next=>
			out.println("[ADD] " + next.file);
			p4.doCommand("add", "-c", changeListNum.toString(), next.file);
		}
		s.deletions.files.foreach{next=>
			out.println("[DEL] " + next.file);
			p4.doCommand("delete", "-c", changeListNum.toString(), next.file);
		}

		s.modifications.files.foreach{next=>
			out.println("[MOD] " + next.file);
			if(filesMoved.contains(next.file)){
				// NO NEED TO DO ANYTHING ... WE'VE ALREADY OPENED THIS FILE FOR EDITS
			}else{
				p4.doCommand("edit", "-c", changeListNum.toString(), next.file);
			}
		}
		out.println("Your bzr changes have been saved to " + changeListNum);
	}


	private def createP4MessageFromBzrStatus(s:BzrStatus):String = {
			val changeLog = new StringBuilder();
			
			val messages = s.pendingMerges.map{next=>
				changeLog.append(next.message);
			}
			
			val almostDone = messages.reverse.mkString("\n")
			
			almostDone.toString().replaceAll(Pattern.quote("\n"), "\n	");
	}


	private def createChangelist(changelistText:String ,  p4:P4):Int = {
		
		val output = p4.doCommand(new ByteArrayInputStream(changelistText.getBytes()), "changelist", "-i");
		out.println(output);

		val txt = output
					.replaceAll(("created."), "")
					.replaceAll(("Change"), "")
					.trim();
		out.println("Txt: " + txt);
		val changeListNum = Integer.parseInt(txt);
		out.println("Found number " + changeListNum);
		changeListNum
	}


	private def recursiveMove(currentOnDiskLocation:File, oldRelPath:String, newRelPath:String , changeListNum:Integer, 
								filesMoved:java.util.Set[String]){
		currentOnDiskLocation.listFiles().foreach{childPath=>
			val childOldRelPath = oldRelPath + "/" + childPath.getName();
			val childNewRelPath = newRelPath + "/" + childPath.getName();

			if(!childPath.exists()){
				throw new RuntimeException(childPath.getAbsolutePath() + " doesn't exist!");
			}else if(childPath.isDirectory()){
				recursiveMove(childPath, childOldRelPath, childNewRelPath, changeListNum, filesMoved);
			}else{
				out.println("Moving child file at " + childPath.getAbsolutePath());
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
