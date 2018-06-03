package moe.jmcardon

import cats.Id
import cats.effect.IO
import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._
import tsec.common._
import tsec.hashing.CryptoHash
import tsec.hashing.jca.SHA1
import tsec.signature.CryptoSignature
import tsec.signature.jca.{SHA256withECDSA, SigPrivateKey, SigPublicKey}

sealed abstract case class Transaction(from: String,
                                       to: String,
                                       quantity: Int,
                                       sig: CryptoSignature[SHA256withECDSA]) {
  import Transaction._
  def hash: CryptoHash[SHA1] = SHA1.hashPure(tEncoder(this).toString().utf8Bytes)
  def verify(pub: SigPublicKey[SHA256withECDSA]): IO[Boolean] =
    SHA256withECDSA
      .verifyBool[IO]((from + to).utf8Bytes ++ quantity.toBytes, sig, pub)
}

object Transaction {
  def apply(from: String,
            to: String,
            quantity: Int,
            sigPrivateKey: SigPrivateKey[SHA256withECDSA]): IO[Transaction] =
    signBody(from, to, quantity, sigPrivateKey).map(
      new Transaction(from, to, quantity, _) {})

  implicit val csencoder: Encoder[CryptoSignature[SHA256withECDSA]] =
    new Encoder[CryptoSignature[SHA256withECDSA]] {
      def apply(a: CryptoSignature[SHA256withECDSA]): Json =
        Json.fromString(a.toHexString)
    }

  implicit val csdecoder = new Decoder[CryptoSignature[SHA256withECDSA]] {
    def apply(c: HCursor): Result[CryptoSignature[SHA256withECDSA]] =
      c.as[String].map(c => CryptoSignature[SHA256withECDSA](c.hexBytesUnsafe))
  }

  implicit val tEncoder = new Encoder[Transaction] {
    def apply(a: Transaction): Json =
      Json.obj("from" -> a.from.asJson,
               "to" -> a.to.asJson,
               "quantity" -> a.quantity.asJson,
               "sig" -> a.sig.asJson)
  }
  implicit val tDecoder = new Decoder[Transaction] {
    def apply(c: HCursor): Result[Transaction] =
      for {
        from <- c.downField("from").as[String]
        to <- c.downField("to").as[String]
        quantity <- c.downField("quantity").as[Int]
        sig <- c.downField("sig").as[CryptoSignature[SHA256withECDSA]]
      } yield new Transaction(from, to, quantity, sig) {}
  }

  def signBody(
      from: String,
      to: String,
      quantity: Int,
      key: SigPrivateKey[SHA256withECDSA]): IO[CryptoSignature[SHA256withECDSA]] = {
    SHA256withECDSA.sign[IO]((from + to).utf8Bytes ++ quantity.toBytes, key)
  }

}
