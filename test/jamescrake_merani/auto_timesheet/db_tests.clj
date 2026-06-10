(ns jamescrake-merani.auto-timesheet.db-tests
  (:require [jamescrake-merani.auto-timesheet.db-helpers :as sut]
            [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [clojure.test :as t]))

(def db (db-init/open-database ":memory:"))
