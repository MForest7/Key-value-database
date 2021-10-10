import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.xml.crypto.Data
import kotlin.test.*

internal class TestBaseTransforms {
    @BeforeTest
    fun clearBase() {
        HashBasedBase.reset()
    }

    @Test
    fun testExpand() {
        HashBasedBase.add("a", "b")
        HashBasedBase.add("c", "d")
        HashBasedBase.expand()
        assertEquals("b", HashBasedBase.get("a"))
        assertEquals("d", HashBasedBase.get("c"))
    }

    @Test
    fun testExpandMoreData() {
        val lines = readStringsFromFile(File("./testData/testExpand.txt"))
        lines.forEach {
            val (key, value) = it.split(" ")
            HashBasedBase.add(key, value)
        }
        HashBasedBase.expand()
        lines.forEach {
            val (key, value) = it.split(" ")
            assertEquals(value, HashBasedBase.get(key))
        }
        HashBasedBase.expand()
        lines.forEach {
            val (key, value) = it.split(" ")
            assertEquals(value, HashBasedBase.get(key))
        }
    }

    @Test
    fun testGarbageClear() {
        val linesAdd = readStringsFromFile(File("./testData/testClearAdd.txt"))
        val linesDel = readStringsFromFile(File("./testData/testClearDel.txt"))
        val linesNeed = readStringsFromFile(File("./testData/testClearNeed.txt"))
        linesAdd.forEach {
            val (key, value) = it.split(" ")
            HashBasedBase.add(key, value)
        }
        assertEquals(15, HashBasedBase.size)
        linesDel.forEach {
            val key = it
            HashBasedBase.delete(key)
        }
        HashBasedBase.garbageClear()
        assertEquals(8, HashBasedBase.size)

        linesNeed.forEach {
            val (key, value) = it.split(" ")
            assertEquals(value, HashBasedBase.get(key))
        }
        assertEquals(8, HashBasedBase.size)
        linesNeed.forEach {
            val (key, value) = it.split(" ")
            assertEquals(value, HashBasedBase.get(key))
        }
    }

    @Test
    fun testReset() {
        val lines = readStringsFromFile(File("./testData/testExpand.txt"))
        lines.forEach {
            val (key, value) = it.split(" ")
            HashBasedBase.add(key, value)
        }
        assertEquals(13, HashBasedBase.size)
        HashBasedBase.reset()
        assertEquals(0, HashBasedBase.size)
    }
}