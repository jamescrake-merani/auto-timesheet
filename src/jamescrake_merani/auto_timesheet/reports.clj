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

(ns jamescrake-merani.auto-timesheet.reports
  (:import (java.time.format DateTimeFormatter)
           (java.time Duration
                      LocalDate
                      LocalDateTime
                      DayOfWeek)
           (java.util Locale)
           (java.time.format TextStyle))
  (:require [clojure.string :as str]
            [jamescrake-merani.auto-timesheet.db-helpers :refer [group-clocks-by-day clocks-in-week sum-clocks]]))

(defn format-duration [^java.time.Duration d]
  (format "%d hours, %d minutes"
          (.toHours d)
          (.toMinutesPart d)))

(defn format-clock [clock]
  (let [time-formatter (DateTimeFormatter/ofPattern "HH:mm")
        ^LocalDateTime start-time (:starttime clock)
        ^LocalDateTime end-time (:stoptime clock)
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
  (conj
   (reduce-kv (fn [lines day clocks]
                (into lines (day-summary day clocks)))
              [] grouped-clocks)
   (format "Total work completed: %s" (-> grouped-clocks
                                          vals
                                          flatten
                                          sum-clocks
                                          format-duration))))

;; TODO: Reports should be able to take in parameters. For now, we need to use
;; sensible defaults.
(defn human-readable-report
  ([db] (human-readable-report db (constantly true) (LocalDateTime/now)))
  ([db filter-function] (human-readable-report db filter-function (LocalDateTime/now)))
  ([db filter-function date] (->> (clocks-in-week db date) (filter filter-function) group-clocks-by-day human-readable-summary)))

(def reports-available
  {:human-readable human-readable-report})
