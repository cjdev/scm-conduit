package us.penrose.scmconduit.core.bzr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="status")
public class BzrStatus {
	
	public static BzrStatus read(String xml){
		return BzrStatus.read(new ByteArrayInputStream(xml.getBytes()));
	}
	public static BzrStatus read(InputStream in){
		try {
			JAXBContext c = JAXBContext.newInstance(BzrStatus.class);
			return (BzrStatus) c.createUnmarshaller().unmarshal(in);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	@XmlElement(name="workingtree_root")
	public String workingTreeRoot;

	@XmlElement(name="added")
	public ChangedNodesList additions = new ChangedNodesList();
	
	@XmlElement(name="removed")
	public ChangedNodesList deletions = new ChangedNodesList();
	
	@XmlElement(name="renamed")
	public ChangedNodesList renames = new ChangedNodesList();
	

	@XmlElement(name="modified")
	public ChangedNodesList modifications = new ChangedNodesList();
	
	@XmlElementWrapper(name="pending_merges")
	@XmlElement(name="log")
	public List<LogEntry> pendingMerges = new LinkedList<LogEntry>();
	
	@XmlElementWrapper(name="unknown")
	@XmlElement(name="log")
	public List<LogEntry> unknown = new LinkedList<LogEntry>();
	
	
	public boolean isUnchanged(){
		return 
			(additions.size()==0)
			&&
			(deletions.size()==0)
			&&
			(renames.size()==0)
			&&
			(pendingMerges.size()==0)
			&&
			(unknown.size()==0);
	}
}
