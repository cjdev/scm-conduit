package com.cj.scmconduit.core.bzr;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.cj.scmconduit.core.bzr.BzrStatus;

public class BzrStatusTest {
	
	@Test
	public void testIt() throws Exception {
		BzrStatus s = BzrStatus.read(getClass().getResourceAsStream("xmlstatus.casea.xml"));
		
		assertEquals(4, s.additions.size());
		assertEquals("myfirstdir/there.txt", s.additions.getFile(0).file);
		assertEquals("x.txt", s.additions.getFile(1).file);
		assertEquals("y.txt", s.additions.getFile(2).file);
		assertEquals("z.txt", s.additions.getFile(3).file);
		
		assertEquals(1, s.deletions.size());
		assertEquals("Test.java", s.deletions.getFile(0).file);
		
		assertEquals(4, s.pendingMerges.size());
		assertEquals("\n* Added all sorts of things", s.pendingMerges.get(0).message);
		assertEquals("\n* First move", s.pendingMerges.get(1).message);
		

		assertEquals(1, s.renames.size());
		assertEquals("xx.txt", s.renames.getFile(0).file);
		assertEquals("x.txt", s.renames.getFile(0).oldPath);
	}
	
}
