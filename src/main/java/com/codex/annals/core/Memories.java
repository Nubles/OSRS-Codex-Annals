package com.codex.annals.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * On this day: what the annals wrote down on today's date in earlier years, and a month and six
 * months ago, so an old 99 or pet comes back round on its anniversary.
 */
public final class Memories
{
	/** Memories shown at most. */
	static final int MAX = 4;

	/** One memory: how long ago, and the entry. */
	public static final class Memory
	{
		private final String when;
		private final int years;
		private final Entry entry;

		Memory(String when, int years, Entry entry)
		{
			this.when = when;
			this.years = years;
			this.entry = entry;
		}

		/** "One year ago", "In 2024", "Six months ago", "A month ago". */
		public String getWhen()
		{
			return when;
		}

		/** Whole years ago, or 0 for a memory from this year. */
		public int getYears()
		{
			return years;
		}

		public Entry getEntry()
		{
			return entry;
		}
	}

	private Memories()
	{
	}

	/** The memories for a day, a year ago first; the most telling entry of each day. */
	public static List<Memory> of(Annals annals, LocalDate today, ZoneId zone)
	{
		List<Memory> found = new ArrayList<>();
		if (annals == null || !annals.isBegun())
		{
			return found;
		}
		LocalDate begun = Instant.ofEpochMilli(annals.begunAt).atZone(zone).toLocalDate();
		for (int years = 1; years <= today.getYear() - begun.getYear(); years++)
		{
			LocalDate then = today.minusYears(years);
			add(found, best(annals, same(today, then), zone, 2), years == 1 ? "One year ago" : "In " + then.getYear(), years);
		}
		add(found, best(annals, same(today, today.minusMonths(6)), zone, 1), "Six months ago", 0);
		add(found, best(annals, same(today, today.minusMonths(1)), zone, 1), "A month ago", 0);
		return found.size() > MAX ? new ArrayList<>(found.subList(0, MAX)) : found;
	}

	/**
	 * The days that come round on today: the same date, or nothing when that date does not exist
	 * then (30 March has no 30 February; its day was told on the 28th). On the last day of a month
	 * the days it lacks come round too, so a leap day is told on 28 February in other years.
	 */
	private static List<LocalDate> same(LocalDate today, LocalDate then)
	{
		List<LocalDate> days = new ArrayList<>();
		if (then.getDayOfMonth() != today.getDayOfMonth())
		{
			return days;
		}
		days.add(then);
		if (today.getDayOfMonth() == today.lengthOfMonth())
		{
			for (int d = then.getDayOfMonth() + 1; d <= then.lengthOfMonth(); d++)
			{
				days.add(then.withDayOfMonth(d));
			}
		}
		return days;
	}

	private static void add(List<Memory> found, List<Entry> entries, String when, int years)
	{
		for (Entry e : entries)
		{
			found.add(new Memory(when, years, e));
		}
	}

	/** The most telling entries of a day, leaving out ordinary low levels. */
	private static List<Entry> best(Annals annals, List<LocalDate> days, ZoneId zone, int limit)
	{
		List<Entry> day0 = new ArrayList<>();
		for (Entry e : annals.entries)
		{
			if (e.getKind() == Entry.Kind.LEVEL && e.getLevel() < 70)
			{
				continue;
			}
			if (days.contains(Instant.ofEpochMilli(e.at).atZone(zone).toLocalDate()))
			{
				day0.add(e);
			}
		}
		day0.sort(Comparator.comparingDouble(Entry::rank).reversed());
		return day0.size() > limit ? day0.subList(0, limit) : day0;
	}

	/** The memory to tell in chat at login: the best from an earlier year, or null. */
	public static Memory anniversary(List<Memory> memories)
	{
		for (Memory m : memories)
		{
			if (m.years > 0)
			{
				return m;
			}
		}
		return null;
	}
}
