(ns jamescrake-merani.auto-timesheet.db-test
  (:require [jamescrake-merani.auto-timesheet.db-helpers :as sut]
            [jamescrake-merani.auto-timesheet.db :as db-raw]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [clojure.test :as t])
  (:import (java.time LocalDateTime
                      LocalDate
                      LocalTime
                      Duration)))

(def db nil)

(defn db-test-fixture [f]
  (set! db (db-init/open-database ":memory:"))
  (f))

;; Each, because we want to empty the database for each test run.
(t/use-fixtures :each db-test-fixture)

(t/deftest clockin-clockout-test
  (let [db (db-init/open-database ":memory:")]
    (t/is (= (count (sut/hanging-clockins db)) 0))
    (sut/clock-in db "test")
    (t/is (= (count (sut/hanging-clockins db)) 1))
    (sut/clock-out db)
    (t/is (= (count (sut/hanging-clockins db)) 0))))

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
       (Duration/between (:starttime clock)
                         (:stoptime clock)))
      expected-minutes)))

(t/deftest clockin-duration-test
  (doseq [datum duration-test-data]
    (let [db (db-init/open-database ":memory:")]
      (sut/clock-in db "test" (first datum))
      (sut/clock-out db (second datum))
      (let [full-clock (first
                        (sut/clocks-within-timeperiod
                         db (LocalDateTime/of 2026 6 11 0 0)
                         (LocalDateTime/of 2026 6 11 23 59)))]
        (verify-duration full-clock (nth datum 2))))))

(t/deftest manual-clock-duration-test
  (doseq [datum duration-test-data]
    (let [db (db-init/open-database ":memory:")]
      (db-raw/create-category db {:name "test"})
      (sut/manual-entry db (.toLocalTime (first datum)) (.toLocalTime (second datum)) "test")
      (let [full-clock (first (sut/clocks-within-timeperiod
                               db (LocalDateTime/of (LocalDate/now) (LocalTime/of 0 0))
                               (LocalDateTime/of (LocalDate/now) (LocalTime/of 23 59))))]
        (verify-duration full-clock (nth datum 2))))))

(def deletion-clock-test-data
  [;; Single clock fully within deletion period
   {:clocks [[(LocalDateTime/of 2026 6 11 10 00) (LocalDateTime/of 2026 6 11 15 00)]]
    :period [(LocalDateTime/of 2026 6 11 00 00) (LocalDateTime/of 2026 6 11 20 00)]
    :expected-remaining-clocks 0}
   ;; Multiple clocks, all within the deletion period
   {:clocks [[(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 9 00)]
             [(LocalDateTime/of 2026 6 11 10 00) (LocalDateTime/of 2026 6 11 12 00)]
             [(LocalDateTime/of 2026 6 11 14 00) (LocalDateTime/of 2026 6 11 16 00)]]
    :period [(LocalDateTime/of 2026 6 11 00 00) (LocalDateTime/of 2026 6 11 23 59)]
    :expected-remaining-clocks 0}
   ;; Single clock starts before the deletion period — should survive
   {:clocks [[(LocalDateTime/of 2026 6 10 22 00) (LocalDateTime/of 2026 6 10 23 00)]]
    :period [(LocalDateTime/of 2026 6 11 00 00) (LocalDateTime/of 2026 6 11 23 59)]
    :expected-remaining-clocks 1}
   ;; Two clocks: one inside period (deleted), one outside (survives)
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 10 00)]
             [(LocalDateTime/of 2026 6 12 9 00) (LocalDateTime/of 2026 6 12 10 00)]]
    :period [(LocalDateTime/of 2026 6 11 00 00) (LocalDateTime/of 2026 6 11 23 59)]
    :expected-remaining-clocks 1}
   ;; Clock starting exactly on the period start boundary — should be deleted
   {:clocks [[(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 9 30)]]
    :period [(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 17 00)]
    :expected-remaining-clocks 0}
   ;; Clock starting exactly on the period end boundary — should be deleted
   {:clocks [[(LocalDateTime/of 2026 6 11 17 00) (LocalDateTime/of 2026 6 11 18 00)]]
    :period [(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 17 00)]
    :expected-remaining-clocks 0}
   ;; Three clocks across multiple days, period covers only the middle day
   {:clocks [[(LocalDateTime/of 2026 6 10 14 00) (LocalDateTime/of 2026 6 10 16 00)]
             [(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]
             [(LocalDateTime/of 2026 6 12 10 00) (LocalDateTime/of 2026 6 12 11 00)]]
    :period [(LocalDateTime/of 2026 6 11 00 00) (LocalDateTime/of 2026 6 11 23 59)]
    :expected-remaining-clocks 2}
   ;; Narrow deletion window removes only the matching clock
   {:clocks [[(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 9 00)]
             [(LocalDateTime/of 2026 6 11 10 00) (LocalDateTime/of 2026 6 11 11 00)]
             [(LocalDateTime/of 2026 6 11 14 00) (LocalDateTime/of 2026 6 11 15 00)]]
    :period [(LocalDateTime/of 2026 6 11 9 30) (LocalDateTime/of 2026 6 11 12 00)]
    :expected-remaining-clocks 2}
   ;; Deletion period matches no clocks — all survive
   {:clocks [[(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 9 00)]
             [(LocalDateTime/of 2026 6 11 15 00) (LocalDateTime/of 2026 6 11 16 00)]]
    :period [(LocalDateTime/of 2026 6 11 10 00) (LocalDateTime/of 2026 6 11 14 00)]
    :expected-remaining-clocks 2}])

(t/deftest deletion-test
  (doseq [datum deletion-clock-test-data]
    (let [db (db-init/open-database ":memory:")]
      (db-raw/create-category db {:name "test"})
      (doseq [[clock-start clock-end] (:clocks datum)]
        (sut/manual-entry db clock-start clock-end "test"))
      (sut/delete-clocks db (first (:period datum)) (second (:period datum)))
      (t/is (= (count (sut/all-clocks db)) (:expected-remaining-clocks datum)))
      (t/is (= (count (sut/clocks-within-timeperiod db (first (:period datum))
                                                    (second (:period datum))))
               0)))))

(def within-day-clock-test-data
  [{:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]
             [(LocalDateTime/of 2026 6 11 13 00) (LocalDateTime/of 2026 6 11 17 00)]
             [(LocalDateTime/of 2026 6 12 9 00) (LocalDateTime/of 2026 6 12 12 00)]]
    :expected-duration (Duration/ofHours 7)}
   {:clocks [[(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 10 00)]
             [(LocalDateTime/of 2026 6 11 10 30) (LocalDateTime/of 2026 6 11 12 30)]
             [(LocalDateTime/of 2026 6 11 14 00) (LocalDateTime/of 2026 6 11 16 00)]]
    :expected-duration (Duration/ofHours 6)}
   {:clocks [[(LocalDateTime/of 2026 6 10 9 00) (LocalDateTime/of 2026 6 10 12 00)]
             [(LocalDateTime/of 2026 6 12 13 00) (LocalDateTime/of 2026 6 12 17 00)]
             [(LocalDateTime/of 2026 6 13 8 00) (LocalDateTime/of 2026 6 13 10 00)]]
    :expected-duration (Duration/ofHours 0)}
   {:clocks [[(LocalDateTime/of 2026 6 11 7 00) (LocalDateTime/of 2026 6 11 9 00)]
             [(LocalDateTime/of 2026 6 9 10 00) (LocalDateTime/of 2026 6 9 13 00)]
             [(LocalDateTime/of 2026 6 11 18 00) (LocalDateTime/of 2026 6 11 20 00)]
             [(LocalDateTime/of 2026 6 14 8 00) (LocalDateTime/of 2026 6 14 12 00)]]
    :expected-duration (Duration/ofHours 4)}])

(t/deftest within-day-clocks
  (doseq [datum within-day-clock-test-data]
    (let [db (db-init/open-database ":memory:")]
      (db-raw/create-category db {:name "test"})
      (doseq [[clock-start clock-end] (:clocks datum)]
        (sut/manual-entry db clock-start clock-end "test"))
      (t/is (= (sut/sum-clocks (sut/clocks-within-date db (LocalDate/of 2026 6 11))) (:expected-duration datum))))))

(def amendment-test-data
  [{:clocks [[(LocalDateTime/of 2026 6 11 12 00 25) (LocalDateTime/of 2026 6 11 15 00)]]
    :amendments [{:clock-in? true :oldtime (LocalDateTime/of 2026 6 11 12 00) :newtime (LocalDateTime/of 2026 6 11 9 00)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 15 00)]]}
   ;; Amend a clock-out time
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 17 00)]]
    :amendments [{:clock-in? false :oldtime (LocalDateTime/of 2026 6 11 17 00) :newtime (LocalDateTime/of 2026 6 11 16 30)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 16 30)]]}
   ;; Amend a clock-in to a later time
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]]
    :amendments [{:clock-in? true :oldtime (LocalDateTime/of 2026 6 11 9 00) :newtime (LocalDateTime/of 2026 6 11 9 30)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 9 30) (LocalDateTime/of 2026 6 11 12 00)]]}
   ;; Amend both the clock-in, and the clock-out of the same clock
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 17 00)]]
    :amendments [{:clock-in? true :oldtime (LocalDateTime/of 2026 6 11 9 00) :newtime (LocalDateTime/of 2026 6 11 8 30)}
                 {:clock-in? false :oldtime (LocalDateTime/of 2026 6 11 17 00) :newtime (LocalDateTime/of 2026 6 11 17 45)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 8 30) (LocalDateTime/of 2026 6 11 17 45)]]}
   ;; No amendments: clocks remain unchanged
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]
             [(LocalDateTime/of 2026 6 11 13 00) (LocalDateTime/of 2026 6 11 17 00)]]
    :amendments []
    :clocks-now [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]
                 [(LocalDateTime/of 2026 6 11 13 00) (LocalDateTime/of 2026 6 11 17 00)]]}
   ;; Multiple clocks: only one clock-out is amended; the rest are untouched
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]
             [(LocalDateTime/of 2026 6 11 13 00) (LocalDateTime/of 2026 6 11 17 00)]]
    :amendments [{:clock-in? false :oldtime (LocalDateTime/of 2026 6 11 17 00) :newtime (LocalDateTime/of 2026 6 11 16 00)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]
                 [(LocalDateTime/of 2026 6 11 13 00) (LocalDateTime/of 2026 6 11 16 00)]]}
   ;; Clock-out recorded with seconds: the amendment still matches by the minute
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 17 00 45)]]
    :amendments [{:clock-in? false :oldtime (LocalDateTime/of 2026 6 11 17 00) :newtime (LocalDateTime/of 2026 6 11 18 00)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 18 00)]]}
   ;; Amend one end of each of two different clocks
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]
             [(LocalDateTime/of 2026 6 11 13 00) (LocalDateTime/of 2026 6 11 17 00)]]
    :amendments [{:clock-in? true :oldtime (LocalDateTime/of 2026 6 11 9 00) :newtime (LocalDateTime/of 2026 6 11 8 45)}
                 {:clock-in? false :oldtime (LocalDateTime/of 2026 6 11 17 00) :newtime (LocalDateTime/of 2026 6 11 17 15)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 8 45) (LocalDateTime/of 2026 6 11 12 00)]
                 [(LocalDateTime/of 2026 6 11 13 00) (LocalDateTime/of 2026 6 11 17 15)]]}
   ;; Chained amendments to the same clock-in
   {:clocks [[(LocalDateTime/of 2026 6 11 9 00) (LocalDateTime/of 2026 6 11 12 00)]]
    :amendments [{:clock-in? true :oldtime (LocalDateTime/of 2026 6 11 9 00) :newtime (LocalDateTime/of 2026 6 11 8 00)}
                 {:clock-in? true :oldtime (LocalDateTime/of 2026 6 11 8 00) :newtime (LocalDateTime/of 2026 6 11 7 30)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 7 30) (LocalDateTime/of 2026 6 11 12 00)]]}
   ;; Three clocks: amend the middle clock's clock-in only
   {:clocks [[(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 10 00)]
             [(LocalDateTime/of 2026 6 11 11 00) (LocalDateTime/of 2026 6 11 13 00)]
             [(LocalDateTime/of 2026 6 11 14 00) (LocalDateTime/of 2026 6 11 18 00)]]
    :amendments [{:clock-in? true :oldtime (LocalDateTime/of 2026 6 11 11 00) :newtime (LocalDateTime/of 2026 6 11 11 15)}]
    :clocks-now [[(LocalDateTime/of 2026 6 11 8 00) (LocalDateTime/of 2026 6 11 10 00)]
                 [(LocalDateTime/of 2026 6 11 11 15) (LocalDateTime/of 2026 6 11 13 00)]
                 [(LocalDateTime/of 2026 6 11 14 00) (LocalDateTime/of 2026 6 11 18 00)]]}])

(t/deftest amendment-test
  (doseq [datum amendment-test-data]
    (let [db (db-init/open-database ":memory:")]
      (db-raw/create-category db {:name "test"})
      (doseq [[clock-start clock-end] (:clocks datum)]
        (sut/manual-entry db clock-start clock-end "test"))
      (doseq [amendment (:amendments datum)]
        (sut/amend-clock db (:clock-in? amendment) (:oldtime amendment) (:newtime amendment)))
      (let [new-clocks (map #(vector (:starttime %) (:stoptime %)) (map sut/convert-clock (db-raw/all-clocks db)))]
        (t/is (= (frequencies (:clocks-now datum))
                 (frequencies new-clocks)))))))
