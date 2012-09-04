package com.cj.scmconduit.core.util;

import java.io.InputStream;
import java.io.OutputStream;

public interface CommandRunner {

	void run(OutputStream sink, OutputStream errSink, InputStream input, String command, String hideKey, String ... args);

	void runPassThrough(String command, String hideKey, String ... args);

	String run(InputStream in, String command, String ... args);
	String runWithHiddenKey(InputStream in, String command, String hideKey, String ... args);

	String run(String command, String ... args);
	String runWithHiddenKey(String command, String hideKey, String ... args);
}
