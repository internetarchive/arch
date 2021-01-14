import sbt.ExclusionRule

lazy val commonSettings = Seq(
  name := "ars-cloud",
  version := "0.1.0",
  scalaVersion := "2.12.8"
)

val circeVersion = "0.10.0"

lazy val webapp = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.scalatra" %% "scalatra" % "2.5.4",
      "org.scalatra" %% "scalatra-cache-guava" % "2.5.4" exclude(org = "com.google.guava", name = "guava"),
      "org.scalatra" %% "scalatra-scalate" % "2.5.4",
      "org.scalatra" %% "scalatra-scalatest" % "2.5.4" % "test",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
      "org.eclipse.jetty" % "jetty-webapp" % "9.2.19.v20160908" % "container;compile",
      "org.apache.hadoop" % "hadoop-client" % "2.6.0", // % "provided", // TODO: "provided" for cluster mode
      "org.apache.spark" %% "spark-core" % "2.4.5", // % "provided", // TODO: "provided" for cluster mode
      "org.apache.spark" %% "spark-sql" % "2.4.5", // % "provided", // TODO: "provided" for cluster mode
      "org.apache.spark" %% "spark-yarn" % "2.4.5", // % "provided", // TODO: "provided" for cluster mode
      // "org.archive.helge" %% "sparkling" % "0.2.0-SNAPSHOT" // TODO: put JAR into `lib`
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )

assemblyOption in assembly := (assemblyOption in assembly).value.copy(cacheOutput = false)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

enablePlugins(ScalatraPlugin)