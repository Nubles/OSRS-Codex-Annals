package com.codex.annals.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A year told as one story, ready for the Year tab and the Year in Gielinor card: the totals,
 * the skills and bosses it was spent on, how the days fell across the months and the entries
 * that mattered most.
 */
public final class Recap
{
	/** One skill's year. */
	public static final class SkillYear
	{
		private final String skill;
		private final long gained;
		private final int startLevel;
		private final int endLevel;

		SkillYear(String skill, long gained, int startLevel, int endLevel)
		{
			this.skill = skill;
			this.gained = gained;
			this.startLevel = startLevel;
			this.endLevel = endLevel;
		}

		public String getSkill()
		{
			return skill;
		}

		public long getGained()
		{
			return gained;
		}

		public int getStartLevel()
		{
			return startLevel;
		}

		public int getEndLevel()
		{
			return endLevel;
		}
	}

	/** One boss or activity's year. */
	public static final class KillYear
	{
		private final String name;
		private final int kills;
		private final int killCount;

		KillYear(String name, int kills, int killCount)
		{
			this.name = name;
			this.kills = kills;
			this.killCount = killCount;
		}

		public String getName()
		{
			return name;
		}

		public int getKills()
		{
			return kills;
		}

		/** The game's own count after the year, or 0. */
		public int getKillCount()
		{
			return killCount;
		}
	}

	private final int year;
	private final String player;
	private final long playedMs;
	private final int daysPlayed;
	private final int longestStreak;
	private final long xpGained;
	private final int levelsGained;
	private final int kills;
	private final int[] monthDays = new int[12];
	private final int[] monthEntries = new int[12];
	private final List<SkillYear> skills = new ArrayList<>();
	private final List<KillYear> bosses = new ArrayList<>();
	private final List<Entry> highlights = new ArrayList<>();
	private final Entry biggestDrop;
	private int quests;
	private int diaries;
	private int tasks;
	private int collection;
	private int pets;
	private int maxes;
	private final int clues;
	private final String headline;
	private final long begunAt;
	private final int firstMonth;

	private Recap(Annals annals, Year y, ZoneId zone, int highlightCount)
	{
		year = y.year;
		player = annals.getPlayer();
		begunAt = annals.getBegunAt();
		LocalDate begun = Instant.ofEpochMilli(begunAt).atZone(zone).toLocalDate();
		firstMonth = begunAt > 0 && begun.getYear() == year ? begun.getMonthValue() - 1 : 0;
		playedMs = y.playedMs;
		daysPlayed = y.days.size();
		longestStreak = streak(y.days);
		xpGained = y.totalGained();
		levelsGained = y.levelsGained();
		kills = y.totalKills();
		clues = y.clues;
		for (long day : y.days)
		{
			monthDays[LocalDate.ofEpochDay(day).getMonthValue() - 1]++;
		}

		for (String skill : y.endXp.keySet())
		{
			long gained = y.gained(skill);
			if (gained > 0)
			{
				skills.add(new SkillYear(skill, gained, y.startLevel(skill), y.endLevel(skill)));
			}
		}
		skills.sort(Comparator.comparingLong(SkillYear::getGained).reversed());
		for (Map.Entry<String, Integer> e : y.kills.entrySet())
		{
			Integer kc = annals.kc.get(e.getKey());
			bosses.add(new KillYear(e.getKey(), e.getValue(), kc == null ? 0 : kc));
		}
		bosses.sort(Comparator.comparingInt(KillYear::getKills).reversed());

		List<Entry> inYear = new ArrayList<>();
		Entry drop = null;
		for (Entry e : annals.entries)
		{
			LocalDate date = Instant.ofEpochMilli(e.at).atZone(zone).toLocalDate();
			if (date.getYear() != year)
			{
				continue;
			}
			monthEntries[date.getMonthValue() - 1]++;
			switch (e.getKind())
			{
				case QUEST:
					quests++;
					break;
				case DIARY:
					diaries++;
					break;
				case TASK:
					tasks++;
					break;
				case COLLECTION:
					collection++;
					if (e.getValue() > 0 && (drop == null || e.getValue() > drop.getValue()))
					{
						drop = e;
					}
					break;
				case PET:
					pets++;
					collection += e.isSlot() ? 1 : 0;
					break;
				case MAX:
					maxes++;
					break;
				case DROP:
					if (drop == null || e.getValue() > drop.getValue())
					{
						drop = e;
					}
					break;
				default:
					break;
			}
			if (e.getKind() != Entry.Kind.BEGIN)
			{
				inYear.add(e);
			}
		}
		biggestDrop = drop;
		inYear.sort(Comparator.comparingDouble(Entry::rank).reversed().thenComparingLong(Entry::getAt));
		for (int i = 0; i < inYear.size() && highlights.size() < highlightCount; i++)
		{
			highlights.add(inYear.get(i));
		}
		headline = headline(inYear);
	}

	/** The recap of one year, or null if the annals have nothing for it. */
	public static Recap of(Annals annals, int year, ZoneId zone)
	{
		Year y = annals.getYear(year);
		return y == null ? null : new Recap(annals, y, zone, 6);
	}

	/** The years the annals hold, newest first. */
	public static List<Integer> years(Annals annals)
	{
		List<Integer> list = new ArrayList<>();
		for (Year y : annals.getYears())
		{
			list.add(y.year);
		}
		Collections.reverse(list);
		return list;
	}

	private String headline(List<Entry> ranked)
	{
		for (Entry e : ranked)
		{
			if (e.getKind() == Entry.Kind.TOTAL && Annals.MAX_TOTAL_DETAIL.equals(e.getDetail()))
			{
				return "The year of the max cape";
			}
		}
		if (maxes > 1)
		{
			return "The year of " + count(maxes) + " 99s";
		}
		if (maxes == 1)
		{
			for (Entry e : ranked)
			{
				if (e.getKind() == Entry.Kind.MAX)
				{
					return "The year of " + e.getTitle();
				}
			}
		}
		if (pets > 0)
		{
			return pets == 1 ? "The year a pet came home" : "The year of " + count(pets) + " pets";
		}
		if (!bosses.isEmpty() && bosses.get(0).kills >= 100)
		{
			return "The year of " + bosses.get(0).name;
		}
		if (!skills.isEmpty())
		{
			return "A year of " + skills.get(0).skill;
		}
		return "A quiet year";
	}

	private static String count(int n)
	{
		String[] words = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve"};
		return n < words.length ? words[n] : String.valueOf(n);
	}

	/** The longest run of days played back to back. */
	static int streak(List<Long> days)
	{
		int best = 0;
		int run = 0;
		long last = Long.MIN_VALUE;
		for (long day : days)
		{
			run = day == last + 1 ? run + 1 : 1;
			best = Math.max(best, run);
			last = day;
		}
		return best;
	}

	public int getYear()
	{
		return year;
	}

	public String getPlayer()
	{
		return player;
	}

	public long getBegunAt()
	{
		return begunAt;
	}

	/** The first month the annals kept in this year, 0 for January; the months before it were never kept. */
	public int getFirstMonth()
	{
		return firstMonth;
	}

	public long getPlayedMs()
	{
		return playedMs;
	}

	public int getDaysPlayed()
	{
		return daysPlayed;
	}

	public int getLongestStreak()
	{
		return longestStreak;
	}

	public long getXpGained()
	{
		return xpGained;
	}

	public int getLevelsGained()
	{
		return levelsGained;
	}

	public int getKills()
	{
		return kills;
	}

	/** Days played in each month, January first. */
	public int[] getMonthDays()
	{
		return monthDays.clone();
	}

	/** Entries written in each month, January first. */
	public int[] getMonthEntries()
	{
		return monthEntries.clone();
	}

	/** The busiest month by days played, 0 for January, or -1 if no day was played. */
	public int busiestMonth()
	{
		int best = -1;
		for (int m = 0; m < 12; m++)
		{
			if (monthDays[m] > 0 && (best < 0 || monthDays[m] > monthDays[best]))
			{
				best = m;
			}
		}
		return best;
	}

	public List<SkillYear> getSkills()
	{
		return Collections.unmodifiableList(skills);
	}

	public List<KillYear> getBosses()
	{
		return Collections.unmodifiableList(bosses);
	}

	public List<Entry> getHighlights()
	{
		return Collections.unmodifiableList(highlights);
	}

	public Entry getBiggestDrop()
	{
		return biggestDrop;
	}

	public int getQuests()
	{
		return quests;
	}

	public int getDiaries()
	{
		return diaries;
	}

	public int getTasks()
	{
		return tasks;
	}

	public int getCollection()
	{
		return collection;
	}

	public int getPets()
	{
		return pets;
	}

	public int getMaxes()
	{
		return maxes;
	}

	public int getClues()
	{
		return clues;
	}

	public String getHeadline()
	{
		return headline;
	}
}
