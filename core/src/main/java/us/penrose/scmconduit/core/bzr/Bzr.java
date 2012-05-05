package us.penrose.scmconduit.core.bzr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.penrose.scmconduit.core.util.CommandRunner;

public class Bzr {
	private final CommandRunner shell;

	public Bzr(CommandRunner shell) {
		super();
		this.shell = shell;
	}
	
	public void createStackedBranch(String source, String destination){
		runNoDashD("branch", "--no-tree",  "--stacked", source, destination);
	}
	
	private String runNoDashD(String ... args){
		List<String> a = new ArrayList<String>(Arrays.asList(args));
		
		return shell.run("bzr", a.toArray(new String[]{}));
	}
}
