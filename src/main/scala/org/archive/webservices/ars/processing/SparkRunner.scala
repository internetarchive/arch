package org.archive.webservices.ars.processing

import java.io.File

import org.apache.tools.ant.taskdefs.Java
import org.apache.tools.ant.{DefaultLogger, Project}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object SparkRunner {
  def run(job: DerivationJob, conf: DerivationJobConf): Future[Boolean] = Future {
    val mainClass = getClass.getName.stripSuffix("$")
    val args = Seq(job.getClass.getName, conf.serialize)

    val command = System.getProperty("sun.java.command")
    val jar = command.split(' ').head
    val isSbt = jar.endsWith("/sbt-launch.jar")

    val project = new Project

    val logger = new DefaultLogger
    project.addBuildListener(logger)
    logger.setOutputPrintStream(System.out)
    logger.setErrorPrintStream(System.err)
    logger.setMessageOutputLevel(Project.MSG_DEBUG)

    val jvm = new Java
    jvm.setTaskName(job.getClass.getSimpleName.stripSuffix("$"))
    jvm.setProject(project)
    jvm.setFork(true)
    jvm.setCloneVm(true)
    jvm.setJar(new File(jar))

    if (isSbt) jvm.createArg.setValue("runMain " + mainClass + " " + args.mkString(" "))
    else {
      jvm.setClassname(mainClass)
      for (arg <- args) jvm.createArg.setValue(arg)
    }

    jvm.executeJava == 0
  }

  def main(args: Array[String]): Unit = {
    SparkJobManager.init()
    val Array(className, confStr) = args
    val conf = DerivationJobConf.deserialize(confStr)
    val job = Class.forName(className).getField("MODULE$").get(null).asInstanceOf[DerivationJob]
    val success = Await.result(job.run(conf), Duration.Inf)
    Await.ready(SparkJobManager.context.map { sc =>
      sc.stop()
      while (!sc.isStopped) Thread.sleep(1)
    }, Duration.Inf)
    System.exit(if (success) 0 else 1)
  }
}
