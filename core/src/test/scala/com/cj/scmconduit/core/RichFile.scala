package com.cj.scmconduit.core

import java.io.{File => LocalPath}
import scala.xml.Elem
import java.io.IOException
import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import java.io.FileWriter

object RichFile {
	implicit def enrichFile( file: LocalPath ) = new RichFile( file )
	implicit def stringify( file: LocalPath ) = file.getAbsolutePath()
	
	def tempPath(name:String = "") = {
	  val path = LocalPath.createTempFile("temp",  name + ".path");
	  if(!path.delete()) throw new Exception("Could not delete file from " + path);
	  path
	}
	def tempDir(name:String = "") = {
	  val path = LocalPath.createTempFile("temp", name + ".dir");
	  if(!(path.delete() && path.mkdir())) throw new Exception("Could not create directory at " + path);
	  path
	}
	
	def realDirectory(file:LocalPath):LocalPath = {
	    if(!file.exists() && !file.mkdirs())
	      throw new Exception("Could not create directory at " + file.getAbsolutePath())
	    
	    file
	  }
}

class RichFile( file: LocalPath ) {
	  def write( xml:Elem ) {
	    write(xml.toString())
	  }
	  
	  def readString() = {
	    val in = new FileInputStream(file);
	    try{
	    	IOUtils.toString(in)
	    }finally{
	    	in.close();
	    }
	  }
	  
	  def /(s:String) = childNamed(s)
	  
	  def write( s: String ) {
	    try {
	     if(!file.exists() && !file.createNewFile()) throw new Exception("Cannot create file: " + file.getAbsolutePath) 
	    }catch {
	      case e:IOException => throw new Exception("Error creating " + file.getAbsolutePath(), e);
		}
		val out = new FileWriter(file)
	    try{ out.write( s ) }
	    finally{ out.close }
	  }
	  
	 def childNamed(name:String) = new LocalPath(file, name)

}