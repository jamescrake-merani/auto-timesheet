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
    timestamp      datetime not null
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

-- :name get-category-from-name :? :1
select categoryid from category
where :name = name;

-- :name create-category
-- :command :execute
-- :result :raw
insert into category (name)
values (:name);
