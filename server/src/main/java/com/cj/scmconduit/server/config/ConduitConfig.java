package com.cj.scmconduit.server.config;

import java.io.File;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.cj.scmconduit.server.jaxb.FileXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class ConduitConfig {
	
	public String hostingPath;
	
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File localPath;
	
	
	public ConduitConfig() {
	}


	public ConduitConfig(String hostingPath, File localPath) {
		super();
		this.hostingPath = hostingPath;
		this.localPath = localPath;
	}
	
}
