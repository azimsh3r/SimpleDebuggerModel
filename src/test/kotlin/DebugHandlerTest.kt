import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DebugHandlerTest {
    @Test
    fun testLoadPath() {
        val debugHandler = DebugHandler("debugger/path")
        debugHandler.loadPath("/path/to/debugger")
        assertEquals("/path/to/debugger", debugHandler.path)
    }

    @Test
    fun testSetBreakpoints() {
        val debugHandler = DebugHandler("debugger/path")
        debugHandler.setBreakpoints("testFile", listOf(10, 20, 30))
        assertEquals("testFile", debugHandler.fileName)
        assertEquals(listOf(10, 20, 30), debugHandler.breakPoints)
    }



    // Add more test cases for other functions as needed
}
