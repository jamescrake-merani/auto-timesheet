(ns jamescrake-merani.auto-timesheet.repl
  (:require [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [jamescrake-merani.auto-timesheet.db :as as-db]
            [jamescrake-merani.auto-timesheet.db-helpers :as helpers]
            [jamescrake-merani.auto-timesheet.reports :as reports]
            [clojure.java.io :as io])
  (:import (dev.dirs ProjectDirectories)))

(def ^ProjectDirectories proj-dirs (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet"))

(def db (db-init/open-database (io/file (.dataDir proj-dirs) "data.db")))

;; Create all the tables in the database if they haven't already been created.
(comment
  (db-init/init-database db))

;; Make a clock in
(comment
  (as-db/clock-in db {:category-id 2}))

;; Find category
(comment
  (as-db/get-category-from-name db {:name "test"}))

;; Try clocking in.
(comment
  (helpers/clock-in db "work"))

(comment
  (as-db/hanging-clockins db))

(comment
  (helpers/clock-out db))

;; Try clock in & out manually
(comment
  (helpers/manual-entry db
                        (java.time.LocalTime/parse "10:00")
                        (java.time.LocalTime/parse "12:00")
                        "work"))

;; Try listing all clocks in week.
(comment
  (def clocks-in-week (helpers/clocks-in-week db)))

(comment
  (def grouped-clocks (helpers/group-clocks-by-day clocks-in-week)))

(comment
  (reports/format-clock (first clocks-in-week))
  (reports/human-readable-summary grouped-clocks))


