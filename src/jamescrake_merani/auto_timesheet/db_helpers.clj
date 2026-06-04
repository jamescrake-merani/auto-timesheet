(ns jamescrake-merani.auto-timesheet.db-helpers
  (:require [jamescrake-merani.auto-timesheet.db :as as-db])
  (:import (java.time LocalDateTime
                      LocalDate
                      LocalTime
                      DayOfWeek)))

(defn clock-in [db category]
  (cond (integer? category)
        (as-db/clock-in db {:category-id category})
        (or (keyword? category) (string? category))
        (let [category-id (as-db/get-category-from-name db {:name category})]
          (if (nil? category-id)
            (clock-in db (:categoryid (as-db/create-category db {:name category})))
            (as-db/clock-in db {:category-id (:categoryid (as-db/get-category-from-name db {:name category}))})))
        :else
        (throw (.Exception "Category needs to be an id, or a name."))))

(defn clock-out
  ([db] (clock-out db (-> (as-db/hanging-clockins db) first :clockinid)))
  ([db clockin-id] (as-db/attach-clock-out db {:clockinid clockin-id
                                               :clockoutid (as-db/clock-out db)})))

;; TODO: Doesn't do the same category checks as `clock-in`
(defn manual-entry
  [db clockin-time clockout-time category]
  (let [category-id (as-db/get-category-from-name db {:name category})
        ;; TODO: At the moment this assumes that clockin-time, and clockout-time
        ;; are both times without dates but this may not always be the case.
        clockin-timestamp (LocalDateTime/of (LocalDate/now) clockin-time)
        clockout-timestamp (LocalDateTime/of (LocalDate/now) clockout-time)]
    (as-db/manual-clock-in db {:timestamp clockin-timestamp
                               :category-id category-id
                               :clockoutid (as-db/manual-clock-out db {:timestamp clockout-timestamp})})))

(defn clocks-in-week
  [db]
  (let [period-beginning (-> (LocalDateTime/now)
                             (.with DayOfWeek/MONDAY)
                             (.with LocalTime/MIDNIGHT))
        period-end (.plusWeeks period-beginning 1)]
    (as-db/clocks-within-timeperiod db {:periodstart period-beginning
                                        :periodend period-end})))


