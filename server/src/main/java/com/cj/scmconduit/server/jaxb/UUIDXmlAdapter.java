package com.cj.scmconduit.server.jaxb;

import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class UUIDXmlAdapter extends XmlAdapter<String, UUID> {
	@Override
	public String marshal(UUID uuid) throws Exception {
		return uuid==null?"":uuid.toString();
	}
	
	@Override
	public UUID unmarshal(String text) throws Exception {
		return text==null?null:UUID.fromString(text);
	}

}
