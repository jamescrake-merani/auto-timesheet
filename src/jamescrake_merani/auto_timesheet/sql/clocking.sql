-- :name create-clock-in-table
-- :command :execute
-- :result :raw
create table if not exists clockin(
    clockinid    integer auto_increment primary key,
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
    clockoutid     integer auto_increment primary key,
    timestamp      datetime not null
);

-- :name create-category-table
-- :command :execute
-- :result :raw
create table if not exists category(
    categoryid  integer auto_increment primary key,
    name        text not null
);

-- :name clock-in
-- :command :execute
-- :result :raw
insert into clockin (categoryid)
values (:category-id);
