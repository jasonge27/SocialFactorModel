package weixin.utils

import scala.annotation.tailrec
import scala.collection.mutable


object DistributedUtils {

  case class LocalSingleton[T](ctor: () => T) {
    @transient lazy val value = ctor()
  }

  case class LocalPool[T <: AnyRef](ctor: () => T, maxSize: Int) {
    @transient private var pool: mutable.Queue[T] = null
    @transient private var restSize = 0

    private def ensureValid(): Unit = synchronized {
      if (pool eq null) {
        pool = mutable.Queue.empty
        restSize = maxSize
      }
    }

    def reset(): Unit = synchronized {
      ensureValid()
      restSize += pool.size
      pool.clear()
      notifyAll()
    }

    private def get: T = {
      ensureValid()
      @tailrec
      def getTrunk: () => T = {
        if (pool.nonEmpty) {
          val value = pool.dequeue()
          () => value
        } else if (restSize > 0) {
          restSize -= 1
          ctor
        } else {
          wait()
          getTrunk
        }
      }
      val trunk = synchronized(getTrunk)
      trunk()
    }

    private def put(t: T) = synchronized {
      ensureValid()
      pool.enqueue(t)
      notify()
    }

    def on[U](f: T => U): U = {
      val t = get
      try t.synchronized(f(t))
      finally put(t)
    }

    def foreach(f: T => Unit): Unit = on(f)

    def flatMap[U](f: T => U): U = on(f)
  }

}
