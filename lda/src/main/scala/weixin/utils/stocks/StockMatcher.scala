package weixin.utils.stocks

import weixin.utils.formats.StockBasics



class StockMatcher private(matcher: StringMatcher[StockBasics.Entry]) {
  def query(s: String) = matcher.query(s)
}


object StockMatcher {
  def apply(stockBasics: StockBasics): StockMatcher = {
    val pairs = stockBasics.entries.flatMap { e =>
      e.name -> e :: e.code -> e :: Nil
    }
    require(pairs.map(_._1).distinct.size == pairs.size, "duplicated entry detected")
    val map = pairs.toMap
    val matcher = StringMatcher(map)
    new StockMatcher(matcher)
  }
}
