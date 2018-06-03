package moe.jmcardon

import tsec.signature.jca.{SHA256withECDSA, SigKeyPair}

case class User(name: String, totalCoin: Int, keypair: SigKeyPair[SHA256withECDSA])

