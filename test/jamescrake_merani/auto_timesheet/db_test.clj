(ns jamescrake-merani.auto-timesheet.db-test
  (:require [jamescrake-merani.auto-timesheet.db-helpers :as sut]
            [jamescrake-merani.auto-timesheet.db :as db-raw]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [clojure.test :as t])
  (:import (java.time LocalDateTime
                      LocalDate
                      LocalTime
                      Duration)))

(t/deftest clockin-clockout-test
  (let [db (db-init/open-database ":memory:")]
    (t/is (= (count (db-raw/hanging-clockins db)) 0))
    (sut/clock-in db "test")
    (t/is (= (count (db-raw/hanging-clockins db)) 1))
    (sut/clock-out db)
    (t/is (= (count (db-raw/hanging-clockins db)) 0))))

(def duration-test-data
  [[(LocalDateTime/of 2026 6 11 10 00) (LocalDateTime/of 2026 6 11 12 00) 120]
   [(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 8 01) 1]
   [(LocalDateTime/of 2026 6 11 14 00) (LocalDateTime/of 2026 6 11 14 30) 30]
   [(LocalDateTime/of 2026 6 11 9 15) (LocalDateTime/of 2026 6 11 10 00) 45]
   [(LocalDateTime/of 2026 6 11 12 00) (LocalDateTime/of 2026 6 11 12 00) 0]
   [(LocalDateTime/of 2026 6 11 6 00) (LocalDateTime/of 2026 6 11 9 00) 180]
   [(LocalDateTime/of 2026 6 11 7 30) (LocalDateTime/of 2026 6 11 13 00) 330]
   [(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 17 00) 480]
   [(LocalDateTime/of 2026 6 11 16 45) (LocalDateTime/of 2026 6 11 17 00) 15]
   [(LocalDateTime/of 2026 6 11 22 00) (LocalDateTime/of 2026 6 11 23 59) 119]])

(defn verify-duration [clock expected-minutes]
  (t/is
   (= (.toMinutes
       (Duration/between (LocalDateTime/parse (:starttime clock))
                         (LocalDateTime/parse (:stoptime clock))))
      expected-minutes)))

(t/deftest clockin-duration-test
  (doseq [datum duration-test-data]
    (let [db (db-init/open-database ":memory:")]
      (sut/clock-in db "test" (first datum))
      (sut/clock-out db (second datum))
      (let [full-clock (first
                        (db-raw/clocks-within-timeperiod
                         db {:periodstart (LocalDateTime/of 2026 6 11 0 0)
                             :periodend (LocalDateTime/of 2026 6 11 23 59)}))]
        (verify-duration full-clock (nth datum 2))))))

(t/deftest manual-clock-duration-test
  (doseq [datum duration-test-data]
    (let [db (db-init/open-database ":memory:")]
      (db-raw/create-category db {:name "test"})
      (sut/manual-entry db (.toLocalTime (first datum)) (.toLocalTime (second datum)) "test")
      (let [full-clock (first (db-raw/clocks-within-timeperiod
                               db {:periodstart (LocalDateTime/of (LocalDate/now) (LocalTime/of 0 0))
                                   :periodend (LocalDateTime/of (LocalDate/now) (LocalTime/of 23 59))}))]
        (verify-duration full-clock (nth datum 2))))))

(def deletion-clock-test-data
  [{:clocks [[(LocalDateTime/of 2026 6 11 10 00) (LocalDateTime/of 2026 6 11 15 00)]]
    :period [(LocalDateTime/of 2026 6 11 00 00) (LocalDateTime/of 2026 6 11 20 00)]
    :expected-remaining-clocks 0}])

(t/deftest deletion-test
  (let [db (db-init/open-database ":memory:")]
    (doseq [datum deletion-clock-test-data]
      (db-raw/create-category db {:name "test"})
      (doseq [[clock-start clock-end] (:clocks datum)]
        (sut/manual-entry db clock-start clock-end "test"))
      (sut/delete-clocks db (first (:period datum)) (second (:period datum)))
      (t/is (= (count (db-raw/all-clocks db)) (:expected-remaining-clocks datum)))
      (t/is (= (count (db-raw/clocks-within-timeperiod (:periodstart (first (:period datum))
                                                                     (second (:period datum)))))
               0)))))
