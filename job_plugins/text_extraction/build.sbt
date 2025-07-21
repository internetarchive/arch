val namespace = "org.archive.webservices.arch.addons.jobs"

val sourceDirs = Seq("addon", "functions", "jobs")

val provided = Seq(
  "org.apache.spark" %% "spark-core" % "2.4.5",
  "org.apache.spark" %% "spark-sql" % "2.4.5",
  "org.archive.webservices" %% "sparkling" % "0.3.8-SNAPSHOT",
  "org.archive.webservices" %% "archivespark" % "3.3.8-SNAPSHOT",
  "org.archive.webservices" %% "arch" % "2.1.1-SNAPSHOT"
)

val dependencies = Seq(
  "net.sourceforge.nekohtml" % "nekohtml" % "1.9.13",
  "xerces" % "xercesImpl" % "2.9.1",
  "com.syncthemall" % "boilerpipe" % "1.2.2",
  "org.apache.tika" % "tika-core" % "2.9.2",
  "org.apache.tika" % "tika-parsers-standard-package" % "2.9.2",
  "org.apache.commons" % "commons-compress" % "1.26.1",
  "io.circe" %% "circe-core" % "0.13.0")

val shaded = Seq(
  "org.apache.commons",
  "com.fasterxml.jackson")

lazy val plugin = (project in file("."))
  .settings(
    libraryDependencies ++= provided.map { lib =>
      lib % "provided" intransitive()
    } ++ dependencies,
    Compile / unmanagedSourceDirectories ++= sourceDirs.map(baseDirectory.value / _),
  )

assemblyShadeRules in assembly := shaded.map { p =>
  val renameTo = namespace + "." + baseDirectory.value.getName + ".shaded.@0"
  ShadeRule.rename(p + ".**" -> renameTo).inAll
} ++ sourceDirs.map { p =>
  val renameTo = namespace + "." + baseDirectory.value.getName + ".@0"
  ShadeRule.rename(p + ".**" -> renameTo).inProject
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) =>
    xs.map(_.toLowerCase) match {
      case "services" :: xs => MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}