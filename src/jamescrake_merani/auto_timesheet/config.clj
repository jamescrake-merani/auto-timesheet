;; Auto Timesheet
;; Copyright (C) 2026 James Crake-Merani

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see <https://www.gnu.org/licenses/>.

(ns jamescrake-merani.auto-timesheet.config
  (:import (dev.dirs ProjectDirectories))
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def ^ProjectDirectories directories (delay (ProjectDirectories/from "me" "jamescrake-merani" "auto-timesheet")))

(defn make-default-config []
  {:default-category nil
   :sql-directory (io/file (.dataDir ^ProjectDirectories @directories) "data.db")}
  :def)

(defn load-config
  []
  (let [config-path (io/file (.configDir ^ProjectDirectories @directories) "config.edn")
        config-contents (if (.exists config-path) (edn/read-string (slurp config-path)) {})]
    (merge (make-default-config) config-contents)))
