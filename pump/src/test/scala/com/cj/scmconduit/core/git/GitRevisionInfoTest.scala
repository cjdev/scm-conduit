package com.cj.scmconduit.core.git
import org.junit.Test
import org.junit.Assert._
import com.cj.scmconduit.core.git.GitRevisionInfo.{
  Change, ChangeType
}

class GitRevisionInfoTest {
  
	@Test
	def parsesRevisionWithOneAddAndAOneLineMessage(){
      // given
	  val input = """
	                |commit 50a4bc6916ff49247626c0668e8dace436092c33
					|Author: Stu Penrose <stu@penrose.us>
					|Date:   Thu Jun 14 19:25:49 2012 -0700
					|
					|    Added_file.txt
					|
					|A	file.txt""".trim.stripMargin
      // when
	  val result = new GitRevisionInfo(input)
	  
	  // then
	  assertEquals("Stu Penrose <stu@penrose.us>", result.author)
	  assertEquals("50a4bc6916ff49247626c0668e8dace436092c33", result.commit)
	  assertEquals("Thu Jun 14 19:25:49 2012 -0700", result.date)
	  assertEquals("Added_file.txt", result.message)
	  assertEquals(1, result.changes.size())
	  val change = result.changes.get(0)
	  assertEquals("file.txt", change.path)
	  assertEquals(ChangeType.A, change.kind)
	}
	
	@Test
	def parsesRevisionWithOneAddOneDeleteAndOneRename(){
      // given
	  val input = """
					|commit 2ebf229d300917763969f24de6d34a3df93ce486
					|Author: Stu Penrose <stu@penrose.us>
					|Date:   Thu Jun 14 20:07:31 2012 -0700
					|
					|    Added, deleted and modified a file
					|
					|D       a.txt
					|M       b.txt
					|A       c.txt""".trim.stripMargin
      // when
	  val result = new GitRevisionInfo(input)
	  
	  // then
	  assertEquals("Stu Penrose <stu@penrose.us>", result.author)
	  assertEquals("2ebf229d300917763969f24de6d34a3df93ce486", result.commit)
	  assertEquals("Thu Jun 14 20:07:31 2012 -0700", result.date)
	  assertEquals("Added, deleted and modified a file", result.message)
	  assertEquals(3, result.changes.size())
	  
	  assertChangeEquals(ChangeType.D, "a.txt", result.changes.get(0))
	  assertChangeEquals(ChangeType.M, "b.txt", result.changes.get(1))
	  assertChangeEquals(ChangeType.A, "c.txt", result.changes.get(2))
	}	
	
	@Test
	def parsesRevisionWithOneMoveAndAOneLineMessage(){
      // given
	  val input = """
					|commit 903713a87159d810abfef413a51a1e46fdb38091
					|Author: Stu Penrose <stu@penrose.us>
					|Date:   Thu Jun 14 20:20:09 2012 -0700
					|
					|    Moved b.txt to b.txt.bak
					|
					|R100    b.txt   b.txt.bak""".trim.stripMargin
					
      // when
	  val result = new GitRevisionInfo(input)
	  
	  // then
	  assertEquals("Stu Penrose <stu@penrose.us>", result.author)
	  assertEquals("903713a87159d810abfef413a51a1e46fdb38091", result.commit)
	  assertEquals("Thu Jun 14 20:20:09 2012 -0700", result.date)
	  assertEquals("Moved b.txt to b.txt.bak", result.message)
	  assertEquals(1, result.changes.size())
	  
	  assertChangeEquals(ChangeType.R, "b.txt", "b.txt.bak", result.changes.get(0))
	}	
	
	@Test
	def parsesRevisionWithOneAddAndAMultiLineMessage(){
      // given
	  val input = """
					|commit aee7f96faa23f9dfa54f2c9efe16f411abc51609
					|Author: Stu Penrose <stu@penrose.us>
					|Date:   Thu Jun 14 20:59:25 2012 -0700
					|
					|    Added a file that
					|    is
					|    really
					|    cool
					|
					|A       d.txt""".trim.stripMargin
					
      // when
	  val result = new GitRevisionInfo(input)
	  
	  // then
	  assertEquals("Stu Penrose <stu@penrose.us>", result.author)
	  assertEquals("aee7f96faa23f9dfa54f2c9efe16f411abc51609", result.commit)
	  assertEquals("Thu Jun 14 20:59:25 2012 -0700", result.date)
	  assertEquals("""
			  		|Added a file that
					|is
					|really
					|cool""".trim.stripMargin, result.message)
	  assertEquals(1, result.changes.size())
	  
	  assertChangeEquals(ChangeType.A, "d.txt", result.changes.get(0))
	}	
	
	

	
	private def assertChangeEquals(expectedType:ChangeType, expectedPath:String, change:Change) {
	  assertChangeEquals(expectedType, expectedPath, null, change)
	}
	
	private def assertChangeEquals(expectedType:ChangeType, expectedPath:String, expectedDestPath:String, change:Change) {
	  assertEquals(expectedPath, change.path)
	  assertEquals(expectedDestPath, change.destPath)
	  assertEquals(expectedType, change.kind)
	}

}