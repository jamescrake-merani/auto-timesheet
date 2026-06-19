(ns jamescrake-merani.auto-timesheet.reports-test
  (:require [jamescrake-merani.auto-timesheet.reports :as sut]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [jamescrake-merani.auto-timesheet.db :as as-db]
            [jamescrake-merani.auto-timesheet.db-helpers :as helpers]
            [clojure.test :as t]
            [clojure.string :as str])
  (:import (java.time LocalDateTime)))

(defn- setup-db
  [entries]
  (let [db (db-init/open-database ":memory:")]
    (as-db/create-category db {:name "work"})
    (doseq [[start end] entries]
      (helpers/manual-entry db
                            (LocalDateTime/parse start)
                            (LocalDateTime/parse end)
                            "work"))
    db))

(defn- report-string
  [db date-str]
  (->> (sut/human-readable-report db (constantly true) (LocalDateTime/parse date-str))
       flatten
       (str/join "\n")))

;; Each datum is:
;;   [description
;;    reference-date-str
;;    [[entry-start entry-end] ...]
;;    expected-output-string]
(def report-test-data
  [["empty week produces empty report"
    "2026-06-11T00:00"
    []
    "Total work completed: 0 hours, 0 minutes"]

   ["single entry on Monday"
    "2026-06-11T00:00"
    [["2026-06-08T09:00" "2026-06-08T17:00"]]
    "Monday:\n09:00-17:00 (8 hours, 0 minutes)\nTotal work completed: 8 hours, 0 minutes"]

   ["multiple entries across Monday and Thursday"
    "2026-06-11T00:00"
    [["2026-06-08T10:00" "2026-06-08T12:00"]
     ["2026-06-08T13:00" "2026-06-08T16:00"]
     ["2026-06-11T02:00" "2026-06-11T09:30"]
     ["2026-06-11T15:10" "2026-06-11T16:15"]]
    "Monday:\n10:00-12:00 (2 hours, 0 minutes)\n13:00-16:00 (3 hours, 0 minutes)\nThursday:\n02:00-09:30 (7 hours, 30 minutes)\n15:10-16:15 (1 hours, 5 minutes)\nTotal work completed: 13 hours, 35 minutes"]

   ["entries on every weekday"
    "2026-06-12T00:00"
    [["2026-06-08T09:00" "2026-06-08T10:00"]
     ["2026-06-09T09:00" "2026-06-09T10:00"]
     ["2026-06-10T09:00" "2026-06-10T10:00"]
     ["2026-06-11T09:00" "2026-06-11T10:00"]
     ["2026-06-12T09:00" "2026-06-12T10:00"]]
    "Monday:\n09:00-10:00 (1 hours, 0 minutes)\nTuesday:\n09:00-10:00 (1 hours, 0 minutes)\nWednesday:\n09:00-10:00 (1 hours, 0 minutes)\nThursday:\n09:00-10:00 (1 hours, 0 minutes)\nFriday:\n09:00-10:00 (1 hours, 0 minutes)\nTotal work completed: 5 hours, 0 minutes"]

   ["weekend entries appear with correct day names"
    "2026-06-14T00:00"
    [["2026-06-13T11:00" "2026-06-13T14:30"]
     ["2026-06-14T08:00" "2026-06-14T12:00"]]
    "Saturday:\n11:00-14:30 (3 hours, 30 minutes)\nSunday:\n08:00-12:00 (4 hours, 0 minutes)\nTotal work completed: 7 hours, 30 minutes"]

   ["very short one-minute entry"
    "2026-06-11T00:00"
    [["2026-06-09T12:00" "2026-06-09T12:01"]]
    "Tuesday:\n12:00-12:01 (0 hours, 1 minutes)\nTotal work completed: 0 hours, 1 minutes"]

   ["late-night entry close to midnight"
    "2026-06-11T00:00"
    [["2026-06-10T22:00" "2026-06-10T23:59"]]
    "Wednesday:\n22:00-23:59 (1 hours, 59 minutes)\nTotal work completed: 1 hours, 59 minutes"]

   ["three entries on a single day"
    "2026-06-11T00:00"
    [["2026-06-09T08:00" "2026-06-09T10:00"]
     ["2026-06-09T11:00" "2026-06-09T12:30"]
     ["2026-06-09T14:00" "2026-06-09T16:45"]]
    "Tuesday:\n08:00-10:00 (2 hours, 0 minutes)\n11:00-12:30 (1 hours, 30 minutes)\n14:00-16:45 (2 hours, 45 minutes)\nTotal work completed: 6 hours, 15 minutes"]])

(t/deftest human-readable-report-test
  (doseq [[description ref-date entries expected] report-test-data]
    (t/testing description
      (let [db (setup-db entries)]
        (t/is (= expected (report-string db ref-date)))))))

(def category-filter-test-data
  [{:data {:work ["2026-06-08T09:00"  "2026-06-08T17:00"]
           :personal ["2026-06-08T18:00" "2026-06-08T21:00"]}
    :expected {:work "Monday:\n09:00-17:00 (8 hours, 0 minutes)\nTotal work completed: 8 hours, 0 minutes"
               :personal "Monday:\n18:00-21:00 (3 hours, 0 minutes)\nTotal work completed: 3 hours, 0 minutes"}}])

(t/deftest human-readable-report-category-filter-test
  (doseq [datum-map category-filter-test-data]
    (let [db (db-init/open-database ":memory:")]
      (doseq [to-add-key (keys datum-map)]
        (doseq [[start end] (get to-add-key datum-map)]
          (helpers/manual-entry db start end (str to-add-key))))
      (doseq [category (keys datum-map)]
        (t/is (= (-> category-filter-test-data :expected (get (keyword category)))
                 (report-string db "2026-06-08T00:00")))))))
