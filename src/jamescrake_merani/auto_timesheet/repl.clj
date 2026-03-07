(ns jamescrake-merani.auto-timesheet.repl
  (:require [jamescrake-merani.auto-timesheet.db-init :as db-init]
            [clojure.java.io :as io])
  (:import (dev.dirs ProjectDirectories)))

(def proj-dirs (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet"))

(def db (db-init/open-database (io/file (.dataDir proj-dirs) "data.db")))

