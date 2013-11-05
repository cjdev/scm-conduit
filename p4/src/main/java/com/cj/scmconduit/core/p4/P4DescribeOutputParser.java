package com.cj.scmconduit.core.p4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class P4DescribeOutputParser {

	public enum ChangeType {EDIT};
	
	public static final class Change{
		public final ChangeType type;
		public final String depotPath;
		public final Integer fileVersion;
		
		public Change(ChangeType type, String file, Integer fileVersion) {
			super();
			this.type = type;
			this.depotPath = file;
			this.fileVersion = fileVersion;
		}
	}
	
	public List<Change> parse(String text){
	    return parse(new StringReader(text));
	}
	
	public List<Change> parse(Reader text){
		try {
			List<Change> changes = new ArrayList<P4DescribeOutputParser.Change>();
			BufferedReader lines = new BufferedReader(text);
			{
			    String line;
			    while((line=lines.readLine())!=null){
			        if(line.equals("Affected files ...")){
			            break;
			        }
			    }
			}
			{
			    String line;
                while((line=lines.readLine())!=null){
                    if(line.trim().isEmpty()) continue;
                    if(line.trim().startsWith("Differences ...")) break;
                        
                        try {
                            String[] parts = line.split(Pattern.quote(" "));
                            
                            String middlePart = parts[1];
                            
                            int hashPos = middlePart.indexOf('#');
                            String depotPath = middlePart.substring(0, hashPos);
                            int version = Integer.parseInt(middlePart.substring(hashPos+1));
                            
                            ChangeType type = ChangeType.valueOf(parts[2].trim().toUpperCase());
                            changes.add(new Change(type, depotPath, version));
                        } catch (Exception e) {
                            throw new RuntimeException("Error parsing: " + line, e);
                        }
                }
			}
			
			return changes;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
