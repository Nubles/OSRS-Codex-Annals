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
	}

	/** The oldest reading kept, or null. */
	Snap oldest()
	{
		return days.isEmpty() ? null : days.get(0);
	}
}
