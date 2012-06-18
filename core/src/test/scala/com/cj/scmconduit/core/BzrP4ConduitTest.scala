package com.cj.scmconduit.core

import org.junit.{Test, Assert}
import com.cj.scmconduit.core.p4.P4Time

class BzrP4ConduitTest {
	@Test
	def canFormatP4TimesWithSmallerFieldValuesInProperBzrCommitDateFormat(){
		Assert.assertEquals(
				"2010-06-01 05:07:01 -0100", 
				BzrP4Conduit.toBzrCommitDateFormat(new P4Time(2010, 6, 1, 5, 7, 1), -1)
		);
	}
	@Test
	def canFormatP4TimesWithLargerFieldValuesInProperBzrCommitDateFormat(){
		Assert.assertEquals(
				"2010-10-10 10:10:10 +1000", 
				BzrP4Conduit.toBzrCommitDateFormat(new P4Time(2010, 10, 10, 10, 10, 10), 10)
		);

	}
}