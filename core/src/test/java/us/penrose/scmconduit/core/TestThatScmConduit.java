package us.penrose.scmconduit.core;

import junit.framework.Assert;

import org.junit.Test;

import us.penrose.scmconduit.core.p4.P4Time;


public class TestThatScmConduit {
	@Test
	public void canFormatP4TimesWithSmallerFieldValuesInProperBzrCommitDateFormat(){
		Assert.assertEquals(
				"2010-06-01 05:07:01 -0100", 
				ScmConduit.toBzrCommitDateFormat(new P4Time(2010, 6, 1, 5, 7, 1), -1)
		);
	}
	@Test
	public void canFormatP4TimesWithLargerFieldValuesInProperBzrCommitDateFormat(){
		Assert.assertEquals(
				"2010-10-10 10:10:10 +1000", 
				ScmConduit.toBzrCommitDateFormat(new P4Time(2010, 10, 10, 10, 10, 10), 10)
		);

	}
}
