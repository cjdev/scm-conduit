package com.cj.scmconduit.server.jaxb;

import java.io.File;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class FileXmlAdapter extends XmlAdapter<String, File> {
	@Override
	public String marshal(File file) throws Exception {
		return file!=null?file.getPath():null;
	}
	@Override
	public File unmarshal(String text) throws Exception {
		return text!=null?new File(text):null;
	}
}
