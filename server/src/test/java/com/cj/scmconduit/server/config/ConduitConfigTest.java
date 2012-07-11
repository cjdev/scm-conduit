package com.cj.scmconduit.server.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;

import javax.xml.bind.JAXBContext;

import org.junit.Test;

public class ConduitConfigTest {
	private final JAXBContext ctx;
	
	public ConduitConfigTest() throws Exception{
		ctx = JAXBContext.newInstance(Config.class);
	}
	
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
	
	
	@Test
	public void run() throws Exception {
		
		Config orig = new Config();
		orig.path = new File("");
		
		String xml = marshal(orig);
		System.out.println(xml);
		
		Config copy = unmarshal(xml);

		assertEquals(orig.path, copy.path);
		
		String xml2 = marshal(copy);
		assertEquals(xml, xml2);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T unmarshal(String xml){
		try {
			return (T) ctx.createUnmarshaller().unmarshal(new StringReader(xml));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private String marshal(Object o){
		try {
			StringWriter writer = new StringWriter();
			ctx.createMarshaller().marshal(o, writer);
			writer.flush();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
