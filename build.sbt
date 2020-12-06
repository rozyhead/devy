name := "devy"

version := "1.0"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.10"
lazy val akkaHttpVersion = "10.2.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "org.scalamock" %% "scalamock" % "4.4.0" % Test,
  "io.rest-assured" % "scala-support" % "4.3.2" % Test
)

lazy val EndToEndTest = config("e2e") extend Test
lazy val e2eSettings = inConfig(EndToEndTest)(Defaults.testSettings) ++ Seq(
  fork in EndToEndTest := false,
  parallelExecution in EndToEndTest := false,
  scalaSource in EndToEndTest := baseDirectory.value / "src/e2e/scala"
)

lazy val root = (project in file("."))
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm, EndToEndTest)
  .settings(inConfig(EndToEndTest)(Defaults.testSettings): _*)
