package us.penrose.scmconduit.core.p4;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class P4Util {
	private static final String CHANGE = "Change";
	
	private static P4Time parseMoment(String date, String time){

		final int year;
		final int monthOfYear;
		final int dayOfMonth;
		final int hourOfDay;
		final int minuteOfHour;
		final int secondOfMinute;
		
		String[] dateParts = date.split(Pattern.quote("/"));

		year = Integer.parseInt(dateParts[0]);
		monthOfYear = Integer.parseInt(dateParts[1]);
		dayOfMonth = Integer.parseInt(dateParts[2]);
		
		String[] timeParts = time.split(Pattern.quote(":"));
		hourOfDay = Integer.parseInt(timeParts[0]);
		minuteOfHour = Integer.parseInt(timeParts[1]);
		secondOfMinute = Integer.parseInt(timeParts[2]);
		
		return new P4Time(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute);
	}
	
	/*
 p4 changelists -l  ...@2,#head 
Change 3 on 2010/12/20 by joe@dummy1-main

	added b

Change 2 on 2010/12/20 by joe@dummy1-main

	added a
	 */
	
	
	
	public static List<P4Changelist> parseChangesFromLongPlusTimeFormat(String text){
		try {
			
			List<P4Changelist> changes = new LinkedList<P4Changelist>();
			
			Cursor c = new Cursor(text);
			
			while(c.currentChar()!=null){
				String firstWord = c.readTo(' ');
				if(firstWord.isEmpty()){
					// not a line
					break;
				}else if(firstWord.equals(CHANGE)){
					//Change 3 on 2010/12/20 by joe@dummy1-main
					c.asssertEqualsAndNext(' ');
					long revNo = Long.parseLong(c.readTo(' '));
					c.movePast(" on ");
					String date = c.readTo(' ');
					c.movePast(" ");
					String time = c.readTo(' ');
					c.movePast(" by ");
					String whoString = c.readTo('\n');
					c.movePast("\n");// empty line
					c.movePast("\n");// empty line
					
					StringBuilder description = new StringBuilder();
					
					int lineNum = -1;
					while(true){
						if(c.currentChar()==null){
							break;
						}else{
							String next = c.readTo('\n');
							if(next.startsWith(CHANGE)){
								c.backup(next);
								c.movePast("\n");// empty line
								break;
							}else{
								c.movePast("\n");
								
								if(next.startsWith("\t")){
									lineNum++;
									if(lineNum>0){
										description.append("\n");
									}
									description.append(next.substring(1));
								}
								
							}
						}
					}
					
					P4Time when = parseMoment(date, time);
					
					P4Changelist changelist = new P4Changelist(revNo, description.toString(), whoString, when);
					changes.add(changelist);
				}else{
					throw new RuntimeException("Expected \"" + CHANGE + "\" but found \"" + firstWord + "\" at " + c.position);
				}
			}
			
			return changes;
		} catch (Exception e) {
			throw new RuntimeException("Error parsing text: " + text, e);
		}
		
	}
	
	
	static class Cursor {
		final CharSequence text;
		int position;
		
		public Cursor(CharSequence text) {
			super();
			this.text = text;
		}
		
		void movePast(String expectedChars){
			for(int x=0;x<expectedChars.length();x++){
				asssertEqualsAndNext(expectedChars.charAt(x));
			}
		}
		
		void backup(String expectedChars){
			previous();
			for(int x=expectedChars.length()-1;x>=0;x--){
				asssertEqualsAndPrevious(expectedChars.charAt(x));
			}
		}
		
		void asssertEqualsAndPrevious(char expected){
			Character actual = currentChar();
			if(actual==null || !actual.equals(expected)){
				throw new RuntimeException("Expected '" + expected + "' but was '" + actual + "' at " + position);
			}
			
			previous();
		}
		
		void asssertEqualsAndNext(char expected){
			Character actual = currentChar();
			if(actual==null || !actual.equals(expected)){
				throw new RuntimeException("Expected '" + expected + "' but was '" + actual + "' at " + position);
			}
			
			next();
		}
		
		void forwardTo(char c){
			
			while(currentChar()!=null && currentChar()!=c){
				next();
			}
		}
		
		String readTo(char c){
			if(currentChar()==null){
				return null;
			}else{
				StringBuilder text = new StringBuilder();
				
				while(currentChar()!=null && currentChar()!=c){
					text.append(currentChar());
					next();
				}
				
				return text.toString();
			}
			
		}
		
		void next(){
			position++;
		}
		void previous(){
			position--;
		}
		
		Character currentChar(){
			if(position>text.length()-1){
				return null;
			}else{
				return text.charAt(position);
			}
		}
	}
}
