package org.archive.webservices.ars.io

import _root_.io.circe.parser._
import org.archive.webservices.sparkling.util.StringUtil

import java.io.InputStream
import java.net.URL
import scala.io.Source

object Vault {
  val SessionIdCacheKey = "vaultSessionId"
  val LoginUrl = "https://vault.archive-it.org/accounts/login"
  val SessionIdCookie = "vault-session-id"

  def session(username: String, password: String): String = {
    FileAccessContext.KeyValueCache.get(SessionIdCacheKey).map(_.asInstanceOf[String]).getOrElse {
      val source = Source.fromURL(LoginUrl)
      val loginPage = try {
        source.mkString
      } finally source.close()
      val csrfMiddleWareTokenTag = "<input type=\"hidden\" name=\"csrfmiddlewaretoken\" value=\""
      val loginPageStripped = StringUtil.stripPrefixBySeparator(loginPage, csrfMiddleWareTokenTag)
      val csrfMiddleWareToken = StringUtil.prefixBySeparator(loginPageStripped, "\"")
      val r = requests.post(LoginUrl, data = Map(
        "csrfmiddlewaretoken" -> csrfMiddleWareToken,
        "username" -> username,
        "password" -> password))
      val sessionId = r.cookies(SessionIdCookie).getValue
      FileAccessContext.KeyValueCache += SessionIdCacheKey -> sessionId
      sessionId
    }
  }

  private var treeNodes = Map.empty[(Int, String), Int]

  def treeNode(sessionId: String, collectionTreenode: Int, path: String): Int = {
    val split = path.split('/').filter(_.nonEmpty).toSeq
    if (split.isEmpty) return collectionTreenode
    val (parentPath, parentNode) = split.reverse.tails.map { tail =>
      val prefix = tail.reverse
      treeNodes.get((collectionTreenode, prefix.mkString("/"))).map((prefix, _))
    }.find(_.isDefined).flatten.getOrElse((Seq.empty, collectionTreenode))
    val remaining = split.drop(parentPath.length)
    var current = parentNode
    if (remaining.nonEmpty) {
      for (name <- remaining) {
        current = children(sessionId, current).find(_._1 == name).get._2
      }
      treeNodes += (collectionTreenode, path) -> current
    }
    current
  }

  def children(sessionId: String, collectionTreenode: Int, path: String): Seq[(String, Int)] = {
    children(sessionId, treeNode(sessionId, collectionTreenode, path))
  }

  def children(sessionId: String, parent: Int): Seq[(String, Int)] = {
    val source = Source.fromInputStream {
      access("https://vault.archive-it.org/api/treenodes?limit=-1&parent=" + parent, sessionId)
    }
    try {
      parse(source.mkString).toOption.flatMap(_.hcursor.values).get.map(_.hcursor).flatMap { item =>
        for {
          name <- item.get[String]("name").toOption
          id <- item.get[Int]("id").toOption
        } yield (name, id)
      }.toSeq
    } finally {
      source.close()
    }
  }

  def contentUrl(sessionId: String, collectionTreenode: Int, path: String): Option[String] = {
    contentUrl(sessionId, treeNode(sessionId, collectionTreenode, path))
  }

  def contentUrl(sessionId: String, treenodeId: Int): Option[String] = {
    val source = Source.fromInputStream {
      access("https://vault.archive-it.org/api/treenodes/" + treenodeId, sessionId)
    }
    try {
      parse(source.mkString).toOption.flatMap(_.hcursor.get[String]("content_url").toOption)
    } finally {
      source.close()
    }
  }

  def access(url: String, sessionId: String): InputStream = {
    val connection = new URL(url).openConnection
    connection.setRequestProperty("Cookie", s"$SessionIdCookie=$sessionId")
    connection.getInputStream
  }
}
