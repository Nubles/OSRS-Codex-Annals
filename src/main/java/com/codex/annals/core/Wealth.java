package com.codex.annals.core;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The account's worth over a year, read from Codex Ledger's own saved history when that plugin
 * is kept too. Only the parts needed are read; the ledger is never written.
 */
public final class Wealth
{
	/** Codex Ledger's settings group and the key its ledger is saved under, per account. */
	public static final String LEDGER_GROUP = "codexledger";
	public static final String LEDGER_KEY = "ledger";

	/** The worth at a moment, as Codex Ledger saves it. */
	static final class Point
	{
		long t;
		long v;

		Point()
		{
		}

		Point(long t, long v)
		{
			this.t = t;
			this.v = v;
		}
	}

	/** The parts of Codex Ledger's save read here. */
	public static final class Saved
	{
		List<Point> history = new ArrayList<>();
		long total;

		public Saved()
		{
		}

		Saved(long total, List<Point> history)
		{
			this.total = total;
			this.history = history;
		}
	}

	/**
	 * Reads Codex Ledger's save, or null when there is none or it does not read. It is only
	 * read, so a damaged one costs nothing but the worth figures until the ledger saves again.
	 */
	public static Saved read(com.google.gson.Gson gson, String raw)
	{
		if (raw == null || raw.isEmpty())
		{
			return null;
		}
		try
		{
			return gson.fromJson(raw, Saved.class);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private final int year;
	private final long start;
	private final long end;
	private final long from;
	private final boolean partial;

	private Wealth(int year, long start, long end, long from, boolean partial)
	{
		this.year = year;
		this.start = start;
		this.end = end;
		this.from = from;
		this.partial = partial;
	}

	/**
	 * The worth a year began and ended on, or null when the ledger did not keep any of it.
	 *
	 * @param current whether the year is still going, so it ends on the worth now
	 */
	public static Wealth of(Saved saved, int year, ZoneId zone, boolean current)
	{
		if (saved == null || saved.history == null || saved.history.isEmpty())
		{
			return null;
		}
		long yearStart = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli();
		long yearEnd = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli();
		Point before = null;
		Point first = null;
		Point last = null;
		for (Point p : saved.history)
		{
			if (p == null)
			{
				continue;
			}
			if (p.t < yearStart)
			{
				before = p;
			}
			else if (p.t < yearEnd)
			{
				first = first == null ? p : first;
				last = p;
			}
		}
		// Nothing read this year: the ledger was not kept, or was removed and its old save is left
		// behind, and its worth then is not the worth now.
		if (first == null)
		{
			return null;
		}
		long start = before != null ? before.v : first.v;
		long from = before != null ? yearStart : first.t;
		long end = current ? saved.total : last.v;
		return new Wealth(year, start, end, from, before == null);
	}

	public int getYear()
	{
		return year;
	}

	public long getStart()
	{
		return start;
	}

	public long getEnd()
	{
		return end;
	}

	public long change()
	{
		return end - start;
	}

	/** When the ledger's count of the year begins: the first of January, or the day it began. */
	public long getFrom()
	{
		return from;
	}

	/** True when the ledger began part way through the year. */
	public boolean isPartial()
	{
		return partial;
	}
}
