(ns jamescrake-merani.auto-timesheet.repl
  (:require [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [jamescrake-merani.auto-timesheet.db :as as-db]
            [jamescrake-merani.auto-timesheet.db-helpers :as helpers]
            [clojure.java.io :as io])
  (:import (dev.dirs ProjectDirectories)))

(def proj-dirs (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet"))

(def db (db-init/open-database (io/file (.dataDir proj-dirs) "data.db")))

;; Create all the tables in the database if they haven't already been created.
(comment
  (db-init/init-database db))

;; Make a clock in
(comment
  (as-db/clock-in db {:category-id 2}))

;; Try clocking in.
(comment
  (helpers/clock-in "work")
  )


