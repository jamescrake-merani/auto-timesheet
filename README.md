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

This project uses a [Justfile](https://just.systems/man/en/), where you can find all the build commands. If you have `just` installed, the quickest way to build everything is to run

``` sh
just build
```

To run this command, you'll require

- Clojure, and the Clojure CLI tools
- A JDK with GraalVM (see below for building without)
- Just

This command will build a native executable for your operating system with GraalVM. GraalVM is recommended because the JVM has a long startup time (that is exacerbated by Clojure loading in _a lot_ of classes), which is a bit problematic for a CLI application like auto-timesheet.

## Build without GraalVM

If you'd prefer to build without GraalVM, and are willing to put up with the long JVM startup time, you can run

```sh
just build-jar
```

