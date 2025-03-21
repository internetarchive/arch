import sbtassembly.AssemblyPlugin.autoImport.{assemblyMergeStrategy, assemblyOption}

lazy val commonSettings = Seq(name := "arch", organization := "org.archive.webservices", version := "2.1.1-SNAPSHOT", scalaVersion := "2.12.8")

val guava = "com.google.guava" % "guava" % "29.0-jre"

val guavaDependent = Seq(
  "org.scalatra" %% "scalatra-cache-guava" % "2.5.4",
  "io.archivesunleashed" % "aut" % "0.90.0" intransitive (),
  "com.optimaize.languagedetector" % "language-detector" % "0.6",
  "org.apache.tika" % "tika-core" % "1.22",
  "org.apache.tika" % "tika-langdetect" % "1.22" exclude (org = "com.optimaize.languagedetector",
  name = "language-detector"),
  "org.apache.tika" % "tika-parsers" % "1.22" excludeAll ExclusionRule(
    organization = "com.fasterxml.jackson.core"))

val prodProvided = Seq(
  "org.apache.hadoop" % "hadoop-common" % "2.6.0",
  "org.apache.hadoop" % "hadoop-client" % "2.6.0",
  "org.apache.spark" %% "spark-core" % "2.4.5",
  "org.apache.spark" %% "spark-sql" % "2.4.5",
  "org.apache.spark" %% "spark-yarn" % "2.4.5")

val dependencies = prodProvided.map(_ % "provided") ++ Seq(
  "org.archive.webservices" %% "sparkling" % "0.3.8-SNAPSHOT" % "provided",
  "org.archive.webservices" %% "archivespark" % "3.3.8-SNAPSHOT" % "provided",
  "commons-codec" % "commons-codec" % "1.12",
  "org.json4s" %% "json4s-native" % "3.5.0",
  "org.scalatra" %% "scalatra" % "2.5.4",
  "org.scalatra" %% "scalatra-scalate" % "2.5.4",
  "org.scalatra" %% "scalatra-scalatest" % "2.5.4" % "test",
  "org.scalatra" %% "scalatra-swagger"  % "2.5.4",
  "org.scalamock" %% "scalamock" % "5.2.0" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.19.v20160908" % "compile",
  "com.lihaoyi" %% "requests" % "0.8.0",
  "com.thoughtworks.paranamer" % "paranamer" % "2.8",
  "com.syncthemall" % "boilerpipe" % "1.2.2",
  "xerces" % "xercesImpl" % "2.12.0",
  "org.jsoup" % "jsoup" % "1.13.1",
  "com.fasterxml.uuid" % "java-uuid-generator" % "4.1.1",
  "io.sentry" % "sentry" % "6.28.0",
  "com.amazonaws" % "aws-java-sdk" % "1.7.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "4.3.1" % "provided",
  "io.github.nremond" %% "pbkdf2-scala" % "0.7.0") ++ guavaDependent.map(
  _ exclude (org = "com.google.guava", name = "guava")) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser").map(_ % "0.13.0")

val buildSettings = commonSettings ++ Seq(
  mainClass in (Compile, run) := Some("org.archive.webservices.ars.Arch"),
  resolvers += Resolver.mavenLocal,
  publishMavenStyle := false)

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided") ++ dependencies)

lazy val dev = (project in file("build/dev"))
  .dependsOn(root)
  .settings(libraryDependencies ++= prodProvided)
  .settings(buildSettings: _*)
  .settings(
    fork in run := true,
    outputStrategy := Some(StdoutOutput),
    baseDirectory in run := file("."))

lazy val prod = (project in file("build/prod"))
  .dependsOn(root)
  .settings(libraryDependencies += guava)
  .settings(buildSettings: _*)
  .settings(
    assemblyShadeRules in assembly := Seq(
      ShadeRule
        .rename("com.google.common.**" -> (name.value + ".shade.@0"))
        .inLibrary(guava)
        .inLibrary(guavaDependent: _*)
        .inProject),
    assemblyOption in assembly := (assemblyOption in assembly).value
      .copy(includeScala = false, includeDependency = false),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    })

enablePlugins(ScalatraPlugin)
