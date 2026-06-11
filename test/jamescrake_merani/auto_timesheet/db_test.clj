(ns jamescrake-merani.auto-timesheet.db-test
  (:require [jamescrake-merani.auto-timesheet.db-helpers :as sut]
            [jamescrake-merani.auto-timesheet.db :as db-raw]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [clojure.test :as t]))

(t/deftest clockin-clockout-test
  (let [db (db-init/open-database ":memory:")]
    (t/is (count (db-raw/hanging-clockins db)) 0)
    (sut/clock-in db "test")
    (t/is (count (db-raw/hanging-clockins db)) 1)
    (sut/clock-out db)
    (t/is (count (db-raw/hanging-clockins db)) 0)))

