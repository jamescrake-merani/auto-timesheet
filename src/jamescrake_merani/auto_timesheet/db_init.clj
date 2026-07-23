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

(ns jamescrake-merani.auto-timesheet.db-init
  (:require [jamescrake-merani.auto-timesheet.db :as d]
            [hugsql.core :as h]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [next.jdbc :as jdbc]
            [clojure.java.io :as io]))

(defn init-database [db]
  (d/create-category-table db)
  (d/create-clock-in-table db)
  (d/create-clock-out-table db))

;; TODO: Might be better just to take the project directory, and work out where
;; the db should be in there.
(defn open-database [db-path]
  ;; Make sure the directory exists
  (io/make-parents db-path)
  (h/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))
  (let [conn (jdbc/get-connection {:dbtype "sqlite" :dbname db-path})]
    (init-database conn)
    conn))
