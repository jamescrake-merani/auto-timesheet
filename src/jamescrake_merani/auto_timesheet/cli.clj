;; Auto Timesheet
;; Copyright (C) 2026 James Crake-Merani

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see <https://www.gnu.org/licenses/>.

(ns jamescrake-merani.auto-timesheet.cli
  (:require [babashka.cli :as cli]
            [jamescrake-merani.auto-timesheet.db-init :refer [open-database]]
            [jamescrake-merani.auto-timesheet.db :as as-db]
            [clojure.java.io :as io]
            [jamescrake-merani.auto-timesheet.db-helpers :as helpers]
            [jamescrake-merani.auto-timesheet.reports :refer [reports-available format-duration]]
            [jamescrake-merani.auto-timesheet.config :as configuration]
            [clojure.string :as str])
  (:import (java.time Duration
                      LocalDateTime
                      LocalDate
                      LocalTime)
           (dev.dirs ProjectDirectories))
  (:gen-class))

(def config (delay (configuration/load-config)))
;; TODO: I'm not sure whether this should be at this level.
(def db (delay (open-database (:sql-directory @config))))

(def clock-spec
  {:category {:alias :c
              :desc "The category to clock into. This only needs to be specified if you don't have a default category in your config."}
   :force {:alias :f
           :coerce :boolean
           :desc "Create a clock in even if you are already clocked in."}})

;: TODO: Probably want to be able to provide a category.
(defn clockout [{{:keys [category]} :opts}]
  (let [hanging-clockins (helpers/hanging-clockins @db)
        time-since-clockin (when (not (empty? hanging-clockins))
                             (:starttime (first hanging-clockins)))]
    (cond (empty? hanging-clockins)
          (do
            (.println ^java.io.PrintWriter *err* "You are not clocked in.")
            (System/exit 1))
          (= (count hanging-clockins) 1)
          (helpers/clock-out @db)
          (nil? category)
          (do
            (.println ^java.io.PrintWriter *err* "You have multiple clock ins. You must resolve this ambiguity by specifying a category (with the --category flag).")
            (System/exit 1))
          :else (let [category-id (as-db/get-category-from-name @db {:name category})]
                  (helpers/clock-out @db category-id)))
    (println (format "Clocked out. You have worked %s"
                     (format-duration (Duration/between time-since-clockin (LocalDateTime/now)))))))

;: TODO Allow the user to disable this check.
;; TODO: Also this check only looks for all categories not one specific one.
(defn clockin [{{:keys [category force]} :opts}]
  (let [category-to-use (or category (:default-category @config))]
    (cond
      (not (or (empty? (helpers/hanging-clockins @db)) force)) (println "You are already clocked in. (use the --force flag to ignore this check.)")
      ;; TODO: Probably want to explain a bit better how to add a default one - perhaps link to documentation when thats available?
      (nil? category-to-use) (do (.println ^java.io.PrintWriter *err*  "You need to provide a category with clock ins as you haven't provided a default one in your config.")
                                 (System/exit 1))
      :else (do
              (helpers/clock-in @db category-to-use)
              (println "Clocked in.")))))

(def report-spec
  {:type {:alias :t
          :require true
          :spec "The type of report to generate."}
   :category {:alias :c
              :spec "Only show clocks from this specific category."}})

(defn report [{{:keys [type category]} :opts}]
  (let [category-id (if category (:categoryid (as-db/get-category-from-name @db {:name category})))
        filter-function (if (nil? category-id)
                          (constantly true)
                          #(= (:categoryid %) category-id))
        report-function (get reports-available (keyword type))]
    (if (and category (nil? category-id))
      (do
        (.println ^java.io.PrintWriter *err* "That category does not exist.")
        (System/exit 1)))
    (if (nil? report-function)
      (do
        (.println ^java.io.PrintWriter *err* "That report type does not exist.")
        (System/exit 1))
      (println (->> (report-function @db filter-function) flatten (str/join "\n"))))))

(defn format-clockin [clockin one-clockin?]
  (format "%s %s. %s elapsed since clockin."
          (if one-clockin? "You are currently clocked into" "You are clocked into")
          (:name (as-db/get-category-name-from-id @db {:id (:categoryid clockin)}))
          (format-duration (Duration/between (:starttime clockin)
                                             (LocalDateTime/now)))))

(defn print-status []
  (println
   (format "You have worked %s today"
            (-> @db
                (helpers/clocks-within-date (LocalDate/now))
                helpers/sum-clocks
                format-duration)))
  (let [hanging-clockins (helpers/hanging-clockins @db)]
    (cond (empty? hanging-clockins) (println "You are not currently clocked in.")
          (= (count hanging-clockins) 1) (println (format-clockin (first hanging-clockins) true))
          :else (do
                  (println "You currently have multiple clock ins:")
                  (doseq [clockin hanging-clockins]
                    (println (format-clockin clockin false)))))))

;; TODO: Display all clock ins.

(defn print-clocks [clocks]
  (println
   (str/join "\n"
             (map (fn [clock]
                    (format "%s - %s %s"
                            (:starttime clock)
                            (:stoptime clock)
                            (format-duration (Duration/between (:starttime clock)
                                                               (:stoptime clock)))))
                  clocks))))

(def range-spec
  {:start-time {:alias :s
                :require true
                :desc "The start time of this range."}
   :end-time {:alias :e
              :require true
              :desc "The end time of this range."}
   :date {:alias :d
          :desc "The date of both the start, and end time."}})

;; TODO: Right now this only works for today. Possibly specify a date as well.
(defn delete-range-command [{{:keys [start-time end-time date]} :opts}]
  (let [period-date (if (nil? date) (LocalDate/now) (LocalDate/parse date))
        period-start (LocalDateTime/of period-date (LocalTime/parse start-time))
        period-end (LocalDateTime/of period-date (LocalTime/parse end-time))
        to-remove
        (helpers/clocks-within-timeperiod
         @db
         period-start
         period-end)]
    (if (empty? to-remove)
      (do
        (.println ^java.io.PrintWriter *err* "No clocks were found in the period you specified.")
        (System/exit 1))
      (do
        (print-clocks to-remove)
        (println "These clocks will all be PERMANENTLY deleted. Are you sure you wish to continue? (y/N)")
        (if (= (str/trim (read-line)) "y")
          (do
            (helpers/delete-clocks @db period-start period-end)
            (println "Deleted."))
          (println "Cancelled."))))))

(defn status-command [_]
  (print-status))

(defn no-command [opts]
  ;; If args is nil, no subcommand was provided so we can assume the user wants
  ;; the status. Otherwise, we assume the user entered a subcommand that does
  ;; not exist, and thus we give them a warning.
  (if (nil? (:args opts))
    (do
      (print-status)
      (println "Run 'auto-timesheet --help' for a list of all commands."))
    (do
      (.println ^java.io.PrintWriter *err* "The command you provided does not exist. Run 'auto-timesheet --help' for a list of all commands.")
      (System/exit 1))))

(def manual-entry-spec
  (assoc range-spec :category {:alias :c
                               :require true
                               :desc "The category of this manual entry."}))

(defn manual-entry [{{:keys [start-time end-time date category]} :opts}]
  (let [start-local-time (LocalTime/parse start-time)
        end-local-time (LocalTime/parse end-time)
        local-date (if date (LocalDate/parse date))]
    (if local-date
      (helpers/manual-entry @db (LocalDateTime/of local-date start-local-time) (LocalDateTime/of local-date end-local-time) category)
      (helpers/manual-entry @db start-local-time end-local-time category))))

(defn directories [_]
  (println
   (format "Your config is stored in %s" (.dataDir ^ProjectDirectories @configuration/directories)))
  (println
   (format "The database is stored in %s" (:sql-directory @config))))

(def table
  [{:cmds ["clockin"] :fn clockin :doc "Make a clock in." :spec clock-spec}
   {:cmds ["clockout"] :fn clockout :doc "Make a clock out." :spec clock-spec}
   {:cmds ["report"] :fn report :doc "Display reports" :spec report-spec}
   {:cmds ["status"] :fn status-command :doc "Shows current clock in status"}
   {:cmds ["delete-range"] :fn delete-range-command :spec range-spec :doc "Deletes clocks within a specified range during today."}
   {:cmds ["manual-entry"] :fn manual-entry :spec manual-entry-spec :doc "Manually make a clock in, and clock out."}
   {:cmds ["directories"] :fn directories :doc "Show the directories of where the config, and database is stored."}
   {:cmds [] :fn no-command :doc "Display the status."}])

;; TODO: Might only want to init the db for some commands later.
(defn -main [& args]
  (cli/dispatch table args {:error-fn (fn [{:keys [spec type cause msg option] :as data}]
                                        (if (= :org.babashka/cli type)
                                          (.println ^java.io.PrintWriter *err* msg)
                                          (throw (ex-info msg data)))
                                        (System/exit 1))
                            :prog "auto-timesheet"
                            :help true}))

