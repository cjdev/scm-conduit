package com.cj.scmconduit.core;

import java.io.ByteArrayInputStream;
import java.util.regex.Pattern;

import com.cj.scmconduit.core.git.GitRevisionInfo;
import com.cj.scmconduit.core.p4.P4;

public class Translator {
	private final P4 p4;
	
	public Translator(P4 p4) {
		super();
		this.p4 = p4;
	}
	
	public Integer translate(GitRevisionInfo info){
		Integer changeListNum = createP4ChangelistWithMessage(info.message, p4);
		for(GitRevisionInfo.Change change : info.changes){ 

			switch(change.kind){
			case A: {
				p4.doCommand("add", "-c", changeListNum.toString(), change.path);
				break;
			}
			case D: {
				p4.doCommand("delete", "-c", changeListNum.toString(), change.path);
				break;
			}
			case M: {
				p4.doCommand("edit", "-c", changeListNum.toString(), "-k", change.path);
				p4.doCommand("move", "-c", changeListNum.toString(), "-k", change.path, change.destPath);
				break;
			}
			default: throw new RuntimeException("I don't know how to handle " + change.kind);
			}
		}
		return changeListNum;
	}
	

	private static Integer createP4ChangelistWithMessage(final String message,  final P4 p4) {
		System.out.println("Changes:\n" + message);

		System.out.println("Creating changelist");

		final String changelistText = p4.doCommand("changelist", "-o").replaceAll(Pattern.quote("<enter description here>"), message);

		final Integer changeListNum = createChangelist(changelistText, p4);
		return changeListNum;
	}

	private static Integer createChangelist(final String changelistText,  final P4 p4) {
		final Integer changeListNum;
		
		final String output = p4.doCommand(new ByteArrayInputStream(changelistText.getBytes()), "changelist", "-i");
		System.out.println(output);

		try {
			String txt = output
			.replaceAll(("created."), "")
			.replaceAll(("Change"), "")
			.trim();
			System.out.println("Txt: " + txt);
			changeListNum = Integer.parseInt(txt);
			System.out.println("Found number " + changeListNum);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return changeListNum;
	}
}