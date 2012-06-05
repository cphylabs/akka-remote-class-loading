package com.earldouglas.remoteakka

import akka.actor.Actor
import akka.actor.Actor._

case object Greeting
case class Jar(val bytes: Array[Byte])
case class RegisterRemote(val name: String, val className: String)

object Runner {
  def main(args: Array[String]) {
    remote.start("localhost", 2552)
    remote.register("actorLoader", actorOf[ActorLoader])
  }
}

class ActorLoader extends Actor {
  import java.net.{URL, URLClassLoader}

  var classLoader: URLClassLoader = _
  val remoteJar = "file:remote.jar"

  def receive = {
    case jar: Jar =>
      var fos = new java.io.FileOutputStream("remote.jar")
      new java.io.PrintStream(fos).write(jar.bytes)
      fos.close
      classLoader = new URLClassLoader(Array(new URL(remoteJar)), Thread.currentThread().getContextClassLoader())
    case registerRemote: RegisterRemote =>
      val clazz = classLoader.loadClass(registerRemote.className)
      val actorClass = classOf[Actor]
      clazz match {
        case actorClass => remote.register(registerRemote.name, actorOf(clazz.asInstanceOf[Class[Actor]]))
      }
  }
}
