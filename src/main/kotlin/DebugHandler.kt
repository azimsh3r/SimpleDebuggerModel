import java.io.*

class DebugHandler (debuggerPath: String) {

    var path: String? = null
    var breakPoints: List<Int> = emptyList()
    var fileName: String? = null
    var breakHandlerFun: () -> Unit = {}

    var paused = false
    var gdbOutput: BufferedReader? = null
    var gdbInput: BufferedWriter? = null

    private fun compileFile() {
        try {
            val processBuilder = ProcessBuilder("gcc", "-g", fileName, "-o", "exec")
                .directory(File(path ?: throw NullPointerException("Path cannot be null!")))
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Compilation failed with exit code: $exitCode! Make sure that your code contains no errors!")
            }
        } catch (e: Exception) {
            println("Error during compilation: ${e.message}")
        }
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
        this.breakHandlerFun = breakHandleFunction
    }

    fun resume() {
        paused = false
        Thread.sleep(1000)
    }

    private fun pause () {
        paused = true
    }

    fun resumeExecution() {
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
                    return viewBackTrace(line ?: "Backtrace is not available")
                    //return line ?: "Backtrace is not available"
                } else if (line?.startsWith("^error") == true) {
                    throw Exception("Error: " + line?.substringAfterLast("msg=\"")?.substringBefore("\""))
                }
            }
        } catch (e: Exception) {
            println("Error reading backtrace: ${e.message}")
        }
        return "Backtrace was not found"
    }

    private fun viewBackTrace(backTrace: String) : String {
        val stringBuilder = StringBuilder()
        val stackTraceContent = backTrace.substringAfter("[frame={").substringBeforeLast("}]")

        // Split the stack trace content by "},"
        val frameStrings = stackTraceContent.split("},")

        // Iterate over each frame string
        for (frameString in frameStrings) {
            // Extract individual frame attributes
            val attributes = frameString.split(",") // Split attributes by comma
            val level = getValue(attributes[0])
            val addr = getValue(attributes[1])
            val func = getValue(attributes[2])
            val file = getValue(attributes[3])
            val line = getValue(attributes[4])

            // Append formatted frame information to the StringBuilder
            stringBuilder.append("Level: $level, Address: $addr, Function: $func, File: $file, Line: $line\n")
        }
        return stringBuilder.toString()
    }

    private fun getValue(attribute: String): String {
        return attribute.substringAfter("=").trim('"')
    }

    fun run() {
        try {
            // Compiles a file
            compileFile()

            // Creates a process for GDB
            val gdbBuilder = ProcessBuilder("gdb", "--interpreter=mi", "$path/exec.exe")
            gdbBuilder.directory(File(path ?: throw NullPointerException("Path cannot be Null!")))
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
            while (gdbOutput?.readLine().also { line = it ?: "null" } != null) {
                if (line.contains("^done,bkpt")) {
                    gdbOutput?.mark(4096)
                    // Pauses the implementation
                    pause()
                    viewBreakpoint(line)

                    // Logic to be implemented inside a breakpoint
                    breakHandlerFun()

                    gdbOutput?.reset()
                    while (paused) {
                        Thread.sleep(1000)
                    }
                    resumeExecution()
                } else if (line.startsWith("^error")) {
                    throw Exception("Error response from GDB: $line")
                } else if (line.startsWith("=thread-group-exited")) {
                    break
                }
            }
        } catch(e: Exception) {
            println(e.message)
        } finally {
            gdbInput?.close()
            gdbOutput?.close()
        }
    }
}
