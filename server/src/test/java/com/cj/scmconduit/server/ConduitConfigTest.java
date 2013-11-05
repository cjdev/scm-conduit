package com.cj.scmconduit.server;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;

import org.junit.Test;

import com.cj.scmconduit.server.Config;

public class ConduitConfigTest {
	
	@Test
	public void parsesConfigWithUnspecifiedHostname() throws Exception{
		// GIVEN
		String [] args = {"43080", "/path/to/data", "myp4host.corp.com:34325", "larryTheConduitServer"};
		
		// WHEN
		Config config = Config.fromArgs(args);
		
		// THEN
		assertEquals(43080, config.port.intValue());
		assertEquals("/path/to/data", config.path.getPath());
		assertEquals("myp4host.corp.com:34325", config.p4Address);
		assertEquals("larryTheConduitServer", config.p4User);
		assertEquals(InetAddress.getLocalHost().getCanonicalHostName(), config.publicHostname);
	}
	
	@Test
	public void parsesConfigWithSpecifiedHostname() throws Exception{
		// GIVEN
		String [] args = {"43080", "/path/to/data", "myp4host.corp.com:34325", "larryTheConduitServer", "scm-conduit.corp.com"};
		
		// WHEN
		Config config = Config.fromArgs(args);
		
		// THEN
		assertEquals(43080, config.port.intValue());
		assertEquals("/path/to/data", config.path.getPath());
		assertEquals("myp4host.corp.com:34325", config.p4Address);
		assertEquals("larryTheConduitServer", config.p4User);
		assertEquals("scm-conduit.corp.com", config.publicHostname);
	}
	
	
}
