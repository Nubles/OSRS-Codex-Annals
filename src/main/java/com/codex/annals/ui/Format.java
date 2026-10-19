package com.codex.annals.ui;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Experience, coins, durations and dates in the short forms the panel and the card use. */
public final class Format
{
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter SINCE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
	private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August",
		"September", "October", "November", "December"};
	private static final MathContext THREE = new MathContext(3, RoundingMode.HALF_UP);
	private static final DateTimeFormatter FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm", Locale.ENGLISH);

	private Format()
	{
	}

	/**
	 * "8,240", "41.2k", "412k", "1.24M", "41.0M", "412M", "1.50B": three figures at every size,
	 * trailing nought kept, so amounts side by side on one card are told to the same precision.
	 */
	public static String amount(long n)
	{
		if (n < 10_000)
		{
			return String.format(Locale.ENGLISH, "%,d", n);
		}
		// Rounded first, so 999,600 reads "1.00M" rather than "1000k".
		long r = new BigDecimal(n).round(THREE).longValue();
		if (r < 1_000_000)
		{
			return fixed(r / 1_000.0, r < 100_000 ? 1 : 0) + "k";
		}
		if (r < 1_000_000_000)
		{
			return fixed(r / 1_000_000.0, r < 10_000_000 ? 2 : r < 100_000_000 ? 1 : 0) + "M";
		}
		return fixed(r / 1_000_000_000.0, r < 10_000_000_000L ? 2 : r < 100_000_000_000L ? 1 : 0) + "B";
	}

	/** Gained experience with its sign: "+412k". */
	public static String xp(long xp)
	{
		return "+" + amount(Math.max(0, xp));
	}

	/** A change in coins with its sign: "+312M", "-4.1M", "0". */
	public static String signedCoins(long gp)
	{
		return gp > 0 ? "+" + amount(gp) : gp < 0 ? "-" + amount(-gp) : "0";
	}

	/** A change in coins with its sign in three figures, for tight spaces: "+248M", "-4.10M". */
	public static String signedCompact(long gp)
	{
		return signedCoins(gp);
	}

	public static String coins(long gp)
	{
		return amount(Math.max(0, gp));
	}

	private static String fixed(double value, int places)
	{
		return String.format(Locale.ENGLISH, "%." + places + "f", value);
	}

	/** "312k/hr", or "" when there is no rate yet. */
	public static String rate(long perHour)
	{
		return perHour <= 0 ? "" : amount(perHour) + "/hr";
	}

	/** "2h 14m", "38m", "under a minute". */
	public static String duration(long ms)
	{
		long minutes = ms / 60_000;
		if (minutes < 1)
		{
			return "under a minute";
		}
		if (minutes < 60)
		{
			return minutes + "m";
		}
		return minutes / 60 + "h " + minutes % 60 + "m";
	}

	/** "1:42" for a session clock. */
	public static String clock(long ms)
	{
		long minutes = ms / 60_000;
		return minutes / 60 + ":" + String.format(Locale.ENGLISH, "%02d", minutes % 60);
	}

	/** "14:02" in the player's own time. */
	public static String time(long epochMs)
	{
		return CLOCK.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "Thu 16 Oct". */
	public static String day(long epochMs)
	{
		return DAY.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "Thursday 16 October 2026". */
	public static String date(long epochMs)
	{
		return DATE.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "2026-10-16 14-02", safe in a file name. */
	public static String file(long epochMs)
	{
		return FILE.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** An item stack the way the game writes it: "950", "45K", "12M". */
	public static String stack(long n)
	{
		if (n < 100_000)
		{
			return String.valueOf(n);
		}
		if (n < 10_000_000)
		{
			return n / 1_000 + "K";
		}
		return n / 1_000_000 + "M";
	}

	/** The colour the game writes a stack of that size in. */
	public static java.awt.Color stackColor(long n)
	{
		return n < 100_000 ? Theme.QTY_YELLOW : n < 10_000_000 ? Theme.QTY_WHITE : Theme.QTY_GREEN;
	}

	/** "16 Oct". */
	public static String shortDay(LocalDate date)
	{
		return SHORT.format(date);
	}

	/** "12 Mar 2026". */
	public static String since(long epochMs)
	{
		return SINCE.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "October 2026". */
	public static String month(LocalDate date)
	{
		return MONTH.format(date);
	}

	/** "October" for 9. */
	public static String monthName(int zeroBased)
	{
		return MONTHS[zeroBased];
	}

	/** "312 hours", "1 hour", "45 minutes". */
	public static String hours(long ms)
	{
		long hours = ms / 3_600_000;
		if (hours < 1)
		{
			long minutes = ms / 60_000;
			return minutes + (minutes == 1 ? " minute" : " minutes");
		}
		return String.format(Locale.ENGLISH, "%,d", hours) + (hours == 1 ? " hour" : " hours");
	}

	/** Text to follow a comma: its first letter lowered, unless it starts a name such as a boss's. */
	public static String lower(String text)
	{
		if (text == null || text.length() < 2 || !Character.isUpperCase(text.charAt(0)) || Character.isUpperCase(text.charAt(1)))
		{
			return text;
		}
		return Character.toLowerCase(text.charAt(0)) + text.substring(1);
	}

	/** "1,412". */
	public static String number(long n)
	{
		return String.format(Locale.ENGLISH, "%,d", n);
	}

	/** "1 day", "148 days". */
	public static String plural(long n, String one, String many)
	{
		return number(n) + " " + (n == 1 ? one : many);
	}
}
