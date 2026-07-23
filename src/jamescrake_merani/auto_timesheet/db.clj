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

(ns jamescrake-merani.auto-timesheet.db
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "jamescrake_merani/auto_timesheet/sql/clocking.sql")
