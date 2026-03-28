(ns jamescrake-merani.auto-timesheet.db-helpers
  (:require [jamescrake-merani.auto-timesheet.db :as as-db]))

(defn clock-in [category]
  (cond (integer? category) (as-db/clock-in {:category-id category})
        (or (keyword? category) (string? category)) (as-db/clock-in {:category-id (as-db/get-category-from-name category)})
        :else (throw (.Exception "Category needs to be an id, or a name."))))

