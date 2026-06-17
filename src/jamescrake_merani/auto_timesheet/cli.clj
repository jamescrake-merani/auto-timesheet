(ns jamescrake-merani.auto-timesheet.cli
  (:require [babashka.cli :as cli]
            [jamescrake-merani.auto-timesheet.db-init :refer [open-database]]
            [jamescrake-merani.auto-timesheet.db :as as-db]
            [clojure.java.io :as io]
            [jamescrake-merani.auto-timesheet.db-helpers :as helpers]
            [jamescrake-merani.auto-timesheet.reports :refer [reports-available format-duration]]
            [clojure.string :as str])
  (:import (dev.dirs ProjectDirectories)
           (java.time Duration
                      LocalDateTime))
  (:gen-class))

(def directories (delay (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet")))
;; TODO: I'm not sure whether this should be at this level.
(def db (delay (open-database (io/file (.dataDir ^ProjectDirectories @directories) "data.db"))))

(def clock-spec
  {:category {:alias :c}})

;: TODO: Probably want to be able to provide a category.
(defn clockout [_]
  (let [time-since-clockin (-> (as-db/hanging-clockins @db) first :starttime LocalDateTime/parse)]
    (helpers/clock-out @db)
    (println (format "Clocked out. You have worked %s"
                     (format-duration (Duration/between time-since-clockin (LocalDateTime/now)))))))

;: TODO Allow the user to disable this check.
;; TODO: Also this check only looks for all categories not one specific one.
(defn clockin [{{:keys [category]} :opts}]
  (cond
    (not (empty? (as-db/hanging-clockins @db))) (println "You are already clocked in.")
    (nil? category) (do (.println *err*  "You need to provide a category with clock ins.")
                        (System/exit 1))
    :else (do
            (helpers/clock-in @db category)
            (println "Clocked in."))))

(def report-spec
  {:type {:alias :t
          :require true}})

(defn report [{{:keys [type]} :opts}]
  (let [report-function (get reports-available (keyword type))]
    (if (nil? report-function)
      (do
        (.println *err* "That report type does not exist.")
        (System/exit 1))
      (println (->> @db report-function flatten (str/join "\n"))))))

(defn print-status []
  (let [hanging-clockins (as-db/hanging-clockins @db)]
    (if (empty? hanging-clockins)
      (println "You are not currently clocked in.")
      (println
       (format "You are currently clocked in. %s elapsed since clockin."
               (format-duration (Duration/between (LocalDateTime/parse (-> hanging-clockins first :starttime))
                                                  (LocalDateTime/now))))))))

(defn status-command [_]
  (print-status))

(defn no-command [_]
  (print-status)
  (println "Run 'auto-timesheet --help' for a list of all commands."))

(def table
  [{:cmds ["clockin"] :fn clockin :doc "Clock in" :spec clock-spec}
   {:cmds ["clockout"] :fn clockout :doc "Clock out" :spec clock-spec}
   {:cmds ["report"] :fn report :doc "Display reports" :spec report-spec}
   {:cmds ["status"] :fn status-command :doc "Shows current clock in status"}
   {:cmds [] :fn no-command :doc "No command"}])

;; TODO: Might only want to init the db for some commands later.
(defn -main [& args]
  (cli/dispatch table args {:error-fn (fn [{:keys [spec type cause msg option] :as data}]
                                        (if (= :org.babashka/cli type)
                                          (.println *err* msg)
                                          (throw (ex-info msg data)))
                                        (System/exit 1))
                            :prog "auto-timesheet"
                            :help true}))

