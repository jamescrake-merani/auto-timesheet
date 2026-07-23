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
  ;; This ensures the path for the db actually exists before its created.
  (io/make-parents db-path)
  (h/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))
  (let [conn (jdbc/get-connection {:dbtype "sqlite" :dbname db-path})]
    (init-database conn)
    conn))
