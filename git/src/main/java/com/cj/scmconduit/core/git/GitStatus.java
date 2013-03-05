package com.cj.scmconduit.core.git;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class GitStatus {
	private static final String UNKNOWN_TOKEN = "?? ";
	private final String text;
	private final List<GitFileStatus> modifications = new ArrayList<GitFileStatus>();
	
	public GitStatus(String text) {
		super();
		try {
			this.text = text;
			
			BufferedReader reader = new BufferedReader(new StringReader(text));
			
			for(String line = reader.readLine();line!=null;line = reader.readLine()){
				if(line.startsWith(UNKNOWN_TOKEN)){
					String file = line.substring(UNKNOWN_TOKEN.length());
					modifications.add(new GitFileStatus(null, null, file));
				}else{
					String first = charAt(0, line);
					String second = charAt(1, line);
					String third = charAt(2, line);
					String fourth = charAt(3, line);
					
					
					String indexOp;
					String unstagedOp;
					String file;
					
					if(second.equals(" ") && third.equals(" ")){
						// PATTERN X__
						indexOp = first;
						unstagedOp = null;
						file = line.substring(3);
					} else if(first.equals(" ") && !second.equals(" ") && third.equals(" ")){
						// PATTERN _X_
						indexOp = null;
						unstagedOp = second;
						file = line.substring(3);
					} else if(third.equals(" ") && !fourth.equals(" ")){
						// PATTERN X_XX
						indexOp = first;
						unstagedOp = second;
						file = line.substring(3);
					} else if(second.equals(" ") && (!third.equals(" ") && !fourth.equals(" "))){
						// PATTERN X_XX
						indexOp = first;
						unstagedOp = null;
						file = line.substring(2);
					} else {
						throw new RuntimeException("Unable to interpret beginning of line");
					}
					
					StatusCode stagedStatus = statusCodeFor(indexOp);
					StatusCode unStagedStatus = statusCodeFor(unstagedOp);

					modifications.add(new GitFileStatus(stagedStatus, unStagedStatus, file));
					
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException("Error parsing " + text, e);
		}
	}
	
	private String charAt(int pos, String text){
		return Character.valueOf(text.charAt(pos)).toString();
	}
	
	private StatusCode statusCodeFor(String id){
		if(id==null){
			return null;
		}else if(id.equals("A")){
			return StatusCode.ADD;
		} else if(id.equals("M")){
			return StatusCode.MODIFY;
		}else if(id.equals("D")){
			return StatusCode.RM;
		}else if(id.equals("U")){
			return StatusCode.UNMERGED;			
		} else {
			throw new RuntimeException("I don't know how to interpret '" + id + "'");
		}
	}
	
	public boolean isUnchanged(){
		return text.trim().isEmpty();
	}

	public List<GitFileStatus> files() {
		return modifications;
	}
}