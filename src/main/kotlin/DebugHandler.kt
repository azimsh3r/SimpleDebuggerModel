import exceptionHandling.NullPathException
import java.io.*

class DebugHandler (debuggerPath: String) {

    var path: String? = null
    var breakPoints: List<Int> = emptyList()
    var fileName: String? = null
    private var breakHandler: () -> Unit = {}

    private var paused = false
    private var gdbOutput: BufferedReader? = null
    private var gdbInput: BufferedWriter? = null

    private fun compileFile() {
        ProcessBuilder(listOf("gcc", "-g", fileName, "-o", "exec"))
            .directory(File(path ?: throw NullPathException()))
            .start()
            .waitFor()
    }

    private fun setBreakpoint(gdbInput: BufferedWriter, location: String) {
        gdbInput.write("-break-insert $location\n")
        gdbInput.flush()
    }

    private fun viewBreakpoint(line: String) {
        val numberPattern = "number=\"(\\d+)\"".toRegex()
        val funcPattern = "func=\"([^\"]+)\"".toRegex()
        val linePattern = "line=\"(\\d+)\"".toRegex()

        val breakPointNumber = numberPattern.find(line)?.groupValues?.get(1)?.toInt() ?: 0
        val funName = funcPattern.find(line)?.groupValues?.get(1) ?: ""
        val lineNumber = linePattern.find(line)?.groupValues?.get(1)?.toInt() ?: 0

        println("Breakpoint $breakPointNumber, $funName () at $fileName:$lineNumber")
    }

    fun loadPath(path: String) {
        this.path = path
    }

    fun setBreakpoints(fileName: String, breakPoints: List<Int>) {
        this.fileName = fileName
        this.breakPoints = breakPoints
    }

    fun setBreakHandler(breakHandleFunction: () -> Unit) {
        this.breakHandler = breakHandleFunction
    }

    fun resume() {
        paused = false
    }

    private fun pause () {
        paused = true
    }

    private fun resumeExecution() {
        gdbInput?.write("-exec-continue\n")
        gdbInput?.flush()
    }

    fun getBackTrace(): String {
        try {
            // Clear any previous stack trace information
            while (gdbOutput?.readLine()?.startsWith("^done") == false)

            // Request the stack trace for the current breakpoint
            gdbInput?.write("-stack-list-frames\n")
            gdbInput?.flush()

            var line: String?
            while (gdbOutput?.readLine().also { line = it } != null) {
                if (line?.startsWith("^done,stack=") == true) {
                    // Extract and return the stack trace
                    return line ?: "Backtrace is not available"
                } else if (line?.startsWith("^error") == true) {
                    throw Exception("Error: " + line?.substringAfterLast("msg=\"")?.substringBefore("\""))
                }
            }
        } catch (e: Exception) {
            println("Error reading backtrace: ${e.message}")
        }
        return "Backtrace was not found"
    }

    fun run() {
        try {
            // Compiles a file
            compileFile()

            // Creates a process for GDB
            val gdbBuilder = ProcessBuilder("gdb", "--interpreter=mi", "--args", "$path/exec.exe")
            gdbBuilder.directory(File(path ?: throw NullPathException()))
            val gdbProcess = gdbBuilder.start()

            gdbInput = gdbProcess.outputStream.bufferedWriter()
            gdbOutput = BufferedReader(InputStreamReader(gdbProcess.inputStream))

            // Sets breakpoints
            for (point in breakPoints) {
                setBreakpoint(gdbInput!!, "$fileName:$point")
            }

            // Runs GDB
            gdbInput?.write("-exec-run\n")
            gdbInput?.flush()

            // Works with breakpoints
            var line: String
            while (gdbOutput!!.readLine().also { line = it } != null) {
                if (line.contains("^done,bkpt")) {
                    gdbOutput?.mark(4096)
                    // Pauses the implementation
                    pause()
                    viewBreakpoint(line)

                    // Logic to be implemented inside a breakpoint
                    breakHandler()

                    gdbOutput?.reset()
                    while (paused) {
                        Thread.sleep(1000)
                    }
                    resumeExecution()
                } else if (line.startsWith("^error")) {
                    throw Exception("Error response from GDB: $line")
                }
            }
        } catch(e: Exception) {
            println(e.message)
        }
    }
}
