fun main(args: Array<String>) {
    val debugHandler = DebugHandler("/usr/bin/gdb")

    debugHandler.loadPath("C:/Users/Azim/Desktop/JetBrainsTasks/ToyDebugger/src/main/c")
    debugHandler.setBreakpoints("code.c", listOf(4, 5))

    debugHandler.setBreakHandler {
        println(debugHandler.getBackTrace())

        debugHandler.resume()
        Thread.sleep(1000)
    }

    debugHandler.run()
}