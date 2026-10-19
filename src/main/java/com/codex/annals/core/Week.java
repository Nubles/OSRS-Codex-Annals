package com.codex.annals.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One week of play, Monday to Sunday in the player's own time: the experience each skill gained,
 * the kills of each boss and the time logged in. Weeks are what the account's chapters are found
 * in, and what the weekly look back is told from.
 */
public class Week
{
	/** The Monday the week begins on, as an epoch day. */
	long w;
	/** Experience gained, by skill. */
	Map<String, Long> xp = new LinkedHashMap<>();
	Map<String, Integer> kills = new LinkedHashMap<>();
	long playedMs;
	/**
	 * Each trained skill's experience when last seen that week, so its levels can be told later
	 * however much is gained away from RuneLite since; null in a week kept before this was.
	 */
	Map<String, Long> end = new LinkedHashMap<>();

	Week()
	{
	}

	Week(long monday)
	{
		this.w = monday;
	}

	/** The Monday of the week a day falls in, as an epoch day. */
	public static long mondayOf(LocalDate day)
	{
		return day.with(DayOfWeek.MONDAY).toEpochDay();
	}

	public LocalDate getMonday()
	{
		return LocalDate.ofEpochDay(w);
	}

	public long getStart()
	{
		return w;
	}

	public Map<String, Long> getXp()
	{
		return xp;
	}

	public Map<String, Integer> getKills()
	{
		return kills;
	}

	public long getPlayedMs()
	{
		return playedMs;
	}

	public long totalXp()
	{
		long total = 0;
		for (long v : xp.values())
		{
			total += v;
		}
		return total;
	}

	public int totalKills()
	{
		int total = 0;
		for (int k : kills.values())
		{
			total += k;
		}
		return total;
	}

	/** Whether anything at all happened that week. */
	public boolean isEmpty()
	{
		return playedMs <= 0 && xp.isEmpty() && kills.isEmpty();
	}

	void repair()
	{
		if (xp == null)
		{
			xp = new LinkedHashMap<>();
		}
		if (kills == null)
		{
			kills = new LinkedHashMap<>();
		}
		if (end == null)
		{
			end = new LinkedHashMap<>();
		}
		xp.values().removeIf(v -> v == null || v <= 0);
		kills.values().removeIf(v -> v == null || v <= 0);
	}
}
