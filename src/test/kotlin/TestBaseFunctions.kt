import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.xml.crypto.Data
import kotlin.test.*

internal class TestBaseFunctions {
    @Test
    fun testBaseFunctions() {
        DataBase.get("")
        assertEquals(null, DataBase.get("a"),
            "expected null, found value")
        DataBase.add("aa", "b")
        DataBase.add("A", "b")
        DataBase.add("amazing", "b")
        DataBase.add("", "b")
        assertEquals(null, DataBase.get("a"),
            "expected null, found value")

        DataBase.delete("a")
        assertEquals(null, DataBase.get("a"),
            "expected null, found value")
        DataBase.add("a", "b")
        assertEquals("b", DataBase.get("a"))
        DataBase.add("aaa", "bbb")
        assertEquals("bbb", DataBase.get("aaa"))
        assertEquals("b", DataBase.get("a"))

        assertEquals("b", DataBase.get("a"))
        DataBase.replace("a", "c")
        assertEquals("c", DataBase.get("a"))
        assertEquals("c", DataBase.get("a"))
        DataBase.replace("a", "aaa")
        assertEquals("aaa", DataBase.get("a"))

        DataBase.delete("a")
        assertEquals(null, DataBase.get("a"))
        DataBase.delete("a")
        assertEquals(null, DataBase.get("a"))
    }
}