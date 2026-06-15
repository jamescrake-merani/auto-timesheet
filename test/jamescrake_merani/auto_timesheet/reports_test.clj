(ns jamescrake-merani.auto-timesheet.reports-test
  (:require [jamescrake-merani.auto-timesheet.reports :as sut]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [jamescrake-merani.auto-timesheet.db :as as-db]
            [jamescrake-merani.auto-timesheet.db-helpers :as helpers]
            [clojure.test :as t]
            [clojure.string :as str])
  (:import (java.time LocalDateTime)))

(t/deftest human-readable-report-test
  (let [db (db-init/open-database ":memory:")]
    (as-db/create-category db {:name "work"})
    (helpers/manual-entry db
                          (LocalDateTime/parse "2026-06-08T10:00")
                          (LocalDateTime/parse "2026-06-08T12:00")
                          "work")
    (helpers/manual-entry db
                          (LocalDateTime/parse "2026-06-08T13:00")
                          (LocalDateTime/parse "2026-06-08T16:00")
                          "work")
    (helpers/manual-entry db
                          (LocalDateTime/parse "2026-06-11T02:00")
                          (LocalDateTime/parse "2026-06-11T09:30")
                          "work")
    (helpers/manual-entry db
                          (LocalDateTime/parse "2026-06-11T15:10")
                          (LocalDateTime/parse "2026-06-11T16:15")
                          "work")
    (t/is (=
           "Monday:
10:00-12:00 (2 hours, 0 minutes)
13:00-16:00 (3 hours, 0 minutes)
Thursday:
02:00-09:30 (7 hours, 30 minutes)
15:10-16:15 (1 hours, 5 minutes)"
           (->> (sut/human-readable-report db (LocalDateTime/parse "2026-06-11T16:15"))
                flatten
                (str/join "\n"))))))

