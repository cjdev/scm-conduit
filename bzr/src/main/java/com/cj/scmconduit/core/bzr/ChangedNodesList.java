package com.cj.scmconduit.core.bzr;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class ChangedNodesList {
	@XmlElement(name="file")
	public List<BzrFile> files = new LinkedList<BzrFile>();
	
	@XmlElement(name="directory")
	public List<BzrFile> directories = new LinkedList<BzrFile>();
	
	public int numFiles(){
		return files.size();
	}
	
	public BzrFile getFile(int x){
		return files.get(x);
	}
	public int numDirectories(){
		return directories.size();
	}
	
	public BzrFile getDirectory(int x){
		return directories.get(x);
	}
	
	public int size(){
		return numFiles() + numDirectories();
	}
}
