package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.collections.inputspecs.InputSpec

class JobQueue(val name: String) {
  private val queue = collection.mutable.Queue.empty[DerivationJobInstance]

  def items: Iterator[DerivationJobInstance] = queue.toIterator

  def isEmpty: Boolean = queue.isEmpty
  def nonEmpty: Boolean = queue.nonEmpty
  def size: Int = queue.size

  def enqueue(instance: DerivationJobInstance): Unit = synchronized {
    queue.enqueue(instance)
    instance.updateState(ProcessingState.Queued)
  }

  def dequeue(instance: DerivationJobInstance): Boolean = synchronized {
    queue.dequeueFirst(_.uuid == instance.uuid).isDefined
  }

  def dequeue: DerivationJobInstance = synchronized(queue.dequeue)

  def dequeue(
      freeSlots: Int,
      excludeSources: Set[InputSpec.Identifier] = Set.empty,
      recentUsers: Seq[String]): Option[DerivationJobInstance] = synchronized {
    val idxs = recentUsers.zipWithIndex.map { case (user, idx) => user -> (idx + 1) }.toMap
    var minIdx = 0
    var minUserInstance: Option[DerivationJobInstance] = None
    queue
      .dequeueFirst { instance =>
        instance.slots <= freeSlots && !excludeSources.contains(
          instance.conf.inputSpec) && instance.user.map(_.id).forall { id =>
          idxs.get(id) match {
            case Some(idx) =>
              if (minIdx == 0 || idx < minIdx) {
                minIdx = idx
                minUserInstance = Some(instance)
              }
              false
            case None => true
          }
        }
      }
      .orElse(minUserInstance.flatMap(instance => queue.dequeueFirst(_ == instance)))
  }
}
