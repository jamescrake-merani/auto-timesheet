# Auto Timesheet

**DISCLAIMER:** This is ALPHA software! Expect bugs, and breaking changes.

This is a terminal-based timesheet software to help with keeping track of your work hours. It allows you to make clock ins, clock outs, and generate reports to help you keep track of how long you've spent at work.

# Documentation

Firstly, the `auto-timesheet --help` command shows reference documentation for all of the available commands. You can get help on a specific command by adding that flag to it. E.g.

``` sh
> auto-timesheet clockin --help
```

A more detailed manual as to how to use the software is provided via MkDocs. If you have [uv](https://docs.astral.sh/uv/#projects) installed, the easiest way to build the docs is to run:

``` sh
uvx --with mkdocs-autorefs mkdocs serve
```

# Build Instructions

This is a Clojure project, so you'll need to have **Clojure** installed alongside a **JDK**. Then run:

```sh
clj -T:build ci
```

Which will run the tests, and (if successful) build the uberjar of this application.

While optional, its also recommended that you have use **GraalVM** for compiling a native image of the program. This is because the JVM has a long startup time, which is a bit problematic for a terminal-based application like auto-timesheet. A script is provided (`build_native_image.sh`) for doing this, which you can run after producing the uberjar from the previous step.
