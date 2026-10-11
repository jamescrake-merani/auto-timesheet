# Configuration

The program's user configuration file is stored in different locations depending on the operating system. The easiest way to check where it should be editing your config file is by running:

```txt
auto-timesheet directories
```

The program keeps a default internal configuration file. The default configuration is merged with the user configuration, with values in the user configuration taking priority. However, it is not necessary to specify all values. In fact, by default a configuration file is not even created automatically. If you'd like to create a configuration file with all the defaults, run:

```txt
auto-timesheet init-config
```

The config is stored as an EDN file. 

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
