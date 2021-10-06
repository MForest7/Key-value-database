import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

object HashBasedBase {
    private val hashTable = RandomAccessFile(File("./kvdbData/hashLinks.txt"), "rw")
    private val keyLists = RandomAccessFile(File("./kvdbData/keyLists.txt"), "rw")
    private val values = RandomAccessFile(File("./kvdbData/values.txt"), "rw")
    private val info = File("./kvdbData/info.txt")

    var hashMod = info.readText(Charset.defaultCharset()).drop(8).toInt()
    val linkLength = 14L

    fun get(key: String): String? {
        val hash = key.hashCode() % hashMod
        //println(hash)
        hashTable.seek(hash * (linkLength + 1))
        val beginOfKeyList = hashTable.readLine().toLong()
        //println(beginOfKeyList)
        if (beginOfKeyList == 0L) return null

        keyLists.seek(beginOfKeyList)
        var linkToValue = 0L
        while (true) {
            val (keyFromBase, linkValueStr, linkUpStr) = keyLists.readLine().split(" ")
            if (keyFromBase == key) {
                linkToValue = linkValueStr.toLong()
                break
            }
            if (linkUpStr.toLong() == 0L) return null
            keyLists.seek(linkUpStr.toLong())
        }

        values.seek(linkToValue)
        val (deleted, value) = values.readLine().split(" ")
        return if (deleted == "1")
            null
        else
            value
    }

    fun add(key: String, value: String): Boolean {
        if (get(key) != null) return false

        val hash = key.hashCode() % hashMod
        hashTable.seek(hash * (linkLength + 1))
        val linkToKeyList = hashTable.readLine().toLong()
        hashTable.seek(hash * (linkLength + 1))
        hashTable.writeBytes("${padLong(keyLists.length())}\n")

        val linkToValue = values.length()
        keyLists.seek(keyLists.length())
        keyLists.writeBytes("$key $linkToValue $linkToKeyList\n")

        values.seek(values.length())
        values.writeBytes("0 $value\n")

        return true
    }

    fun delete(key: String): Boolean {
        val hash = key.hashCode() % hashMod
        //println(hash)
        hashTable.seek(hash * (linkLength + 1))
        val beginOfKeyList = hashTable.readLine().toLong()
        //println(beginOfKeyList)
        if (beginOfKeyList == 0L) return false

        keyLists.seek(beginOfKeyList)
        var linkToValue = 0L
        while (true) {
            val (keyFromBase, linkValueStr, linkUpStr) = keyLists.readLine().split(" ")
            if (keyFromBase == key) {
                linkToValue = linkValueStr.toLong()
                break
            }
            if (linkUpStr.toLong() == 0L) return false
            keyLists.seek(linkUpStr.toLong())
        }

        values.seek(linkToValue)
        val (deleted, value) = values.readLine().split(" ")
        return if (deleted == "1") {
            false
        } else {
            values.seek(linkToValue)
            values.writeBytes("1")
            true
        }
    }

    fun replace(key: String, value: String) {
        delete(key)
        add(key, value)
    }

    fun expand() {
        val newMod = hashMod * 2

        val tempCopy = File("copiedHash.txt")
        val hashWriter = RandomAccessFile(tempCopy, "rw")

        repeat(hashMod) {
            hashWriter.writeBytes(hashTable.readLine())
            hashWriter.writeBytes("\n")
        }

        hashTable.setLength(0)
        hashTable.seek(0)
        repeat(newMod) {
            hashTable.writeBytes(padLong(0))
            hashTable.writeBytes("\n")
        }

        for (hash in 0 until hashMod) {
            hashTable.seek(hash * (linkLength + 1))

            val keys: MutableList<String> = mutableListOf()
            var linkToNextKey = hashTable.readLine().toLong()
            while (linkToNextKey != 0L) {
                keyLists.seek(linkToNextKey)
                val (key, linkValueStr, linkUpStr) = keyLists.readLine().split(" ")
                keys.add(key)
                linkToNextKey = linkUpStr.toLong()
            }

            for (key in keys) {
                val linkToKeyList = hashTable.readLine().toLong()
                hashTable.seek(hash * (linkLength + 1))
                hashTable.writeBytes("${padLong(keyLists.length())}\n")

                val linkToValue = values.length()
                keyLists.seek(keyLists.length())
                keyLists.writeBytes("$key $linkToValue $linkToKeyList\n")
            }
        }


    }

    private fun padLong(x: Long): String = x.toString().padStart(linkLength.toInt(), '0')
}