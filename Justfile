alias build := build-native

ci:
    clj -T:build ci

build-jar:
    clj -T:build uberjar

build-native: build-jar
    native-image -march=native --features=clj_easy.graal_build_time.InitClojureClasses -jar target/com.github.jamescrake-merani/auto-timesheet-*.jar -o target/auto-timesheet

test:
    clj -M:test

[working-directory: 'manual']
build-docs:
    uvx --with mkdocs-autorefs mkdocs build

[working-directory: 'manual']
serve-docs:
    uvx --with mkdocs-autorefs mkdocs serve

