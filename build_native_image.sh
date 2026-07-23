#!/usr/bin/env sh

native-image --no-fallback --features=clj_easy.graal_build_time.InitClojureClasses -jar target/com.github.jamescrake-merani/auto-timesheet-*.jar -o target/auto-timesheet
