package com.codex.annals.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * What lies ahead at the pace the account keeps: when each skill reaches 99 (and those at 99 reach
 * 200M), the next total level, the max cape, and the next kill count milestones.
 *
 * <p>The pace is taken over every calendar day of the last four weeks, days off included, so a
 * date is an honest one. Until the annals have four weeks of readings, a skill's pace is this
 * year's so far; until they have a week of either, nothing is forecast.
 */
public final class Ahead
{
	/** Days of pace needed before anything is forecast. */
	static final int MIN_DAYS = 7;
	/** Further than this and a date says nothing useful. */
	static final long HORIZON_DAYS = 3_650;
	private static final int[] KC_MILESTONES = {50, 100, 250, 500, 1000, 2000, 5000, 10000};

	/** One milestone ahead and when it falls. */
	public static final class Mark
	{
		private final String title;
		private final String skill;
		private final LocalDate date;
		private final double perDay;
		private final long remaining;

		Mark(String title, String skill, LocalDate date, double perDay, long remaining)
		{
			this.title = title;
			this.skill = skill;
			this.date = date;
			this.perDay = perDay;
			this.remaining = remaining;
		}

		public String getTitle()
		{
			return title;
		}

		/** The skill it belongs to, or null for a total, the cape or a boss. */
		public String getSkill()
		{
			return skill;
		}

		/** When it falls at this pace, or null when it does not within ten years. */
		public LocalDate getDate()
		{
			return date;
		}

		/** Experience or kills a day at this pace. */
		public double getPerDay()
		{
			return perDay;
		}

		/** Experience or kills still to go. */
		public long getRemaining()
		{
			return remaining;
		}
	}

	private final int days;
	private final boolean recent;
	private final List<Mark> nineties = new ArrayList<>();
	private final List<Mark> beyond = new ArrayList<>();
	private final List<Mark> kills = new ArrayList<>();
	private Mark total;
	private Mark cape;
	private final List<String> stalled = new ArrayList<>();
	private final List<String> far = new ArrayList<>();
	private boolean maxed;

	private Ahead(int days, boolean recent)
	{
		this.days = days;
		this.recent = recent;
	}

	public static Ahead of(Annals annals, LocalDate today, ZoneId zone)
	{
		Pace.Snap oldest = annals.pace == null ? null : annals.pace.oldest();
		int paceDays = oldest == null ? 0 : (int) (today.toEpochDay() - oldest.d);
		Year year = annals.getYear(today.getYear());
		LocalDate begun = annals.isBegun() ? Instant.ofEpochMilli(annals.begunAt).atZone(zone).toLocalDate() : today;
		LocalDate yearStart = begun.getYear() == today.getYear() ? begun : today.withDayOfYear(1);
		// The year's readings start when it was first played, so its days run from there to the end of today.
		int yearDays = (int) ChronoUnit.DAYS.between(yearStart, today) + 1;
		boolean recent = paceDays >= MIN_DAYS && (paceDays >= Pace.DAYS || year == null || paceDays >= yearDays);
		int days = recent ? paceDays : year != null ? yearDays : 0;
		Ahead ahead = new Ahead(days, recent);
		if (days < MIN_DAYS)
		{
			return ahead;
		}

		Map<String, Long> now = annals.xp;
		long[] xp = new long[now.size()];
		double[] rate = new double[now.size()];
		String[] names = new String[now.size()];
		int i = 0;
		for (Map.Entry<String, Long> e : now.entrySet())
		{
			String skill = e.getKey();
			long from = recent ? oldest.xp.getOrDefault(skill, e.getValue())
				: year.startXp.getOrDefault(skill, e.getValue());
			names[i] = skill;
			xp[i] = e.getValue();
			rate[i] = Math.max(0, e.getValue() - from) / (double) days;
			i++;
		}

		LocalDate latest = null;
		boolean anyBelow = false;
		long max99 = Experience.forLevel(Experience.MAX_LEVEL);
		for (i = 0; i < names.length; i++)
		{
			if (xp[i] < max99)
			{
				anyBelow = true;
				LocalDate date = date(today, max99 - xp[i], rate[i]);
				if (date == null)
				{
					(rate[i] > 0 ? ahead.far : ahead.stalled).add(names[i]);
				}
				else
				{
					ahead.nineties.add(new Mark("99 " + names[i], names[i], date, rate[i], max99 - xp[i]));
					latest = latest == null || date.isAfter(latest) ? date : latest;
				}
			}
			else if (xp[i] < Experience.MAX_XP && rate[i] > 0)
			{
				LocalDate date = date(today, Experience.MAX_XP - xp[i], rate[i]);
				if (date != null)
				{
					ahead.beyond.add(new Mark("200M " + names[i], names[i], date, rate[i], Experience.MAX_XP - xp[i]));
				}
			}
		}
		ahead.maxed = !anyBelow && names.length > 0;
		if (anyBelow)
		{
			ahead.cape = new Mark("Max cape", null, ahead.stalled.isEmpty() && ahead.far.isEmpty() ? latest : null, 0, 0);
			ahead.total = total(today, xp, rate, annals.totalLevel(), names.length * Experience.MAX_LEVEL);
		}
		Comparator<Mark> soonest = Comparator.comparing(Mark::getDate);
		ahead.nineties.sort(soonest);
		ahead.beyond.sort(soonest);

		// Kill counts go by this year's kills alone, so they wait for a week of the year too.
		if (year != null && yearDays >= MIN_DAYS)
		{
			for (Map.Entry<String, Integer> e : year.kills.entrySet())
			{
				Integer count = annals.kc.get(e.getKey());
				if (count == null || e.getValue() <= 0)
				{
					continue;
				}
				int next = 0;
				for (int milestone : KC_MILESTONES)
				{
					if (milestone > count)
					{
						next = milestone;
						break;
					}
				}
				double perDay = e.getValue() / (double) yearDays;
				LocalDate date = next == 0 ? null : date(today, next - count, perDay);
				if (date != null)
				{
					ahead.kills.add(new Mark(e.getKey() + " " + Annals.number(next), null, date, perDay, next - count));
				}
			}
			ahead.kills.sort(soonest);
		}
		return ahead;
	}

	/** The next hundred of total level, found by walking the skills forward at their pace. */
	private static Mark total(LocalDate today, long[] xp, double[] rate, int totalNow, int maxTotal)
	{
		int target = Math.min(maxTotal, (totalNow / 100 + 1) * 100);
		if (totalAfter(xp, rate, HORIZON_DAYS) < target)
		{
			return new Mark("Total level " + Annals.number(target), null, null, 0, target - totalNow);
		}
		long lo = 0;
		long hi = HORIZON_DAYS;
		while (lo < hi)
		{
			long mid = (lo + hi) / 2;
			if (totalAfter(xp, rate, mid) >= target)
			{
				hi = mid;
			}
			else
			{
				lo = mid + 1;
			}
		}
		return new Mark("Total level " + Annals.number(target), null, today.plusDays(Math.max(1, lo)), 0, target - totalNow);
	}

	private static int totalAfter(long[] xp, double[] rate, long days)
	{
		int total = 0;
		for (int i = 0; i < xp.length; i++)
		{
			total += Experience.level((long) Math.min(Experience.MAX_XP, xp[i] + rate[i] * days));
		}
		return total;
	}

	/** The day so much is done at so much a day, or null when it never is within the horizon. */
	static LocalDate date(LocalDate today, long remaining, double perDay)
	{
		if (perDay <= 0)
		{
			return null;
		}
		double ahead = Math.ceil(remaining / perDay);
		return ahead > HORIZON_DAYS ? null : today.plusDays(Math.max(1, (long) ahead));
	}

	/** Days the pace was taken over; fewer than a week means it is too early to say. */
	public int getDays()
	{
		return days;
	}

	public boolean hasPace()
	{
		return days >= MIN_DAYS;
	}

	/** True when the pace is the last four weeks', false when it is this year's so far. */
	public boolean isRecent()
	{
		return recent;
	}

	/** Skills below 99 that are moving, soonest first. */
	public List<Mark> getNineties()
	{
		return Collections.unmodifiableList(nineties);
	}

	/** Skills at 99 on their way to 200M, soonest first. */
	public List<Mark> getBeyond()
	{
		return Collections.unmodifiableList(beyond);
	}

	/** The next kill count milestones of bosses killed this year, soonest first. */
	public List<Mark> getKills()
	{
		return Collections.unmodifiableList(kills);
	}

	/** The next hundred of total level, or null when every skill is 99. */
	public Mark getTotal()
	{
		return total;
	}

	/** The max cape: its date, or none when a skill below 99 is not moving. Null once maxed. */
	public Mark getCape()
	{
		return cape;
	}

	/** Skills below 99 that have not moved at all at this pace. */
	public List<String> getStalled()
	{
		return Collections.unmodifiableList(stalled);
	}

	/** Skills below 99 that are moving, but too slowly to get there within ten years. */
	public List<String> getFar()
	{
		return Collections.unmodifiableList(far);
	}

	public boolean isMaxed()
	{
		return maxed;
	}
}
