# Configuration

The program's configuration file is stored in different locations depending on the operating system. The easiest way to check where you should be editing your config file is by running:

```txt
auto-timesheet directories
```

The config is stored as an EDN file. It is loaded, and merged with the default config. This means that if you do not specify a field in your config, the program will automatically revert to the default value.

## Default category

There is no default category meaning that, by default, you cannot clock in without specifying a category. You may want to specify one. For example

```clojure
{:default-category "work"}
```

## Database

This program uses an SQLite database. By default, it'll be stored in the data directory as defined by your operating system. However, this is a bit hidden away. It might be fine if you're always going to use the same computer, but if you want to put it somewhere where you can sync it, you'll probably want to move it. e.g.

```clojure
{:sql-directory "/home/james/Nextcloud/Timesheets/data.db"}
```
