(ns jamescrake-merani.auto-timesheet.cli
  (:require [babashka.cli :as cli])
  (:gen-class))

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

(defn -main [& args]
  (cli/dispatch table args))

