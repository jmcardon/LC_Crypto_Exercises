package moe.jmcardon

import java.time.Instant

import cats.effect.IO
import cats.syntax.flatMap._
import tsec.common._
import tsec.hashing.jca.SHA1

case class AlmostBlock(messageTree: MerkleTree[SHA1], timestamp: Instant) {
  def toHashBytes: Array[Byte] = messageTree match {
    case Node(_, _, hash) => hash ++ timestamp.getEpochSecond.toBytes
    case _                => throw new IllegalArgumentException("Invalid Tree construction")
  }
}

case class Block(nonce: Int,
                 messageTree: MerkleTree[SHA1],
                 timestamp: Instant) {
  def printMessages: IO[Unit] = {
    def recursePrint(t: MerkleTree[SHA1]): IO[Unit] = t match {
      case Leaf(content, hash) =>
        IO(println(s"content=${content.toUtf8String}")) >> IO(
          println(s"Hash(hex): ${hash.toHexString}"))
      case Node(l, r, hash) =>
        IO(println(s"Node hash: ${hash.toHexString}")) >> IO.suspend(
          recursePrint(l)) >> IO.suspend(recursePrint(r))
    }

    recursePrint(messageTree)
  }
}

case class DogeChain(chain: List[Block]) {
  def prepend(b: Block) = DogeChain(b :: chain)
}
