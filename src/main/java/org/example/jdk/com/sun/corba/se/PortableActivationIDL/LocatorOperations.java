package com.sun.corba.se.PortableActivationIDL;


/**
* com/sun/corba/se/PortableActivationIDL/LocatorOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /System/Volumes/Data/jenkins/workspace/8-2-build-macosx-aarch64-OS12.X-Xcode12.4-sans-NAS/jdk8u441/1521/corba/src/share/classes/com/sun/corba/se/PortableActivationIDL/activation.idl
* Wednesday, December 4, 2024 8:04:25 AM GMT
*/

public interface LocatorOperations 
{

  /** locate server - returns the port with a specific type for all registered
  	* ORBs of an active server.
  	* Starts the server if it is not already running.
  	*/
  com.sun.corba.se.PortableActivationIDL.LocatorPackage.ServerLocationPerType locateServer (String serverId, String endPoint) throws com.sun.corba.se.PortableActivationIDL.NoSuchEndPoint, com.sun.corba.se.PortableActivationIDL.ServerNotRegistered, com.sun.corba.se.PortableActivationIDL.ServerHeldDown;

  /** locate server - returns all ports registered with a specified ORB for
  	* an active server
  	* Starts the server if it is not already running.
  	*/
  com.sun.corba.se.PortableActivationIDL.LocatorPackage.ServerLocationPerORB locateServerForORB (String serverId, String orbId) throws com.sun.corba.se.PortableActivationIDL.InvalidORBid, com.sun.corba.se.PortableActivationIDL.ServerNotRegistered, com.sun.corba.se.PortableActivationIDL.ServerHeldDown;

  /** get the port for the endpoint of the locator
  	*/
  int getEndpoint (String endPointType) throws com.sun.corba.se.PortableActivationIDL.NoSuchEndPoint;

  /** Useful from external BadServerIdHandlers which need
  	* to pick a particular port type.
  	*/
  int getServerPortForType (com.sun.corba.se.PortableActivationIDL.LocatorPackage.ServerLocationPerORB location, String endPointType) throws com.sun.corba.se.PortableActivationIDL.NoSuchEndPoint;
} // interface LocatorOperations
