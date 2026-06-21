(ns jamescrake-merani.auto-timesheet.db-helpers
  (:require [jamescrake-merani.auto-timesheet.db :as as-db])
  (:import (java.time LocalDateTime
                      LocalDate
                      LocalTime
                      DayOfWeek
                      Duration
                      Instant
                      ZoneOffset)))

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

(defn clocks-within-timeperiod [db period-start period-end]
  (as-db/clocks-within-timeperiod db {:periodstart (to-epoch period-start)
                                      :periodend (to-epoch period-end)}))

(defn clocks-in-week
  ([db] (clocks-in-week db (LocalDateTime/now)))
  ([db ^LocalDateTime date]
   (let [^LocalDateTime period-beginning (-> date
                                             (.with DayOfWeek/MONDAY)
                                             (.with LocalTime/MIDNIGHT))
         period-end (.plusWeeks period-beginning 1)]
     (as-db/clocks-within-timeperiod db {:periodstart (to-epoch period-beginning)
                                         :periodend (to-epoch period-end)}))))

(defn group-clocks-by-day
  [clocks]
  (group-by
   (fn [clock]
     (.toLocalDate (from-epoch (:starttime clock))))
   clocks))

;; Returns duration.
(defn sum-clocks [clocks]
  (reduce #(.plus ^java.time.Duration %1
                  (Duration/between (from-epoch (:starttime %2))
                                    (from-epoch (:stoptime %2))))
          Duration/ZERO clocks))

