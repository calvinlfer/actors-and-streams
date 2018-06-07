name := "akka-sink-experiments"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.5.12"

  Seq(
    akka %% "akka-actor"  % akkaV,
    akka %% "akka-stream" % akkaV
  )
}