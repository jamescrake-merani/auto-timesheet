-- :name create-clock-in-table
-- :command :execute
-- :result :raw
create table if not exists clockin(
    clockinid    integer primary key autoincrement ,
    starttime     datetime not null default (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    clockoutid  integer,
    categoryid integer not null,
    foreign key(clockoutid) references clockout(clockoutid),
    foreign key(categoryid) references category(categoryid) 
);

-- :name create-clock-out-table
-- :command :execute
-- :result :raw
create table if not exists clockout(
    clockoutid     integer primary key autoincrement ,
    stoptime      datetime not null default (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- :name create-category-table
-- :command :execute
-- :result :raw
create table if not exists category(
    categoryid  integer primary key autoincrement ,
    name        text not null
);

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

-- :name manual-clock-out
-- :command :execute
-- :result :one
insert into clockout (stoptime)
values (:stoptime)
returning clockoutid

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
-- :commnd :execute
-- :result :many
select *
from clockin as i
join clockout as o on i.clockinid = o.clockoutid 
where i.starttime >= :periodstart and i.starttime <= :periodend

-- :name get-category-from-name
-- :command :execute
-- :result :one
select categoryid from category
where :name = name;

-- :name create-category
-- :command :execute
-- :result :one
insert into category (name)
values (:name)
returning categoryid;
