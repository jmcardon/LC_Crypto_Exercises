package moe.jmcardon

import java.time.Instant

import cats.Traverse
import cats.effect._
import cats.syntax.all._
import cats.instances.list._
import fs2.async.Ref
import fs2.async.mutable.{Queue, Semaphore}
import org.http4s._
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext

class CoinRoutes(sem: Semaphore[IO],
                 messages: Ref[IO, List[Transaction]],
                 serverHardness: Ref[IO, Int],
                 dogeChain: Ref[IO, DogeChain],
                 lastblock: Ref[IO, Option[AlmostBlock]],
                 jose: User,
                 hoseB: User,
                 blockingEc: ExecutionContext)
    extends Http4sDsl[IO] {

  def joseToHoseB(i: Int): IO[Transaction] = {
    require(i > 0)
    Transaction(jose.name, hoseB.name, i, jose.keypair.privateKey)
  }

  def hoseBToJose(i: Int): IO[Transaction] = {
    require(i > 0)
    Transaction(hoseB.name, jose.name, i, hoseB.keypair.privateKey)
  }

  val serviceRoutes: HttpService[IO] = HttpService[IO] {
    case POST -> Root / "tick" =>
      lastblock.get.flatMap {
        case None =>
          for {
            m <- messages.get
            tree <- IO.pure(ProofOfWorkCoin.foldToTree(m))
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

    case POST -> Root / runTransactions =>
      Traverse[List]
        .sequence(List(hoseBToJose(20), hoseBToJose(4), joseToHoseB(3)))
        .flatMap { l =>
          messages.setSync(l) >> Ok()
        }

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

  def workAndAdd(block: AlmostBlock, hardness: Int): IO[Unit] = {
    IO.pure(ProofOFWorkBBS.work(block, hardness)).flatMap { b =>
      IO(println("Completed Block! $b")) >> dogeChain.modify(_.prepend(b)) >> {
        IO(println(s"TOTAL FOR JOSE: ${ProofOfWorkCoin
          .getTotalForUser(b.messageTree, jose.totalCoin, jose)}")) >>
          IO(println(s"TOTAL FOR HoseB: ${ProofOfWorkCoin
            .getTotalForUser(b.messageTree, hoseB.totalCoin, hoseB)}"))
      }
    }
  }

}
