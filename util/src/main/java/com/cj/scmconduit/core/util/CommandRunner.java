package com.cj.scmconduit.core.util;

import java.io.InputStream;

public interface CommandRunner {
	String run(String command, String ... args);
	String run(InputStream in, String command, String ... args);
	
	String runWithHiddenKey(String command, String hideKey, String ... args);
	String runWithHiddenKey(InputStream in, String command, String hideKey, String ... args);
}
