(ns jamescrake-merani.auto-timesheet.config
  (:import (dev.dirs ProjectDirectories))
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def ^ProjectDirectories directories (delay (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet")))

(defn make-default-config []
  {:default-category nil
   :sql-directory (io/file (.dataDir ^ProjectDirectories @directories) "data.db")})

(defn load-config
  []
  (let [config-path (io/file (.configDir ^ProjectDirectories @directories) "config.edn")
        config-contents (if (.exists config-path) (edn/read-string (slurp config-path)) {})]
    (merge (make-default-config config-contents))))
