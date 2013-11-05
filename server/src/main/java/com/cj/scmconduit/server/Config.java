package com.cj.scmconduit.server;

import java.io.File;
import java.net.InetAddress;

import javax.xml.bind.annotation.XmlRootElement;

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
	public final File path;
	public final String publicHostname;
	public final String clientIdPrefix;
	public final String p4Address;
	public final String p4User;
	public final File basePathForNewConduits;
	public final Integer port;
	
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
