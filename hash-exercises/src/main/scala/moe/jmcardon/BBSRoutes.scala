package moe.jmcardon

import java.time.Instant

import cats.effect._
import cats.syntax.all._
import fs2.async.Ref
import fs2.async.mutable.{Queue, Semaphore}
import org.http4s._
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext

final class BBSRoutes(sem: Semaphore[IO],
                messages: Ref[IO, List[BBSMessage]],
                serverHardness: Ref[IO, Int],
                dogeChain: Ref[IO, DogeChain],
                lastblock: Ref[IO, Option[AlmostBlock]],
                blockingEc: ExecutionContext)
    extends Http4sDsl[IO] {

  val serviceRoutes: HttpService[IO] = HttpService[IO] {
    case POST -> Root / "tick" =>
      lastblock.get.flatMap {
        case None =>
          for {
            m <- messages.get
            tree <- IO.pure(ProofOFWorkBBS.foldToTree(m))
            instant <- IO(Instant.now())
            cond <- lastblock.tryModify {
              case None      => Some(AlmostBlock(tree, instant))
              case Some(lol) => Some(lol)
            }
            r <- if (cond.isDefined) Ok()
            else Conflict("Block was set by another request")
          } yield r

        case Some(_) => BadRequest("Last Block work is not done yet!")
      }

    case POST -> Root / "message" / from / message =>
      messages.modify(BBSMessage(message, from) :: _) >> Ok()

    case POST -> Root / "doWork" =>
      lastblock.get.flatMap {
        case None => BadRequest("Tick not called yet")
        case Some(block) =>
          sem.tryDecrement.flatMap {
            case false =>
              BadRequest("Processing underway")
            case true =>
              for {
                h <- serverHardness.get
                _ <- (Async.shift[IO](blockingEc) >> workAndAdd(block, h)).start
              } yield Response[IO](Ok)
          }

      }
  }

  def workAndAdd(block: AlmostBlock, hardness: Int) = {
    IO.pure(ProofOFWorkBBS.work(block, hardness)).flatMap { b =>
      IO(println("Completed Block! $b")) >> dogeChain.modify(_.prepend(b))
    }
  }

}
