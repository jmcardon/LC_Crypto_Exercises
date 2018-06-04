package moe.jmcardon

import tsec.common._
import tsec.hashing.jca.SHA1


object ProofOFWorkBBS {
  def foldToTree(l: List[BBSMessage]): MerkleTree[SHA1] = {
    def foldRec(m: List[MerkleTree[SHA1]]): List[MerkleTree[SHA1]] = m match {
      case x::y::rest =>
        Node(x, y, SHA1.hashPure(x.hash ++ y.hash))::foldRec(rest)
      case x::Nil =>
        List(Node(x, x, SHA1.hashPure(x.hash ++ x.hash)))
      case Nil => Nil
    }

    def extractTree(m: List[MerkleTree[SHA1]]): MerkleTree[SHA1] = m match {
      case x::Nil => x
      case _ => extractTree(foldRec(m))
    }

    extractTree(l.map(_.toLeaf))
  }
  def work(m: AlmostBlock, hardness: Int): Block = {
    def checkUntil(n: Int): Block = {
      val hash = SHA1.hashPure(m.messageTree.hash ++ m.timestamp.toEpochMilli.toBytes ++ ByteUtils.intToBytes(n))
      if(hash.take(hardness).toList == List.fill[Byte](hardness)(0))
        Block(n, m.messageTree, m.timestamp)
      else checkUntil(n+1)
    }
    checkUntil(0)
  }
}
