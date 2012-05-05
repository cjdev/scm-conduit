package us.penrose.scmconduit.server.fs;

import java.io.File;

public interface TempDirAllocator {
	File newTempDir();
	void dispose(File tempDir);
}