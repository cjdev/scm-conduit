package com.cj.scmconduit.core

import java.io.File
import RichFile._

package object p4 {
   
  def createDummyInitialP4Commit(path:File, p4:P4, conduit:ScmPump){
	conduit.pullChangesFromPerforce()
    if(path.listFiles().filter(!_.getName().startsWith(".")).isEmpty){
	  val firstFile = path/"README.md"
	  firstFile.write("This file lovingly created by " + getClass().getSimpleName())
	  p4.doCommand("add", "README.md")
	  p4.doCommand("submit", "-d", "initial commit")
	  p4.syncTo(P4RevSpec.forChangelist(0))
	  conduit.pullChangesFromPerforce()
	}
  }
}