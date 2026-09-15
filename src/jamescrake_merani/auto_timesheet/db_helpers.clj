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

(ns jamescrake-merani.auto-timesheet.db-helpers
  (:require [jamescrake-merani.auto-timesheet.db :as as-db])
  (:import (java.time LocalDateTime
                      LocalDate
                      LocalTime
                      DayOfWeek
                      Duration
                      Instant
                      ZoneOffset)
           (java.time.temporal ChronoUnit)))

(defn- to-epoch
  "Converts `ldt` to seconds since the UTC epoch."
  [^LocalDateTime ldt]
  (.toEpochSecond ldt ZoneOffset/UTC))

(defn from-epoch
  "Converts `epoch-seconds` into a Java LocalDateTime"
  [epoch-seconds]
  (LocalDateTime/ofInstant (Instant/ofEpochSecond epoch-seconds) ZoneOffset/UTC))

(defn convert-clock
  "Takes in a `clock` map that has come straight from
  the database, and makes sure any timestamps are converted so that they use the
   Java.Date API data structures."
  [clock]
  (into {}
        (map (fn [[key value]]
               (if (contains? #{:starttime :stoptime} key)
                 [key (from-epoch value)]
                 [key value])) clock)))

(defn category-to-id
  [db raw-category]
  (cond
    (integer? raw-category) raw-category
    (or (keyword? raw-category) (string? raw-category))
    (let [category-id (as-db/get-category-from-name db {:name raw-category})]
      (if (nil? category-id)
        (as-db/create-category db {:name raw-category})
        category-id))
    :else
    (throw (Exception. "Category needs to be an id, or a name."))))

(defn clock-in
  "Make a clock in. `category` needs to be specified, and optionally
  `current-timestamp` can be specified if the clock in is to be made at a
  specific time. Otherwise, `current-timestamp` is taken to be the current
  time."
  ([db category] (clock-in db category (LocalDateTime/now)))
  ([db category current-timestamp]
   (as-db/clock-in db {:category-id (category-to-id db category)
                       :starttime (to-epoch current-timestamp)})))

(defn- hanging-clockin-id [db]
  (-> (as-db/hanging-clockins db) first :clockinid))

(defn hanging-clockins
  "Checks to see if there are any clock ins that don't have an associated clock out."
  [db]
  (map convert-clock (as-db/hanging-clockins db)))

(defn clock-out
  "Make a clock out. Without providing any arguments, the clock out will be
  attached to the last clock in, but you can optionally a specific clock in id.
  The clock out is taken to be `current-timestamp` if specified. Otherwise, the
  current time will be used."
  ([db] (clock-out db (hanging-clockin-id db) (LocalDateTime/now)))
  ([db clockin-id-or-timestamp]
   (if (integer? clockin-id-or-timestamp)
     (clock-out db clockin-id-or-timestamp (LocalDateTime/now))
     (clock-out db (hanging-clockin-id db) clockin-id-or-timestamp)))
  ([db clockin-id current-timestamp]
   (as-db/attach-clock-out db {:clockinid clockin-id
                               :clockoutid (:clockoutid (as-db/clock-out db {:stoptime (to-epoch current-timestamp)}))})))

(defn- full-date [date-or-time]
  (if (instance? LocalDateTime date-or-time)
    date-or-time
    (LocalDateTime/of (LocalDate/now) date-or-time)))

(defn manual-entry
  "Manually create a clock in, and clock out in one go by specifying the times for
  clock in, and clock out."
  [db clockin-time clockout-time category]
  (let [category-id (:categoryid (category-to-id category))
        ;; TODO: At the moment this assumes that clockin-time, and clockout-time
        ;; are both times without dates but this may not always be the case.
        clockin-starttime (full-date clockin-time)
        clockout-stoptime (full-date clockout-time)]
    (as-db/manual-clock-in db {:starttime (to-epoch clockin-starttime)
                               :category-id category-id
                               :clockoutid (:clockoutid (as-db/clock-out db {:stoptime (to-epoch clockout-stoptime)}))})))

(defn delete-clocks
  "Delete all clock ins, and clock outs which lie within `period-start`, and `period-end`."
  [db period-start period-end]
  (as-db/delete-clockouts-within-timeperiod db {:periodstart (to-epoch period-start)
                                                :periodend (to-epoch period-end)})
  (as-db/delete-clockins-within-timeperiod db {:periodstart (to-epoch period-start)
                                               :periodend (to-epoch period-end)}))
(defn clocks-within-timeperiod
  "Return all the clocks that fall within `period-start`, and `period-end`."
  ([db period-start period-end]
   (clocks-within-timeperiod db period-start period-end true))
  ([db period-start period-end use-starttime?]
   (map convert-clock
        (as-db/clocks-within-timeperiod
         db
         {:periodstart (to-epoch period-start)
          :periodend (to-epoch period-end)
          :timeparam (if use-starttime? "starttime" "stoptime")}))))

(defn clocks-within-date
  "Get all the clocks within the day specifed by `date`"
  [db ^LocalDate date]
  (clocks-within-timeperiod db
                            (LocalDateTime/of date LocalTime/MIDNIGHT)
                            (LocalDateTime/of (.plusDays date 1) LocalTime/MIDNIGHT)))

(defn clocks-in-week
  "Get all the clocks with the week of `date`. If `date` is not specified, then
  get all the clocks within the current week."
  ([db] (clocks-in-week db (LocalDateTime/now)))
  ([db ^LocalDateTime date]
   (let [^LocalDateTime period-beginning (-> date
                                             (.with DayOfWeek/MONDAY)
                                             (.with LocalTime/MIDNIGHT))
         period-end (.plusWeeks period-beginning 1)]
     (clocks-within-timeperiod db period-beginning period-end))))

(defn all-clocks
  "Get all of the clocks that are recorded in the database."
  [db]
  (map convert-clock (as-db/all-clocks db)))

(defn group-clocks-by-day
  "Create a hashmap with days as the key, and clocks that fall within that day as the value."
  [clocks]
  (group-by
   (fn [clock]
     (.toLocalDate ^LocalDateTime (:starttime clock)))
   clocks))

(defn get-clock-at-time
  "Get a clock at a specific `datetime`. This will look at the day, hour, and
  minute values of the object, and then look for everything between the
  beginning, and end of that period."
  [db ^LocalDateTime datetime clock-in?]
  ;; TODO: Assumes there will only be one clock at that time. This could fail if
  ;; this assumption is not true.
  (first
   (clocks-within-timeperiod
    db
    (.truncatedTo datetime ChronoUnit/MINUTES)
    (-> datetime (.withSecond 59) (.withNano 999999999))
    clock-in?)))

(defn amend-clock
  "Find the clock out (or clock-in if `clock-in?` is true) at `oldtime`, and replace it with `newtime`."
  [db clock-in? ^LocalDateTime oldtime ^LocalDateTime newtime]
  (let [to-amend (get-clock-at-time db oldtime clock-in?)]
    (if clock-in?
      (as-db/amend-clockin db {:newstarttime (to-epoch newtime)
                               :clockinid (:clockinid to-amend)})
      (as-db/amend-clockout db {:newstoptime (to-epoch newtime)
                                :clockoutid (:clockoutid to-amend)}))))

;; Returns duration.
(defn sum-clocks
  "Return the duration of all the clocks in `clocks`."
  [clocks]
  (reduce #(.plus ^java.time.Duration %1
                  (Duration/between (:starttime %2)
                                    (:stoptime %2)))
          Duration/ZERO clocks))

