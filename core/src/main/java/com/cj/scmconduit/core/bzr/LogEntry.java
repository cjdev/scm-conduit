package com.cj.scmconduit.core.bzr;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class LogEntry {
	@XmlElement(name="revisionid")
	public String revisionId;
	
	public String committer;
	
	public String timestamp;
	
	public String message;
}
