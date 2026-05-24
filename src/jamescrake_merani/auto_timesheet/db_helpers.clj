(ns jamescrake-merani.auto-timesheet.db-helpers
  (:require [jamescrake-merani.auto-timesheet.db :as as-db]))

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
  ([db clockin-id] (as-db/clock-out {:clockinid clockin-id})))

