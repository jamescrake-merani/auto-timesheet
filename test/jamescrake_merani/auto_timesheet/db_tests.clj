(ns jamescrake-merani.auto-timesheet.db-tests
  (:require [jamescrake-merani.auto-timesheet.db-helpers :as sut]
            [jamescrake-merani.auto-timesheet.db :as db-raw]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [clojure.test :as t]))

(def db (db-init/open-database ":memory:"))

(t/deftest clockin-clockout-test
  (t/is (count (db-raw/hanging-clockins db)) 0)
  (sut/clock-in db "test")
  (t/is (count (db-raw/hanging-clockins db)) 1)
  (sut/clock-out db)
  (t/is (count (db-raw/hanging-clockins db)) 0))
