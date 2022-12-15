package com.northpole.santas

import cats.effect.IO

trait SantasAddressBook {
  def updateAddress(addressUpdate: AddressUpdate): IO[Unit]

  def getAddress(fullName: FullName): IO[Option[Address]]
}

object SantasInMemoryAddressBook {
  def default(): IO[SantasAddressBook] =
    IO.ref[Map[FullName, Address]](Map.empty).map { addressBookState =>
      new SantasAddressBook {
        override def updateAddress(addressUpdate: AddressUpdate): IO[Unit] =
          IO.println(s"Received address update for ${addressUpdate.fullName}") >> addressBookState
            .update(_ + (addressUpdate.fullName -> addressUpdate.address))

        override def getAddress(fullName: FullName): IO[Option[Address]] =
          addressBookState.get.map(_.get(fullName))
      }
    }
}
