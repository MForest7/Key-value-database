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

    val file: File = initLocation()

    fun initLocation(): File {
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

    fun add(key: String, value: String) {
        val destination = location(key)
        writeValue(key, value, destination)
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
                    DataBase.add(args[1], args[2])
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
}

fun main(args: Array<String>) {
    while (Interactor.readCommand()) {}
}
