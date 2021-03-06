package com.github.tkawachi.doctest

import java.io.File

import scala.io.Source
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4

object ScaladocTestGenerator {

  private def decodeHtml(comment: ScaladocComment) = comment.copy(text = unescapeHtml4(comment.text))

  /**
   * Generates test source code from scala source file.
   */
  def apply(srcFile: File, srcEncoding: String, testGen: TestGen, decodeHtmlEnabled: Boolean): Seq[TestSource] = {
    val src = Source.fromFile(srcFile, srcEncoding).mkString
    val basename = FilenameUtils.getBaseName(srcFile.getName)
    ScaladocExtractor.extract(src)
      .map(comment => if (decodeHtmlEnabled) decodeHtml(comment) else comment)
      .flatMap(comment => CommentParser(comment).right.toOption.filter(_.components.nonEmpty))
      .groupBy(_.pkg).map {
        case (pkg, examples) =>
          TestSource(pkg, basename, testGen.generate(basename, pkg, examples))
      }
      .toSeq
  }

  def findEncoding(scalacOptions: Seq[String]): Option[String] = scalacOptions match {
    case Seq() => None
    case Seq(_, tail @ _*) => scalacOptions.zip(tail).collectFirst { case ("-encoding", enc) => enc }
  }
}
