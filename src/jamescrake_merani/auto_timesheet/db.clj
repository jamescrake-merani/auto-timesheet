(ns jamescrake-merani.auto-timesheet.db
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "jamescrake_merani/auto_timesheet/sql/clocking.sql")
