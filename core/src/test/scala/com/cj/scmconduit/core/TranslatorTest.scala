package com.cj.scmconduit.core
import org.junit.Test
import org.junit.Assert._
import com.cj.scmconduit.core.p4.P4
import com.cj.scmconduit.core.p4.P4RevRangeSpec
import com.cj.scmconduit.core.p4.P4RevSpec
import java.io.InputStream
import com.cj.scmconduit.core.git.GitRevisionInfo
import com.cj.scmconduit.core.git.GitRevisionInfo.{
  Change, ChangeType
}
import org.apache.commons.io.IOUtils

class TranslatorTest {

  @Test
  def translatesEdits(){
    // GIVEN
    val commandsIssued = new scala.collection.mutable.ListBuffer[List[String]]()
    val p4:P4 = new P4Stub(){
      
	  override def doCommand(in:InputStream, args:String*) = args.toList match {
		    case List("changelist", "-i")  => "Change 21 created."
	  }
      
	  override def doCommand(parts:String*) = parts.toList match {
	    case List("changelist", "-o") => "<enter description here>"
	    case _ => commandsIssued.+=(parts.toList);"<enter description here>"
	  }
	}
    val t = new Translator(p4) 
    
    val changes = new GitRevisionInfo(
			"jerry lee lewis",
			"2012-03-02 23:00 PDT",
			"xyz123",
			"I am legend",
			new Change(ChangeType.M, "test.txt")
    )
    
    // WHEN
    t.translate(changes)
    
    // THEN
    println(commandsIssued)
    assertEquals(1, commandsIssued.size)
    assertTrue(commandsIssued.contains(List("edit", "-c", "21", "test.txt")))
  }
    
  @Test
  def translatesAdds(){
    // GIVEN
    val commandsIssued = new scala.collection.mutable.ListBuffer[List[String]]()
    val p4:P4 = new P4Stub(){
      
	  override def doCommand(in:InputStream, args:String*) = args.toList match {
		    case List("changelist", "-i")  => "Change 21 created."
	  }
      
	  override def doCommand(parts:String*) = parts.toList match {
	    case List("changelist", "-o") => "<enter description here>"
	    case _ => commandsIssued.+=(parts.toList);"<enter description here>"
	  }
	}
    val t = new Translator(p4) 
    
    val changes = new GitRevisionInfo(
			"jerry lee lewis",
			"2012-03-02 23:00 PDT",
			"xyz123",
			"I am legend",
			new Change(ChangeType.A, "test.txt")
    )
    
    // WHEN
    t.translate(changes)
    
    // THEN
    println(commandsIssued)
    assertEquals(1, commandsIssued.size)
    assertTrue(commandsIssued.contains(List("add", "-c", "21", "test.txt")))
  }
  
    
  @Test
  def translatesDeletes(){
    // GIVEN
    val commandsIssued = new scala.collection.mutable.ListBuffer[List[String]]()
    val p4:P4 = new P4Stub(){
      
	  override def doCommand(in:InputStream, args:String*) = args.toList match {
		    case List("changelist", "-i")  => "Change 21 created."
	  }
      
	  override def doCommand(parts:String*) = parts.toList match {
	    case List("changelist", "-o") => "<enter description here>"
	    case _ => commandsIssued.+=(parts.toList);"<enter description here>"
	  }
	}
    val t = new Translator(p4) 
    
    val changes = new GitRevisionInfo(
			"jerry lee lewis",
			"2012-03-02 23:00 PDT",
			"xyz123",
			"I am legend",
			new Change(ChangeType.D, "test.txt")
    )
    
    // WHEN
    t.translate(changes)
    
    // THEN
    println(commandsIssued)
    assertEquals(1, commandsIssued.size)
    assertTrue(commandsIssued.contains(List("delete", "-c", "21", "test.txt")))
  }
  
    @Test
    def translatesMoves(){
    // GIVEN
    val commandsIssued = new scala.collection.mutable.ListBuffer[List[String]]()
    val p4:P4 = new P4Stub(){
      
	  override def doCommand(in:InputStream, args:String*) = args.toList match {
		    case List("changelist", "-i")  => "Change 21 created."
	  }
      
	  override def doCommand(parts:String*) = parts.toList match {
	    case List("changelist", "-o") => "<enter description here>"
	    case _ => commandsIssued.+=(parts.toList);"<enter description here>"
	  }
	}
    val t = new Translator(p4) 
    
    val change = new Change(ChangeType.R, "test.txt", "file.txt")
    
    val changes = new GitRevisionInfo(
			"jerry lee lewis",
			"2012-03-02 23:00 PDT",
			"xyz123",
			"I am legend",
			change
    )
    
    // WHEN
    t.translate(changes)
    
    // THEN
    println(commandsIssued)
    assertEquals(2, commandsIssued.size)
    assertEquals(List("edit", "-c", "21", "-k", "test.txt"), commandsIssued(0))
    assertEquals(List("move", "-c", "21", "-k", "test.txt", "file.txt"), commandsIssued(1))
  }
  
  @Test
  def translatesTheCommitMessage(){
    // GIVEN
    var changeDescription:String = null;
    val p4:P4 = new P4Stub(){
      
	  override def doCommand(in:InputStream, args:String*) = args.toList match {
		    case List("changelist", "-i")  => {
		    	changeDescription = IOUtils.toString(in) 
		    	"Change 21 created."
		    }
	  }
      
	  override def doCommand(parts:String*) = parts.toList match {
	    case List("changelist", "-o") => "<enter description here>"
	    case _ => ""
	  }
	}
    val t = new Translator(p4)
    
    val changes = new GitRevisionInfo(
			"jerry lee lewis",
			"2012-03-02 23:00 PDT",
			"xyz123",
			"I am legend"
    )
    
    // WHEN
    t.translate(changes)
    
    // THEN
    println("change: " + changeDescription)
    assertTrue("Should have translated the commit message", changeDescription.contains("I am legend"))
  }
  
  
  private class P4Stub extends P4 {
    
	override def changesBetween(range:P4RevRangeSpec) = null

	override def syncTo(rev:P4RevSpec) = null

	override def doCommand(parts:String*) = ""

	override def doCommand(in:InputStream, args:String*) = ""
  }
}