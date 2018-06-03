package moe.jmcardon

import cats.effect.IO
import tsec.hashing.jca.SHA1

object ProofOfWorkCoin {
  def foldToTree(l: List[Transaction]): MerkleTree[SHA1] = ???
  def getTotalForUser(tree: MerkleTree[SHA1], amtStart: Int, user: User): IO[Int] = ???
}
