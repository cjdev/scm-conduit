package com.cj.scmconduit.core.p4;

import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;


public class TestThatP4Util {
	
	@Test
	public void parsesLongFormatChangelistListingsSampleA() throws Exception {
		String text = IOUtils.toString(TestThatP4Util.class.getResourceAsStream("longFormatChangeListingSampleA.txt"));
		
		List<P4Changelist> listsExpected = Arrays.asList(new P4Changelist[]{
				new P4Changelist(3, "added b", "joe@dummy1-main", new P4Time(2010, 12, 20, 17, 47, 43)),
				new P4Changelist(2, "added a", "sally@deathstar.com", new P4Time(2010, 12, 20, 9, 56, 27))
		});
		
		List<P4Changelist> listsFound = P4Util.parseChangesFromLongPlusTimeFormat(text);
		
		Assert.assertEquals(listsExpected.size(), listsFound.size());
		
		for(int x=0;x<listsExpected.size();x++){
			P4Changelist expected = listsExpected.get(x);
			P4Changelist found = listsFound.get(x);
			
			assertChangelistsAreEqual(expected, found);
		}
	}
	
	@Test
	public void parsesLongFormatChangelistListingsSampleB() throws Exception {
		String text = IOUtils.toString(getClass().getResourceAsStream("longFormatChangeListingSampleB.txt"));
		
		List<P4Changelist> listsExpected = Arrays.asList(new P4Changelist[]{
				new P4Changelist(108941, "\nMade it happen", "someuser@joe-bridge-cjo-main", new P4Time(2010, 12, 20, 9, 56, 27)),
				new P4Changelist(108940, "No Issue: Whatever", "harry@mymachine", new P4Time(2010, 12, 20, 17, 47, 43))
		});
		
		List<P4Changelist> listsFound = P4Util.parseChangesFromLongPlusTimeFormat(text);
		
		Assert.assertEquals(listsExpected.size(), listsFound.size());
		
		for(int x=0;x<listsExpected.size();x++){
			P4Changelist expected = listsExpected.get(x);
			P4Changelist found = listsFound.get(x);
			
			assertChangelistsAreEqual(expected, found);
		}
	}
	
	private void assertChangelistsAreEqual(P4Changelist expected, P4Changelist found){
		assertEquals(expected.id, found.id);
		assertEquals(expected.description, found.description);
		assertEquals(expected.whoString, found.whoString);
		assertEquals(expected.when, found.when);
	}
}
