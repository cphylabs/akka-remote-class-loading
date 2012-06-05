package com.earldouglas.remoteakka

import akka.actor.Actor
import akka.actor.Actor._

import java.io.FileInputStream

case class Jar(val bytes: Array[Byte])
case class RegisterRemote(val name: String, val className: String)

object Runner {
  def main(args: Array[String]) {
    val actorLoader = remote.actorFor("actorLoader", "localhost", 2552)
    actorLoader !! remoteJar

    actorLoader !! new RegisterRemote("remote1", "com.earldouglas.remoteakka.Hello")
    actorLoader !! new RegisterRemote("remote2", "com.earldouglas.remoteakka.Hola")

    val remote1 = remote.actorFor("remote1", "localhost", 2552)
    remote1 ! Greeting

    val remote2 = remote.actorFor("remote2", "localhost", 2552)
    remote2 ! Greeting
  }

  def remoteJar: Jar = {
    import java.io._
    import java.nio.channels.FileChannel.MapMode._

    val file = new File("remote.jar")
    val fileSize = file.length
    val stream = new FileInputStream(file)
    val buffer = stream.getChannel.map(READ_ONLY, 0, fileSize)
    val bytes = new Array[Byte](buffer.capacity)
    buffer.get(bytes)
    stream.close
    new Jar(bytes)
  }
}

case object Greeting

class Hello extends Actor {
  def receive: Receive = {
    case Greeting => println("hello")
  }
}

class Hola extends Actor {
  def receive: Receive = {
    case Greeting => println("hola")
  }
}
