(ns jamescrake-merani.auto-timesheet.db
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "jamescrake-merani/auto-timesheet/sql/clocking.sql")
