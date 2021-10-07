import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

fun readStringsFromFile(file: File): List<String> = file.readLines(Charset.defaultCharset())

fun main(args: Array<String>) {
    //println("123".padStart(11, '0'))
    //TreeBasedBase.appendNode(123)
    //println(TreeBasedBase.bitSequence("abc").contentToString())
    //TreeBasedBase.getNodeIDOrCreate("a")
    HashBasedBase.reset()
    HashBasedBase.add("a", "b")
    HashBasedBase.add("b", "c")
    HashBasedBase.add("c", "d")
    HashBasedBase.delete("a")
    HashBasedBase.replace("c", "k")
    HashBasedBase.add("f", "b")
    HashBasedBase.replace("c", "d")
    //HashBasedBase.expand()
    HashBasedBase.garbageClear()
    return
    while (Interactor.runCommand(Interactor.receiveCommand())) {}
}
