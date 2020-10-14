object Test2 {
  implicit val number: Int = 5
  implicit val crazy1: Int = implicitly[Int](number)
}
