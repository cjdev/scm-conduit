package com.cj.scmconduit.core
import com.cj.scmconduit.core.p4.P4Credentials
import com.cj.scmconduit.core.p4.ClientSpec

trait Conduit {
	def pull(source:String, using:P4Credentials):Boolean
	def commit(using:P4Credentials)
	def rollback()
	def push() 
	def p4Path():String
	def backlogSize():Int
	def currentP4Changelist():Long
	def delete();
}