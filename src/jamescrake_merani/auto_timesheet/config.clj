(ns jamescrake-merani.auto-timesheet.config
  (:import (dev.dirs ProjectDirectories))
  (:require [clojure.java.io :as io]))

(def ^ProjectDirectories directories (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet"))

(def default-config
  {:default-category nil
   :sql-directory (io/file (.dataDir directories "data.db"))})
