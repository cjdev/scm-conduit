package com.cj.scmconduit.core.git;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.cj.scmconduit.core.util.CommandRunner;

public class Git {
	private final CommandRunner shell;
	private final File path;
	
	public Git(CommandRunner shell, File path) {
		super();
		this.shell = shell;
		this.path = path;
	}

	public String run(String ... args) {
		List<String> a = new java.util.ArrayList<String>(Arrays.asList(args));
		a.add(0, "--git-dir=" + new File(path, ".git").getAbsolutePath());
		a.add(0, "--work-tree=" + path.getAbsolutePath());
		return shell.run("git", a.toArray(new String[]{}));
	}
}
