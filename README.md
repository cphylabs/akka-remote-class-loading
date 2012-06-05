# Remote Actor Class Loading with Akka

_19 Mar 2011_

One of the challenges in scaling out any system is synchronizing libraries on new nodes. This is the case with Akka, for which adding remote nodes requires deploying domain dependencies including `Actor` implementations.

A way around this is to send these dependencies over the wire to be dynamically loaded into a remote class loader, then registering new remote actors for use.

On the remote nodes, a special actor is needed to listen for classes to be loaded:

```scala
case class Jar(val bytes: Array[Byte])
case class RegisterRemote(val name: String, val className: String)

class ActorLoader extends Actor {
  import java.net.{URL, URLClassLoader}

  var classLoader: URLClassLoader = _
  val remoteJar = "file:remote.jar"

  def receive = {
    case jar: Jar =>
      var fos = new java.io.FileOutputStream("remote.jar")
      new java.io.PrintStream(fos).write(jar.bytes)
      fos.close
      classLoader = new URLClassLoader(Array(new URL(remoteJar)),
                                       Thread.currentThread().getContextClassLoader())
    case registerRemote: RegisterRemote =>
      val clazz = classLoader.loadClass(registerRemote.className)
      val actorClass = classOf[Actor]
      clazz match {
        case actorClass => remote.register(registerRemote.name, 
                                           actorOf(clazz.asInstanceOf[Class[Actor]]))
      }
  }
}
```

`ActorLoader` listens for `Jar` case classes which contain a `.jar` file represented as an array of bytes. This file is saved to the file system and loaded with a `URLClassLoader` for later use. `ActorLoader` also listens for `RegisterRemote` case classes which contain the name of an actor class to register, and a name under which to register it.

The client can now dynamically load and utilize actor classes on remote nodes by sending them in `.jar` files and registering them as needed:

```scala
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
```

