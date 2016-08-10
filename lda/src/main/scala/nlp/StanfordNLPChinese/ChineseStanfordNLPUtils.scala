package nlp.StanfordNLPChinese

import java.util.Properties

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.process.ChineseDocumentToSentenceProcessor
import edu.stanford.nlp.sequences.SeqClassifierFlags
import edu.stanford.nlp.wordseg.Sighan2005DocumentReaderAndWriter

import scala.collection.convert.decorateAll._

object ChineseStanfordNLPUtils {
  val stanfordCoreNLPPropertyPath = "StanfordCoreNLP-chinese.properties"
  lazy val stanfordCoreNLPProperties = {
    val p = new Properties()
    val stream = this.getClass.getClassLoader.getResourceAsStream(stanfordCoreNLPPropertyPath)
    try p.load(stream)
    finally stream.close()
    p
  }

  def createStanfordCoreNLP = new edu.stanford.nlp.pipeline.StanfordCoreNLP(stanfordCoreNLPProperties)

  lazy val seqClassifierFlagsProperties = {
    val props = new Properties()
    stanfordCoreNLPProperties.asScala.foreach { case (k, v) =>
      val prefix = "segment."
      val nkopt = k match {
        case s if s.startsWith(prefix) =>
          Some(s.substring(prefix.length))
        case s if s.startsWith("ner.") =>
          Some(s)
        case "customAnnotatorClass.segment" =>
          Some("tokensAnnotationClassName")
        case _ => None
      }
      nkopt.foreach { nk =>
        props.setProperty(nk, v)
      }
    }
    props
  }

  def createSeqClassifierFlags = new SeqClassifierFlags(seqClassifierFlagsProperties)

  def createChineseDocumentToSentenceProcessor = new ChineseDocumentToSentenceProcessor(createSeqClassifierFlags.normalizedFile)

  def createSighan2005DocumentReaderAndWriter = {
    val drw = new Sighan2005DocumentReaderAndWriter
    drw.init(createSeqClassifierFlags)
    drw
  }

  def splitDocumentToSentence(document: String): Seq[String] = ChineseDocumentToSentenceProcessor.fromPlainText(document).asScala

  def createChineseCRFClassifier = {
    val props = seqClassifierFlagsProperties
    val flags = new SeqClassifierFlags(props)
    val loadPath = flags.loadClassifier
    require(loadPath ne null)
    CRFClassifier.getClassifier[CoreLabel](loadPath, props)
  }

}
