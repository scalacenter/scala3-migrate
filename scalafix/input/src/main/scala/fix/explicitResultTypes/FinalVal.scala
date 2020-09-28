/*
rules = Infertypes
*/
package fix.explicitResultTypes

// Should not infer types for ConstantTypes
object FinalVal {
  final val a = "hello"
  final val b = a
}
