package com.cj.scmconduit.core

import java.io.{File => LocalPath, IOException, ByteArrayInputStream}
import com.cj.scmconduit.core.util.CommandRunner
import com.cj.scmconduit.core.util.CommandRunnerImpl
import scala.xml._
import com.cj.scmconduit.core.p4._
import com.cj.scmconduit.core.bzr._
import org.junit.Test
import RichFile._
import org.junit.Assert._

class BzrP4ConduitE2ETest {
  
	def runE2eTest(test:(CommandRunner, ClientSpec, BzrP4Conduit)=>Unit){
		val path = tempDir()
		
		val shell = new CommandRunnerImpl
		
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
			
			val conduit = createConduit(spec, shell)
			
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
	
	def p4(spec:ClientSpec, shell:CommandRunner)= new P4Impl(new P4DepotAddress("localhost:1666"), new P4ClientId(spec.clientId), new P4Credentials(spec.owner, null), spec.localPath, shell);
	
	def createConduit(spec:ClientSpec, shell:CommandRunner) = {
//			println(spec)
//			val changes = p4(spec, shell).doCommand(new ByteArrayInputStream(spec.toString().getBytes()), "client", "-i")
//			println(changes)
//			
//			shell.run("bzr", "init", spec.localPath)
//					
//			// start conduit
//			
//			spec.localPath/".scm-conduit" write(
//				<scm-conduit-state>
//					<last-synced-p4-changelist>0</last-synced-p4-changelist>
//					<p4-port>localhost:1666</p4-port>
//					<p4-read-user>{spec.owner}</p4-read-user>
//					<p4-client-id>{spec.clientId}</p4-client-id>
//				</scm-conduit-state>
//			    )
			
			if(!spec.localPath.exists && !spec.localPath.mkdirs()) throw new Exception("Could not create directory: " + spec.localPath)
	  
			BzrP4Conduit.create(new P4DepotAddress("localhost:1666"), spec, 0, shell)
			
			var pathToDirHintFile = spec.localPath/".p4-directory" 
			pathToDirHintFile.write("this file tells perforce that this is a directory")
			p4(spec, shell).doCommand("add", pathToDirHintFile)
			p4(spec, shell).doCommand("submit", "-d", "initial commit")
			
			val conduit = new BzrP4Conduit(spec.localPath, shell)
			conduit.push()
			conduit
	}
	
	@Test
	def usersCanPushToABlankDepotThroughANewConduit() {
		runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:BzrP4Conduit) =>
		  	
			// GIVEN: A new conduit connected to a depot with an empty history, and a bzr branch with one change
			val branch = tempPath()
			shell.run("bzr", "clone",  spec.localPath, branch)
			val newFile = branch / "file.txt"
			newFile.write("hello world")
			shell.run("bzr", "add", "-D", branch)
			shell.run("bzr", "commit", "-m", "Added_file.txt", "-D", branch)
			
			val haveBefore = p4(spec, shell).doCommand("have")
			
			// when: the change is submitted to the conduit
			val credentials = new P4Credentials("larry", "")
			val changesFlowed = conduit.pull(branch, credentials)
			conduit.commit(credentials);
			
			// then: 
			//   1) The changes make their way through to perforce
			assertTrue("Some changes should make their way to perforce", changesFlowed)
			
			//   2) The pull workspace should be in the correct state
			
			// there should be no p4 opened files
			val openedFilesList = p4(spec, shell).doCommand("opened")
			assertEquals("There should be no opened p4 files", "", openedFilesList)
			
			// it should be on the next p4 CL
			val haveAfter = p4(spec, shell).doCommand("have")
			println("Has: " + haveAfter)
			assertTrue("The have list for the client should have changed", haveBefore != haveAfter)
			assertTrue("The client should have the files", haveAfter.contains("//depot/file.txt#1"))
			
			// there should be no bzr changes
			val bzrChanges = shell.run("bzr", "status", "-D", branch)
			assertEquals("", bzrChanges)
			
			// Once I fix the issue that causes all the extra commits to show-up in the history, we can uncomment this:
//			// it should be on the same bzr revision it was before the attempt
//			val bzrInfo = shell.run("bzr", "revno", "-D", branch)
//			assertEquals("1", bzrInfo.trim())
		}
	}
	
	
//	@Test
//	def usersCantPushWithBadCredentials() {
//		runE2eTest{(shell:CommandRunner, spec:ClientSpec, conduit:ScmConduit) =>
//		  	
//			// GIVEN: A new conduit connected to a depot with an empty history, and a bzr branch with one change
//			val branch = tempPath()
//			shell.run("bzr", "branch",  spec.localPath, branch)
//			val newFile = branch / "file.txt"
//			newFile.write("hello world")
//			shell.run("bzr", "add", "-D", branch)
//			shell.run("bzr", "commit", "-m", "Added_file.txt", "-D", branch)
//			
//			// when: the change is submitted to the conduit using the wrong username
//			val changesFlowed =
//			try{
//			  conduit.pull(branch, new P4Credentials("bob", ""))
//			} catch {
//			  case _=> false// expected
//			}
//			
//			// then: 
//			assertFalse("the conduit should have rejected the request", changesFlowed)
//			
//			//   2) The pull workspace should be in the correct state
//			
//			// there should be no p4 opened files
//			// it should be on the same p4 cl it was before the attempt
//			
//			// there should be no bzr changes
//			// it should be on the same bzr revision it was before the attempt
//			
//		}
//	}
	
//	dir("files",
//			    file("a.txt", "joeff in a"),
//			    file("b.txt", "joeff in b"),
//			    dir("details", 
//			    	file("larry.txt", "joeff in larry")
//			    )
//			)	
//	def dir(name:String, nodes:FSNode*) = new Directory(name, nodes:_*)
//	def file(name:String, contents:String) = new File(name, contents)
//	
//	class FSNode(name:String)
//	class Directory(name:String, nodes:FSNode*) extends FSNode(name)
//	class File(name:String, contents:String) extends FSNode(name)
	
//	class UserSpec(val id:String, val email:String, val fullName:String){
//	  override def toString = """User: """ + id + """
//Email: """ + email + """
//FullName: """ + fullName
//	}
//	
//	class ClientSpec(val localPath:LocalPath, val owner:String, val clientId:String, val host:String, val view:List[Tuple2[String, String]]){
//		  override def toString = """
//Client:  """ + clientId + """
//Owner:  """ + owner + """
//Host:    """ + host + """
//Root:   """ + localPath.getAbsolutePath() + """
//Options:        noallwrite noclobber nocompress unlocked nomodtime normdir
//SubmitOptions:  submitunchanged
//LineEnd:        local
//View:""" + view.map(mapping=> "\n   " + mapping._1 + " " + "//" + clientId + mapping._2).mkString("")
//		}
	
}