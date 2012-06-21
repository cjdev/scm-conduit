package com.cj.scmconduit.core

import java.io.File
import RichFile._

package object p4 {
   
  def createDummyInitialP4Commit(path:File, p4:P4, conduit:Conduit){
	conduit.push()
    if(path.listFiles().filter(!_.getName().startsWith(".")).isEmpty){
	  val firstFile = path/"firstFile.txt"
	  firstFile.write("hello world")
	  p4.doCommand("add", "firstFile.txt")
	  p4.doCommand("submit", "-d", "initial commit")
	  p4.syncTo(P4RevSpec.forChangelist(0))
	  conduit.push()
	}
  }
}