package com.cj.scmconduit.core
import org.junit.Test
import RichFile._
import com.cj.scmconduit.core.p4.P4SyncOutputParser.{Change, ChangeType}
import com.cj.scmconduit.core.p4.P4Changelist
import com.cj.scmconduit.core.p4.P4Time
import scala.collection.JavaConversions._
import org.junit.Assert._

class P42GitTranslatorTest {
	
  @Test
  def translatesASimpleAdd = {
    // GIVEN
    val where = tempPath("sallysP4")
    val testSubject = new P42GitTranslator(where);
    val cl = new P4Changelist(23, "bought a dog", "sally", new P4Time(2012, 04, 24, 11, 32, 323))
    val changes = List[Change](
    		new Change(ChangeType.ADD, "fido.dog", where/"fido.dog" getAbsolutePath, 0)
    )
    val p4TimeZoneOffset:Int = 5
    
    // WHEN
    val result = testSubject.translate(cl, changes, p4TimeZoneOffset)
    
    // THEN
    assertEquals(List("add", "-f", "fido.dog"),result.get(0).toList)
    assertEquals(List("commit", "--author=sally <sally>", "--date=2012-04-24 11:32:323 +0500", "-m", "bought a dog\n\n[lives in perforce as CL#23]"),result.get(1).toList)
    assertEquals(List("tag", "cl23"), result.get(2).toList)
  }
  
  @Test
  def translatesASimpleDelete = {
    // GIVEN
    val where = tempPath("sallysP4")
    val testSubject = new P42GitTranslator(where);
    val cl = new P4Changelist(23, "ate the dog", "sally", new P4Time(2012, 04, 24, 11, 32, 323))
    val changes = List[Change](
    		new Change(ChangeType.DELETE, "dog.txt", where/"dog.txt" getAbsolutePath, 0)
    )
    val p4TimeZoneOffset:Int = 5
    
    // WHEN
    val result = testSubject.translate(cl, changes, p4TimeZoneOffset)
    
    // THEN
    assertEquals(List("rm", "dog.txt"),result.get(0).toList)
    assertEquals(List("commit", "--author=sally <sally>", "--date=2012-04-24 11:32:323 +0500", "-m", "ate the dog\n\n[lives in perforce as CL#23]"),result.get(1).toList)
    assertEquals(List("tag", "cl23"), result.get(2).toList)
  }
  
  @Test
  def translatesASimpleModification = {
    // GIVEN
    val where = tempPath("sallysP4")
    val testSubject = new P42GitTranslator(where);
    val cl = new P4Changelist(23, "surgically implanted a nuclear power source in the dog", "sally", new P4Time(2012, 04, 24, 11, 32, 323))
    val changes = List[Change](
    		new Change(ChangeType.UPDATE, "dog.txt", where/"dog.txt" getAbsolutePath, 0)
    )
    val p4TimeZoneOffset:Int = 5
    
    // WHEN
    val result = testSubject.translate(cl, changes, p4TimeZoneOffset)
    
    // THEN
    assertEquals(List("add", "dog.txt"),result.get(0).toList)
    assertEquals(List("commit", "--author=sally <sally>", "--date=2012-04-24 11:32:323 +0500", "-m", "surgically implanted a nuclear power source in the dog\n\n[lives in perforce as CL#23]"),result.get(1).toList)  
    assertEquals(List("tag", "cl23"), result.get(2).toList)
  }
  
}