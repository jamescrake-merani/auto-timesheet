(ns jamescrake-merani.auto-timesheet.cli
  (:require [babashka.cli :as cli]
            [jamescrake-merani.auto-timesheet.db-init :refer [open-database]]
            [jamescrake-merani.auto-timesheet.db :as as-db]
            [clojure.java.io :as io]
            [jamescrake-merani.auto-timesheet.db-helpers :as helpers])
  (:import (dev.dirs ProjectDirectories))
  (:gen-class))

(def directories (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet"))
;; TODO: I'm not sure whether this should be at this level.
(def db (open-database (io/file (.dataDir directories) "data.db")))

(def clock-spec
  {:category {:alias :c}})

;: TODO: Probably want to be able to provide a category.
(defn clockout [_]
  (helpers/clock-out db))

;: TODO Allow the user to disable this check.
;; TODO: Also this check only looks for all categories not one specific one.
(defn clockin [{{:keys [category]} :opts}]
  (if (empty? (as-db/hanging-clockins db))
    (do
      (helpers/clock-in db category)
      (println "Clocked in."))
    (println "You are already clocked in.")))

(defn no-command [_]
  (println "You need to use a command."))

(def table
  [{:cmds ["clockin"] :fn clockin :doc "Clock in" :spec clock-spec}
   {:cmds ["clockout"] :fn clockout :doc "Clock out" :spec clock-spec}
   {:cmds [] :fn no-command :doc "No command"}])

;; TODO: Might only want to init the db for some commands later.
(defn -main [& args]
  (cli/dispatch table args))

