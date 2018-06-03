package moe.jmcardon

import tsec.common._
import tsec.hashing.jca.SHA1

case class BBSMessage(content: String, from: String) {
  def toLeaf: MerkleTree[SHA1] = {
    val leafContent = s"from=$from;content=$content".utf8Bytes
    Leaf(leafContent, SHA1.hashPure(leafContent))
  }
}