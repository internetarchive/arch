package org.archive.webservices.ars.io

import _root_.io.circe.parser._
import io.circe.HCursor
import org.archive.webservices.sparkling.util.{IteratorUtil, StringUtil}

import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import scala.io.Source

object Vault {
  val SessionIdCacheKey = "vaultSessionId"
  val LoginUrl = "https://vault.archive-it.org/accounts/login"
  val SessionIdCookie = "vault-session-id"
  val CsrfTokenCookie = "csrftoken"

  def sessionIdCacheKey(username: String): String = SessionIdCacheKey + "/" + username

  def session(username: String, password: String): String = {
    FileAccessContext.KeyValueCache
      .get(sessionIdCacheKey(username))
      .map(_.asInstanceOf[String])
      .getOrElse {
        val get = requests.get(LoginUrl)
        val csrfToken = get.cookies(CsrfTokenCookie).getValue
        val loginPage = get.text
        val csrfMiddleWareTokenTag =
          "<input type=\"hidden\" name=\"csrfmiddlewaretoken\" value=\""
        val loginPageStripped =
          StringUtil.stripPrefixBySeparator(loginPage, csrfMiddleWareTokenTag)
        val csrfMiddleWareToken = StringUtil.prefixBySeparator(loginPageStripped, "\"")
        val post = requests.post(
          LoginUrl,
          data = Map(
            "csrfmiddlewaretoken" -> csrfMiddleWareToken,
            "username" -> username,
            "password" -> password),
          headers = Seq("Referer" -> "https://vault.archive-it.org/accounts/login/"),
          cookieValues = Map(CsrfTokenCookie -> csrfToken),
          check = false)
        val sessionId = post.cookies(SessionIdCookie).getValue
        FileAccessContext.KeyValueCache += sessionIdCacheKey(username) -> sessionId
        sessionId
      }
  }

  private var treeNodes = Map.empty[(Int, String), TreeNode]

  case class TreeNode(
      id: Int,
      name: String,
      nodeType: String,
      fileType: Option[String],
      size: Long,
      contentUrl: Option[String]) {
    def isFile: Boolean = nodeType == "FILE"
  }

  private def treeNode(cursor: HCursor) = TreeNode(
    cursor.get[Int]("id").toOption.get,
    cursor.get[String]("name").toOption.get,
    cursor.get[String]("node_type").toOption.get,
    cursor.get[String]("file_type").toOption.filter(_ != null).map(_.split(';').head),
    cursor.get[Long]("size").toOption.getOrElse(-1),
    cursor.get[String]("content_url").toOption.filter(_ != null))

  def treeNode(sessionId: String, id: Int): Option[TreeNode] = {
    val source = Source.fromInputStream {
      access(s"https://vault.archive-it.org/api/treenodes/$id/", sessionId)
    }
    try {
      parse(source.mkString).toOption.map(_.hcursor).map(treeNode)
    } finally {
      source.close()
    }
  }

  def treeNode(sessionId: String, collectionTreenode: Int, path: String): Option[TreeNode] = {
    val split = path.split('/').filter(_.nonEmpty).toSeq
    if (split.isEmpty) return None
    val (parentPath, parentNode) = split.reverse.tails
      .map { tail =>
        val prefix = tail.reverse
        treeNodes.get((collectionTreenode, prefix.mkString("/"))).map(n => (prefix, Some(n)))
      }
      .find(_.isDefined)
      .flatten
      .getOrElse((Seq.empty, None))
    val remaining = split.drop(parentPath.length)
    var currentNode = parentNode
    var current = currentNode.map(_.id).getOrElse(collectionTreenode)
    for (name <- remaining) {
      currentNode = children(sessionId, current).find(_.name == name)
      if (currentNode.isEmpty) return None
      for (node <- currentNode) {
        current = node.id
        treeNodes += (collectionTreenode, path) -> node
      }
    }
    currentNode
  }

  def children(sessionId: String, collectionTreenode: Int, path: String): Seq[TreeNode] = {
    if (path.stripPrefix("/").stripSuffix("/").trim.isEmpty) {
      children(sessionId, collectionTreenode)
    } else
      treeNode(sessionId, collectionTreenode, path).toSeq.flatMap { node =>
        children(sessionId, node.id)
      }
  }

  def children(sessionId: String, parent: Int): Seq[TreeNode] = {
    val source = Source.fromInputStream {
      access("https://vault.archive-it.org/api/treenodes/?limit=-1&parent=" + parent, sessionId)
    }
    try {
      parse(source.mkString).toOption
        .flatMap(_.hcursor.values)
        .get
        .map(_.hcursor)
        .map { item =>
          treeNode(item)
        }
        .toSeq
    } finally {
      source.close()
    }
  }

  def access(url: String, sessionId: String): InputStream = {
    val connection = new URL(url).openConnection.asInstanceOf[HttpsURLConnection]
    connection.setRequestProperty("Accept", "*/*")
    connection.setRequestProperty("Cookie", s"$SessionIdCookie=$sessionId")
    connection.getInputStream
  }

  def glob(
      sessionId: String,
      collectionTreenode: Int,
      glob: String): Iterator[(String, TreeNode)] = {
    var remaining = Set(glob)
    IteratorUtil.whileDefined {
      if (remaining.nonEmpty) Some {
        remaining.toIterator.flatMap { g =>
          val (resolved, newRemaining) = iterateGlob(sessionId, collectionTreenode, g)
          remaining = newRemaining
          resolved
        }
      }
      else None
    }.flatten
  }

  def iterateGlob(
      sessionId: String,
      collectionTreenode: Int,
      glob: String): (Seq[(String, TreeNode)], Set[String]) = {
    if (glob.contains("*")) {
      val split = glob.split('/')
      val wildcardIdx = split.zipWithIndex.find(_._1.contains("*")).get._2
      val wildcard = split(wildcardIdx)
      val parentPath = split.take(wildcardIdx)
      val wildcardPath = split.drop(wildcardIdx)
      val tailPath = wildcardPath.drop(1)
      val sub = children(sessionId, collectionTreenode, parentPath.mkString("/"))
      if (wildcard == "**") {
        val resolved = if (tailPath.isEmpty) {
          sub.map(node => ((parentPath :+ node.name).mkString("/"), node))
        } else Seq.empty[(String, TreeNode)]
        (
          resolved,
          sub
            .filter(!_.isFile)
            .map(_.name)
            .flatMap { name =>
              Iterator(
                ((parentPath :+ name) ++ wildcardPath).mkString("/"),
                ((parentPath :+ name) ++ tailPath).mkString("/"))
            }
            .toSet)
      } else {
        val prefixSuffix = wildcard.split('*')
        val (prefix, suffix) =
          if (prefixSuffix.isEmpty) ("", "")
          else {
            (prefixSuffix.head, if (prefixSuffix.length > 1) prefixSuffix.last else "")
          }
        val candidates = sub.filter { node =>
          node.name.startsWith(prefix) && node.name.endsWith(suffix)
        }
        if (tailPath.isEmpty) {
          (
            candidates.map(node => ((parentPath :+ node.name).mkString("/"), node)),
            Set.empty[String])
        } else {
          (
            Seq.empty[(String, TreeNode)],
            candidates
              .filter(!_.isFile)
              .map { node =>
                ((parentPath :+ node.name) ++ tailPath).mkString("/")
              }
              .toSet)
        }
      }
    } else {
      (
        treeNode(sessionId, collectionTreenode, glob).map { node =>
          (glob, node)
        }.toSeq,
        Set.empty[String])
    }
  }

  def iterateGlob(
      sessionId: String,
      collectionTreenode: Int,
      glob: Set[String]): (Set[(String, TreeNode)], Set[String]) = {
    glob.toIterator
      .map { g =>
        val (resolved, remaining) = iterateGlob(sessionId, collectionTreenode, g)
        (resolved.toSet, remaining)
      }
      .fold((Set.empty[(String, TreeNode)], Set.empty[String])) { case ((a1, b1), (a2, b2)) =>
        (a1 ++ a2, b1 ++ b2)
      }
  }
}
