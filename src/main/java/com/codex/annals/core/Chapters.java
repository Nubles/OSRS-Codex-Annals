package com.codex.annals.core;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The chapters of an account's story: the stretches of weeks spent on one thing. Each week is
 * given its focus, a boss fought often or a skill that took most of the week's experience, and a
 * run of weeks with the same focus becomes a chapter, named for how long it lasted and the season
 * it fell in: "A fortnight of Agility", "The Zulrah spring".
 *
 * <p>Combat experience comes with any boss, so a week of kills counts as that boss's week when
 * most of its experience went into combat skills. Slayer earns about a quarter of the experience
 * of the combat it rides on, so a week where Slayer keeps pace with that is a Slayer week.
 * Hitpoints is never a focus.
 */
public final class Chapters
{
	/** Kills of one boss in a week before the week is that boss's. */
	static final int BOSS_KILLS = 20;
	/** Experience in one skill before it can be a week's focus. */
	static final long SKILL_XP = 50_000;
	/** The share of a week's experience a skill must take to be its focus. */
	static final double SKILL_SHARE = 0.4;
	/** Slayer against the top combat skill's experience that makes a Slayer week. */
	static final double SLAYER_SHARE = 0.2;
	/** Weeks of one focus that make a chapter. */
	static final int MIN_WEEKS = 2;
	private static final Set<String> COMBAT = new HashSet<>(Arrays.asList("Attack", "Strength", "Defence", "Ranged", "Magic"));
	private static final String HITPOINTS = "Hitpoints";
	private static final String SLAYER = "Slayer";

	/** One chapter. */
	public static final class Chapter
	{
		private final String focus;
		private final boolean boss;
		private final long first;
		private final long last;
		private final int weeks;
		private final long xp;
		private final int kills;
		private final long playedMs;

		Chapter(String focus, boolean boss, long first, long last, int weeks, long xp, int kills, long playedMs)
		{
			this.focus = focus;
			this.boss = boss;
			this.first = first;
			this.last = last;
			this.weeks = weeks;
			this.xp = xp;
			this.kills = kills;
			this.playedMs = playedMs;
		}

		/** The skill or boss the chapter was spent on. */
		public String getFocus()
		{
			return focus;
		}

		public boolean isBoss()
		{
			return boss;
		}

		/** The Monday of its first week. */
		public LocalDate getStart()
		{
			return LocalDate.ofEpochDay(first);
		}

		/** The Sunday of its last week. */
		public LocalDate getEnd()
		{
			return LocalDate.ofEpochDay(last + 6);
		}

		/** Weeks from its first to its last, a week away in the middle included. */
		public int getWeeks()
		{
			return weeks;
		}

		/** Experience gained in the focus skill, for a skill's chapter. */
		public long getXp()
		{
			return xp;
		}

		/** Kills of the focus boss, for a boss's chapter. */
		public int getKills()
		{
			return kills;
		}

		/** Time logged in over the chapter's weeks. */
		public long getPlayedMs()
		{
			return playedMs;
		}

		/** Its name: "A fortnight of Agility", "Three weeks of Zulrah", "The Slayer summer". */
		public String title()
		{
			if (weeks <= 2)
			{
				return "A fortnight of " + focus;
			}
			if (weeks == 3)
			{
				return "Three weeks of " + focus;
			}
			if (weeks > 13)
			{
				return "The long " + focus + " " + (boss ? "campaign" : "grind");
			}
			return "The " + focus + " " + season(LocalDate.ofEpochDay((first + last + 6) / 2));
		}

		/** Whether any of it falls in a calendar year. */
		public boolean in(int year)
		{
			return getStart().getYear() <= year && getEnd().getYear() >= year;
		}
	}

	private Chapters()
	{
	}

	static String season(LocalDate day)
	{
		Month m = day.getMonth();
		switch (m)
		{
			case DECEMBER:
			case JANUARY:
			case FEBRUARY:
				return "winter";
			case MARCH:
			case APRIL:
			case MAY:
				return "spring";
			case JUNE:
			case JULY:
			case AUGUST:
				return "summer";
			default:
				return "autumn";
		}
	}

	/** What a week was spent on: a boss, a skill, or null when nothing stood out. */
	static String focus(Week w)
	{
		String boss = null;
		int bossKills = 0;
		for (Map.Entry<String, Integer> e : w.kills.entrySet())
		{
			if (e.getValue() > bossKills)
			{
				boss = e.getKey();
				bossKills = e.getValue();
			}
		}
		String skill = null;
		long top = 0;
		long total = 0;
		long topCombat = 0;
		for (Map.Entry<String, Long> e : w.xp.entrySet())
		{
			if (HITPOINTS.equals(e.getKey()))
			{
				continue;
			}
			total += e.getValue();
			if (e.getValue() > top)
			{
				top = e.getValue();
				skill = e.getKey();
			}
			if (COMBAT.contains(e.getKey()))
			{
				topCombat = Math.max(topCombat, e.getValue());
			}
		}
		boolean combatWeek = skill != null && COMBAT.contains(skill);
		if (bossKills >= BOSS_KILLS && (combatWeek || skill == null || top < SKILL_XP))
		{
			return "boss:" + boss;
		}
		long slayer = w.xp.getOrDefault(SLAYER, 0L);
		if (combatWeek && slayer >= SKILL_XP / 4 && slayer >= topCombat * SLAYER_SHARE)
		{
			return SLAYER;
		}
		if (skill != null && top >= SKILL_XP && top >= total * SKILL_SHARE)
		{
			return skill;
		}
		return bossKills >= BOSS_KILLS ? "boss:" + boss : null;
	}

	/**
	 * Every chapter in the weeks kept, oldest first. A single week away from the focus, or not
	 * played at all, does not end a chapter, so a week's holiday in a long grind leaves it whole.
	 */
	public static List<Chapter> of(List<Week> weeks)
	{
		Map<Long, String> foci = new LinkedHashMap<>();
		Map<Long, Week> byStart = new LinkedHashMap<>();
		for (Week w : weeks)
		{
			byStart.put(w.w, w);
			String f = focus(w);
			if (f != null)
			{
				foci.put(w.w, f);
			}
		}
		List<Chapter> out = new ArrayList<>();
		List<Long> starts = new ArrayList<>(foci.keySet());
		int i = 0;
		while (i < starts.size())
		{
			String f = foci.get(starts.get(i));
			long first = starts.get(i);
			long last = first;
			int focused = 1;
			int j = i + 1;
			// The next week of the same focus joins when at most one week, spent on anything, lies between.
			while (true)
			{
				int next = -1;
				for (int k = j; k < starts.size() && starts.get(k) - last <= 14; k++)
				{
					if (foci.get(starts.get(k)).equals(f))
					{
						next = k;
						break;
					}
				}
				if (next < 0)
				{
					break;
				}
				last = starts.get(next);
				focused++;
				j = next + 1;
			}
			if (focused >= MIN_WEEKS)
			{
				out.add(build(f, first, last, byStart));
				i = j;
			}
			else
			{
				i++;
			}
		}
		return out;
	}

	private static Chapter build(String f, long first, long last, Map<Long, Week> byStart)
	{
		boolean boss = f.startsWith("boss:");
		String name = boss ? f.substring(5) : f;
		long xp = 0;
		int kills = 0;
		long played = 0;
		for (long d = first; d <= last; d += 7)
		{
			Week w = byStart.get(d);
			if (w == null)
			{
				continue;
			}
			xp += boss ? 0 : w.xp.getOrDefault(name, 0L);
			kills += boss ? w.kills.getOrDefault(name, 0) : 0;
			played += w.playedMs;
		}
		return new Chapter(name, boss, first, last, (int) ((last - first) / 7) + 1, xp, kills, played);
	}
}
