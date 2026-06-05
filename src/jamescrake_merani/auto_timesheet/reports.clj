(ns jamescrake-merani.auto-timesheet.reports
  (:import (java.time.format DateTimeFormatter)
           (java.time Duration
                      LocalDateTime
                      DayOfWeek)
           (java.util Locale)
           (java.time.format TextStyle))
  (:require [clojure.string :as str]))

(defn format-clock [clock]
  (let [time-formatter (DateTimeFormatter/ofPattern "HH:mm")
        start-time (LocalDateTime/parse (:starttime clock))
        end-time (LocalDateTime/parse (:stoptime clock))
        clock-duration (Duration/between start-time end-time)]
    (format "%s-%s (%d hours, %d minutes)"
            (.format start-time time-formatter)
            (.format end-time time-formatter)
            (.toHoursPart clock-duration)
            (.toMinutesPart clock-duration))))

;; NOTE: These functions return lines which should later be flattened into one
;; string. This can be done in the CLI code.

;; TODO: Probably want to make all the locales configurable.
(defn day-summary [date clocks]
  (cons (format "%s:" (.getDisplayName (.getDayOfWeek date) TextStyle/FULL Locale/UK))
        (map format-clock clocks)))

;; TODO: Add weekly total.
(defn human-readable-summary [grouped-clocks]
  (reduce-kv (fn [lines day clocks]
               (cons (day-summary day clocks) lines))
             [] grouped-clocks))
