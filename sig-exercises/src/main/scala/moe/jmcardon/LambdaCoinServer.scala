package moe.jmcardon

import java.util.concurrent.Executors

import cats.effect.IO
import fs2._
import fs2.async.Ref
import fs2.async.mutable.Semaphore
import org.http4s.server.blaze.BlazeBuilder
import tsec.signature.jca.SHA256withECDSA

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext.{fromExecutorService => fec}

object LambdaCoinServer extends StreamApp[IO] {
  def stream(args: List[String],
             requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    for {
      hardness <- Stream.eval(Ref[IO, Int](1))
      dogeChain <- Stream.eval(Ref[IO, DogeChain](DogeChain(Nil)))
      lastBlock <- Stream.eval(Ref[IO, Option[AlmostBlock]](None))
      messages <- Stream.eval(Ref[IO, List[Transaction]](Nil))
      blockingEc <- Stream.bracket(IO(fec(Executors.newFixedThreadPool(2))))(
        Stream.emit(_),
        ex => IO(ex.shutdown()))
      sem <- Stream.eval(Semaphore[IO](1)(implicitly, global))
      kpJose <- Stream.eval(SHA256withECDSA.generateKeyPair[IO])
      kpHoseB <- Stream.eval(SHA256withECDSA.generateKeyPair[IO])
      jose = User("jose", 110, kpJose)
      hoseB = User("hoseB", 55, kpHoseB)
      service = new CoinRoutes(sem,
                               messages,
                               hardness,
                               dogeChain,
                               lastBlock,
                               jose,
                               hoseB,
                               blockingEc)
      exitCode <- BlazeBuilder[IO]
        .withWebSockets(false)
        .mountService(service.serviceRoutes)
        .serve(implicitly, global)
    } yield exitCode
  }
}
