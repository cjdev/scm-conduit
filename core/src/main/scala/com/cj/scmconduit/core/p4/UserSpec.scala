package com.cj.scmconduit.core.p4

class UserSpec(val id:String, val email:String, val fullName:String){
	  override def toString = """User: """ + id + """
Email: """ + email + """
FullName: """ + fullName
	}