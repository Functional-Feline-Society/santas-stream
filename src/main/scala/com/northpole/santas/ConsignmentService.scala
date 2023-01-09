package com.northpole.santas

import cats.effect.IO
import cats.implicits.catsSyntaxTuple2Semigroupal

trait ConsignmentService {
  def getConsignment(fullName: FullName): IO[ChristmasConsignment]
}

object ConsignmentService {
  def apply(
      addressBook: SantasAddressBookService,
      ledger: SantasLedger
  ): ConsignmentService = new ConsignmentService {
    override def getConsignment(fullName: FullName): IO[ChristmasConsignment] =
      (addressBook.getAddress(fullName), ledger.getPresent(fullName))
        .mapN(ChristmasConsignment(fullName, _, _))
  }
}
