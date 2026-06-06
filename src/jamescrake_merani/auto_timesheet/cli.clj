(ns jamescrake-merani.auto-timesheet.cli
  (:require [babashka.cli :as cli]
            [jamescrake-merani.auto-timesheet.db-init :refer [open-database]]
            [clojure.java.io :as io])
  (:import (dev.dirs ProjectDirectories))
  (:gen-class))

(def directories (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet"))

(def clock-spec
  {:category {:alias :c}})

(defn clockout [_]
  (println "Clock out"))

(defn clockin [_]
  (println "Clock in"))

(defn no-command [_]
  (println "You need to use a command."))

(def table
  [{:cmds ["clockin"] :fn clockin :doc "Clock in" :spec clock-spec}
   {:cmds ["clockout"] :fn clockout :doc "Clock out" :spec clock-spec}
   {:cmds [] :fn no-command :doc "No command"}])

;; TODO: Might only want to init the db for some commands later.
(defn -main [& args]
  (let [db (open-database (io/file (.dataDir directories) "data.db"))]
    (cli/dispatch table args)))

