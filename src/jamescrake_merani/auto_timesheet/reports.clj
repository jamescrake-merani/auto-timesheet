(ns jamescrake-merani.auto-timesheet.reports
  (:import (java.time.format DateTimeFormatter)
           (java.time Duration
                      LocalDateTime
                      DayOfWeek))
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

(defn day-summary [date clocks]
  (str/join "\n" (cons (format "~s:" (.ToString (.getDayOfWeek date)))
                       (map format-clock clocks))))

;; TODO: Add weekly total.
(defn human-readable-summary [grouped-clocks]
  (reduce (fn [lines day clocks]
            (cons (day-summary day clocks) lines))
          [] (keys grouped-clocks) (vals grouped-clocks)))
