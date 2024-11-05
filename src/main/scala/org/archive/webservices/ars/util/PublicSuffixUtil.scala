package org.archive.webservices.ars.util

import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast

import scala.io.Source

import org.archive.webservices.ars.model.ArchConf

object PublicSuffixUtil {
  private var _broadcast: Option[(String, Broadcast[Set[String]])] = None

  def broadcast(sc: SparkContext): Broadcast[Set[String]] = {
    if (_broadcast.isDefined && _broadcast.get._1 == sc.applicationId) _broadcast.get._2
    else {
      for ((_, bc) <- _broadcast) bc.destroy()
      val bc = sc.broadcast(Suffixes)
      _broadcast = Some((sc.applicationId, bc))
      bc
    }
  }

  lazy val Suffixes: Set[String] = {
    val source = Source
      .fromURL(ArchConf.publicSuffixListUrl, "utf-8")
    try {
      source.getLines
        .map(_.trim)
        .filter(_.nonEmpty)
        .filter(!_.startsWith("//"))
        .toSet
    } catch {
      case _: Exception =>
        Set.empty
    } finally {
      source.close()
    }
  }

  def resolve(host: String): String = resolve(host, Suffixes)

  def resolve(host: String, suffixes: Set[String]): String = {
    val hostSplit = host.split('.')
    hostSplit.tails
      .filter(_.length > 1)
      .find { domain =>
        val suffix = domain.tail
        suffixes.contains(suffix.mkString(".")) || (suffix.length > 1 && {
          suffixes.contains("*." + suffix.tail.mkString("."))
        })
      }
      .getOrElse(hostSplit)
      .mkString(".")
  }
}
