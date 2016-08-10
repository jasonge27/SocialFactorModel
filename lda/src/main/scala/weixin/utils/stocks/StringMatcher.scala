package weixin.utils.stocks

import scala.collection.mutable
import scala.util.matching.Regex

class StringMatcher[T] private(regex: Regex, map: Map[String, T]) {
  def query(content: String): TraversableOnce[(T, Int)] = {
    val counted = regex
      .findAllIn(content)
      .foldLeft(mutable.Map.empty[String, Int].withDefaultValue(0)) { case (counter, matched) =>
        counter += matched -> (counter(matched) + 1)
      }
    counted.iterator.map { case (matched, count) =>
      map(matched) -> count
    }
  }
}

object StringMatcher {
  def apply[T](map: Map[String, T]) = {
    val pattern = map.keys.map(Regex.quote).reduceOption(_ + "|" + _).getOrElse("")
    val regex = new Regex(pattern)
    new StringMatcher(regex = regex, map = map)
  }
}

