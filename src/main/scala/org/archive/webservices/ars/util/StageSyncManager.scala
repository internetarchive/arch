package org.archive.webservices.ars.util

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.SystemProcess
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.IOUtil

import java.io.File
import java.time.Instant
import scala.reflect.ClassTag
import scala.util.Try

object StageSyncManager {
  private var stages = Map.empty[String, StageSyncManager]

  def stageId(stageId: Int): String = {
    Sparkling.appId + "-" + stageId
  }

  def stageId: String = stageId(Sparkling.taskContext.map(_.stageId).getOrElse(0))

  def syncFile(workingDir: String, stageId: String): File = {
    new File(workingDir, stageId + "._sync")
  }

  def claimedFile(workingDir: String, stageId: String): File = {
    new File(workingDir, stageId + "._claimed")
  }

  def pidFile(workingDir: String, stageId: String): File = {
    new File(workingDir, stageId + ".pid")
  }

  def launchDetachableShell(proc: SystemProcess): Int = proc.synchronized {
    val tmpSesId = "bash" + Instant.now.toEpochMilli
    proc.exec("tmux new-session -d -s " + tmpSesId + " '/bin/bash' \\; display-message -p -t " + tmpSesId + ":0 '#{pane_pid}'")
    val pid = proc.readAllInput().mkString.trim.toInt
    proc.exec(s"kill -STOP $pid; reptyr -T $pid 2>/dev/null; kill -CONT $pid", supportsEcho = true, blocking = true)
    pid
  }

  def stage: StageSyncManager = {
    val id = stageId
    stages.getOrElse(id, synchronized {
      stages.getOrElse(id, {
        val stage = new StageSyncManager(id)
        stages += stageId -> stage
        stage
      })
    })
  }

  def syncProcess(cmd: String, workingDir: String, shell: => SystemProcess, exec: (SystemProcess, String) => Unit): SystemProcess = {
    stage.syncProcess(cmd, workingDir, shell, exec)
  }

  def claimProcess(workingDir: String, proc: SystemProcess): Unit = {
    stage.claimProcess(workingDir, proc)
  }

  def sync[A: ClassTag](rdd: RDD[A]): RDD[A] = {
    rdd.mapPartitions(syncPartition)
  }

  def syncPartition[A](partition: Iterator[A]): Iterator[A] = {
    stage.syncPartition(partition)
  }

  def cleanup(stageId: String): Unit = synchronized {
    for (stage <- stages.get(stageId)) {
      stage.cleanup()
      stages -= stageId
    }
  }
}

class StageSyncManager private (stageId: String) {
  private var activeTasks = Set.empty[Long]
  private var syncTasks = Set.empty[Long]
  private var syncing: Boolean = false

  private var processes = Map.empty[String, SystemProcess]
  private var claimed = Set.empty[String]
  private var lastClaimed = Map.empty[String, Long]

  def syncPartition[A](partition: Iterator[A]): Iterator[A] = {
    val task = Sparkling.taskId
    Iterator(true).flatMap { _ =>
      while (syncing) Thread.`yield`()
      synchronized(activeTasks += task)
      Iterator.empty
    } ++ partition ++ Iterator(true).flatMap { _ =>
      synchronized {
        activeTasks -= task
        syncTasks += task
        if (activeTasks.isEmpty) {
          syncStage()
          syncing = true
        }
      }
      while (!syncing) Thread.`yield`()
      synchronized {
        syncTasks -= task
        if (syncTasks.isEmpty) syncing = false
      }
      Iterator.empty
    }
  }

  private def syncStage(): Unit = synchronized {
    for (f <- claimed.map(StageSyncManager.syncFile(_, stageId))) f.delete()
    claimed = Set.empty
  }

  def cleanup(): Unit = synchronized {
    for ((_, p) <- processes) p.destroy()
    for (c <- claimed) StageSyncManager.pidFile(c, stageId).delete()
    syncStage()
    for ((d, _) <- lastClaimed) {
      val claimf = StageSyncManager.claimedFile(d, stageId)
      if (claimf.exists()) claimf.delete()
    }
  }

  def syncProcess(cmd: String, workingDir: String, shell: => SystemProcess, exec: (SystemProcess, String) => Unit): SystemProcess = {
    def checkProcess(p: SystemProcess => Boolean): Boolean = {
      for (process <- processes.get(workingDir)) p(process)
      true
    }

    lazy val syncf = StageSyncManager.syncFile(workingDir, stageId)
    lazy val pidf = StageSyncManager.pidFile(workingDir, stageId)
    while (checkProcess(return _) && !pidf.exists() && !syncf.createNewFile()) Thread.`yield`()

    synchronized {
      checkProcess(return _)
      val p = shell
      if (!pidf.exists) {
        syncf.deleteOnExit()
        p.synchronized {
          val pid = StageSyncManager.launchDetachableShell(p)
          exec(p, s"exec $cmd")
          IOUtil.writeLines(pidf.getAbsolutePath, Seq(pid.toString))
          pidf.deleteOnExit()
        }
        registerClaim(workingDir)
      }
      processes += workingDir -> p
      p
    }
  }

  private def registerClaim(workingDir: String): Unit = synchronized {
    claimed += workingDir
    val millis = Instant.now.toEpochMilli
    val claimf = StageSyncManager.claimedFile(workingDir, stageId)
    IOUtil.writeLines(claimf.getAbsolutePath, Seq(millis.toString))
    lastClaimed += workingDir -> millis
  }

  private def isLastClaim(workingDir: String): Boolean = {
    lastClaimed.get(workingDir).contains {
      val claimf = StageSyncManager.claimedFile(workingDir, stageId)
      Try(IOUtil.lines(claimf.getAbsolutePath).mkString.trim.toLong).getOrElse(0)
    }
  }

  def claimProcess(workingDir: String, proc: SystemProcess): Unit = {
    if (claimed.contains(workingDir)) return

    synchronized {
      if (claimed.contains(workingDir)) return

      val syncf = StageSyncManager.syncFile(workingDir, stageId)
      while (!syncf.createNewFile()) Thread.`yield`()
      syncf.deleteOnExit()

      val pidf = StageSyncManager.pidFile(workingDir, stageId)
      if (pidf.exists) {
        if (!isLastClaim(workingDir)) {
          val pid = IOUtil.lines(pidf.getAbsolutePath).mkString.trim
          proc.synchronized {
            proc.consumeAllInput(true)
            proc.exec(s"kill -STOP $pid; reptyr $pid 2>/dev/null; kill -CONT $pid", supportsEcho = false)
          }
        }
        registerClaim(workingDir)
      } else {
        throw new RuntimeException(s"No process available under $workingDir ($stageId).")
      }
    }
  }
}
