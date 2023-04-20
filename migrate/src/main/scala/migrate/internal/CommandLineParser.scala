package scala.cmd

object CommandLineParser {
  def tokenize(line: String): List[String] = scala.sys.process.Parser.tokenize(line)
}
