fun main(args: Array<String>) {
    testApplication()
}


fun testApplication () {
    // Please specify path to your debugger here (gdb)
    val debugHandler = DebugHandler("/usr/bin/gdb")

    //Please specify path to your file
    debugHandler.loadPath("path/to/your/file")

    // Specify breakpoints and testName
    debugHandler.setBreakpoints("test.c", listOf(4, 6))

    debugHandler.setBreakHandler {
        // You can add your actions here
        println(debugHandler.getBackTrace())

        debugHandler.resume()
    }
    debugHandler.run()
}