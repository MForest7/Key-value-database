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
        } catch (IOException: FileNotFoundException) {
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
}

fun main(args: Array<String>) {
    println(DataBase.file.path)
    val (key, value) = readLine()!!.split(" ")
    DataBase.add(key, value)
}
