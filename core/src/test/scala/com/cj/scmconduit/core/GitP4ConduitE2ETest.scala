package com.cj.scmconduit.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.{File => LocalPath}
import java.io.File
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils
import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.util.FilesystemResourcesObject
import org.junit.Assert._
import org.junit.Before
import org.junit.Test
import com.cj.scmconduit.core.git.GitStatus
import com.cj.scmconduit.core.p4._
import com.cj.scmconduit.core.util.CommandRunner
import com.cj.scmconduit.core.util.CommandRunnerImpl
import RichFile._
import java.io.OutputStream
import java.io.PrintStream
import java.util.regex.Pattern
import java.io.InputStream

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
 
  @Test
  def gracefullyHandlesRacesWithOtherPerforceUsers(){
    runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
        // GIVEN:
      
        {// An existing file in perforce
            val pathToWorkspace = tempPath("initp4")
            
            val iSpec = createP4Workspace("MrSetup", pathToWorkspace, shell)
            
            val readmeDotMd = pathToWorkspace/"README.md" 
            
            readmeDotMd.delete()
            readmeDotMd.write("Coming soon...")
            
            p4(iSpec, shell).doCommand("edit", readmeDotMd.getAbsolutePath())
            p4(iSpec, shell).doCommand("submit", "-d", "Initial submit")
            conduit.push()
        }
          
        {// A perforce user's submission
            val pathToSallysWorkspace = tempPath("sallysP4")
            
            val sallysSpec = createP4Workspace("sally", pathToSallysWorkspace, shell)
            
            val sallyDotTxt = pathToSallysWorkspace/"sally.txt" 
            
            sallyDotTxt.delete()
            sallyDotTxt.write("I am mankind")
            
            p4(sallysSpec, shell).doCommand("add", sallyDotTxt.getAbsolutePath())
            p4(sallysSpec, shell).doCommand("submit", "-d", "Added a file wherein I have declared my kind")
        }
            
        {// A git user's conduit submission
            val pathToFredsClone = tempPath("fredsP4")
            shell.run("git", "clone", spec.localPath.getAbsolutePath(), pathToFredsClone.getAbsolutePath())
            
            val readmeDotMd = pathToFredsClone/"README.md" 
            
            readmeDotMd.write("Welcome to the project!")
            runGit(shell, pathToFredsClone, "add", "--all")
            runGit(shell, pathToFredsClone, "commit", "-m", "Fleshed-out the REAME.md a little")
            
            conduit.pull(pathToFredsClone, new P4Credentials("larry", ""))
        }
        
        // WHEN:
        conduit.push()
        
        // THEN:
          
        {// the git user's changes should be there
          val pathToReadme = spec.localPath/"README.md";
          assertEquals("Welcome to the project!", pathToReadme.readString)
        }
        {// the p4 user's changes should be there
          val pathToSallyDotTxt = spec.localPath/"sally.txt" ;
          assertEquals("I am mankind", pathToSallyDotTxt.readString)
        }

    }
  }
  
    @Test
    def accomodatesPerforceChangelistsThatConsistsExcluseivelyOfFilesThatHaveNotChanged() {
        runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
            
            // GIVEN: A new conduit connected to a depot with an empty history, and a git branch with one change
            {// An existing file in perforce
                val pathToWorkspace = tempPath("initp4")
                
                val iSpec = createP4Workspace("MrSetup", pathToWorkspace, shell)
                
                val readmeDotMd = pathToWorkspace/"README.md" 
                
                readmeDotMd.delete()
                readmeDotMd.write("Coming soon...")
                
                p4(iSpec, shell).doCommand("edit", readmeDotMd.getAbsolutePath())
                p4(iSpec, shell).doCommand("submit", "-d", "Initial submit")
                conduit.push()
            }
              
            
            {// A changelist consisting of 1 file for which there are no changes
                val pathToWorkspace = tempPath("sallysP4")
                
                val sallysSpec = createP4Workspace("sally", pathToWorkspace, shell)
                
                val readmeDotMd = pathToWorkspace/"README.md" 
                
                p4(sallysSpec, shell).doCommand("edit", readmeDotMd.getAbsolutePath())
                p4(sallysSpec, shell).doCommand("submit", "-d", "Looks like I changed this but I actually did nothing!")
                conduit.push()
            }
            
            
            // WHEN:
            val maybeErr = try{
                    conduit.push()
                    None
                }catch {case t:Throwable=>Some(t)}
            
            // THEN
            assertEquals(None, maybeErr)//"The conduit should not blow up", err)
        }
    }
  
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
	def conduitDoesNotSpitOutPasswordsInLog() {
		val baos = new ByteArrayOutputStream()
		val cmdRunner = new CommandRunnerImpl(new PrintStream(baos), new PrintStream(baos))
		
  		runE2eTestWithCustomCommandRunner(cmdRunner, {(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
	  		//GIVEN some p4 workspace history
  		  	val pathToSallysWorkspace = tempPath("sallysP4")
	  		val sallysSpec = createP4Workspace("sally", pathToSallysWorkspace, shell)
	  		val sallysADotTxt = pathToSallysWorkspace/"README.md" 
	
	  		sallysADotTxt.delete()
	  		sallysADotTxt.write("I am a tree")
	
	  		p4(sallysSpec, shell).doCommand("edit", sallysADotTxt.getAbsolutePath())
	  		p4(sallysSpec, shell).doCommand("submit", "-d", "declared my nature as a tree")
	  		conduit.push();
	  		
  		  	//WHEN branch it in git, then add some stuff
  		  	val branch = tempPath("myclone")
  			shell.run("git", "clone", spec.localPath.getAbsolutePath(), branch.getAbsolutePath())
  			
  			(branch/"README.md").write("I am a bee")
		    runGit(shell, branch, "add", "README.md")
		    runGit(shell, branch, "commit", "-m", "I think I'm a bee")
  		})
  		
  		//THEN the password is not printed in the log file
  		assertFalse(baos.toString().contains("PASSWORD_YOU_SHOULDNT_SEE"))
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
		  
		  val larrysCredentials = new P4Credentials("larry", "")
		  
		  // WHEN:
		  // the conduit is asked to push the conflicting history through to perforce
		  val error = try {
			  conduit.pull(branch, larrysCredentials)
			  null
		  }catch{
		    case e:Throwable=> {
		      conduit.rollback(larrysCredentials)
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
    def aGitPushThatContainsMultipleNewCommitsShouldTranslateToMultipleP4Changelists() {
        runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
            
            // GIVEN: A new conduit connected to a depot with an empty history, and a git branch with one change
            val myclone = tempPath("myclone");
            shell.run("git", "clone", spec.localPath, myclone)
            
            {
                val newFile = myclone / "a.txt"
                newFile.write("hello world")
                runGit(shell, myclone, "add", "--all")
                runGit(shell, myclone, "commit", "-m", "Added a.txt")
            }
            
            {
                val newFile = myclone / "b.txt"
                newFile.write("hello world")
                runGit(shell, myclone, "add", "--all")
                runGit(shell, myclone, "commit", "-m", "Added b.txt")
            }
            
            
            {
                val existingFile = myclone / "a.txt"
                existingFile.write("yo yo yo")
                runGit(shell, myclone, "add", "--all")
                runGit(shell, myclone, "commit", "-m", "Edited a.txt")
            }
            
            
            {
                val existingFile = myclone / "b.txt"
                existingFile.write("yo yo yo")
                runGit(shell, myclone, "add", "--all")
                runGit(shell, myclone, "commit", "-m", "Edited b.txt")
            }
            // when: the change is submitted to the conduit
            val changesFlowed = conduit.pull(myclone, new P4Credentials("larry", ""))
            
            // then: 
            assertTrue("Some changes should make their way to perforce", changesFlowed)
            
            // there should be no p4 opened files
            val openedFilesList = p4(spec, shell).doCommand("opened")
            assertEquals("There should be no opened p4 files", "", openedFilesList)
            
            val changelists = p4(spec, shell).changesBetween(P4RevRangeSpec.everythingAfter(1));
            println("CHANGELISTS: " + changelists.map(_.description()).toList.mkString("\n"))
            assertEquals(4, changelists.size());
            
            {
              val cl = changelists(0)
              assertEquals("Added a.txt", cl.description)
              val changes = p4(spec, shell).doCommand("describe", "-du", cl.id().toString)
//              assertEquals("whatever", changes)
              println(changes)
            }
            
            {
              val cl = changelists(1)
              assertEquals("Added b.txt", cl.description)
              val changes = p4(spec, shell).doCommand("describe", "-du", cl.id().toString)
//              assertEquals("whatever", changes)
              println(changes)
            }
             {
              val cl = changelists(2)
              assertEquals("Edited a.txt", cl.description)
              val changes = new P4DescribeOutputParser().parse(p4(spec, shell).doCommand("describe", cl.id().toString))
              assertEquals(1, changes.size())
              val change = changes.get(0)
              assertEquals("//depot/a.txt", change.depotPath);
              assertEquals(2, change.fileVersion.intValue());
              println(changes)
            }
             {
              val cl = changelists(3)
              assertEquals("Edited b.txt", cl.description)
              val changes = new P4DescribeOutputParser().parse(p4(spec, shell).doCommand("describe", "-s", cl.id().toString))
              assertEquals(1, changes.size())
              println(changes)
            }
//            changelists.foreach {changelist=>
//            }
//            
//            // it should be on the next p4 CL
//            val haveAfter = p4(spec, shell).doCommand("have")
//            println("Has: " + haveAfter)
//            assertTrue("The have list for the client should have changed", haveBefore != haveAfter)
//            assertTrue("The client should have the files", haveAfter.contains("//depot/file.txt#1"))
//            
//            // there should be no git changes
//            val gitChanges = runGit(shell, myclone, "status", "-s")
//            assertEquals("", gitChanges)
//            
//            println(runGit(shell, myclone, "fetch", "--tags")) // synchronize tags with remote git conduit
//            val taggedRev = toString(new File(myclone, ".git/refs/tags/cl2")).trim()
//            val currentRev = runGit(shell, myclone, "log", "-1", "--format=%H").trim()
//            
//            assertEquals(currentRev, taggedRev)
        }
    }
    class MockShell (commandOutput:String) extends CommandRunner {
        val commands = new scala.collection.mutable.ListBuffer[String]()
        
        private def record(command:String, args:String*):String = {
          val t = command :: args.toList
          
          commands.add(t.mkString(" "))
          commandOutput
        }
        override def run(command:String , args:String*):String = record(command, args:_*)
        override def run(in:InputStream, command:String , args:String*):String = record(command, args:_*)
        override def runWithHiddenKey(command:String , hideKey: String, args:String*):String = record(command, args:_*)
        override def runWithHiddenKey(in:InputStream, hideKey: String, command:String , args:String*):String = record(command, args:_*)
    }
    
    @Test
    def checksForPendingChangelistsBeforeDoingAnythingElse() {
        runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
            
            
            // given
            
            val myclone = tempPath("myclone");
            shell.run("git", "clone", spec.localPath, myclone)
            val mockShell = new MockShell("output-from-command")
            
            val conduit = new GitP4Conduit(spec.localPath, mockShell, System.out)
            
            // when
            val errorThrown = try{
              conduit.pull(myclone, new P4Credentials("larry", ""))
              None
            }catch{
              case e:Throwable => Some(e)
            }
            
            // then
            assertTrue(errorThrown.isDefined)
            assertEquals("p4 -c larrys-client -d " + spec.localPath.getAbsolutePath() + " -p localhost:1666 -u larry changes -c "+ spec.clientId +" -s pending", mockShell.commands.mkString("\n"))
            assertEquals("There are pending changelists:\noutput-from-command", errorThrown.get.getMessage());
        }
    }
	
    @Test
    def rollbackShouldRemoveTheUsersPendingChangelistAndRollbackTheirFiles() {
        runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:GitP4Conduit) =>
            
            val aDotTxt = spec.localPath/"a.txt" 
            
            aDotTxt.write("hello world")
            
            def createChangelist(changelistText:String, p4:P4):Integer = {
                val output = p4.doCommand(new ByteArrayInputStream(changelistText.getBytes()), "changelist", "-i");
                System.out.println(output);
        
                val txt = output
                            .replaceAll(("created."), "")
                            .replaceAll(("Change"), "")
                            .trim();
                System.out.println("Txt: " + txt);
                
                val changeListNum = Integer.parseInt(txt);
                System.out.println("Found number " + changeListNum);
                
                return changeListNum;
            }
            
            def createP4ChangelistWithMessage(message:String,  p4:P4):Integer = {
                System.out.println("Changes:\n" + message);
        
                System.out.println("Creating changelist");
        
                val changelistText = p4.doCommand("changelist", "-o").replaceAll(Pattern.quote("<enter description here>"), message);
        
                val changeListNum = createChangelist(changelistText, p4);
                return changeListNum;
            }
            createUser(new UserSpec(id="bob", email="bob@host.com", fullName = "Robert"), shell)
            val credentials = new P4Credentials("bob", "");
            val p4AsBob = p4(spec, shell, credentials) 
            
            val changelist = createP4ChangelistWithMessage("a pending changelist", p4AsBob)
            
            p4AsBob.doCommand("add", aDotTxt.getAbsolutePath(), "-c" + changelist)
            
            // when
            conduit.rollback(credentials)
            
            // then
            
            val pendingChangelists = p4(spec, shell).doCommand("changes", "-s", "pending")
            assertEquals("", pendingChangelists)
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
			{
    			val branch = tempPath("myclone")
                shell.run("git", "clone", spec.localPath, branch)
                val localADotTxt = branch/"a.txt"
                assertTrue("the file should be in git", localADotTxt exists)
                assertEquals("hello world", localADotTxt.readString())
              
			}
			
			
			{// the data should be accesible to a 'dumb' server
			  val jetty = HttpObjectsJettyHandler.launchServer(8080, new FilesystemResourcesObject("/{resource*}", spec.localPath));
			  
			  try{
//			    Thread.sleep(1000*80)
			    
			    val branch = tempPath("myclone")
                shell.run("git", "clone", "http://localhost:8080/.git/", branch)
                val localADotTxt = branch/"a.txt"
                assertTrue("the file should be in git", localADotTxt exists)
                assertEquals("hello world", localADotTxt.readString())
			    
			  }finally{
			    jetty.stop();
			  }
			}
		}
	}
	
	private def createUser(userSpec:UserSpec, shell:CommandRunner) {
        shell.run(new ByteArrayInputStream(userSpec.toString.getBytes()), "p4", "-p", "localhost:1666", "user", "-i", "-f")
	}
	
	private def createP4Workspace(userName:String, where:LocalPath, shell:CommandRunner) = {
		val userSpec = new UserSpec(id=userName, email=userName+"@host.com", fullName = userName)
		createUser(userSpec, shell)//shell.run(new ByteArrayInputStream(userSpec.toString.getBytes()), "p4", "-p", "localhost:1666", "user", "-i", "-f")
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
	
	def runE2eTestWithCustomCommandRunner(shell:CommandRunner, test:(CommandRunner, ClientSpec, GitP4Conduit)=> Unit) {
		val path = tempDir("conduit")
		
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
	
	def runE2eTest(test:(CommandRunner, ClientSpec, GitP4Conduit)=>Unit) {
		runE2eTestWithCustomCommandRunner(new CommandRunnerImpl(System.out, System.err), test)
	}
  
	def startP4dAndWaitUntilItsReady(p4dDataDirectory:LocalPath) = {
		var text = ""

		var process = new ProcessBuilder("p4d", "-r", p4dDataDirectory.getAbsolutePath)
					.redirectErrorStream(true)
		var env = process.environment()
		env.put("P4PORT", "localhost:1666")
		
		var p = process.start()
		
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
	
	def p4(spec:ClientSpec, shell:CommandRunner, maybeCredentials:P4Credentials = null) = {
	  val credentials = if(maybeCredentials==null){
	    new P4Credentials(spec.owner, "PASSWORD_YOU_SHOULDNT_SEE")
	  }else{
	    maybeCredentials
	  }
	    new P4Impl(new P4DepotAddress("localhost:1666"), new P4ClientId(spec.clientId), credentials, spec.localPath, shell); 
	}
	
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