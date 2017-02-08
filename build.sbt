
organization := "org.homermultitext"
name := "hmt_textmodel"

version := "1.1.0"

crossScalaVersions := Seq("2.11.8", "2.12.1")


resolvers += Resolver.jcenterRepo


libraryDependencies ++= Seq(
  "edu.holycross.shot.cite" %% "xcite" % "1.3.0",
  "edu.holycross.shot" %% "ohco2" % "3.1.0",
  "edu.holycross.shot" %% "orca" % "2.1.0",
  "edu.holycross.shot" %% "greek" % "1.2.2",


   "org.scalatest" %% "scalatest" % "3.0.1" %  "test",
   "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
)
