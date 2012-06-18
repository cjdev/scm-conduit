package com.cj.scmconduit.core
import com.cj.scmconduit.core.p4.P4Credentials

trait Conduit {
	def pull(source:String, using:P4Credentials):Boolean
	def commit(using:P4Credentials)
	def rollback()
	def push() 
}