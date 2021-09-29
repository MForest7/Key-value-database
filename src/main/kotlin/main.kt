import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.Path

fun readStringsFromFile(file: File): List<String> = file.readLines(Charset.defaultCharset())

object DataBase {
    private fun createBaseFile(): File {
        val baseFile = File("data/base.txt")
        baseFile.writeText("")
        return baseFile
    }

    private val file: File = initLocation()

    private fun initLocation(): File {
        return try {
            File(readStringsFromFile(File("./data/info.txt")).first())
        } catch (ex: FileNotFoundException) {
            createBaseFile()
        }
    }

    private fun location(key: String): File {
        return file
    }

    private fun writeValue(key: String, value: String, file: File) {
        file.appendText("$key\n")
        file.appendText("$value\n")
    }

    fun add(key: String, value: String): Boolean {
        val destination = location(key)
        if (get(key) != null) {
            return false
        }
        writeValue(key, value, destination)
        return true
    }

    fun get(key: String): String? {
        try {
            val keyValuePairs = readStringsFromFile(location(key)).chunked(2)
            keyValuePairs.forEach() { pair ->
                if (pair[0] == key)
                    return pair[1]
            }
            return null
        }
        catch (ex: FileNotFoundException) {
            return null
        }
    }

    fun replace(key: String, value: String) {
        val destination = location(key)
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
                    assert(args.size == 3)
                    if (!DataBase.add(args[1], args[2]))
                        tryReplace(args[1], args[2])
                    return true
                }
                "get" -> {
                    assert(args.size == 2)
                    println(DataBase.get(args[1]))
                    return true
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
}

fun main(args: Array<String>) {
    while (Interactor.readCommand()) {}
}
