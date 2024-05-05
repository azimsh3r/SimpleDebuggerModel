# DebugHandler

The `DebugHandler` class provides functionality to debug C/C++ programs using GDB (GNU Debugger) through Java code. It allows setting breakpoints, handling breakpoints, and viewing backtraces during debugging sessions.

## Prerequisites
- Java Development Kit (JDK) installed
- GDB (GNU Debugger) installed on your system

## Usage

1. **Initialization**: Create an instance of `DebugHandler` by providing the path to your debugger (GDB).

    ```java
    DebugHandler debugHandler = new DebugHandler("/usr/bin/gdb");
    ```

2. **Load File Path**: Specify the path to the C/C++ file you want to debug.

    ```java
    debugHandler.loadPath("path/to/your/file");
    ```

3. **Set Breakpoints**: Define the file name and a list of line numbers where you want to set breakpoints.

    ```java
    debugHandler.setBreakpoints("test.c", Arrays.asList(4, 6));
    ```

4. **Set Break Handler**: Define actions to be performed when a breakpoint is hit.

    ```java
    debugHandler.setBreakHandler(() -> {
        // Add your actions here
        System.out.println(debugHandler.getBackTrace());
        debugHandler.resume();
    });
    ```

5. **Run Debugging Session**: Start the debugging session.

    ```java
    debugHandler.run();
    ```

## Notes

- Make sure that the file path and debugger path are correctly specified.
- The `setBreakHandler` method allows you to define custom actions to be executed when a breakpoint is encountered. You can access the backtrace information using `getBackTrace()` method within the break handler.
- After defining the `DebugHandler` instance and its configurations, execute the debugging session by calling the `run()` method.

## Example

```java
public static void main(String[] args) {
    testApplication();
}

public static void testApplication() {
    DebugHandler debugHandler = new DebugHandler("/usr/bin/gdb");

    debugHandler.loadPath("path/to/your/file");

    debugHandler.setBreakpoints("test.c", Arrays.asList(4, 6));

    debugHandler.setBreakHandler(() -> {
        System.out.println(debugHandler.getBackTrace());
        debugHandler.resume();
    });

    debugHandler.run();
}
