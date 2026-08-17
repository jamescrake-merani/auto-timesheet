-- Auto Timesheet
-- Copyright (C) 2026 James Crake-Merani

-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.

-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.

-- You should have received a copy of the GNU General Public License
-- along with this program.  If not, see <https://www.gnu.org/licenses/>.

-- :name create-clock-in-table
-- :command :execute
-- :result :raw
create table if not exists clockin(
    clockinid    integer primary key autoincrement ,
    starttime     integer not null,
    clockoutid  integer,
    categoryid integer not null,
    foreign key(clockoutid) references clockout(clockoutid),
    foreign key(categoryid) references category(categoryid) 
) strict;

-- :name create-clock-out-table
-- :command :execute
-- :result :raw
create table if not exists clockout(
    clockoutid     integer primary key autoincrement ,
    stoptime      integer not null
) strict;

-- :name create-category-table
-- :command :execute
-- :result :raw
create table if not exists category(
    categoryid  integer primary key autoincrement ,
    name        text not null
) strict;

-- :name clock-in
-- :command :execute
-- :result :raw
insert into clockin (categoryid, starttime)
values (:category-id, :starttime);

-- :name manual-clock-in
-- :command :execute
-- :result :one
insert into clockin (starttime, categoryid, clockoutid)
values (:starttime, :category-id, :clockoutid)
returning clockinid


-- :name hanging-clockins
-- :command :execute
-- :result :many
select * from clockin
where clockoutid is null;

-- :name clock-out
-- :command :execute
-- :result :one
insert into clockout (stoptime)
values (:stoptime)
returning clockoutid

-- :name attach-clock-out
-- :command :execute
-- :result :raw
update clockin
set clockoutid = :clockoutid
where clockinid = :clockinid;

-- :name clocks-within-timeperiod
-- :command :execute
-- :result :many
select *
from clockin as i
join clockout as o on i.clockoutid = o.clockoutid 
where :i:timeparam >= :periodstart and :i:timeparam <= :periodend;

-- :name all-clocks
-- :command :execute
-- :result :many
select *
from clockin as i
join clockout as o on i.clockoutid = o.clockoutid;

-- :name delete-clockouts-within-timeperiod
-- :command :execute
-- :result :raw
delete from clockout
where clockoutid in (
      select clockoutid
      from clockin
      where starttime >= :periodstart and starttime <= :periodend
);

-- :name delete-clockins-within-timeperiod
-- :command :execute
-- :result :raw
delete from clockin
where starttime >= :periodstart and starttime <= :periodend;

-- :name amend-clockin
-- :command :execute
-- :result :raw
update clockin
set starttime = :newstarttime
where starttime = :originalstarttime

-- :name amend-clockout
-- :command :execute
-- :result :raw
update clockout
set stoptime = :newstoptime
where stoptime = :originalstoptime


-- :name get-category-from-name
-- :command :execute
-- :result :one
select categoryid from category
where :name = name;

-- :name get-category-name-from-id
-- :command :execute
-- :result :one
select name from category
where :id = categoryid;

-- :name create-category
-- :command :execute
-- :result :one
insert into category (name)
values (:name)
returning categoryid;
