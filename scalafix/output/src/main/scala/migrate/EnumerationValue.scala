package migrate

object EnumerationValue {
  object Day extends Enumeration {
    type Day = Value
    val Weekday, Weekend = Value
  }
  object Bool extends Enumeration {
    type Bool = Value
    val True, False = Value
  }
  import Bool._
  def day(d: Day.Value): Unit = ???
  val d: migrate.EnumerationValue.Day.Value =
    if (true) Day.Weekday
    else Day.Weekend
  day(d)
  val b: migrate.EnumerationValue.Bool.Value =
    if (true) True
    else False
}