(ns jamescrake-merani.auto-timesheet.db-init
  (:require [jamescrake-merani.auto-timesheet.db :as d]
            [hugsql.core :as h]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [next.jdbc :as jdbc]))

(defn init-database [db]
  (d/create-category-table db)
  (d/create-clock-in-table db)
  (d/create-clock-out-table db))

(defn open-database [db-path]
  (h/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))
  (jdbc/get-datasource {:dbtype "sqlite" :dbname db-path}))
