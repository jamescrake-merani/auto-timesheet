ci:
    clj -T:ci

build:
    clj -T:uberjar && native-image --no-fallback --features=clj_easy.graal_build_time.InitClojureClasses -jar target/com.github.jamescrake-merani/auto-timesheet-*.jar -o target/auto-timesheet

run:
    target/auto-timesheet


