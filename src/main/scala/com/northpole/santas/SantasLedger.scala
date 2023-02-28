package com.northpole.santas

import cats.effect.IO
import cats.implicits._
import com.northpole.santas.ChristmasPresent._
import org.http4s.implicits.http4sLiteralsSyntax

trait SantasLedger {
  def addBehaviourReport(
      fullName: FullName,
      behaviourReport: BehaviourReport
  ): IO[Unit]
  def getPresent(fullName: FullName): IO[ChristmasPresent]
}

object SantasInMemoryLedger {
  def default(): IO[SantasLedger] =
    IO.ref[Map[FullName, Int]](Map.empty).map { ledgerState =>
      new SantasLedger {
        override def addBehaviourReport(
            fullName: FullName,
            behaviourReport: BehaviourReport
        ): IO[Unit] =
          IO.println(s"Received report for $fullName") >> ledgerState.update(
            _.updatedWith(fullName)(
              calculateUpdatedScore(_, behaviourReport.score).some
            )
          )

        private def calculateUpdatedScore(
            maybeOldScore: Option[Int],
            newScore: Int
        ): Int =
          maybeOldScore.fold(newScore)(oldScore => (oldScore + newScore) / 2)

        override def getPresent(fullName: FullName): IO[ChristmasPresent] = {
          val borderOfGoodness = 6
          ledgerState.get.map(ledger =>
            ledger.get(fullName) match {
              case Some(number) if number >= borderOfGoodness =>
                // Sorry we can't show you how we decide the present.
                Nice(uri"nice_present", number)
              case Some(number) => Coal(uri"coal", number)
              case None         => Nice(uri"average_present", borderOfGoodness)
            }
          )
        }
      }
    }

}
