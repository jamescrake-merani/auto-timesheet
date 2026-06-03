-- :name create-clock-in-table
-- :command :execute
-- :result :raw
create table if not exists clockin(
    clockinid    integer primary key autoincrement ,
    timestamp     datetime not null default current_timestamp,
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
    timestamp      datetime not null default current_timestamp
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
insert into clockin (categoryid)
values (:category-id);

-- :name manual-clock-in
-- :command :execute
-- :result raw
insert into clockin (timestamp, categoryid, clockoutid)
values (:timestamp :category-id :clockoutid)

-- :name manual-clock-out
-- :command :execute
-- :result one
insert into clockout (timestamp)
values (:timestamp)
returning clockoutid

-- :name hanging-clockins
-- :command :execute
-- :result :many
select * from clockin
where clockoutid is null;

-- :name clock-out
-- :command :execute
-- :result :one
insert into clockout default values
returning clockoutid

-- :name attach-clock-out
-- :command :execute
-- :result :raw
update clockin
set clockoutid = :clockoutid
where clockinid = :clockinid;

-- :name get-category-from-name :? :1
select categoryid from category
where :name = name;

-- :name create-category
-- :command :execute
-- :result :one
insert into category (name)
values (:name)
returning categoryid;
