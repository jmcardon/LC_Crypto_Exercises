package moe.jmcardon

import tsec.hashing._

sealed trait MerkleTree[A] extends Product with Serializable {
  def hash: CryptoHash[A]
}
case class Node[A](left: MerkleTree[A], right: MerkleTree[A], hash: CryptoHash[A]) extends MerkleTree[A]
case class Leaf[A](content: Array[Byte], hash: CryptoHash[A]) extends MerkleTree[A]
