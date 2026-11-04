package com.codex.annals.core;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gielinor Wrapped: a year told as a short story of cards, one figure each, to page through and
 * share. Everything is read from what the annals already keep for the year: its recap, its
 * entries and its weeks. A card is only dealt when the year has something to put on it.
 */
public final class Wrapped
{
	/** The cards, in the order they are dealt. */
	public enum Card
	{
		/** The year in numbers: hours, days, experience and levels. */
		OPENING,
		/** The skill the year went to, and the biggest climb in one day. */
		SKILL,
		/** The rarest thing that dropped, with its wiki rate, or else the finest. */
		DROP,
		/** The busiest day and the longest run of days. */
		DAYS,
		/** What kind of player the year made you. */
		ARCHETYPE,
		/** The year month by month. */
		MONTHS,
		/** The year's tallies, signed off. */
		CLOSING
	}

	/** The month Wrapped opens for the year under way: December. */
	static final int OPENS = 12;

	/** The most levels one skill climbed in one day. */
	public static final class Climb
	{
		private final String skill;
		private final int from;
		private final int to;
		private final LocalDate day;

		Climb(String skill, int from, int to, LocalDate day)
		{
			this.skill = skill;
			this.from = from;
			this.to = to;
			this.day = day;
		}

		public String getSkill()
		{
			return skill;
		}

		public int getFrom()
		{
			return from;
		}

		public int getTo()
		{
			return to;
		}

		public int levels()
		{
			return to - from;
		}

		public LocalDate getDay()
		{
			return day;
		}
	}

	/** The day with the most written down. */
	public static final class Day
	{
		private final LocalDate date;
		private final int count;
		private final List<Entry> best;

		Day(LocalDate date, int count, List<Entry> best)
		{
			this.date = date;
			this.count = count;
			this.best = Collections.unmodifiableList(best);
		}

		public LocalDate getDate()
		{
			return date;
		}

		/** Entries written that day. */
		public int getCount()
		{
			return count;
		}

		/** The most telling few of them, most telling first. */
		public List<Entry> getBest()
		{
			return best;
		}
	}

	private final Recap recap;
	private final Archetype archetype;
	private final Archetype.Figures figures;
	private final Climb climb;
	private final Entry drop;
	private final DropRates.Rate rate;
	private final Day busiest;
	private final LocalDate streakStart;
	private final long[] monthXp = new long[12];
	private final List<Card> cards = new ArrayList<>();

	private Wrapped(Annals annals, Recap r, ZoneId zone)
	{
		recap = r;
		figures = Archetype.Figures.of(r);
		archetype = Archetype.of(figures);
		List<Entry> inYear = new ArrayList<>();
		for (Entry e : annals.getEntries())
		{
			if (e.getKind() != Entry.Kind.BEGIN && day(e, zone).getYear() == r.getYear())
			{
				inYear.add(e);
			}
		}
		climb = climb(inYear, zone);
		Entry rarest = null;
		DropRates.Rate rarestRate = null;
		for (Entry e : inYear)
		{
			DropRates.Rate found = DropRates.of(e);
			if (found != null && (rarestRate == null || found.getChance() < rarestRate.getChance()
				|| found.getChance() == rarestRate.getChance() && e.getValue() > rarest.getValue()))
			{
				rarest = e;
				rarestRate = found;
			}
		}
		if (rarest == null)
		{
			// No drop the wiki's rates here cover: a pet is still the rarest thing there is, then the drop worth most.
			for (Entry e : inYear)
			{
				if (e.getKind() == Entry.Kind.PET && rarest == null)
				{
					rarest = e;
				}
			}
			if (rarest == null)
			{
				rarest = r.getBiggestDrop();
			}
		}
		drop = rarest;
		rate = rarestRate;
		busiest = busiest(inYear, zone);
		streakStart = streakStart(r.getDays(), r.getLongestStreak());
		for (Week w : annals.getWeeks())
		{
			// A week belongs to the month its Thursday falls in, as calendars number weeks.
			LocalDate thursday = w.getMonday().with(DayOfWeek.THURSDAY);
			if (thursday.getYear() == r.getYear())
			{
				monthXp[thursday.getMonthValue() - 1] += w.totalXp();
			}
		}
		deal();
	}

	/**
	 * Gielinor Wrapped for a year, or null when the annals hold nothing for it. Whether it may be
	 * opened yet is {@link #isOpen}'s to say.
	 */
	public static Wrapped of(Annals annals, int year, ZoneId zone)
	{
		Recap r = annals == null ? null : Recap.of(annals, year, zone);
		return r == null ? null : new Wrapped(annals, r, zone);
	}

	/** Whether a year's Wrapped may be opened: any year gone by, and the year under way from 1 December. */
	public static boolean isOpen(int year, LocalDate today)
	{
		return year < today.getYear() || year == today.getYear() && today.getMonthValue() >= OPENS;
	}

	/** The day the year under way's Wrapped opens. */
	public static LocalDate opensOn(int year)
	{
		return LocalDate.of(year, OPENS, 1);
	}

	private void deal()
	{
		cards.add(Card.OPENING);
		if (getTopSkill() != null)
		{
			cards.add(Card.SKILL);
		}
		if (drop != null)
		{
			cards.add(Card.DROP);
		}
		if (recap.getDaysPlayed() >= 2 && (busiest != null || recap.getLongestStreak() >= 2))
		{
			cards.add(Card.DAYS);
		}
		if (figures.totalXp() > 0 || recap.getKills() > 0 || recap.getCollection() > 0 || recap.getQuests() > 0)
		{
			cards.add(Card.ARCHETYPE);
		}
		if (monthsWith() >= 2)
		{
			cards.add(Card.MONTHS);
		}
		if (tallies() > 0)
		{
			cards.add(Card.CLOSING);
		}
	}

	/** Months with play or experience in them. */
	int monthsWith()
	{
		int[] days = recap.getMonthDays();
		int n = 0;
		for (int m = 0; m < 12; m++)
		{
			n += days[m] > 0 || monthXp[m] > 0 ? 1 : 0;
		}
		return n;
	}

	private int tallies()
	{
		return recap.getQuests() + recap.getDiaries() + recap.getCollection() + recap.getPets() + recap.getClues() + recap.getMaxes()
			+ recap.getKills();
	}

	private static LocalDate day(Entry e, ZoneId zone)
	{
		return Instant.ofEpochMilli(e.getAt()).atZone(zone).toLocalDate();
	}

	/**
	 * The most levels one skill climbed on one day, from the levels written down: from the level it
	 * stood at before that day's first, to that day's last. A 99 came from 98 when nothing earlier
	 * that day says otherwise. Null when no level was written down.
	 */
	static Climb climb(List<Entry> entries, ZoneId zone)
	{
		Map<String, int[]> spans = new LinkedHashMap<>();
		Map<String, LocalDate> days = new LinkedHashMap<>();
		for (Entry e : entries)
		{
			Entry.Kind kind = e.getKind();
			if ((kind != Entry.Kind.LEVEL && kind != Entry.Kind.MAX) || e.getSkill() == null || e.getLevel() <= 1)
			{
				continue;
			}
			int to = e.getLevel();
			int from = kind == Entry.Kind.LEVEL && e.getFrom() > 0 ? e.getFrom() : to - 1;
			LocalDate day = day(e, zone);
			String key = day + "/" + e.getSkill();
			int[] span = spans.get(key);
			if (span == null)
			{
				spans.put(key, new int[]{from, to});
				days.put(key, day);
			}
			else
			{
				span[0] = Math.min(span[0], from);
				span[1] = Math.max(span[1], to);
			}
		}
		Climb best = null;
		for (Map.Entry<String, int[]> s : spans.entrySet())
		{
			int[] span = s.getValue();
			int levels = span[1] - span[0];
			// The most levels; on a tie, the higher levels, which took more.
			if (levels > 0 && (best == null || levels > best.levels() || levels == best.levels() && span[1] > best.to))
			{
				String key = s.getKey();
				best = new Climb(key.substring(key.indexOf('/') + 1), span[0], span[1], days.get(key));
			}
		}
		return best;
	}

	/**
	 * The day with the most written down: two entries at the least, the one with the more telling
	 * entries on a tie, the earlier after that. Null when no day had two.
	 */
	static Day busiest(List<Entry> entries, ZoneId zone)
	{
		Map<LocalDate, List<Entry>> byDay = new TreeMap<>();
		for (Entry e : entries)
		{
			byDay.computeIfAbsent(day(e, zone), d -> new ArrayList<>()).add(e);
		}
		LocalDate best = null;
		double bestWeight = 0;
		for (Map.Entry<LocalDate, List<Entry>> d : byDay.entrySet())
		{
			int n = d.getValue().size();
			if (n < 2)
			{
				continue;
			}
			double weight = 0;
			for (Entry e : d.getValue())
			{
				weight += e.rank();
			}
			int bestCount = best == null ? 0 : byDay.get(best).size();
			if (best == null || n > bestCount || n == bestCount && weight > bestWeight)
			{
				best = d.getKey();
				bestWeight = weight;
			}
		}
		if (best == null)
		{
			return null;
		}
		List<Entry> those = new ArrayList<>(byDay.get(best));
		those.sort(Comparator.comparingDouble(Entry::rank).reversed().thenComparingLong(Entry::getAt));
		return new Day(best, byDay.get(best).size(), new ArrayList<>(those.subList(0, Math.min(3, those.size()))));
	}

	/** The first day of the longest run of days played (the earliest such run), or null. */
	static LocalDate streakStart(List<Long> days, int longest)
	{
		if (longest < 1)
		{
			return null;
		}
		int run = 0;
		long last = Long.MIN_VALUE;
		for (long day : days)
		{
			run = day == last + 1 ? run + 1 : 1;
			if (run == longest)
			{
				return LocalDate.ofEpochDay(day - longest + 1);
			}
			last = day;
		}
		return null;
	}

	public Recap getRecap()
	{
		return recap;
	}

	public int getYear()
	{
		return recap.getYear();
	}

	/** The cards dealt for the year, in order. */
	public List<Card> getCards()
	{
		return Collections.unmodifiableList(cards);
	}

	public Archetype getArchetype()
	{
		return archetype;
	}

	/** Why the year is its archetype, in a line. */
	public String getArchetypeReason()
	{
		return archetype.reason(figures);
	}

	/** The share of the year's experience in skills of the craft, combat and Slayer, as fractions; zeros with none. */
	public double[] getShares()
	{
		double total = figures.totalXp();
		if (total <= 0)
		{
			return new double[]{0, 0, 0};
		}
		return new double[]{figures.skillingXp / total, figures.combatXp / total, figures.slayerXp / total};
	}

	/** The skill the most experience went to, or null. */
	public Recap.SkillYear getTopSkill()
	{
		return recap.getSkills().isEmpty() ? null : recap.getSkills().get(0);
	}

	/** The top skill's share of the year's experience, 0 to 1. */
	public double topSkillShare()
	{
		Recap.SkillYear top = getTopSkill();
		return top == null || recap.getXpGained() <= 0 ? 0 : top.getGained() / (double) recap.getXpGained();
	}

	/** The biggest climb in a day, or null. */
	public Climb getClimb()
	{
		return climb;
	}

	/** The rarest drop by its wiki rate; else a pet; else the drop worth the most; or null. */
	public Entry getDrop()
	{
		return drop;
	}

	/** The wiki's rate for {@link #getDrop}, or null when it has none here. */
	public DropRates.Rate getRate()
	{
		return rate;
	}

	public Day getBusiest()
	{
		return busiest;
	}

	/** The first day of the longest run of days played, or null. */
	public LocalDate getStreakStart()
	{
		return streakStart;
	}

	/** Experience gained each month, January first, from the weeks kept. */
	public long[] getMonthXp()
	{
		return monthXp.clone();
	}

	/** The month with the most experience, 0 for January, or -1 with none. */
	public int bestMonth()
	{
		int best = -1;
		for (int m = 0; m < 12; m++)
		{
			if (monthXp[m] > 0 && (best < 0 || monthXp[m] > monthXp[best]))
			{
				best = m;
			}
		}
		return best;
	}

	/** A file name for the saved story: "Gielinor Wrapped 2026". */
	public String fileName()
	{
		return "Gielinor Wrapped " + recap.getYear();
	}
}
