package com.cj.scmconduit.core.p4

import java.io.{File => LocalPath}

class ClientSpec(val localPath:LocalPath, val owner:String, val clientId:String, val host:String, val view:Seq[(String, String)]){
	  override def toString = """
Client:  """ + clientId + """
Owner:  """ + owner + """
Host:    """ + host + """
Root:   """ + localPath.getAbsolutePath() + """
Options:        noallwrite clobber nocompress unlocked nomodtime normdir
SubmitOptions:  submitunchanged
LineEnd:        local
View:""" + view.map(mapping=> "\n   " + mapping._1 + " " + "//" + clientId + mapping._2).mkString("")
	}
