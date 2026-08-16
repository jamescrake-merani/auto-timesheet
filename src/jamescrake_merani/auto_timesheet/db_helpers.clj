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

(defn- to-epoch [^LocalDateTime ldt]
  (.toEpochSecond ldt ZoneOffset/UTC))

(defn from-epoch [epoch-seconds]
  (LocalDateTime/ofInstant (Instant/ofEpochSecond epoch-seconds) ZoneOffset/UTC))

(defn convert-clock [clock]
  (into {}
        (map (fn [[key value]]
               (if (contains? #{:starttime :stoptime} key)
                 [key (from-epoch value)]
                 [key value])) clock)))

(defn clock-in
  ([db category] (clock-in db category (LocalDateTime/now)))
  ([db category current-timestamp]
   (cond (integer? category)
         (as-db/clock-in db {:category-id category :starttime (to-epoch current-timestamp)})
         (or (keyword? category) (string? category))
         (let [category-id (as-db/get-category-from-name db {:name category})]
           (if (nil? category-id)
             (clock-in db (:categoryid (as-db/create-category db {:name category})) current-timestamp)
             (as-db/clock-in db {:category-id (:categoryid (as-db/get-category-from-name db {:name category}))
                                 :starttime (to-epoch current-timestamp)})))
         :else
         (throw (Exception. "Category needs to be an id, or a name.")))))

(defn- hanging-clockin-id [db]
  (-> (as-db/hanging-clockins db) first :clockinid))

(defn hanging-clockins [db]
  (map convert-clock (as-db/hanging-clockins db)))

(defn clock-out
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

;; TODO: Doesn't do the same category checks as `clock-in`
(defn manual-entry
  [db clockin-time clockout-time category]
  (let [category-id (:categoryid (as-db/get-category-from-name db {:name category}))
        ;; TODO: At the moment this assumes that clockin-time, and clockout-time
        ;; are both times without dates but this may not always be the case.
        clockin-starttime (full-date clockin-time)
        clockout-stoptime (full-date clockout-time)]
    (as-db/manual-clock-in db {:starttime (to-epoch clockin-starttime)
                               :category-id category-id
                               :clockoutid (:clockoutid (as-db/clock-out db {:stoptime (to-epoch clockout-stoptime)}))})))

(defn delete-clocks [db period-start period-end]
  (as-db/delete-clockouts-within-timeperiod db {:periodstart (to-epoch period-start)
                                                :periodend (to-epoch period-end)})
  (as-db/delete-clockins-within-timeperiod db {:periodstart (to-epoch period-start)
                                               :periodend (to-epoch period-end)}))
(defn clocks-within-timeperiod
  ([db period-start period-end]
   (clocks-within-timeperiod db period-start period-end true))
  ([db period-start period-end use-starttime?]
   (map convert-clock
        (as-db/clocks-within-timeperiod
         db
         {:periodstart (to-epoch period-start)
          :periodend (to-epoch period-end)
          :timeparam (if use-starttime? :starttime :stoptime)}))))

(defn clocks-within-date [db ^LocalDate date]
  (clocks-within-timeperiod db
                            (LocalDateTime/of date LocalTime/MIDNIGHT)
                            (LocalDateTime/of (.plusDays date 1) LocalTime/MIDNIGHT)))

(defn clocks-in-week
  ([db] (clocks-in-week db (LocalDateTime/now)))
  ([db ^LocalDateTime date]
   (let [^LocalDateTime period-beginning (-> date
                                             (.with DayOfWeek/MONDAY)
                                             (.with LocalTime/MIDNIGHT))
         period-end (.plusWeeks period-beginning 1)]
     (map convert-clock
          (as-db/clocks-within-timeperiod db {:periodstart (to-epoch period-beginning)
                                              :periodend (to-epoch period-end)})))))

(defn all-clocks [db]
  (map convert-clock (as-db/all-clocks db)))

(defn group-clocks-by-day
  [clocks]
  (group-by
   (fn [clock]
     (.toLocalDate ^LocalDateTime (:starttime clock)))
   clocks))

(defn get-clock-at-time [db ^LocalDateTime datetime]
  (clocks-within-timeperiod db
                            (.truncatedTo datetime ChronoUnit/MINUTES)
                            (-> datetime (.withSecond 59) (.withNano 999999999))))

;; For these two functions: clock should be converted with `convert-clock`
(defn amend-clockin [db clock ^LocalDateTime new-start-date]
  (as-db/amend-clockin db {:newstarttime (.toEpochSecond new-start-date)
                           :oldstarttime (.toEpochSecond ^LocalDateTime (:starttime clock))}))

;; Returns duration.
(defn sum-clocks [clocks]
  (reduce #(.plus ^java.time.Duration %1
                  (Duration/between (:starttime %2)
                                    (:stoptime %2)))
          Duration/ZERO clocks))

