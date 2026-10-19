package com.codex.annals.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where every skill stood at the start of each of the last few weeks' days, so the annals can say
 * how fast each one is moving and when it gets where it is going.
 */
public class Pace
{
	/** Days of readings kept; the pace is taken over the oldest of them. */
	static final int DAYS = 28;

	/** One day's first reading of every skill. */
	static class Snap
	{
		/** The day, as an epoch day in the player's own time. */
		long d;
		Map<String, Long> xp = new LinkedHashMap<>();
		/** Time logged in that day; 0 in a reading kept before this was. */
		long ms;
		/** Kills that day, by boss; null in a reading kept before this was. */
		Map<String, Integer> k;

		Snap()
		{
		}

		Snap(long day, Map<String, Long> xp)
		{
			this.d = day;
			this.xp = new LinkedHashMap<>(xp);
		}
	}

	List<Snap> days = new ArrayList<>();

	/** Notes where the skills stand, once a day: the first reading of a day is kept. */
	void note(long day, Map<String, Long> xp)
	{
		if (xp.isEmpty())
		{
			return;
		}
		Snap last = days.isEmpty() ? null : days.get(days.size() - 1);
		if (last != null && last.d >= day)
		{
			return;
		}
		days.add(new Snap(day, xp));
		while (days.size() > DAYS + 1)
		{
			days.remove(0);
		}
	}

	void repair()
	{
		if (days == null)
		{
			days = new ArrayList<>();
		}
		days.removeIf(s -> s == null || s.xp == null);
		for (Snap s : days)
		{
			s.xp.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 0);
			if (s.k != null)
			{
				s.k.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 0);
			}
		}
	}

	/** The reading of that day, or null when the day has none yet. */
	Snap on(long day)
	{
		Snap last = days.isEmpty() ? null : days.get(days.size() - 1);
		return last != null && last.d == day ? last : null;
	}

	/**
	 * The reading a pace of the last {@link #DAYS} calendar days is taken from. The readings kept
	 * are of days played, so with days off they reach back further than four weeks; then the first
	 * reading inside the window stands for the day it opened, since nothing was played between
	 * the reading before it and the window. Without an earlier reading, the oldest; null with none.
	 */
	Snap from(long today)
	{
		long open = today - DAYS;
		Snap earlier = null;
		for (Snap s : days)
		{
			if (s.d >= open)
			{
				return s;
			}
			earlier = s;
		}
		return earlier;
	}

	/** Calendar days a pace from {@link #from} spans: four weeks once the readings reach back that far. */
	int window(long today)
	{
		Snap from = from(today);
		if (from == null)
		{
			return 0;
		}
		Snap oldest = oldest();
		return oldest.d < today - DAYS && from.d >= today - DAYS ? DAYS : (int) (today - from.d);
	}

	/** The oldest reading kept, or null. */
	Snap oldest()
	{
		return days.isEmpty() ? null : days.get(0);
	}
}
