package com.cj.scmconduit.server.config;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;

import org.junit.Test;

public class ConduitConfigTest {
	private final JAXBContext ctx;
	
	public ConduitConfigTest() throws Exception{
		ctx = JAXBContext.newInstance(ConduitConfig.class, Config.class);
	}
	
	@Test
	public void run() throws Exception {
		
		Config orig = new Config(
							new File(""), 
							new ConduitConfig("foo-trunk", new File("here"))
						);
		
		String xml = marshal(orig);
		System.out.println(xml);
		
		Config copy = unmarshal(xml);

		assertEquals(orig.path, copy.path);
		assertEquals(orig.conduits.size(), copy.conduits.size());
		assertEquals(orig.conduits.get(0).hostingPath, copy.conduits.get(0).hostingPath);
		assertEquals(orig.conduits.get(0).localPath, copy.conduits.get(0).localPath);
		
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
