import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

object HashBasedBase {
    private val hashTable = RandomAccessFile(File("./kvdbData/hashLinks.txt"), "rw")
    private val keyLists = RandomAccessFile(File("./kvdbData/keyLists.txt"), "rw")
    private val values = RandomAccessFile(File("./kvdbData/values.txt"), "rw")
    private val readInfo = File("./kvdbData/info.txt")
    private val readSize = File("./kvdbData/size.txt")
    private val readUnused = File("./kvdbData/unused.txt")

    var size = readSize.readText(Charset.defaultCharset()).drop(5).toInt()
    private var hashMod = readInfo.readText(Charset.defaultCharset()).drop(8).toInt()
    private var unused = readUnused.readText(Charset.defaultCharset()).drop(7).toInt()
    private const val limit = 25
    private const val linkLength = 14L

    fun get(key: String): String? {
        val beginOfKeyList = getKeyList(key)
        if (beginOfKeyList == 0L) return null

        keyLists.seek(beginOfKeyList)
        val linkToValue = getKeyFromList(key, beginOfKeyList)
        if (linkToValue == null) return null

        values.seek(linkToValue)
        val (deleted, value) = values.readLine().split(" ")
        return if (deleted == "1")
            null
        else
            value
    }

    fun add(key: String, value: String): Boolean {
        if (get(key) != null) return false

        val hash = getHash(key, hashMod)
        val linkToKeyList = getKeyList(key)
        hashTable.seek(hash * (linkLength + 1))
        hashTable.writeBytes("${padLong(keyLists.length())}\n")

        val linkToValue = values.length()
        keyLists.seek(keyLists.length())
        keyLists.writeBytes("$key $linkToValue $linkToKeyList\n")

        values.seek(values.length())
        values.writeBytes("0 $value\n")

        setNewSize(size + 1)
        if (size / hashMod > limit) {
            garbageClear()
            expand()
        }

        return true
    }

    fun delete(key: String): Boolean {
        val beginOfKeyList = getKeyList(key)
        if (beginOfKeyList == 0L) return false

        keyLists.seek(beginOfKeyList)
        val linkToValue = getKeyFromList(key, beginOfKeyList)
        if (linkToValue == null) return false

        values.seek(linkToValue)
        val (deleted, value) = values.readLine().split(" ")
        return if (deleted == "1") {
            false
        } else {
            values.seek(linkToValue)
            values.writeBytes("1")
            setNewUnused(unused + 1)
            if (unused > size / 2) garbageClear()
            true
        }
    }

    fun replace(key: String, value: String) {
        delete(key)
        add(key, value)
    }

    private data class keyNode(val key: String, val linkValue: Long, val linkUp: Long)

    fun expand() {
        val newMod = hashMod * 2
        val hashTableCopy = RandomAccessFile(File("./kvdbData/copiedHashLinks.txt"), "rw")
        val keyListsCopy = RandomAccessFile(File("./kvdbData/copiedKeyLists.txt"), "rw")
        
        setHashTableCopy(newMod)
        setKeyListsCopy()

        for (hash in 0 until hashMod) {
            hashTable.seek(hash * (linkLength + 1))
            val linkToKeyList = hashTable.readLine().toLong()
            if (linkToKeyList == 0L) continue

            val keys = getListKeysFrom(linkToKeyList)

            for ((key, linkToValue, linkUp) in keys) {
                val newHash = getHash(key, newMod)
                val linkToKey = keyListsCopy.length()
                hashTableCopy.seek(newHash * (linkLength + 1))
                val linkParent = hashTableCopy.readLine().toLong()
                hashTableCopy.seek(newHash * (linkLength + 1))
                hashTableCopy.writeBytes("${padLong(linkToKey)}\n")

                keyListsCopy.seek(keyListsCopy.length())
                keyListsCopy.writeBytes("$key $linkToValue $linkParent\n")
            }
        }

        copyTo("./kvdbData/copiedHashLinks.txt", "./kvdbData/hashLinks.txt")
        copyTo("./kvdbData/copiedKeyLists.txt", "./kvdbData/keyLists.txt")
        setNewMod(newMod)
    }

    fun garbageClear() {
        val valuesCopy = RandomAccessFile(File("./kvdbData/copiedValues.txt"), "rw")
        val hashTableCopy = RandomAccessFile(File("./kvdbData/copiedHashLinks.txt"), "rw")
        val keyListsCopy = RandomAccessFile(File("./kvdbData/copiedKeyLists.txt"), "rw")

        setHashTableCopy(hashMod)
        setKeyListsCopy()
        setValuesCopy()

        for (hash in 0 until hashMod) {
            hashTable.seek(hash * (linkLength + 1))
            val linkToKeyList = hashTable.readLine().toLong()
            val keys = uniqueKeys(getListKeysFrom(linkToKeyList))

            var linkParent = 0L
            for ((key, linkValue, linkUp) in keys) {
                values.seek(linkValue)
                values.seek(linkValue)
                if (values.readLine()[0] == '1')
                    continue

                val newHash = getHash(key, hashMod)
                val linkToKey = keyListsCopy.length()
                hashTableCopy.seek(newHash * (linkLength + 1))
                hashTableCopy.writeBytes("${padLong(linkToKey)}\n")

                keyListsCopy.seek(keyListsCopy.length())
                keyListsCopy.writeBytes("$key ${valuesCopy.length()} $linkParent\n")
                linkParent = linkToKey

                valuesCopy.seek(valuesCopy.length())
                values.seek(linkValue)
                valuesCopy.writeBytes("${values.readLine()}\n")
            }
        }

        copyTo("./kvdbData/copiedHashLinks.txt", "./kvdbData/hashLinks.txt")
        copyTo("./kvdbData/copiedKeyLists.txt", "./kvdbData/keyLists.txt")
        copyTo("./kvdbData/copiedValues.txt", "./kvdbData/values.txt")

        setNewSize(size - unused)
        setNewUnused(0)
    }

    fun reset() {
        setNewMod(4)
        setNewSize(0)
        setNewUnused(0)

        setHashTableCopy(hashMod)
        setKeyListsCopy()
        setValuesCopy()

        copyTo("./kvdbData/copiedHashLinks.txt", "./kvdbData/hashLinks.txt")
        copyTo("./kvdbData/copiedKeyLists.txt", "./kvdbData/keyLists.txt")
        copyTo("./kvdbData/copiedValues.txt", "./kvdbData/values.txt")
    }

    private fun getKeyList(key: String): Long {
        val hash = getHash(key, hashMod)
        hashTable.seek(hash * (linkLength + 1))
        return hashTable.readLine().toLong()
    }

    private fun getKeyFromList(key: String, link: Long): Long? {
        keyLists.seek(link)
        while (true) {
            val (keyFromBase, linkValueStr, linkUpStr) = keyLists.readLine().split(" ")
            if (keyFromBase == key) return linkValueStr.toLong()
            if (linkUpStr.toLong() == 0L) return null
            keyLists.seek(linkUpStr.toLong())
        }
    }

    private fun getHash(key: String, mod: Int): Int {
        val hash = key.hashCode() % mod
        return if (hash < 0)
            hash + mod
        else
            hash
    }

    private fun padLong(x: Long): String = x.toString().padStart(linkLength.toInt(), '0')

    private fun setNewMod(newMod: Int) {
        hashMod = newMod
        val writer = RandomAccessFile(readInfo, "rw")
        writer.writeBytes("hashMod=$hashMod")
    }

    private fun setNewSize(newSize: Int) {
        size = newSize
        val writer = RandomAccessFile(readSize, "rw")
        writer.seek(5)
        writer.setLength(5)
        writer.writeBytes(size.toString())
    }

    private fun setNewUnused(newUnused: Int) {
        unused = newUnused
        val writer = RandomAccessFile(readUnused, "rw")
        writer.seek(7)
        writer.setLength(7)
        writer.writeBytes(unused.toString())
    }

    private fun copyTo(fileName: String, copyName: String): RandomAccessFile {
        val copy = File(copyName)
        val file = File(fileName)
        val reader = RandomAccessFile(file, "r")
        val writer = RandomAccessFile(copy, "rw")

        writer.setLength(0)
        writer.seek(0)
        while (reader.filePointer != reader.length()) {
            writer.writeBytes(reader.readLine())
            writer.writeBytes("\n")
        }
        return writer
    }

    private fun setHashTableCopy(newMod: Int) {
        val file = RandomAccessFile(File("./kvdbData/copiedHashLinks.txt"), "rw")
        file.setLength(0)
        file.seek(0)
        repeat(newMod) { file.writeBytes("${padLong(0)}\n") }
    }

    private fun setKeyListsCopy() {
        val file = RandomAccessFile(File("./kvdbData/copiedKeyLists.txt"), "rw")
        file.setLength(0)
        file.seek(0)
        file.writeBytes("key linkValue linkUp\n")
    }

    private fun setValuesCopy() {
        val file = RandomAccessFile(File("./kvdbData/copiedValues.txt"), "rw")
        file.setLength(0)
    }

    private fun getListKeysFrom(link: Long): List<keyNode> {
        val keys: MutableList<keyNode> = mutableListOf()
        var linkToNextKey = link
        while (linkToNextKey != 0L) {
            keyLists.seek(linkToNextKey)
            val (key, linkValueStr, linkUpStr) = keyLists.readLine().split(" ")
            keys.add(keyNode(key, linkValueStr.toLong(), linkUpStr.toLong()))
            linkToNextKey = linkUpStr.toLong()
        }
        return keys.toList()
    }

    private fun uniqueKeys(keys: List <keyNode>): List<keyNode> {
        val unq: MutableList<keyNode> = mutableListOf()
        val used: MutableSet<keyNode> = mutableSetOf()
        keys.forEach {
            if (it !in used) unq.add(it)
            used.add(it)
        }
        return unq
    }
}