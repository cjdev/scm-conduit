import java.io.{File => LocalPath}
import scala.xml.Elem
import java.io.IOException

object RichFile {
	implicit def enrichFile( file: LocalPath ) = new RichFile( file )
	implicit def stringify( file: LocalPath ) = file.getAbsolutePath()
	
	def tempPath() = {
	  val path = LocalPath.createTempFile("temp", ".path");
	  if(!path.delete()) throw new Exception("Could not delete file from " + path);
	  path
	}
	def tempDir() = {
	  val path = LocalPath.createTempFile("temp", ".dir");
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
	  
	  def /(s:String) = childNamed(s)
	  
	  
	  
	  def write( s: String ) {
	    try {
	     if(!file.exists() && !file.createNewFile()) throw new Exception("Cannot create file: " + file.getAbsolutePath) 
	    }catch {
	      case e:IOException => throw new Exception("Error creating " + file.getAbsolutePath(), e);
		}
		val out = new java.io.PrintWriter( file )
	    try{ out.println( s ) }
	    finally{ out.close }
	  }
	  
	 def childNamed(name:String) = new LocalPath(file, name)

}