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
				System.out.println("next line: " + line);
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
						System.out.println("Detected X__");
						indexOp = first;
						unstagedOp = null;
						file = line.substring(3);
					} else if(third.equals(" ") && !fourth.equals(" ")){
						System.out.println("Detected X_XX");
						indexOp = first;
						unstagedOp = second;
						file = line.substring(3);
					} else if(second.equals(" ") && (!third.equals(" ") && !fourth.equals(" "))){
						System.out.println("Detected X_XX");
						indexOp = first;
						unstagedOp = null;
						file = line.substring(2);
					} else {
						throw new RuntimeException("Unable to interpret beginning of line");
					}
					
					System.out.println("Detected '" + indexOp + "' and '" + unstagedOp + "' and file of " + file);
					
//					int unstagedCharPos;
//					String x = Character.valueOf(line.charAt(2)).toString();
//					System.out.println("x is " + x);
//					if(x.equals(" ")){
//						unstagedCharPos = 1;
//					}else{
//						unstagedCharPos = 2;
//					}
//					
//					final String unstagedOp = Character.valueOf(line.charAt(unstagedCharPos)).toString();
//					System.out.println(unstagedOp + " (at pos " + unstagedCharPos + ") is " + unstagedOp);
					
//					String file = line.substring(unstagedCharPos+1);;
					
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