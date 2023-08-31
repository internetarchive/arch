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

  def dequeue(freeSlots: Int): Option[DerivationJobInstance] = synchronized {
    queue.dequeueFirst(_.slots <= freeSlots)
  }

  def pos: Int = _pos
}
