package com.cj.scmconduit.core.git

import java.io.ByteArrayInputStream
import java.util.regex.Pattern
import com.cj.scmconduit.core.git.GitRevisionInfo
import com.cj.scmconduit.core.git.GitRevisionInfo._
import com.cj.scmconduit.core.p4.P4
import scala.collection.JavaConversions._
import java.io.BufferedReader
import java.io.StringReader

class Git2P4Translator(private val p4:P4) {
	
	def translate(info:GitRevisionInfo):Integer = {
		val changeListNum = createP4ChangelistWithMessage(info.message, p4);
		info.changes.foreach{change=>
		  println(" Change for cl" + changeListNum + ":" + change.path);
			change.kind match {
			case ChangeType.A =>
				p4.doCommand("add", "-c", changeListNum.toString(), change.path);
			case ChangeType.D =>
				p4.doCommand("delete", "-c", changeListNum.toString(), change.path);
			case ChangeType.M =>
				p4.doCommand("edit", "-c", changeListNum.toString(), change.path);
			case ChangeType.R =>
				p4.doCommand("edit", "-c", changeListNum.toString(), "-k", change.path);
				p4.doCommand("move", "-c", changeListNum.toString(), "-k", change.path, change.destPath);
			case _=> throw new RuntimeException("I don't know how to handle " + change.kind);
			}
		}
		return changeListNum;
	}

	private def createP4ChangelistWithMessage(message:String,  p4:P4):Integer = {
	    val lineCount = numLines(message)
	    if(lineCount > 1){
	      throw new RuntimeException(s"I don't support multi-line commit messages, but you gave me a message with $lineCount lines:\n" + message)
	    }
		val changelistText = p4.doCommand("changelist", "-o").replaceAll(Pattern.quote("<enter description here>"), message);

		val changeListNum = createChangelist(changelistText, p4);
		return changeListNum;
	}
	
	private def numLines(text:String) = {
	  val reader = new BufferedReader(new StringReader(text))
	  var numLines = 0
	  while(reader.readLine()!=null){
	    numLines +=1
	  }
	  numLines
	}

	private def createChangelist(changelistText:String, p4:P4):Integer = {
	  
		val output = p4.doCommand(new ByteArrayInputStream(changelistText.getBytes()), "changelist", "-i");

		val txt = output
					.replaceAll(("created."), "")
					.replaceAll(("Change"), "")
					.trim();
		
		val changeListNum = Integer.parseInt(txt);
		
		return changeListNum;
	}
}