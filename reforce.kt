package apkbind.reforce

import java.io.File
import java.lang.Exception
import java.security.MessageDigest
import java.util.zip.Adler32

class reforce{
    init{
        val PACKER_DEX_FILE = "src/classes.dex"
        val SRC_APK_FILE = "src/doublewings.apk"
        val PACKED_DEX_FILE = "src/new.dex"
    }

    fun Int.toByteArray(): ByteArray{
        val tmp = byteArrayOf(0,0,0,0)
        tmp[0] = this.and(0xff).toByte()
        tmp[1] = this.and(0xff00).ushr(8).toByte()
        tmp[2] = this.and(0xff0000).ushr(16).toByte()
        tmp[3] = this.ushr(24).toByte()
        return tmp
    }
    fun main(args: Array<String>){
        try{
            val dumpDex = File(PACKER_DEX_FILE)
            val oriApk = File(SRC_APK_FILE)

            val payloadApkBytes = oriApk.readBytes()//this function has a 2GB size limit
            val dumpDexBytes = dumpDex.readBytes()

            val apkSize = payloadApkBytes.size
            val dumpDexSize = dumpDexBytes.size
            val finalSize = apkSize + dumpDexSize + 4

            var newDex = ByteArray(finalSize)

            System.arraycopy(dumpDexBytes,0,newDex,0,dumpDexSize)
            System.arraycopy(payloadApkBytes,0,newDex,dumpDexSize,apkSize)
            System.arraycopy(
                apkSize.toByteArray(),
                0,newDex,
                dumpDexSize+apkSize,
                4)

            newDex = fixFileSizeHeader(newDex,finalSize)
            newDex = fixSignature(newDex)
            newDex = fixChecksum(newDex)

            val newDexFile = File(PACKED_DEX_FILE)
            newDexFile.createNewFile()
            newDexFile.writeBytes(newDex)
        }catch (e: Exception){
            println(e.toString())
            e.printStackTrace()
        }
    }

    fun fixFileSizeHeader(newDex: ByteArray,fixSize: Int): ByteArray{
        System.arraycopy(fixSize.toByteArray(),0,newDex,32,4)
        return newDex
    }

    fun fixSignature(newDex: ByteArray): ByteArray{
        val tmpSig: ByteArray = MessageDigest.getInstance("SHA-1").digest(newDex.copyOfRange(32,newDex.size))
        System.arraycopy(tmpSig,0,newDex,12,20)
        return newDex
    }

    fun fixChecksum(newDex: ByteArray): ByteArray{
        val adler32 = Adler32()
        adler32.reset()
        adler32.update(newDex,12,newDex.size-12)
        val tmpChecksum = adler32.value.toInt()
        System.arraycopy(tmpChecksum.toByteArray(),0,newDex,8,4)
        return newDex
    }
}


fun main(){
    val tmp = reforce().main(arrayOf())
}