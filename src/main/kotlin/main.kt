import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.Path

fun readStringsFromFile(file: File): List<String> = file.readLines(Charset.defaultCharset())

object DataBase {
    private fun createBaseFile(name: String): File {
        val baseFile = File(name)
        baseFile.writeText("")
        return baseFile
    }

    private val info = File("./data/info.txt")

    private val hashMod = readStringsFromFile(info)[0].toInt()

    private fun hash(key: String) = key.hashCode() % hashMod

    private fun checkLocation(key: String): File {
        val path = "./data/${hash(key)}.txt"
        return try {
            readStringsFromFile(File(path))
            File(path)
        } catch (ex: FileNotFoundException) {
            createBaseFile(path)
        }
    }

    private fun findLocation(key: String): File? {
        val path = "./data/${hash(key)}.txt"
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

    fun add(key: String, value: String): Boolean {
        val destination = checkLocation(key)
        if (get(key) != null) {
            return false
        }
        writeValue(key, value, destination)
        return true
    }

    fun get(key: String): String? {
        val destination = findLocation(key)
        if (destination != null) {
            try {
                val keyValuePairs = readStringsFromFile(destination).chunked(2)
                keyValuePairs.forEach() { pair ->
                    if (pair[0] == key)
                        return pair[1]
                }
                return null
            } catch (ex: FileNotFoundException) {
                return null
            }
        } else {
            return null
        }
    }

    fun replace(key: String, value: String) {
        val destination = checkLocation(key)
        val keyValuePairs = readStringsFromFile(destination).chunked(2)
        destination.bufferedWriter(Charset.defaultCharset()).use { out ->
            keyValuePairs.forEach() {
                out.write("${it[0]}\n")
                if (it[0] == key)
                    out.write("$value\n")
                else
                    out.write("${it[1]}\n")
            }
        }
    }

    fun delete(key: String) {
        val destination = findLocation(key) ?: return
        val keyValuePairs = readStringsFromFile(destination).chunked(2)
        destination.bufferedWriter(Charset.defaultCharset()).use { out ->
            keyValuePairs.forEach() {
                if (it[0] != key) {
                    out.write("${it[0]}\n")
                    out.write("${it[1]}\n")
                }
            }
            if (keyValuePairs.size == 1)
                destination.delete()
        }
    }
}

object Interactor {
    fun readCommand(): Boolean {
        val args = readLine()?.split(" ")
        if (args != null) {
            when (args[0]) {
                "exit" -> {
                    return false
                }
                "add" -> {
                    if (!checkEnoughArgs(args, 3)) return true
                    if (!DataBase.add(args[1], args[2]))
                        tryReplace(args[1], args[2])
                    return true
                }
                "get" -> {
                    assert(args.size == 2)
                    println(DataBase.get(args[1]))
                    return true
                }
                "del" -> {
                    assert(args.size == 2)
                    DataBase.delete(args[1])
                }
                else -> {
                    return true
                }
            }
        }
        return true
    }

    private fun tryReplace(key: String, value: String): Boolean {
        println("This key has already been created")
        println("Value: ${DataBase.get(key)}")
        println("Do you want to replace value? (y/n)")
        val args = readLine()?.split(" ")
        if (args != null) {
            if (args[0] == "y") {
                DataBase.replace(key, value)
                return true
            }
        }
        return false
    }

    private fun checkEnoughArgs(args: List<String>, size: Int):Boolean {
        if (args.size >= size) return true
        println("Incorrect format")
        println("${args[0]} must have ${size - 1} arguments")
        return false
    }
}

fun main(args: Array<String>) {
    while (Interactor.readCommand()) {}
}
