package us.penrose.scmconduit.core.p4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class P4SyncOutputParser {
	private static final class Markers {
		final String phrase;
		final ChangeType signifies;
		public Markers(String phrase, ChangeType signifies) {
			super();
			this.phrase = phrase;
			this.signifies = signifies;
		}
		
	}
	private static final Markers[] phrases = {
			new Markers(" - updating ", ChangeType.UPDATE), 
			new Markers(" - added as ", ChangeType.ADD),
			new Markers(" - deleted as ", ChangeType.DELETE)
	};

	public enum ChangeType {ADD, DELETE, UPDATE};
	
	public static final class Change{
		public final ChangeType type;
		public final String depotPath;
		public final String workspacePath;
		public final Integer fileVersion;
		
		public Change(ChangeType type, String file, String workspacePath, Integer fileVersion) {
			super();
			this.type = type;
			this.depotPath = file;
			this.fileVersion = fileVersion;
			this.workspacePath = workspacePath;
		}
	}
	
	public List<Change> parse(Reader text){
		try {
			List<Change> changes = new ArrayList<P4SyncOutputParser.Change>();
			BufferedReader lines = new BufferedReader(text);
			for(String line = lines.readLine();line!=null;line = lines.readLine()){
				String file = "", workspacePath = "";
				ChangeType changeType = null;
				Integer fileVersion = null;
				
				for(Markers marker : phrases){
					final String phrase = marker.phrase;
					int sPos = line.indexOf(phrase);
					if(sPos!=-1){
						workspacePath = line.substring(sPos + phrase.length());
						String partA = line.substring(0, sPos);
						int poundPos = partA.lastIndexOf('#');
						fileVersion = Integer.parseInt(partA.substring(poundPos+1));
						file = partA.substring(0, poundPos);
						changeType = marker.signifies;
						changes.add(new Change(changeType, file, workspacePath, fileVersion));
						break;
					}
				}
			}
			return changes;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
