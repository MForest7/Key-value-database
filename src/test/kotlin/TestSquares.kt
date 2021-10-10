import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.xml.crypto.Data
import kotlin.test.*

internal class TestSquares {
    @BeforeTest
    fun clearBase() {
        HashBasedBase.reset()
    }

    @Test
    fun testMoreData() {
        val lines = readStringsFromFile(File("./testData/addSquares.txt"))
        lines.forEach {
            val (key, value) = it.split(" ")
            HashBasedBase.add(key, value)
        }

        lines.forEach {
            val (key, value) = it.split(" ")
            assertEquals(value, HashBasedBase.get(key))
        }
    }
}