package com.cj.scmconduit.core.git;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class GitStatusTest {

	@Test
	public void detectsUnchangedStatus() {
		// GIVEN
		GitStatus s = new GitStatus("");
		
		// WHEN
		boolean result = s.isUnchanged();
		
		// THEN
		Assert.assertTrue(result);
	}
	
	@Test
	public void detectsMixedChanges(){
		// GIVEN
		GitStatus s = new GitStatus(
							"A  dummy.txt\n" +
							"D  index.txt\n" + 
							"AM test\n" + 
							"?? whatever.txt\n"
						);
		
		// WHEN
		List<GitFileStatus> result = s.files();
		
		// THEN
		Assert.assertNotNull(result);
		Assert.assertEquals(4, result.size());
		assertGitFile(StatusCode.ADD, null, "dummy.txt", result.get(0));
		assertGitFile(StatusCode.RM, null, "index.txt", result.get(1));
		assertGitFile(StatusCode.ADD, StatusCode.MODIFY, "test", result.get(2));
		assertGitFile(null, null, "whatever.txt", result.get(3));
	}
	
	private void assertGitFile(StatusCode stagedStatus, StatusCode unstagedStatus, String file, GitFileStatus status){
		assertEquals(stagedStatus, status.stagedStatus);
		assertEquals(unstagedStatus, status.unstagedStatus);
		assertEquals(file, status.file);
	}
	
	@Test
	public void parsesASingleFileThatWasDeleted(){
		// GIVEN
		GitStatus s = new GitStatus("D  index.txt");
		
		// WHEN
		List<GitFileStatus> result = s.files();
		
		// THEN
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		assertGitFile(StatusCode.RM, null, "index.txt", result.get(0));
	}
	
	@Test
	public void parsesASingleFileThatWasAddedThenModified(){
		// GIVEN
		GitStatus s = new GitStatus("AM test.txt");
		
		// WHEN
		List<GitFileStatus> result = s.files();
		
		// THEN
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(StatusCode.ADD, result.get(0).stagedStatus);
		Assert.assertEquals(StatusCode.MODIFY, result.get(0).unstagedStatus);
		Assert.assertEquals("test.txt", result.get(0).file);
	}
	
	@Test
	public void parsesASingleUnknownFile(){
		// GIVEN
		GitStatus s = new GitStatus("?? test.txt");
		
		// WHEN
		List<GitFileStatus> result = s.files();
		
		// THEN
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0).isUnknown());
		Assert.assertEquals("test.txt", result.get(0).file);
	}
	
	@Test
	public void parsesASingleModificationFile(){
		// GIVEN
		GitStatus s = new GitStatus("M test.txt");
		
		// WHEN
		List<GitFileStatus> result = s.files();
		
		// THEN
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(StatusCode.MODIFY, result.get(0).stagedStatus);
		Assert.assertEquals("test.txt", result.get(0).file);
	}
	 
	@Test
	public void parsesASingleAdd(){
		// GIVEN
		GitStatus s = new GitStatus("A  test.txt");
		
		// WHEN
		List<GitFileStatus> result = s.files();
		
		// THEN
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(StatusCode.ADD, result.get(0).stagedStatus);
		Assert.assertEquals("test.txt", result.get(0).file);
	}

}
