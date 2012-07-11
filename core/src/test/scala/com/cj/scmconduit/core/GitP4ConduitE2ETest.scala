package com.cj.scmconduit.core

import java.io.{File => LocalPath, IOException, ByteArrayInputStream}
import com.cj.scmconduit.core.util.CommandRunner
import com.cj.scmconduit.core.util.CommandRunnerImpl
import scala.xml._
import com.cj.scmconduit.core.p4._
import com.cj.scmconduit.core.GitP4Conduit
import org.junit.{
  Test, Before
}
import RichFile._
import org.junit.Assert._
import com.cj.scmconduit.core.git.GitStatus
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils
import java.io.File

class GitP4ConduitE2ETest {
  
  @Before
  def safetyCheck(){
    
    var dir = new LocalPath(System.getProperty("user.dir"))
    
    while(dir!=null){
        var path = new LocalPath(dir, ".git")
    	System.out.println(path.getAbsolutePath())
    	if(path.exists()) println("WARNING:\n\nLooks like you are executing this test from a git-tracked directory (" + path + ") ... this can lead to bad things")
    	dir = dir.getParentFile()
    }
  }
  
//  
//  @Test
//  def failsCleanlyWhenUserAccountIsNotSetup(){
//	runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
//	  // GIVEN:
//	  
//	  // WHEN:
//	  
//	  // THEN:
//	  
//	}
//  }
  
  
	@Test
	def returnsFalseWhenThereIsNothingToPush() {
		runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
		  	
			// GIVEN: A new conduit connected to a depot with an empty history, and a git branch with one change
			val branch = tempPath("myclone")
			shell.run("git", "clone", spec.localPath, branch)
			
			val haveBefore = p4(spec, shell).doCommand("have")
			
			// when: the change is submitted to the conduit
			val changesFlowed = conduit.pull(branch, new P4Credentials("larry", ""))
			
			// then: 
			assertTrue("Nothing should have been pushed", !changesFlowed)
			
			// there should be no p4 opened files
			val openedFilesList = p4(spec, shell).doCommand("opened")
			assertEquals("There should be no opened p4 files", "", openedFilesList)
			
			// it should be on the next p4 CL
			val haveAfter = p4(spec, shell).doCommand("have")
			assertEquals("The have list for the client should not have changed", haveBefore, haveAfter)
			
			// there should be no git changes
			val localChanges = runGit(shell, branch, "status", "-s")
			assertEquals("", localChanges)
			
		}
	}
	
	
	@Test
	def conduitFailsCleanlyWhenIncomingGitChangesResultInMergeConflicts() {
		runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
		  // GIVEN:
		  
		  // 1) an existing conduit with some initial history
			val pathToSallysWorkspace = tempPath("sallysP4")
			
			val sallysSpec = createP4Workspace("sally", pathToSallysWorkspace, shell)
			
		  	val sallysADotTxt = pathToSallysWorkspace/"README.md" 
		  	
		  	sallysADotTxt.delete()
		  	sallysADotTxt.write("I am a tree")
			
		  p4(sallysSpec, shell).doCommand("edit", sallysADotTxt.getAbsolutePath())
		  p4(sallysSpec, shell).doCommand("submit", "-d", "declared my nature as a tree")
		  conduit.push();
		  
		  // 2) a git clone at that point in the history
		  
		  val branch = tempPath("myclone")
		  shell.run("git", "clone", spec.localPath.getAbsolutePath(), branch.getAbsolutePath())
		  
		  // 3) subsequent history in p4
		  p4(sallysSpec, shell).doCommand("edit", "README.md")
		  sallysADotTxt.write("I am a pine")
		  p4(sallysSpec, shell).doCommand("submit", "-d", "clarified my treeishness")
		  conduit.push();
		  
		  def currentRev() = runGit(shell, spec.localPath, "log", "-1", "--format=%H").trim();
		  
		  val headBefore = currentRev()
		  
		  // 4) conflicting local changes in the clone
		  
		  (branch/"README.md").write("I am a bee")
		  runGit(shell, branch, "add", "README.md")
		  runGit(shell, branch, "commit", "-m", "I think I'm a bee")
		  
		  // WHEN:
		  // the conduit is asked to push the conflicting history through to perforce
		  val error = try {
			  conduit.pull(branch, new P4Credentials("larry", ""))
			  null
		  }catch{
		    case e:Throwable=> {
		      conduit.rollback()
		      e
		    } 
		  }
		  
		  // THEN:
		  // The request should not succeed
		  assertNotNull(error)
		  //    there should be no perforce changes
		  val p4Changes = p4(sallysSpec, shell).doCommand("sync")
		  assertEquals("", p4Changes.trim())
		  
		  //    there should be no git changes
		  val filesChanged = new GitStatus(runGit(shell, spec.localPath, "status", "-s")).files().filter{!_.file.equals(".scm-conduit")}
		  assertEquals(0, filesChanged.size) 
		  
		  assertEquals(headBefore, currentRev())
		  
		  // The user should receive a useful error message
		  assertTrue(error.getMessage().contains("Merge conflict in README.md"));
		  // The conduit should be left in a working state (e.g. subsequent valid push requests from other conduits should work)
		}
	}
	
	@Test
	def usersCanPushToABlankDepotThroughANewConduit() {
		runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
		  	
			// GIVEN: A new conduit connected to a depot with an empty history, and a git branch with one change
			val myclone = tempPath("myclone")
			shell.run("git", "clone", spec.localPath, myclone)
			val newFile = myclone / "file.txt"
			newFile.write("hello world")
			runGit(shell, myclone, "add", ".")
			runGit(shell, myclone, "commit", "-m", "Added_file.txt")
			
			val haveBefore = p4(spec, shell).doCommand("have")
			
			// when: the change is submitted to the conduit
			val changesFlowed = conduit.pull(myclone, new P4Credentials("larry", ""))
			
			// then: 
			assertTrue("Some changes should make their way to perforce", changesFlowed)
			
			// there should be no p4 opened files
			val openedFilesList = p4(spec, shell).doCommand("opened")
			assertEquals("There should be no opened p4 files", "", openedFilesList)
			
			// it should be on the next p4 CL
			val haveAfter = p4(spec, shell).doCommand("have")
			println("Has: " + haveAfter)
			assertTrue("The have list for the client should have changed", haveBefore != haveAfter)
			assertTrue("The client should have the files", haveAfter.contains("//depot/file.txt#1"))
			
			// there should be no git changes
			val gitChanges = runGit(shell, myclone, "status", "-s")
			assertEquals("", gitChanges)
			
			println(runGit(shell, myclone, "fetch", "--tags")) // synchronize tags with remote git conduit
			val taggedRev = toString(new File(myclone, ".git/refs/tags/cl2")).trim()
		  	val currentRev = runGit(shell, myclone, "log", "-1", "--format=%H").trim()
		  	
		  	assertEquals(currentRev, taggedRev)
		}
	}
	
	private def toString(f:File) = {
	    if(f.exists){
	      FileUtils.readFileToString(f)
	    }else{
	      ""
	    }
	}
	
	@Test
	def usersCanPullASimpleP4AddToAGitBranch() {
		runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
		  
		  // GIVEN:
		  
		    
			val pathToSallysWorkspace = tempPath("sallysP4")
			
			val sallysSpec = createP4Workspace("sally", pathToSallysWorkspace, shell)
			
		  	val sallysADotTxt = pathToSallysWorkspace/"a.txt" 
		  	
		  	sallysADotTxt.write("hello world")
			
			p4(sallysSpec, shell).doCommand("add", sallysADotTxt.getAbsolutePath())
			p4(sallysSpec, shell).doCommand("submit", "-d", "added a.txt")
		  
			
			
			// when
			conduit.push()
			
			// then
			val branch = tempPath("myclone")
			shell.run("git", "clone", spec.localPath, branch)
			val localADotTxt = branch/"a.txt"
			assertTrue("the file should be in git", localADotTxt exists)
			assertEquals("hello world", localADotTxt.readString())
		}
	}
	
	
	
	private def createP4Workspace(userName:String, where:LocalPath, shell:CommandRunner) = {
		val userSpec = new UserSpec(id=userName, email=userName+"@host.com", fullName = userName)
		shell.run(new ByteArrayInputStream(userSpec.toString.getBytes()), "p4", "-p", "localhost:1666", "user", "-i", "-f")
		val sallysSpec = new ClientSpec(
				localPath = realDirectory(where),
				owner = userName,
				clientId = userName + "-client",
				host = shell.run("hostname"),
				view = List(
						("//depot/...", "/...")
						)
				)
		p4(sallysSpec, shell).doCommand(new ByteArrayInputStream(sallysSpec.toString().getBytes()), "client", "-i")
		p4(sallysSpec, shell).doCommand("sync")
		sallysSpec
	}
	
	def runE2eTest(test:(CommandRunner, ClientSpec, GitP4Conduit)=>Unit){
		val path = tempDir("conduit")
		
		val shell = new CommandRunnerImpl(System.out, System.err)
		
		// start p4 server
		val p4d = startP4dAndWaitUntilItsReady(realDirectory(path/"p4d"))
				  
		// turn off user autocreate
		shell.run("p4", "-p", "localhost:1666", "configure", "set", "dm.user.noautocreate=1");
		
		val userSpec = new UserSpec(id="larry", email="larry@host.com", fullName = "larry")
		println(userSpec)
		shell.run(new ByteArrayInputStream(userSpec.toString.getBytes()), "p4", "-p", "localhost:1666", "user", "-i", "-f")
		
		println("Started")
		
		try{
			// given: an existing conduit
			val spec = new ClientSpec(
					localPath = realDirectory(path/"larrys-workspace"),
					owner = "larry",
					clientId = "larrys-client",
					host = shell.run("hostname"),
					view = List(
					    ("//depot/...", "/...")
					)
			)
			
			val conduit = createGitConduit(spec, shell)
			
			test(shell, spec, conduit)
		} finally {
			p4d.destroy()
		}
	  
	}
  
	
	def startP4dAndWaitUntilItsReady(p4dDataDirectory:LocalPath) = {
		var text = ""

		var p = new ProcessBuilder("p4d", "-r", p4dDataDirectory.getAbsolutePath)
					.redirectErrorStream(true)
					.start()
		val in = p.getInputStream()
		val buffer = new Array[Byte](1024 * 1024)

		var keepReading = true
		do {
			var numRead = in.read(buffer)
			if(numRead == -1){
				throw new Exception("Looks like p4d didn't start correctly (exited with " + p.exitValue() + ")")
			}else{
			    val s = new String(buffer, 0, numRead)
			    print(s)
				text += (s)
			}
			if(text.toLowerCase().contains("error"))
				throw new Exception("Looks like p4d had an error starting up:\n" + text)
			if(text.length>200)
				throw new Exception("p4d is really chatty ... I'm assuming it isn't starting up correctly")
		}while(!text.contains("Perforce Server starting..."))

		p
	}
	
	def p4(spec:ClientSpec, shell:CommandRunner)= new P4Impl(new P4DepotAddress("localhost:1666"), new P4ClientId(spec.clientId), new P4Credentials(spec.owner, ""), spec.localPath, shell);
	
	def runGit(shell:CommandRunner, dir:LocalPath, args:String*) = {
	    val gitDir = new LocalPath(dir, ".git") 
	    val boilerplate = List("--git-dir=" + gitDir.getAbsolutePath(), "--work-tree=" + dir.getAbsolutePath())
		shell.run("git", (boilerplate ::: args.toList):_*);
	}
	
	def  createGitConduit(spec:ClientSpec, shell:CommandRunner) = {
		
		GitP4Conduit.create(new P4DepotAddress("localhost:1666"), spec, 0, shell, new P4Credentials(spec.owner, null), System.out)
  
		println("Starting conduit")
		val conduit = new GitP4Conduit(spec.localPath, shell, System.out)
		conduit.push()
		println("Started conduit")
		conduit
	}
	
	

}