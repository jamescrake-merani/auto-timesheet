-- :name create-clock-in-table
-- :command :execute
-- :result :raw
create table clockin(
    clockinid    integer auto_increment primary key,
    timestamp     datetime not null,
    clockoutid  integer,
    categoryid integer,
    foreign key(clockoutid) references clockout(clockoutid),
    foreign key(categoryid) references category(categoryid) 
)

-- :name create-clock-out-table
-- :command :execute
-- :result :raw
create table clockout(
    clockoutid     integer auto_increment primary key,
    timestamp      datetime not null,
)

-- :name create-category-table
-- :command :execute
-- :result :raw
create table category(
    categoryid  integer auto_increment primary key,
    name        text not null
)
