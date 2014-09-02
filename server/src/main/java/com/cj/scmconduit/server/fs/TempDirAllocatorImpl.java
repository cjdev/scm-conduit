package com.cj.scmconduit.server.fs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class TempDirAllocatorImpl implements TempDirAllocator {
	private final File tempDirPath;
	private final Set<String> allocatedPaths = new HashSet<String>();
	
	public TempDirAllocatorImpl(File tempDirPath) {
		super();
		this.tempDirPath = tempDirPath;
	}
	
	public File newTempDir() {
		try {
			File path = File.createTempFile("conduit-server", ".dir", tempDirPath);
			if(!path.delete() & !path.mkdirs()){
				throw new RuntimeException("Could not create directory at path " + tempDirPath.getAbsolutePath());
			}
			allocatedPaths.add(path.getAbsolutePath());
			return path;
		} catch (IOException e) {
			throw new RuntimeException("Error creating temp dir at " + tempDirPath, e);
		}
	}
	
	public void dispose(File tempDir) {
	    final String absolutePath = tempDir.getAbsolutePath();
		if(!allocatedPaths.contains(absolutePath))
			throw new RuntimeException("This is not something I allocated");
		
		try {
			FileUtils.deleteDirectory(tempDir);
			allocatedPaths.remove(absolutePath);
		} catch (IOException e) {
			throw new RuntimeException("Could not delete directory:" + tempDir.getAbsolutePath());
		}
	}
}
