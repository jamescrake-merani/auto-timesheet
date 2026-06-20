(ns jamescrake-merani.auto-timesheet.config
  (:import (dev.dirs ProjectDirectories))
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def ^ProjectDirectories directories (delay (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet")))

(defn make-default-config []
  {:default-category nil
   :sql-directory (io/file (.dataDir @directories) "data.db")})

(defn load-config
  []
  (->>
   (io/file (.configDir @directories) "config.edn")
   slurp
   edn/read-string
   (merge (make-default-config))))
