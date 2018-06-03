package moe.jmcardon

import tsec.common.ByteUtils
import tsec.hashing.jca.SHA1

object ProofOFWorkBBS {
  def foldToTree(l: List[BBSMessage]): MerkleTree[SHA1] = ???
  def work(m: AlmostBlock, hardness: Int): Block = ???
}
