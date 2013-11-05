package com.cj.scmconduit.core
import org.junit.Test
import javax.xml.bind.JAXBContext
import org.junit.Before
import org.junit.After
import org.junit.Assert._
import java.io.File
import scala.collection.mutable.ListBuffer
import org.apache.commons.io.FileUtils
import java.io.StringReader

class ConduitStateTest {
  
  private val tempFiles = ListBuffer[File]()
  
  @After
  def cleanUp(){
    tempFiles.foreach{file=> file.delete();}
  }
  
  private def tempFile()={
    val f = java.io.File.createTempFile("whatever",".temp")
    tempFiles += f
    f
  }
  
  
  @Test
  def isDeserializable(){
    // GIVEN
    val xml = <scm-conduit-state>
					<last-synced-p4-changelist>232</last-synced-p4-changelist>
					<p4-port>localhost:1666</p4-port>
					<p4-read-user>larry</p4-read-user>
					<p4-client-id>larrys-client</p4-client-id>
				</scm-conduit-state>
      
    val jaxb = JAXBContext.newInstance(classOf[PumpState])
    
      
    // WHEN
    val state = jaxb.createUnmarshaller().unmarshal(new StringReader(xml.toString())).asInstanceOf[PumpState]  

    // THEN
    assertEquals("localhost:1666", state.p4Port)
    assertEquals(232, state.lastSyncedP4Changelist)
    assertEquals("larry", state.p4ReadUser)
    assertEquals("larrys-client", state.p4ClientId)
  }
  
  @Test
  def isSerializable(){
    // GIVEN
    val f = tempFile()
    val state = new PumpState(
    				lastSyncedP4Changelist=232, p4Port="superhost:233", 
    				p4ReadUser="larry", p4ClientId="larry's workspace")
    
    // WHEN
    PumpState.write(state, f)
    println(FileUtils.readFileToString(f))
    val result = PumpState.read(f)

    // THEN
    assertEquals("superhost:233", state.p4Port)
    assertEquals(232, state.lastSyncedP4Changelist)
    assertEquals("larry", state.p4ReadUser)
    assertEquals("larry's workspace", state.p4ClientId)
  }
}