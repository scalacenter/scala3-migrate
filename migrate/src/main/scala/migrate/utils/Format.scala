package migrate.utils

object Format {
  def plural(count: Int, name: String): String =
    plural(count, name, name + "s")

  def plural(count: Int, name: String, pluralName: String): String =
    if (count < 2) s"$count $name"
    else s"$count $pluralName"
}
