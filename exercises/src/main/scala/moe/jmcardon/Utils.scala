package moe.jmcardon

object Utils {

  def xor(arr1: Array[Byte], arr2: Array[Byte]): Array[Byte] = {
    val len = math.min(arr1.length, arr2.length)
    val newArr = new Array[Byte](len)
    var i = 0
    while(i < len) {
      newArr(i) = (arr1(i) ^ arr2(i)).toByte
      i += 1
    }
    newArr
  }

}
