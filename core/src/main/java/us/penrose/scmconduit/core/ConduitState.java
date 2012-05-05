package us.penrose.scmconduit.core;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="scm-conduit-state")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConduitState {
	
	public static ConduitState read(File path){
		try {
			return (ConduitState) jaxb().createUnmarshaller().unmarshal(path);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void write(ConduitState state, File path){
		try {
			jaxb().createMarshaller().marshal(state, path);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static JAXBContext jaxb() throws JAXBException{
		return JAXBContext.newInstance(ConduitState.class);
	}
	
	@XmlElement(name="last-synced-p4-changelist")
	public Long lastSyncedP4Changelist;
	
	@XmlElement(name="p4-port")
	public String p4Port;

	@XmlElement(name="p4-read-user")
	public String p4ReadUser;
	

	@XmlElement(name="p4-client-id")
	public String p4ClientId;
}
