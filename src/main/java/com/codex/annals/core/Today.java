package com.codex.annals.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Today so far: time logged in, the experience gained since the day's first reading and where it
 * went, the levels it made, the kills and what was written down, set against the account's usual
 * day of play. Shown at the top of the timeline, so the annals have something to say from the
 * first hour they are kept.
 */
public final class Today
{
	/** Days of play the usual day is taken from before it is worth comparing with. */
	static final int USUAL_DAYS = 3;

	private final LocalDate day;
	private final long playedMs;
	private final long xp;
	private final List<String> skills;
	private final int levels;
	private final String boss;
	private final int bossKills;
	private final int kills;
	private final int slots;
	private final int pets;
	private final int quests;
	private final int diaries;
	private final int drops;
	/** Experience on the usual day of play lately, or -1 when too few days are kept to say. */
	private final long usualXp;

	private Today(LocalDate day, long playedMs, long xp, List<String> skills, int levels, String boss, int bossKills, int kills,
		int slots, int pets, int quests, int diaries, int drops, long usualXp)
	{
		this.day = day;
		this.playedMs = playedMs;
		this.xp = xp;
		this.skills = skills;
		this.levels = levels;
		this.boss = boss;
		this.bossKills = bossKills;
		this.kills = kills;
		this.slots = slots;
		this.pets = pets;
		this.quests = quests;
		this.diaries = diaries;
		this.drops = drops;
		this.usualXp = usualXp;
	}

	/**
	 * The day so far, or null when it has not been played with the annals kept, as just after
	 * midnight or before the first login of the day.
	 */
	public static Today of(Annals annals, LocalDate today, ZoneId zone)
	{
		if (annals == null || !annals.isBegun())
		{
			return null;
		}
		long epochDay = today.toEpochDay();
		Pace.Snap start = annals.pace.on(epochDay);
		if (start == null)
		{
			return null;
		}

		// Experience since the day's first reading, the most first, Hitpoints aside unless it is all there is.
		List<Map.Entry<String, Long>> gained = new ArrayList<>();
		long xp = 0;
		int levels = 0;
		for (Map.Entry<String, Long> e : annals.xp.entrySet())
		{
			Long from = start.xp.get(e.getKey());
			if (from == null || from <= 0 || e.getValue() <= from)
			{
				continue;
			}
			long gain = e.getValue() - from;
			xp += gain;
			levels += Math.max(0, Experience.level(e.getValue()) - Experience.level(from));
			gained.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), gain));
		}
		gained.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
		List<String> skills = new ArrayList<>();
		for (Map.Entry<String, Long> e : gained)
		{
			if (!"Hitpoints".equals(e.getKey()) || gained.size() == 1)
			{
				skills.add(e.getKey());
			}
		}

		String boss = null;
		int bossKills = 0;
		int kills = 0;
		if (start.k != null)
		{
			for (Map.Entry<String, Integer> e : start.k.entrySet())
			{
				kills += e.getValue();
				if (e.getValue() > bossKills)
				{
					boss = e.getKey();
					bossKills = e.getValue();
				}
			}
		}

		int slots = 0;
		int pets = 0;
		int quests = 0;
		int diaries = 0;
		int drops = 0;
		long from = today.atStartOfDay(zone).toInstant().toEpochMilli();
		long to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
		for (Entry e : annals.getEntries())
		{
			if (e.getAt() < from || e.getAt() >= to)
			{
				continue;
			}
			switch (e.getKind())
			{
				case COLLECTION:
					slots++;
					break;
				case PET:
					pets++;
					slots += e.isSlot() ? 1 : 0;
					break;
				case QUEST:
					quests++;
					break;
				case DIARY:
					diaries++;
					break;
				case DROP:
					drops++;
					break;
				default:
					break;
			}
		}
		return new Today(today, start.ms, xp, Collections.unmodifiableList(skills), levels, boss, bossKills, kills,
			slots, pets, quests, diaries, drops, usualXp(annals.pace, epochDay));
	}

	/**
	 * The experience of the usual day of play over the readings kept: each day's is what its first
	 * reading and the next day played's first reading differ by. Days that gained nothing are
	 * left out, as are today and anything later.
	 */
	static long usualXp(Pace pace, long today)
	{
		long sum = 0;
		int counted = 0;
		List<Pace.Snap> days = pace.days;
		for (int i = 0; i + 1 < days.size(); i++)
		{
			Pace.Snap a = days.get(i);
			Pace.Snap b = days.get(i + 1);
			if (a.d >= today || b.d > today || a.d < today - Pace.DAYS)
			{
				continue;
			}
			long gain = 0;
			for (Map.Entry<String, Long> e : b.xp.entrySet())
			{
				Long before = a.xp.get(e.getKey());
				if (before != null && before > 0 && e.getValue() > before)
				{
					gain += e.getValue() - before;
				}
			}
			if (gain > 0)
			{
				sum += gain;
				counted++;
			}
		}
		return counted >= USUAL_DAYS ? sum / counted : -1;
	}

	/**
	 * Milliseconds from a moment to the next local midnight, at least one; a day that is not 24
	 * hours long, as when the clocks change, is measured as it is.
	 */
	public static long untilTomorrow(long now, ZoneId zone)
	{
		LocalDate day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
		long next = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
		return Math.max(1, next - now);
	}

	/** Whether anything at all has happened today worth a card. */
	public boolean isEmpty()
	{
		return playedMs < 60_000 && xp == 0 && kills == 0 && slots + pets + quests + diaries + drops == 0;
	}

	public LocalDate getDay()
	{
		return day;
	}

	public long getPlayedMs()
	{
		return playedMs;
	}

	public long getXp()
	{
		return xp;
	}

	/** The skills trained today, the most experience first. */
	public List<String> getSkills()
	{
		return skills;
	}

	public int getLevels()
	{
		return levels;
	}

	/** The boss killed most today, or null. */
	public String getBoss()
	{
		return boss;
	}

	public int getBossKills()
	{
		return bossKills;
	}

	public int getKills()
	{
		return kills;
	}

	public int getSlots()
	{
		return slots;
	}

	public int getPets()
	{
		return pets;
	}

	public int getQuests()
	{
		return quests;
	}

	public int getDiaries()
	{
		return diaries;
	}

	public int getDrops()
	{
		return drops;
	}

	public long getUsualXp()
	{
		return usualXp;
	}

	/** Today's experience as a share of the usual day's, or NaN when there is none to compare. */
	public double ofUsual()
	{
		return usualXp <= 0 ? Double.NaN : xp / (double) usualXp;
	}
}
