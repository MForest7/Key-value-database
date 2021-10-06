import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.charset.Charset

object DataBase {
    private fun createBaseFile(name: String): File {
        val baseFile = File(name)
        baseFile.createNewFile()
        val abc = RandomAccessFile(baseFile, "")
        return baseFile
    }

    private val info = checkInfo()

    private fun checkInfo(): File {
        return try {
            readStringsFromFile(File("./kvdbData/info.txt"))
            File("./kvdbData/info.txt")
        }
        catch (ex: FileNotFoundException) {
            File("./kvdbData/").deleteRecursively()
            File("./kvdbData/").mkdirs()
            val createdInfo = createBaseFile("./kvdbData/info.txt")
            createdInfo.bufferedWriter().use { out ->
                out.write("""
                    mod=10
                    size=0
                    limit=250000
                """.trimIndent())
            }
            createdInfo
        }
    }

    private fun rewriteInfo() {
        info.bufferedWriter().use { out ->
            out.write("mod=$hashMod\n")
            out.write("size=$size\n")
            out.write("limit=$limit\n")
        }
    }

    private var hashMod = readStringsFromFile(info)[0].drop(4).toInt()
    private var size = readStringsFromFile(info)[1].drop(5).toInt()
    private val limit = readStringsFromFile(info)[2].drop(6).toInt()

    private fun hash(key: String) = key.hashCode() % hashMod

    private fun getLocation(key: String): File {
        val path = "./kvdbData/${hash(key)}.txt"
        return try {
            readStringsFromFile(File(path))
            File(path)
        } catch (ex: FileNotFoundException) {
            createBaseFile(path)
        }
    }

    private fun findLocation(key: String): File? {
        val path = "./kvdbData/${hash(key)}.txt"
        return try {
            readStringsFromFile(File(path))
            File(path)
        } catch (ex: FileNotFoundException) {
            null
        }
    }

    private fun writeValue(key: String, value: String, file: File) {
        file.appendText("$key\n")
        file.appendText("$value\n")
    }

    private fun nameCopy(file: File) = "${file.path.dropLast(4)}Copy.txt"

    private fun createTempCopy(file: File) {
        val copy = createBaseFile(nameCopy(file))
        copy.bufferedWriter().use { out ->
            readStringsFromFile(file).forEach {
                out.write("$it\n")
            }
        }
    }

    private fun expand() {
        if (size / hashMod <= limit) {
            return
        }

        val copies = mutableListOf<File>()
        for (hash in 0 until hashMod) {
            val file = File("./kvdbData/${hash}.txt")
            if (!file.exists())
                continue
            createTempCopy(file)
            copies.add(File(nameCopy(file)))
            file.delete()
        }

        hashMod *= 2
        rewriteInfo()
        copies.forEach { file ->
            val keyValuePairs = readStringsFromFile(file).chunked(2)
            keyValuePairs.forEach { (key, value) ->
                writeValue(key, value, getLocation(key))
            }
        }

        copies.forEach { it.delete() }
    }

    fun add(key: String, value: String): Boolean {
        val destination = getLocation(key)
        if (get(key) != null) {
            return false
        }
        writeValue(key, value, destination)

        size += 1
        rewriteInfo()
        expand()

        return true
    }

    fun get(key: String): String? {
        val destination = findLocation(key)
        if (destination != null) {
            val keyValuePairs = readStringsFromFile(destination).chunked(2)
            keyValuePairs.forEach { pair ->
                if (pair[0] == key)
                    return pair[1]
            }
            return null
        } else {
            return null
        }
    }

    fun replace(key: String, value: String) {
        val destination = getLocation(key)
        val keyValuePairs = readStringsFromFile(destination).chunked(2)
        destination.bufferedWriter(Charset.defaultCharset()).use { out ->
            keyValuePairs.forEach {
                out.write("${it[0]}\n")
                if (it[0] == key)
                    out.write("$value\n")
                else
                    out.write("${it[1]}\n")
            }
        }
    }

    fun delete(key: String) {
        size -= 1
        val destination = findLocation(key) ?: return
        val keyValuePairs = readStringsFromFile(destination).chunked(2)
        destination.bufferedWriter(Charset.defaultCharset()).use { out ->
            keyValuePairs.forEach {
                if (it[0] != key) {
                    out.write("${it[0]}\n")
                    out.write("${it[1]}\n")
                }
            }
            if (keyValuePairs.size == 1)
                destination.delete()
        }
    }

    fun clear() {

    }
}