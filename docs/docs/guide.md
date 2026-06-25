# Guide to the CLI.

## Status

When you first run auto-timesheet without any arguments, it will give you the current status.

```
> auto-timesheet
You are not currently clocked in.
Run 'auto-timesheet --help' for a list of all commands.
```


## Clocking in, and out.

As we've just started, we of course haven't clocked in yet. Lets do so.

The first time you run the clockin command without any arguemnts, you'll get this error:

```
> auto-timesheet clockin
You need to provide a category with clock ins as you haven't provided a default one in your config.
```

If most of your clocks are relevant to just one category (e.g. work), you are probably better off adding that category to your configuration (see configuration). LINK.

For me, most of the clocks I'll make are for work, so I'll make thay my default category, and rerun the command.

```
> auto-timesheet clockin
Clocked in.
```

Now if we get the status again, we should see that we are clocked in.

```
> auto-timesheet status
You are currently clocked into work. 0 hours, 0 minutes elapsed since clockin.
```

Note that we added 'status'. This is the same as not providing a subcommand, but this time we don't get prompted to run the help command.

Once we're finished working, we can clock out:

```
> auto-timesheet clockout
Clocked out. You have worked 0 hours, 30 minutes
```

Note that you cannot have breaks during clockins. If you want to take a break, the easiest thing to do is clock out, then clock back in once you're done.

## Manual Entries

The previous commands make entries baded on what the current time is. Thats useful if we want to clock in, and clock out at the same time we start, and stop working. But if you work for a period, and want to make a clock _after_, you'll need to make a manual entry:

```
> auto-timesheet manual-entry --start-time 11:00 --end-time 13:00 --category work

```

Note that you should entry times in 24 hour format. If you need to make a manual entry that is on a different day, you'll need to specify the date parameter.

```
> auto-timesheet manual-entry --start-time 11:00 --end-time 13:00 --date 2026-06-25 --category work
```

The date parameter needs to be in ISO-8601 format, as parsed in by Java. See the [Java documentation](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE) for an exact description of how this string is parsed in.
