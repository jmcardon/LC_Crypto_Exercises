package moe.jmcardon

import java.time.Instant

import cats.effect.IO
import tsec.common._
import tsec.jws.JWSSerializer
import tsec.jws.mac.{JWSMacHeader, JWTMac}
import tsec.jwt.JWTClaims
import tsec.jwt.algorithms.JWA.HS256
import tsec.mac.MAC
import tsec.mac.jca._

case class SubsetJWT(claims: JWTClaims) {

  def check(k: MacSigningKey[HMACSHA256]): IO[JWTMac[HMACSHA256]] = {
    val toSign = implicitly[JWSSerializer[JWSMacHeader[HMACSHA256]]]
      .toB64URL(JWSMacHeader[HMACSHA256]) + "." + JWTClaims.toB64URL(claims)
    calculate(toSign, k).flatMap(
      mac =>
        JWTMac.verifyAndParse[IO, HMACSHA256](toSign + "." + mac.toB64UrlString,
                                              k))
  }

  def calculate(toSign: String,
                k: MacSigningKey[HMACSHA256]): IO[MAC[HMACSHA256]] = ???

}

object JWTAuth {

  def main(args: Array[String]): Unit = {
    val program = for {
      k <- HMACSHA256.generateKey[IO]
      expiration <- IO(Instant.now.plusSeconds(20))
      subset = SubsetJWT(JWTClaims(expiration = Some(expiration)))
      parsed <- subset.check(k)
    } yield parsed.body.expiration.exists(e => e.equals(expiration) || e.isBefore(expiration))

    println(s"Result: ${program.unsafeRunSync()}")
  }
}
