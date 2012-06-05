import sbt._

class Build(info: ProjectInfo) extends DefaultProject(info) with AkkaProject
