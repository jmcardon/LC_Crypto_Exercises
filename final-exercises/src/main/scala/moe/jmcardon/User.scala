package moe.jmcardon

import java.util.UUID

import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import tsec.signature.jca.{SHA256withECDSA, SigKeyPair}

case class User(id: UUID,
                name: String)

case class AuthedUser(user: User, keyPair: SigKeyPair[SHA256withECDSA], hash: PasswordHash[SCrypt])