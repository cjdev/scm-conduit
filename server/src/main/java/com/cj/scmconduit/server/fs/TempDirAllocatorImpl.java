package com.cj.scmconduit.server.fs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class TempDirAllocatorImpl implements TempDirAllocator {
	private final File tempDirPath;
	private final Set<File> allocatedPaths = new HashSet<File>();
	
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
			return path;
		} catch (IOException e) {
			throw new RuntimeException("Error creating temp dir at " + tempDirPath, e);
		}
	}
	
	public void dispose(File tempDir) {
		if(!allocatedPaths.contains(tempDir))
			throw new RuntimeException("This is not something I allocated");
		
		try {
			FileUtils.deleteDirectory(tempDir);
		} catch (IOException e) {
			throw new RuntimeException("Could not delete directory:" + tempDir.getAbsolutePath());
		}
	}
}
