(ns jamescrake-merani.auto-timesheet.db-test
  (:require [jamescrake-merani.auto-timesheet.db-helpers :as sut]
            [jamescrake-merani.auto-timesheet.db :as db-raw]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [clojure.test :as t])
  (:import (java.time LocalDateTime
                      Duration)))

(t/deftest clockin-clockout-test
  (let [db (db-init/open-database ":memory:")]
    (t/is (count (db-raw/hanging-clockins db)) 0)
    (sut/clock-in db "test")
    (t/is (count (db-raw/hanging-clockins db)) 1)
    (sut/clock-out db)
    (t/is (count (db-raw/hanging-clockins db)) 0)))

(def duration-test-data
  [[(LocalDateTime/of 2026 6 11 10 00) (LocalDateTime/of 2026 6 11 12 00) 120]])

(t/deftest clockin-duration-test
  (doseq [datum duration-test-data]
    (let [db (db-init/open-database ":memory:")]
      (sut/clock-in db "test" (first datum))
      (sut/clock-out db (second datum))
      (let [full-clock (first
                        (db-raw/clocks-within-timeperiod
                         db {:periodstart (LocalDateTime/of 2026 6 11 0 0)
                             :periodend (LocalDateTime/of 2026 6 11 23 59)}))]
        (t/is
         (.toMinutes
          (Duration/between (LocalDateTime/parse (:starttime full-clock))
                            (LocalDateTime/parse (:stoptime full-clock))))
         (nth datum 2))))))

