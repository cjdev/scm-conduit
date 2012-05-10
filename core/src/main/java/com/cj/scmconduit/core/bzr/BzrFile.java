package com.cj.scmconduit.core.bzr;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class BzrFile {
	
	@XmlAttribute
	public String fid;
	
	@XmlAttribute(name="oldpath")
	public String oldPath;
	
	@XmlValue
	public String file;
}
