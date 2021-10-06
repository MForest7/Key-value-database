import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.xml.crypto.Data
import kotlin.test.*

internal class TestInteractor {
    private val standardOut = System.out
    private val stream = ByteArrayOutputStream()

    @BeforeTest
    fun setUp() {
        System.setOut(PrintStream(stream))
    }

    @AfterTest
    fun tearDown() {
        System.setOut(standardOut)
    }

    @Test
    fun testInteractor() {
        readStringsFromFile(File("./testData/testCommand.txt")).forEach {
            Interactor.runCommand(it.split(" "))
        }
        assertEquals(
            readStringsFromFile(File("./testData/testOutput.txt")),
            stream.toString().split("\n").dropLast(1)
        )
    }
}