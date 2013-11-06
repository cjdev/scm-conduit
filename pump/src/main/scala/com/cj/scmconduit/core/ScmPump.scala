package com.cj.scmconduit.core
import com.cj.scmconduit.core.p4.P4Credentials
import com.cj.scmconduit.core.p4.ClientSpec

trait ScmPump {
	def pushChangesToPerforce(source:String, using:P4Credentials):Boolean
	def commit(using:P4Credentials)
	def rollback(using:P4Credentials)
	def pullChangesFromPerforce() 
	def p4Path():String
	def backlogSize():Int
	def currentP4Changelist():Long
	def delete();
}