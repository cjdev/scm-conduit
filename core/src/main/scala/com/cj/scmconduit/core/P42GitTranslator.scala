package com.cj.scmconduit.core;

import java.util.Arrays.asList;

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

import scala.collection.JavaConversions._

class P42GitTranslator (private val conduitPath:File) {
	
	def translate(nextChange:P4Changelist, changes:List[Change], p4TimeZoneOffset:Int):List[List[String]] = {
		val gitCommands = new ArrayList[List[String]]();
		
		

		val adds = new LinkedList[Change]();
		
		changes.foreach{next=>
			val changeType = next.`type`;
			
			changeType match {
			case ChangeType.ADD => adds.add(next);
			case ChangeType.DELETE => gitCommands.add(asList("rm", next.workspacePath));
			case ChangeType.UPDATE => gitCommands.add(asList("add", next.workspacePath));
			case _=> throw new RuntimeException("Not sure what to do with " + changeType);
			}
		}
		
		if(adds.size()>0){
			val args = new ArrayList[String](adds.size()+1);
			args.add("add");
			
			adds.foreach{add=>
				val branchPath = add.workspacePath;
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
	
	def toBzrCommitDateFormat(when:P4Time, p4TimeZoneOffsetInHours:Int):String = {
		val p4TimeZoneOffsetInMinutes = p4TimeZoneOffsetInHours * 100;
				
		return "%02d-%02d-%02d %02d:%02d:%02d %+05d".format( 
				when.year(), when.monthOfYear(), when.dayOfMonth(), 
				when.hourOfDay(), when.minuteOfHour(), when.secondOfMinute(), 
				p4TimeZoneOffsetInMinutes
		);
	}
	
	private def assertIsConduitFile(actual:String) {
			val expectation = this.conduitPath.getAbsolutePath();
			if(!actual.startsWith(expectation)){
				throw new RuntimeException("I was expecting " + actual + " to start with " + expectation);
			}
	}
}