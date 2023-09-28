package org.archive.webservices.ars.processing

class JobQueue(val name: String) {
  private var _pos = 0
  private val queue = collection.mutable.Queue.empty[DerivationJobInstance]

  def items: Iterator[DerivationJobInstance] = queue.toIterator

  def isEmpty: Boolean = queue.isEmpty
  def nonEmpty: Boolean = queue.nonEmpty
  def size: Int = queue.size

  def enqueue(instance: DerivationJobInstance): Int = synchronized {
    val remainder = Int.MaxValue - _pos
    val thisPos = if (remainder < queue.size) queue.size - remainder else _pos + queue.size
    queue.enqueue(instance)
    instance.updateState(ProcessingState.Queued)
    thisPos
  }

  def dequeue: DerivationJobInstance = synchronized {
    if (_pos == Int.MaxValue) _pos = 1 else _pos += 1
    queue.dequeue
  }

  def dequeue(
      freeSlots: Int,
      excludeSources: Set[String] = Set.empty,
      recentUsers: Seq[String]): Option[DerivationJobInstance] = synchronized {
    val idxs = recentUsers.zipWithIndex.map { case (user, idx) => user -> (idx + 1) }.toMap
    var minIdx = 0
    var minUserInstance: Option[DerivationJobInstance] = None
    queue
      .find { instance =>
        instance.slots <= freeSlots && !excludeSources.contains(
          instance.collection.sourceId) && instance.user.map(_.id).forall { id =>
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
      .orElse(minUserInstance)
  }

  def pos: Int = _pos
}
