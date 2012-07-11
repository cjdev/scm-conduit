package com.cj.scmconduit.server.config;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.cj.scmconduit.core.p4.P4DepotAddress;
import com.cj.scmconduit.server.jaxb.FileXmlAdapter;

@XmlRootElement
public class Config {
	public static Config fromArgs(String[] args){
		try{
			Integer port = Integer.parseInt(args[0]);
			File path = new File(args[1]);
			String p4Address = args[2];
			String p4User = args[3];
			String publicHostname = args.length>4?args[4]:InetAddress.getLocalHost().getCanonicalHostName();
			
			return new Config(port, publicHostname, path, p4Address, p4User);
		}catch(Exception e){
			throw new RuntimeException("Error parsing arguments", e);
		}
	}
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File path;
	
	@XmlElement(name="publish-host")
	public String publicHostname;
	
	@XmlElement(name="client-id-prefix")
	public String clientIdPrefix;

	@XmlElement(name="perforce-server-location")
	public String p4Address;

	@XmlElement(name="owner-for-new-conduits")
	public String p4User;

	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@XmlElement(name="path-for-new-conduits")
	public File basePathForNewConduits;
	
	public Integer port;
	
	public Config() {
	}

	public Config(Integer port, String publicHostname, File localPath, String p4Address, String p4User) {
		super();
		this.port = port;
		this.publicHostname = publicHostname;
		this.path = localPath;
		this.basePathForNewConduits = new File(localPath, "conduits");
		this.clientIdPrefix = publicHostname + "-conduit-server-";
		this.p4Address = p4Address;
		this.p4User = p4User;
	}
	
	
}
