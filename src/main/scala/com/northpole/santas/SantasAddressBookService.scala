package com.northpole.santas

import cats.effect.IO

trait SantasAddressBookService {
  def updateAddress(addressUpdate: AddressUpdate): IO[Unit]

  def getAddress(fullName: FullName): IO[Option[Address]]

  def getHouseholdList(address: Address): IO[List[FullName]]
}

object SantasInMemoryAddressBook {
  def default(): IO[SantasAddressBookService] =
    IO.ref[Map[FullName, Address]](Map.empty).map { addressBookState =>
      new SantasAddressBookService {
        override def updateAddress(addressUpdate: AddressUpdate): IO[Unit] =
          IO.println(s"Received address update for ${addressUpdate.fullName}") >> addressBookState
            .update(_ + (addressUpdate.fullName -> addressUpdate.address))

        override def getAddress(fullName: FullName): IO[Option[Address]] =
          addressBookState.get.map(_.get(fullName))

        override def getHouseholdList(address: Address): IO[List[FullName]] =
          addressBookState.get.map(_.toList.filter(naAd => naAd._2 == address).map(_._1))
      }
    }
}
