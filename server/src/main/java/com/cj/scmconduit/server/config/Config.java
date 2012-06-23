package com.cj.scmconduit.server.config;

import java.io.File;
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
	
	public Config() {
	}

	public Config(String publicHostname, File localPath, String p4Address, String p4User) {
		super();
		this.publicHostname = publicHostname;
		this.path = localPath;
		this.basePathForNewConduits = new File(localPath, "conduits");
		this.clientIdPrefix = publicHostname + "-conduit-server-";
		this.p4Address = p4Address;
		this.p4User = p4User;
	}
	
	
}
