import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.xml.crypto.Data
import kotlin.test.*

internal class TestBaseFunctions {
    @BeforeTest
    fun clearBase() {
        HashBasedBase.reset()
    }

    @Test
    fun testAdd() {
        HashBasedBase.get("")
        assertEquals(null, HashBasedBase.get("a"))
        HashBasedBase.add("aa", "b")
        HashBasedBase.add("A", "b")
        HashBasedBase.add("amazing", "b")
        HashBasedBase.add("", "b")
        assertEquals(null, HashBasedBase.get("a"))
    }

    @Test
    fun testAddDelete() {
        HashBasedBase.delete("a")
        assertEquals(null, HashBasedBase.get("a"))
        HashBasedBase.add("a", "b")
        assertEquals("b", HashBasedBase.get("a"))
        HashBasedBase.add("aaa", "bbb")
        HashBasedBase.delete("bbb")
        assertEquals("bbb", HashBasedBase.get("aaa"))
        HashBasedBase.delete("a")
        assertEquals(null, HashBasedBase.get("a"))
    }

    @Test
    fun testDeleteFromEmpty() {
        HashBasedBase.delete("a")
        assertEquals(null, HashBasedBase.get("a"))
        HashBasedBase.delete("a")
        assertEquals(null, HashBasedBase.get("a"))
    }

    @Test
    fun testAddReplace() {
        HashBasedBase.add("a", "b")
        assertEquals("b", HashBasedBase.get("a"))
        HashBasedBase.replace("a", "c")
        assertEquals("c", HashBasedBase.get("a"))
        assertEquals("c", HashBasedBase.get("a"))
        HashBasedBase.replace("a", "aaa")
        assertEquals("aaa", HashBasedBase.get("a"))
    }

    @Test
    fun testReplaceDeleted() {
        HashBasedBase.add("a", "b")
        assertEquals("b", HashBasedBase.get("a"))
        HashBasedBase.delete("a")
        assertEquals(null, HashBasedBase.get("a"))
        HashBasedBase.replace("a", "c")
        assertEquals("c", HashBasedBase.get("a"))
    }

    @Test
    fun testDeleteReplaced() {
        HashBasedBase.add("a", "b")
        assertEquals("b", HashBasedBase.get("a"))
        HashBasedBase.replace("a", "c")
        assertEquals("c", HashBasedBase.get("a"))
        HashBasedBase.delete("a")
        assertEquals(null, HashBasedBase.get("a"))
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
}