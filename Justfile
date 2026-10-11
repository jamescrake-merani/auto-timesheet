alias build := build-native

ci:
    clj -T:build ci

build-jar:
    clj -T:build uberjar

build-native: build-jar
    native-image -march=native --features=clj_easy.graal_build_time.InitClojureClasses -jar target/com.github.jamescrake-merani/auto-timesheet-*.jar -o target/auto-timesheet

test:
    clj -M:test

