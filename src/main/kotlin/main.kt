import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

fun readStringsFromFile(file: File): List<String> = file.readLines(Charset.defaultCharset())

fun main() {
    while (Interactor.runCommand(Interactor.receiveCommand())) {}
}
