(ns jamescrake-merani.auto-timesheet.db-helpers
  (:require [jamescrake-merani.auto-timesheet.db :as as-db])
  (:import (java.time LocalDateTime
                      LocalDate
                      LocalTime
                      DayOfWeek)))

(defn clock-in
  ([db category] (clock-in db category (LocalDateTime/now)))
  ([db category current-timestamp]
   (cond (integer? category)
         (as-db/clock-in db {:category-id category :starttime current-timestamp})
         (or (keyword? category) (string? category))
         (let [category-id (as-db/get-category-from-name db {:name category})]
           (if (nil? category-id)
             (clock-in db (:categoryid (as-db/create-category db {:name category})) current-timestamp)
             (as-db/clock-in db {:category-id (:categoryid (as-db/get-category-from-name db {:name category}))
                                 :starttime current-timestamp})))
         :else
         (throw (.Exception "Category needs to be an id, or a name.")))))

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
                               :clockoutid (:clockoutid (as-db/clock-out db {:stoptime current-timestamp}))})))

;; TODO: Doesn't do the same category checks as `clock-in`
(defn manual-entry
  [db clockin-time clockout-time category]
  (let [category-id (as-db/get-category-from-name db {:name category})
        ;; TODO: At the moment this assumes that clockin-time, and clockout-time
        ;; are both times without dates but this may not always be the case.
        clockin-starttime (LocalDateTime/of (LocalDate/now) clockin-time)
        clockout-stoptime (LocalDateTime/of (LocalDate/now) clockout-time)]
    (as-db/manual-clock-in db {:starttime clockin-starttime
                               :category-id category-id
                               :clockoutid (as-db/manual-clock-out db {:stoptime clockout-stoptime})})))

(defn clocks-in-week
  [db]
  (let [period-beginning (-> (LocalDateTime/now)
                             (.with DayOfWeek/MONDAY)
                             (.with LocalTime/MIDNIGHT))
        period-end (.plusWeeks period-beginning 1)]
    (as-db/clocks-within-timeperiod db {:periodstart period-beginning
                                        :periodend period-end})))

(defn group-clocks-by-day
  [clocks]
  (group-by
   (fn [clock]
     (.toLocalDate (LocalDateTime/parse (:starttime clock))))
   clocks))


