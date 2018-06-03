package moe.jmcardon

import java.util

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import tsec.cipher.symmetric.{Iv, PlainText, RawCipherText}
import tsec.common._
import tsec.cipher.symmetric.jca._
import Utils._

object SymmetricCrypto {

  val AESBlockSize = 16

  private def encryptBlock(pt: Array[Byte],
                           key: Array[Byte],
                           ctbuf: Array[Byte]): Unit = {
    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
    val kSpec = new SecretKeySpec(key, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, kSpec)
    cipher.doFinal(pt, 0, pt.length, ctbuf)
  }

  def ctr(pt: PlainText, key: Array[Byte], iv: Iv[AES128CTR]): Array[Byte] = {
    def xorWithPT(ctBuf: Array[Byte], offset: Int, len: Int): Unit = {
      var i = 0
      while (i < len) {
        ctBuf(i) = (pt(offset + i) ^ ctBuf(i)).toByte
        i += 1
      }
    }
    //Mutato
    def ctrToIv(mutIV: Array[Byte], ctr: Long): Unit = {
      val bytes = ByteUtils.longToBytes(ctr)
      var i = bytes.length - 1
      var j = mutIV.length - 1
      while (i >= 0) {
        mutIV(j) = bytes(i)
        j -= 1
        i -= 1
      }
    }

    def encryptStep(curr: Int,
                    mutIv: Array[Byte],
                    counter: Long,
                    ct: Array[Byte],
                    ctBuf: Array[Byte]): Array[Byte] = {
      if (curr == pt.length) ct
      else {
        ctrToIv(mutIv, counter)
        encryptBlock(mutIv, key, ctBuf)
        val remaining = math.min(ctBuf.length, pt.length - curr)
        xorWithPT(ctBuf, curr, remaining)
        System.arraycopy(ctBuf, 0, ct, curr, remaining)
        encryptStep(curr + remaining, mutIv, counter + 1, ct, ctBuf)
      }
    }
    val counter: Long = ByteUtils.unsafeBytesToLong(iv.drop(8))
    val ivbuf = new Array[Byte](iv.length)
    encryptStep(0,
                util.Arrays.copyOf(iv, iv.length),
                counter,
                new Array[Byte](pt.length),
                ivbuf)
  }

  def printDecrypted(bytes: Array[Byte]): Unit = println(bytes.toUtf8String)

  val keyBytes: Array[Byte] = "1cc36897604df72b74a79f463805d043".hexBytesUnsafe

  val ToDecipher1: String =
    "a925e7712ac76f3051190eb22d3cd153173738a0eee0c8da45d15f551a2c54b9263f85f51f19f7ca"
  val ToDecipher2: String =
    "a86caf5219820926520702f3373686121a297df7f2e3d1"
  val ToDecipher3: String =
    "a223a95a1b865d2a511356fb2a3dd553192b7de4f2eccf8354c95a1b196349bf286cd0ba371dfad287dbff4f0195505696"
  val StaticIv: Iv[AES128CTR] =
    Iv[AES128CTR](Array.fill[Byte](AES128CTR.blockSizeBytes)(0))

  def encryptPT(s: String) = ctr(PlainText(s.utf8Bytes), keyBytes, StaticIv)

  def main(args: Array[String]): Unit = {
    //The objective here is to use our "CTR" function as such:
    //Encrypt one plaintext of your choice, and then user it
    // Plaintexts are utf8 encoded, so use .utf8Bytes to get String => Array[Byte]
    // and .toUtf8String to get Array[Byte] => String
    val myPlainText = ???


  }

}
