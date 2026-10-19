package com.codex.annals.core;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A look back at the last whole week: time played, experience and where it went, kills, levels
 * and log slots, set against the account's usual week. Told at the top of the timeline, and once
 * in the chat box at the first login of a new week.
 */
public final class WeekNote
{
	/** Weeks before the last one that make the usual week. */
	static final int USUAL_WEEKS = 4;
	/** Less than this logged in, and with nothing else done, a week is not worth a look back. */
	static final long QUIET_MS = 10 * 60_000L;

	private final Week week;
	private final List<String> skills;
	private final String boss;
	private final int bossKills;
	private final int levels;
	private final int slots;
	private final int pets;
	private final int quests;
	private final int diaries;
	/** The usual week's experience, or -1 when too few weeks are kept to say. */
	private final long usualXp;

	private WeekNote(Week week, List<String> skills, String boss, int bossKills, int levels, int slots, int pets, int quests, int diaries, long usualXp)
	{
		this.week = week;
		this.skills = skills;
		this.boss = boss;
		this.bossKills = bossKills;
		this.levels = levels;
		this.slots = slots;
		this.pets = pets;
		this.quests = quests;
		this.diaries = diaries;
		this.usualXp = usualXp;
	}

	/** The week before the one {@code today} falls in, or null when it was a quiet one. */
	public static WeekNote lastWeek(Annals annals, LocalDate today, ZoneId zone)
	{
		long monday = Week.mondayOf(today) - 7;
		List<Week> weeks = annals.getWeeks();
		Week week = null;
		int at = -1;
		for (int i = 0; i < weeks.size(); i++)
		{
			if (weeks.get(i).w == monday)
			{
				week = weeks.get(i);
				at = i;
			}
		}
		if (week == null || (week.playedMs < QUIET_MS && week.totalXp() == 0 && week.kills.isEmpty()))
		{
			return null;
		}

		// The skills, the most experience first, Hitpoints aside unless it is all there is.
		List<Map.Entry<String, Long>> ranked = new ArrayList<>(week.xp.entrySet());
		ranked.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
		List<String> skills = new ArrayList<>();
		for (Map.Entry<String, Long> e : ranked)
		{
			if (!"Hitpoints".equals(e.getKey()) || ranked.size() == 1)
			{
				skills.add(e.getKey());
			}
		}
		String boss = null;
		int bossKills = 0;
		for (Map.Entry<String, Integer> e : week.kills.entrySet())
		{
			if (e.getValue() > bossKills)
			{
				boss = e.getKey();
				bossKills = e.getValue();
			}
		}

		// Levels from experience: where each skill ended that week, as seen then; for a week kept
		// without that, where it stands now less every later week.
		int levels = 0;
		for (Map.Entry<String, Long> e : week.xp.entrySet())
		{
			Long seen = week.end == null ? null : week.end.get(e.getKey());
			Long now = seen != null ? seen : annals.getExperience().get(e.getKey());
			if (now == null)
			{
				continue;
			}
			long end = now;
			for (int i = at + 1; seen == null && i < weeks.size(); i++)
			{
				end -= weeks.get(i).xp.getOrDefault(e.getKey(), 0L);
			}
			long start = end - e.getValue();
			if (start >= 0)
			{
				levels += Math.max(0, Experience.level(end) - Experience.level(start));
			}
		}

		int slots = 0;
		int pets = 0;
		int quests = 0;
		int diaries = 0;
		long from = LocalDate.ofEpochDay(monday).atStartOfDay(zone).toInstant().toEpochMilli();
		long to = LocalDate.ofEpochDay(monday + 7).atStartOfDay(zone).toInstant().toEpochMilli();
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
				default:
					break;
			}
		}

		long usual = -1;
		long sum = 0;
		int counted = 0;
		for (int i = at - 1; i >= 0 && counted < USUAL_WEEKS; i--)
		{
			Week w = weeks.get(i);
			if (w.w < monday - 7L * (USUAL_WEEKS + 4))
			{
				break;
			}
			if (w.totalXp() > 0)
			{
				sum += w.totalXp();
				counted++;
			}
		}
		if (counted >= 2)
		{
			usual = sum / counted;
		}
		return new WeekNote(week, skills, boss, bossKills, levels, slots, pets, quests, diaries, usual);
	}

	public Week getWeek()
	{
		return week;
	}

	public LocalDate getMonday()
	{
		return week.getMonday();
	}

	/** The skills trained, the most experience first. */
	public List<String> getSkills()
	{
		return skills;
	}

	/** The boss killed most, or null. */
	public String getBoss()
	{
		return boss;
	}

	public int getBossKills()
	{
		return bossKills;
	}

	public int getLevels()
	{
		return levels;
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

	public long getUsualXp()
	{
		return usualXp;
	}

	/** How much more (or less, below 0) experience than the usual week, or NaN when there is none to compare. */
	public double againstUsual()
	{
		return usualXp <= 0 ? Double.NaN : week.totalXp() / (double) usualXp - 1;
	}

	/** "6h 12m played, 1.4M xp (most in Slayer), 112 Vorkath kills, 2 log slots." */
	public String line()
	{
		List<String> parts = new ArrayList<>();
		if (week.playedMs >= 60_000)
		{
			parts.add(hours(week.playedMs) + " played");
		}
		if (week.totalXp() > 0)
		{
			parts.add(amount(week.totalXp()) + " xp" + (skills.isEmpty() ? "" : " (most in " + skills.get(0) + ")"));
		}
		if (boss != null)
		{
			parts.add(bossKills + " " + boss + (bossKills == 1 ? " kill" : " kills"));
		}
		if (levels > 0)
		{
			parts.add(levels + (levels == 1 ? " level" : " levels"));
		}
		if (pets > 0)
		{
			parts.add(pets + (pets == 1 ? " pet" : " pets"));
		}
		if (slots > 0)
		{
			parts.add(slots + (slots == 1 ? " log slot" : " log slots"));
		}
		if (quests > 0)
		{
			parts.add(quests + (quests == 1 ? " quest" : " quests"));
		}
		if (diaries > 0)
		{
			parts.add(diaries + (diaries == 1 ? " diary" : " diaries"));
		}
		return String.join(", ", parts) + ".";
	}

	static String hours(long ms)
	{
		long minutes = ms / 60_000;
		return minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
	}

	static String amount(long n)
	{
		return Amount.of(n);
	}
}
