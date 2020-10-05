package fix

import scalafix.v1.SemanticTree

sealed trait Place extends Product with Serializable

object Place {

  case object Right extends Place
  case object Left extends Place
  case class Middle(pos: Int) extends Place
  case object NoWhere extends Place


  def where(char: Char = '*', in: SemanticTree): Place =
      if (in.toString.startsWith("*")) Left
      else if (in.toString.endsWith("*")) Right
      else {
        if (in.toString().contains("(*)")){
          val position = in.toString().zipWithIndex.toMap.get('*').get
          Middle(position)
        }
        else NoWhere
      }

}