# Auto Timesheet

**DISCLAIMER:** This is ALPHA software! Expect bugs, and breaking changes.

This is a terminal-based timesheet software to help with keeping track of your work hours. It allows you to make clock ins, clock outs, and generate reports to help you keep track of how long you've spent at work.

# Build Instructions

This is a Clojure project, so you'll need to have **Clojure** installed alongside a **JDK**. Then run:

```sh
clj -T:build ci
```

Which will run the tests, and (if succesful) build the uberjar of this application.

While optional, its also recommended that you have use **GraalVM** for compiling a native image of the program. This is because the JVM has a long startup time, which is a bit problematic for a terminal-based application like auto-timesheet. A script is provided (`build_native_image.sh`) for doing this, which you can run after producing the uberjar from the previous step.
