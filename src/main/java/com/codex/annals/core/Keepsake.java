package com.codex.annals.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One moment of the annals told on its own keepsake card: what it was and the day it came, and
 * what the annals know of the road to it. How many days the annals had been kept, how much the
 * account had played by that week, how long a skill had been trained or a boss fought, where it
 * sits among the year's others of its kind, the worth that day when Codex Ledger kept it, and what
 * else the same day held. Everything is read from the annals as they are; nothing is stored.
 */
public final class Keepsake
{
	/** Other moments of the same day told beneath it. */
	static final int SAME_DAY = 3;
	/** A ledger reading older than this before the moment says nothing about that day. */
	static final long WORTH_STALE_MS = 3 * 86_400_000L;

	private final Entry entry;
	private final LocalDate day;
	private final String kind;
	private final int dayOfAnnals;
	private final int daysPlayed;
	private final long playedMs;
	private final String skill;
	private final int skillWeeks;
	private final long skillWeekXp;
	private final String boss;
	private final int bossWeekKills;
	private final int ordinal;
	private final int dropRank;
	private final long worth;
	private final List<Entry> sameDay;

	private Keepsake(Entry entry, LocalDate day, String kind, int dayOfAnnals, int daysPlayed, long playedMs, String skill, int skillWeeks,
		long skillWeekXp, String boss, int bossWeekKills, int ordinal, int dropRank, long worth, List<Entry> sameDay)
	{
		this.entry = entry;
		this.day = day;
		this.kind = kind;
		this.dayOfAnnals = dayOfAnnals;
		this.daysPlayed = daysPlayed;
		this.playedMs = playedMs;
		this.skill = skill;
		this.skillWeeks = skillWeeks;
		this.skillWeekXp = skillWeekXp;
		this.boss = boss;
		this.bossWeekKills = bossWeekKills;
		this.ordinal = ordinal;
		this.dropRank = dropRank;
		this.worth = worth;
		this.sameDay = sameDay;
	}

	/**
	 * The keepsake of one entry, or null for the prologue or when the annals are not begun.
	 *
	 * @param ledger Codex Ledger's saved worth, or null when that plugin is not kept
	 */
	public static Keepsake of(Annals annals, Entry entry, Wealth.Saved ledger, ZoneId zone)
	{
		if (annals == null || entry == null || !annals.isBegun() || entry.getKind() == Entry.Kind.BEGIN)
		{
			return null;
		}
		LocalDate day = Instant.ofEpochMilli(entry.getAt()).atZone(zone).toLocalDate();
		long epochDay = day.toEpochDay();
		LocalDate begun = Instant.ofEpochMilli(annals.getBegunAt()).atZone(zone).toLocalDate();
		int dayOfAnnals = epochDay < begun.toEpochDay() ? 0 : (int) (epochDay - begun.toEpochDay() + 1);

		int daysPlayed = 0;
		for (Year y : annals.getYears())
		{
			for (long d : y.getDays())
			{
				daysPlayed += d <= epochDay ? 1 : 0;
			}
		}

		// The weeks run Monday to Sunday; the moment's own week counts whole, so "by that week".
		long monday = Week.mondayOf(day);
		long played = 0;
		boolean anyWeek = false;
		// Weeks came into the annals later than some annals began, and only five years are kept:
		// figures summed from them are told only when the weeks reach back to the very first.
		long earliest = Long.MAX_VALUE;
		for (Week w : annals.getWeeks())
		{
			earliest = Math.min(earliest, w.getStart());
		}
		boolean whole = earliest <= Week.mondayOf(begun);
		String skill = entry.getSkill();
		int skillWeeks = 0;
		long skillWeekXp = 0;
		String boss = entry.getKind() == Entry.Kind.BOSS ? bossOf(entry.getTitle()) : null;
		int bossWeekKills = 0;
		for (Week w : annals.getWeeks())
		{
			if (w.getStart() > monday)
			{
				continue;
			}
			anyWeek = true;
			played += w.getPlayedMs();
			if (skill != null)
			{
				Long gained = w.getXp().get(skill);
				if (gained != null && gained > 0)
				{
					skillWeeks++;
					skillWeekXp = w.getStart() == monday ? gained : skillWeekXp;
				}
			}
			if (boss != null && w.getStart() == monday)
			{
				Integer k = w.getKills().get(boss);
				bossWeekKills = k == null ? 0 : k;
			}
		}

		// Where it sits among the year's others of its kind: the third pet, the biggest drop.
		int ordinal = 0;
		int dropRank = 0;
		boolean loot = entry.getKind() == Entry.Kind.DROP || entry.getKind() == Entry.Kind.COLLECTION;
		List<Entry> sameDay = new ArrayList<>();
		for (Entry e : annals.getEntries())
		{
			LocalDate d = Instant.ofEpochMilli(e.getAt()).atZone(zone).toLocalDate();
			if (d.getYear() == day.getYear() && e.getKind() == entry.getKind() && e.getAt() <= entry.getAt())
			{
				ordinal++;
			}
			if (loot && d.getYear() == day.getYear() && e.getValue() > entry.getValue()
				&& (e.getKind() == Entry.Kind.DROP || e.getKind() == Entry.Kind.COLLECTION))
			{
				// Ties share the place, so the first of two equal drops is not called second.
				dropRank++;
			}
			if (d.equals(day) && e != entry && e.getKind() != Entry.Kind.LEVEL && e.getKind() != Entry.Kind.BEGIN
				&& !(e.getAt() == entry.getAt() && e.getTitle().equals(entry.getTitle())))
			{
				sameDay.add(e);
			}
		}
		dropRank = loot && entry.getValue() > 0 ? dropRank + 1 : 0;
		sameDay.sort((a, b) -> Double.compare(b.rank(), a.rank()));
		if (sameDay.size() > SAME_DAY)
		{
			sameDay = new ArrayList<>(sameDay.subList(0, SAME_DAY));
		}

		if (!whole)
		{
			played = -1;
			skillWeeks = 0;
		}
		return new Keepsake(entry, day, label(entry), dayOfAnnals, daysPlayed, anyWeek ? played : -1, skill, skillWeeks, skillWeekXp,
			boss, bossWeekKills, ordinal, dropRank, worthOn(ledger, entry.getAt()), Collections.unmodifiableList(sameDay));
	}

	/** The worth Codex Ledger last read at or before a moment, or -1 when it kept nothing close enough. */
	static long worthOn(Wealth.Saved ledger, long at)
	{
		if (ledger == null || ledger.history == null)
		{
			return -1;
		}
		Wealth.Point best = null;
		for (Wealth.Point p : ledger.history)
		{
			if (p != null && p.t <= at && (best == null || p.t > best.t))
			{
				best = p;
			}
		}
		return best == null || at - best.t > WORTH_STALE_MS || best.v <= 0 ? -1 : best.v;
	}

	/** "Zulrah" from "First Zulrah" or "Zulrah 1,000". */
	static String bossOf(String title)
	{
		String name = title.startsWith("First ") ? title.substring(6) : title.replaceFirst("\\s+[0-9][0-9,]*$", "");
		return name.isEmpty() ? null : name;
	}

	/** The line over the title: what kind of moment the card keeps. */
	static String label(Entry e)
	{
		switch (e.getKind())
		{
			case PET:
				return "A pet";
			case MAX:
				return "A ninety-nine";
			case COLLECTION:
				return "A new log slot";
			case DROP:
				return "A drop to remember";
			case TOTAL:
				return "Total level";
			case DIARY:
				return "An achievement diary";
			case QUEST:
				return "A quest complete";
			case BOSS:
				return e.getTitle().startsWith("First ") ? "A first kill" : "A kill count";
			case XP:
				return "Experience";
			case COMBAT:
				return "Combat level";
			case TASK:
				return "A combat task";
			case CLUE:
				return "Treasure trails";
			default:
				return "A level";
		}
	}

	/**
	 * The entry in the annals that is the same moment as one shown earlier, or null when it has
	 * been struck since. The panel is handed a fresh copy of the annals at each change, so the
	 * moment is found again by its time, kind and title.
	 */
	public static Entry find(Annals annals, Entry shown)
	{
		if (annals == null || shown == null)
		{
			return null;
		}
		for (Entry e : annals.getEntries())
		{
			if (e.getAt() == shown.getAt() && e.getKind() == shown.getKind() && e.getTitle().equals(shown.getTitle()))
			{
				return e;
			}
		}
		return null;
	}

	/** A title made safe to offer as a file name: "99 Slayer", "Dragon warhammer", "1,000 x Zulrah's scales" less its odd marks. */
	public static String fileName(String title)
	{
		String safe = title.replaceAll("[^A-Za-z0-9 ,'()-]", " ").replaceAll("\\s+", " ").trim();
		return safe.isEmpty() ? "moment" : safe;
	}

	/** "1st", "2nd", "3rd", "11th". */
	public static String ordinal(int n)
	{
		int tens = n % 100;
		if (tens >= 11 && tens <= 13)
		{
			return n + "th";
		}
		switch (n % 10)
		{
			case 1:
				return n + "st";
			case 2:
				return n + "nd";
			case 3:
				return n + "rd";
			default:
				return n + "th";
		}
	}

	public Entry getEntry()
	{
		return entry;
	}

	public LocalDate getDay()
	{
		return day;
	}

	public String getKind()
	{
		return kind;
	}

	/** Which day of the annals it came on, the first being 1; 0 for a moment from before they began. */
	public int getDayOfAnnals()
	{
		return dayOfAnnals;
	}

	/** Days played with the annals kept, up to and including the moment's own day. */
	public int getDaysPlayed()
	{
		return daysPlayed;
	}

	/** Time played by the end of the moment's week, or -1 when the weeks kept do not reach back to the start. */
	public long getPlayedMs()
	{
		return playedMs;
	}

	public String getSkill()
	{
		return skill;
	}

	/** Weeks the moment's skill gained experience in, up to and including its own; 0 when the weeks kept do not reach back to the start. */
	public int getSkillWeeks()
	{
		return skillWeeks;
	}

	/** The skill's experience in the moment's own week. */
	public long getSkillWeekXp()
	{
		return skillWeekXp;
	}

	public String getBoss()
	{
		return boss;
	}

	/** Kills of the moment's boss in its own week. */
	public int getBossWeekKills()
	{
		return bossWeekKills;
	}

	/** Its place among the year's entries of the same kind, by time, the first being 1. */
	public int getOrdinal()
	{
		return ordinal;
	}

	/** For a priced drop or slot, its place among the year's by value, the biggest being 1; else 0. */
	public int getDropRank()
	{
		return dropRank;
	}

	/** The account's worth that day as Codex Ledger read it, or -1. */
	public long getWorth()
	{
		return worth;
	}

	/** The day's other telling moments, the most telling first; ordinary levels are left out. */
	public List<Entry> getSameDay()
	{
		return sameDay;
	}
}
