(ns jamescrake-merani.auto-timesheet.reports
  (:import (java.time.format DateTimeFormatter)
           (java.time Duration
                      LocalDate
                      LocalDateTime
                      DayOfWeek)
           (java.util Locale)
           (java.time.format TextStyle))
  (:require [clojure.string :as str]
            [jamescrake-merani.auto-timesheet.db-helpers :refer [group-clocks-by-day clocks-in-week]]))

(defn format-duration [^java.time.Duration d]
  (format "%d hours, %d minutes"
          (.toHoursPart d)
          (.toMinutesPart d)))

(defn format-clock [clock]
  (let [time-formatter (DateTimeFormatter/ofPattern "HH:mm")
        start-time (LocalDateTime/parse (:starttime clock))
        end-time (LocalDateTime/parse (:stoptime clock))
        clock-duration (Duration/between start-time end-time)]
    (format "%s-%s (%s)"
            (.format start-time time-formatter)
            (.format end-time time-formatter)
            (format-duration clock-duration))))

;; NOTE: These functions return lines which should later be flattened into one
;; string. This can be done in the CLI code.

;; TODO: Probably want to make all the locales configurable.
(defn day-summary [^LocalDate date clocks]
  (cons (format "%s:" (.getDisplayName (.getDayOfWeek date) TextStyle/FULL Locale/UK))
        (map format-clock clocks)))

;; TODO: Add weekly total.
(defn human-readable-summary [grouped-clocks]
  (reduce-kv (fn [lines day clocks]
               (into lines (day-summary day clocks)))
             [] grouped-clocks))

;; TODO: Reports should be able to take in parameters. For now, we need to use
;; sensible defaults.
(defn human-readable-report
  ([db] (human-readable-report db (LocalDateTime/now)))
  ([db date] (-> db (clocks-in-week date) group-clocks-by-day human-readable-summary)))

(def reports-available
  {:human-readable human-readable-report})
