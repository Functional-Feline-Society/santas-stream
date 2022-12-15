package com.northpole.santas

import io.circe.generic.semiauto.deriveCodec
import io.circe
import org.http4s.circe._
import org.http4s.Uri
import vulcan.generic._

case class FullName(firstName: String, lastName: String)

object FullName {
  implicit val jsonCodec = deriveCodec[FullName]
  implicit val avroCodec = vulcan.Codec.derive[FullName]
}

case class ChristmasConsignment(
    fullName: FullName,
    address: Option[Address],
    present: ChristmasPresent
)

object ChristmasConsignment {
  implicit val jsonCodec: circe.Codec[ChristmasConsignment] = deriveCodec
}

sealed trait ChristmasPresent

object ChristmasPresent {
  case class Nice(gift: Uri, niceScore: Int) extends ChristmasPresent

  case class Coal(aPhotoOfCoal: Uri, naughtyScore: Int) extends ChristmasPresent

  implicit val jsonCodec: circe.Codec[ChristmasPresent] = deriveCodec
}

case class Address(address: String)
object Address {
  implicit val jsonCodec: circe.Codec[Address] = deriveCodec

  implicit val addressUpdateCodec = vulcan.Codec.derive[Address]
}

case class AddressUpdate(fullName: FullName, address: Address)

object AddressUpdate {
  implicit val jsonCodec: circe.Codec[AddressUpdate] = deriveCodec

  implicit val addressUpdateCodec = vulcan.Codec.derive[AddressUpdate]
}

case class BehaviourReport(score: Int)

object BehaviourReport {
  implicit val avroCodec = vulcan.Codec.derive[BehaviourReport]
}
