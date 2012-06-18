package com.cj.scmconduit.core;

import java.io.File
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import scala.reflect.BeanProperty
import scala.annotation.target.beanGetter

object ConduitState {
  def read(path:File) = jaxb().createUnmarshaller().unmarshal(path).asInstanceOf[ConduitState]
  def write(state:ConduitState, path:File) = jaxb().createMarshaller().marshal(state, path)
  def jaxb() = JAXBContext.newInstance(classOf[ConduitState]);
}

@XmlRootElement(name="scm-conduit-state")
class ConduitState(
	@(XmlElement @beanGetter)(name="last-synced-p4-changelist") @BeanProperty
	var lastSyncedP4Changelist:Long, 

	@(XmlElement @beanGetter)(name="p4-port") @BeanProperty 
	var p4Port:String, 
	
	@(XmlElement @beanGetter)(name="p4-read-user") @BeanProperty 
	var p4ReadUser:String,
	
	@(XmlElement @beanGetter)(name="p4-client-id") @BeanProperty 
	var p4ClientId:String
){
  
  def this() = this(-1, null, null, null)
}
