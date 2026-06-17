#!/usr/bin/env sh

native-image --no-fallback --features=clj_easy.graal_build_time.InitClojureClasses -jar target/net.clojars.jamescrake-merani/auto-timesheet-0.1.0-SNAPSHOT.jar -o target/auto-timesheet
