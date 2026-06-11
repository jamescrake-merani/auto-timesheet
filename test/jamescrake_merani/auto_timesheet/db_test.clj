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

(t/deftest clockin-duration-test
  (let [db (db-init/open-database ":memory:")]
    (sut/clock-in db "test" (LocalDateTime/of 2026 6 11 10 00))
    (sut/clock-out db (LocalDateTime/of 2026 6 11 12 00))
    (let [full-clock (first
                      (db-raw/clocks-within-timeperiod
                       db {:periodstart (LocalDateTime/of 2026 6 11 0 0)
                           :periodend (LocalDateTime/of 2026 6 11 23 59)}))]
      (t/is
       (.toMinutes
        (Duration/between (LocalDateTime/parse (:starttime full-clock))
                          (LocalDateTime/parse (:stoptime full-clock))))
       120))))

