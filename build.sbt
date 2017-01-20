
organization := "org.homermultitext"
name := "HmtEdition"

version := "1.0.0"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", "2.12.1")


resolvers += "uh-nexus" at "http://beta.hpcc.uh.edu/nexus/content/groups/public"

libraryDependencies += "edu.holycross.shot" %% "cite" % "3.1.0"
libraryDependencies += "edu.holycross.shot" %% "ohco2" % "2.0.0"


libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" %  "test"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

publishTo := Some("Sonatype Snapshots Nexus" at "http://beta.hpcc.uh.edu/nexus/content/repositories/releases/")

credentials += Credentials(Path.userHome / "nexusauth.txt" )
