package us.penrose.scmconduit.server.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import us.penrose.scmconduit.server.jaxb.FileXmlAdapter;

@XmlRootElement
public class Config {
	
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File path;
	
	@XmlElement(name="conduit")
	public List<ConduitConfig> conduits = new ArrayList<ConduitConfig>();
	
	@XmlElement(name="publish-host")
	public String publicHostname;
	
	public Config() {
	}

	public Config(File localPath, ConduitConfig ... conduits) {
		super();
		this.path = localPath;
		if(conduits!=null){
			this.conduits.addAll(Arrays.asList(conduits));
		}
	}
	
	
}
