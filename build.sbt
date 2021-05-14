import sbtassembly.AssemblyPlugin.autoImport.{assemblyMergeStrategy, assemblyOption}

lazy val commonSettings = Seq(name := "ars-cloud", version := "0.1.0", scalaVersion := "2.12.8")

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
  "commons-codec" % "commons-codec" % "1.12",
  "org.scalatra" %% "scalatra" % "2.5.4",
  "org.scalatra" %% "scalatra-scalate" % "2.5.4",
  "org.scalatra" %% "scalatra-scalatest" % "2.5.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.19.v20160908" % "compile",
  "com.thoughtworks.paranamer" % "paranamer" % "2.8",
  "com.syncthemall" % "boilerpipe" % "1.2.2",
  "xerces" % "xercesImpl" % "2.12.0",
  "org.jsoup" % "jsoup" % "1.13.1") ++ guavaDependent.map(
  _ exclude (org = "com.google.guava", name = "guava")) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser").map(_ % "0.10.0")

val buildSettings = commonSettings ++ Seq(
  mainClass in (Compile, run) := Some("org.archive.webservices.ars.ArsCloud"),
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
