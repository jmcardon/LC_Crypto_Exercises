package moe.jmcardon

import java.util.UUID

import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all._
import fs2.async.Ref
import tsec.authentication.BackingStore

import scala.util.control.NoStackTrace

class DummyStore(internal: Ref[IO, Map[UUID, AuthedUser]])
    extends BackingStore[IO, UUID, AuthedUser] {
  import DummyStore._
  def put(elem: AuthedUser): IO[AuthedUser] =
    internal
      .modify2[IO[Unit]] { m =>
        if (m.contains(elem.user.id))
          (m, IO.raiseError(UserAlreadyExists))
        else
          (m.updated(elem.user.id, elem), IO.unit)
      }
      .flatMap {
        case (_, action) => action >> IO.pure(elem)
      }

  def update(v: AuthedUser): IO[AuthedUser] = {
    internal.modify(_.updated(v.user.id, v)).map(_ => v)
  }

  def delete(id: UUID): IO[Unit] = internal.modify(_ - id).void

  def get(id: UUID): OptionT[IO, AuthedUser] =
    OptionT(internal.get.map(_.get(id)))
}

object DummyStore {
  case object UserAlreadyExists extends Exception with NoStackTrace
}
