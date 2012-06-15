package com.cj.scmconduit.core;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.cj.scmconduit.core.p4.P4Changelist;
import com.cj.scmconduit.core.p4.P4SyncOutputParser;
import com.cj.scmconduit.core.p4.P4Time;
import com.cj.scmconduit.core.p4.P4SyncOutputParser.Change;
import com.cj.scmconduit.core.p4.P4SyncOutputParser.ChangeType;

class P42GitTranslator {
	private final File conduitPath;
	
	public P42GitTranslator(File conduitPath) {
		super();
		this.conduitPath = conduitPath;
	}

	public List<List<String>> translate(P4Changelist nextChange, List<Change> changes, int p4TimeZoneOffset){
		List<List<String>> gitCommands = new ArrayList<List<String>>();
		
		

		List<Change> adds = new LinkedList<P4SyncOutputParser.Change>();
		
		for(Change next : changes){
			ChangeType type = next.type;
			
			switch(type){
			case ADD:
				adds.add(next);
				break;
			case DELETE:
				gitCommands.add(asList("rm", next.workspacePath));
				break;
			case UPDATE:
				gitCommands.add(asList("add", next.workspacePath));
				break;
			default: throw new RuntimeException("Not sure what to do with " + type);
			}
		}
		
		if(adds.size()>0){
			List<String> args = new ArrayList<String>(adds.size()+1);
			args.add("add");
			
			for(Change add : adds){
				
				final String branchPath = add.workspacePath;
				assertIsConduitFile(branchPath);
				args.add(branchPath);
			}
			
			System.out.println("adding " + adds.size());
			gitCommands.add(args);
		}
		//runGit("add");

		gitCommands.add(asList(
				"commit", 
				"--author=Joe Schmo <" + nextChange.whoString() + ">",
				"--date=" + toBzrCommitDateFormat(nextChange.getWhen(), p4TimeZoneOffset),
				"--allow-empty",
				"-m", "[P4 CHANGELIST " + nextChange.id() + "]\n" + nextChange.description()));
		
		return gitCommands;
	}
	

	public static String toBzrCommitDateFormat(P4Time when, int p4TimeZoneOffsetInHours) {
		int p4TimeZoneOffsetInMinutes = p4TimeZoneOffsetInHours * 100;
		return String.format(
				"%02d-%02d-%02d %02d:%02d:%02d %+05d", 
				when.year(), when.monthOfYear(), when.dayOfMonth(), 
				when.hourOfDay(), when.minuteOfHour(), when.secondOfMinute(), 
				p4TimeZoneOffsetInMinutes
		);

	}
	
	private void assertIsConduitFile(String actual) {
			String expectation = this.conduitPath.getAbsolutePath();
			if(!actual.startsWith(expectation)){
				throw new RuntimeException("I was expecting " + actual + " to start with " + expectation);
			}
	}
}