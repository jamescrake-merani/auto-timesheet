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
           (dev.dirs ProjectDirectories)
           (clojure.lang ExceptionInfo))
  (:gen-class))

(defn- error-and-quit [error-message]
  (do
    (.println ^java.io.PrintWriter *err* error-message)
    (System/exit 1)))

(def config (delay (configuration/load-config)))
;; TODO: I'm not sure whether this should be at this level.
(def db (delay (open-database (:sql-directory @config))))

(defn init-config
  "Initialise the config in the location with default values."
  [_]
  ;; TODO: Prompt user if it already exists
  (spit (configuration/get-config-path) (configuration/make-default-config)))

(def clock-spec
  {:category {:alias :c
              :desc "The category to clock into. This only needs to be specified if you don't have a default category in your config."}
   :force {:alias :f
           :coerce :boolean
           :desc "Create a clock in even if you are already clocked in."}
   :time {:alias :t
          :desc "The effective time of the clock. If not specified, then this will be the current time according to your system."}})

(defn- get-effective-datetime
  "Get the datetime which should be used as 'today'"
  [time-from-user]
  (if time-from-user
    (LocalDateTime/of (LocalDate/now) (LocalTime/parse time-from-user))
    (LocalDateTime/now)))

(defn clockout
  "Perform a clock out. `category` is the string of the category which is first to
  be fetched. If none is specified, the config will be checked, and if none is
  specified there either, the program will error. `time` is the effective time.
  If not specified, it'll be the current time."
  [{{:keys [category time]} :opts}]
  (let [hanging-clockins (helpers/hanging-clockins @db)
        time-since-clockin (when (not (empty? hanging-clockins))
                             (:starttime (first hanging-clockins)))
        effective-datetime (get-effective-datetime time)]
    (cond (empty? hanging-clockins)
          (error-and-quit "You are not clocked in.")
          (= (count hanging-clockins) 1)
          (helpers/clock-out @db effective-datetime)
          (nil? category)
          (error-and-quit "You have multiple clock ins. You must resolve this ambiguity by specifying a category (with the --category flag).")
          :else (helpers/clock-out @db (helpers/category-to-id @db category true) effective-datetime))
    (println (format "Clocked out. You have worked %s"
                     (format-duration (Duration/between time-since-clockin (LocalDateTime/now)))))))

(defn get-category-to-use
  "Takes in an `input-category` (which may be nil), and outputs the one to use,
  which could be the default specified in the config."
  [input-category]
  (let [proposed-category (or input-category (:default-category @config))]
    (if (nil? proposed-category)
      ;; TODO: Probably want to explain a bit better how to add a default one - perhaps link to documentation when thats available?
      (error-and-quit "You need to provide a category with clock ins as you haven't provided a default one in your config.")
      proposed-category)))

;: TODO Allow the user to disable this check.
;; TODO: Also this check only looks for all categories not one specific one.
(defn clockin
  "Perform a clock in. `category` is the string of the category which is first to
  be fetched. If none is specified, the config will be checked, and if none is
  specified there either, the program will error. The database will be checked
  to see if there already is a clock in for the category. If `force` is true,
  this check is overrided. `time` is the effective time. If not specified, it'll
  be the current time."
  [{{:keys [category force time]} :opts}]
  (let [category-to-use (get-category-to-use category)
        effective-datetime (get-effective-datetime time)]
    (cond
      (not (or (empty? (helpers/hanging-clockins @db)) force)) (println "You are already clocked in. (use the --force flag to ignore this check.)")
      :else (do
              (helpers/clock-in @db category-to-use effective-datetime)
              (println "Clocked in.")))))

(def report-spec
  {:type {:alias :t
          :spec "The type of report to generate."}
   :category {:alias :c
              :spec "Only show clocks from this specific category."}})

(defn report
  "Prints the report specified by `type`. Optionally, `category` can be specified
  which will filter clocks for just that category."
  [{{:keys [type category]} :opts}]
  (try
    (let [category-id (if category (helpers/category-to-id @db category true))
          filter-function (if (nil? category-id)
                            (constantly true)
                            #(= (:categoryid %) category-id))
          report-type (or (keyword type) (keyword (:default-report @config)))
          report-function (get reports-available report-type)]
      (if (nil? report-function)
        (error-and-quit "That report type does not exist.")
        (println (->> (report-function @db filter-function) flatten (str/join "\n")))))
    (catch ExceptionInfo e
      (error-and-quit (ex-message e)))))

(defn print-reports-available
  "Prints the reports which are available to be viewed from the report command."
  [_]
  (println "The following report types are implemented:")
  (doseq [report (keys reports-available)]
    (println "-" (name report))))

(defn format-clockin [clockin one-clockin?]
  (format "%s %s. %s elapsed since clockin."
          (if one-clockin? "You are currently clocked into" "You are clocked into")
          (:name (as-db/get-category-name-from-id @db {:id (:categoryid clockin)}))
          (format-duration (Duration/between (:starttime clockin)
                                             (LocalDateTime/now)))))

(defn print-status
  "Print the current status message."
  []
  (println
   (format "You have clocked in %s today."
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

(defn print-clocks
  "Print `clocks` into one human readable string."
  [clocks]
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
          :desc "The date of the start time."}
   :end-date-offset {:alias :o
                     :coerce :long
                     :desc "The amount of days to add onto the start date for the end date."}})

(defn handle-date-offset
  "Given `offset` (which may be nil), add that many days onto the start date, or 0
  if one is not provided."
  [start-date offset]
  (.plusDays ^LocalDate start-date (or offset 0)))

(defn parse-range [start-time end-time date-str end-date-offset]
  (let [date (if (nil? date-str) (LocalDate/now) (LocalDate/parse date-str))]
    (helpers/->TimeRange (LocalDateTime/of date (LocalTime/parse start-time))
                         (LocalDateTime/of (handle-date-offset date end-date-offset) (LocalTime/parse end-time)))))

(defn delete-range-command
  "Prompt the user to delete all clocks within a specified time range."
  [{{:keys [start-time end-time date end-date-offset]} :opts}]
  (let [time-range (parse-range start-time end-time date end-date-offset)
        to-remove
        (helpers/clocks-within-timeperiod @db time-range)]
    (if (empty? to-remove)
      (error-and-quit "No clocks were found in the period you specified.")
      (do
        (print-clocks to-remove)
        (println "These clocks will all be PERMANENTLY deleted. Are you sure you wish to continue? (y/N)")
        (if (= (str/trim (read-line)) "y")
          (do
            (helpers/delete-clocks @db time-range)
            (println "Deleted."))
          (println "Cancelled."))))))

(defn status-command
  "Print the status command."
  [_]
  (print-status))

(defn no-command
  "Print the appropriate message when no command has been provided."
  [opts]
  ;; If args is nil, no subcommand was provided so we can assume the user wants
  ;; the status. Otherwise, we assume the user entered a subcommand that does
  ;; not exist, and thus we give them a warning.
  (if (nil? (:args opts))
    (do
      (print-status)
      (println "Run 'auto-timesheet --help' for a list of all commands."))
    (error-and-quit "The command you provided does not exist. Run 'auto-timesheet --help' for a list of all commands.")))

(def manual-entry-spec
  (assoc range-spec :category {:alias :c
                               :desc "The category of this manual entry."}))

(defn manual-entry
  "Create a manual entry from values parsed from strings."
  [{{:keys [start-time end-time date end-date-offset category]} :opts}]
  (let [time-range (parse-range start-time end-time date end-date-offset)]
    (helpers/manual-entry @db time-range (get-category-to-use category))))

(defn directories
  "Print out the directories as fetched by ProjectDirectories"
  [_]
  (println
   (format "Your config is stored in %s" (.configDir ^ProjectDirectories @configuration/directories)))
  (println
   (format "The database is stored in %s" (:sql-directory @config))))

(defn valid-in-or-out?
  "Check that `value` is the string literal in, or out."
  [value]
  (contains? {"in" "out"} value))

(def amend-spec
  {:in-or-out {:alias :i
               :desc "Whether to amend the clock in, or clock out"
               :validate valid-in-or-out? ;; TODO: Add a failed validation message.
               :require true}
   :original-time {:alias :t
                   :desc "The time of the clock to change."
                   :require true}
   :new-time {:alias :n
              :desc "The new time of the clock to change."
              :require true}
   :date {:alias :d
          :desc "The date of the clock. Defaults to today."}})

(defn amend
  "Fetch a clock in/out (as specified by `in-or-out`) based on its `original-time`,
  and `date`, and amend it to `new-time`. `original-time` is expected just be a
  string with only the minute, and second components of the time. The DB will
  then look for a range between the lower, and upper limit of that time."
  [{{:keys [in-or-out original-time new-time date]} :opts}]
  (let [date-to-use (if date (LocalDate/parse date) (LocalDate/now))]
    (helpers/amend-clock
     @db
     (= in-or-out "in")
     (LocalDateTime/of date-to-use (LocalTime/parse original-time))
     (LocalDateTime/of date-to-use (LocalTime/parse new-time))))
  (println "Clock amended."))

(def table
  [{:cmds ["clockin"] :fn clockin :doc "Make a clock in." :spec clock-spec}
   {:cmds ["clockout"] :fn clockout :doc "Make a clock out." :spec clock-spec}
   {:cmds ["report"] :fn report :doc "Display reports" :spec report-spec}
   {:cmds ["reports-available"] :fn print-reports-available :doc "Shows all the reports that are available in this build."}
   {:cmds ["status"] :fn status-command :doc "Shows current clock in status"}
   {:cmds ["delete-range"] :fn delete-range-command :spec range-spec :doc "Deletes clocks within a specified range during today."}
   {:cmds ["manual-entry"] :fn manual-entry :spec manual-entry-spec :doc "Manually make a clock in, and clock out."}
   {:cmds ["directories"] :fn directories :doc "Show the directories of where the config, and database is stored."}
   {:cmds ["amend"] :fn amend :doc "Make an amendment to an existing clock in/out." :spec amend-spec}
   {:cmds [] :fn no-command :doc "Display the status."}])

;; TODO: Might only want to init the db for some commands later.
(defn -main
  "Entry function for the CLI."
  [& args]
  (cli/dispatch table args {:error-fn (fn [{:keys [spec type cause msg option] :as data}]
                                        (if (= :org.babashka/cli type)
                                          (.println ^java.io.PrintWriter *err* msg)
                                          (throw (ex-info msg data)))
                                        (System/exit 1))
                            :prog "auto-timesheet"
                            :help true}))

