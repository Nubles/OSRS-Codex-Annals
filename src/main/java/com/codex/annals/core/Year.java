package com.codex.annals.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One calendar year of play: where every skill stood when it began and now, the days played and the kills. */
public class Year
{
	int year;
	Map<String, Long> startXp = new LinkedHashMap<>();
	Map<String, Long> endXp = new LinkedHashMap<>();
	Map<String, Integer> startLevel = new LinkedHashMap<>();
	Map<String, Integer> endLevel = new LinkedHashMap<>();
	/** Days played, as epoch days in the player's own time, oldest first. */
	List<Long> days = new ArrayList<>();
	long playedMs;
	Map<String, Integer> kills = new LinkedHashMap<>();
	int clues;

	Year()
	{
	}

	Year(int year)
	{
		this.year = year;
	}

	public int getYear()
	{
		return year;
	}

	public long getPlayedMs()
	{
		return playedMs;
	}

	public List<Long> getDays()
	{
		return days;
	}

	public Map<String, Integer> getKills()
	{
		return kills;
	}

	public int getClues()
	{
		return clues;
	}

	/** Makes sure every map exists after Gson has read an older save. */
	void repair()
	{
		if (startXp == null)
		{
			startXp = new LinkedHashMap<>();
		}
		if (endXp == null)
		{
			endXp = new LinkedHashMap<>();
		}
		if (startLevel == null)
		{
			startLevel = new LinkedHashMap<>();
		}
		if (endLevel == null)
		{
			endLevel = new LinkedHashMap<>();
		}
		if (days == null)
		{
			days = new ArrayList<>();
		}
		if (kills == null)
		{
			kills = new LinkedHashMap<>();
		}
		// What damage can put in: a day outside the year, a figure below zero or missing.
		if (year >= MIN_YEAR && year <= MAX_YEAR)
		{
			long first = java.time.LocalDate.of(year, 1, 1).toEpochDay();
			long last = java.time.LocalDate.of(year, 12, 31).toEpochDay();
			java.util.TreeSet<Long> kept = new java.util.TreeSet<>();
			for (Long d : days)
			{
				if (d != null && d >= first && d <= last)
				{
					kept.add(d);
				}
			}
			days = new ArrayList<>(kept);
		}
		startXp.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 0);
		endXp.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 0);
		startLevel.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 1);
		endLevel.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 1);
		kills.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 0);
		playedMs = Math.max(0, playedMs);
		clues = Math.max(0, clues);
	}

	/** A year a save can sensibly hold; any other is damage. */
	static final int MIN_YEAR = 2000;
	static final int MAX_YEAR = 9999;

	/** Experience a skill gained this year. */
	public long gained(String skill)
	{
		Long start = startXp.get(skill);
		Long end = endXp.get(skill);
		return start == null || end == null ? 0 : Math.max(0, end - start);
	}

	public int startLevel(String skill)
	{
		Integer level = startLevel.get(skill);
		return level == null ? 0 : level;
	}

	public int endLevel(String skill)
	{
		Integer level = endLevel.get(skill);
		return level == null ? startLevel(skill) : level;
	}

	public long totalGained()
	{
		long total = 0;
		for (String skill : endXp.keySet())
		{
			total += gained(skill);
		}
		return total;
	}

	public int levelsGained()
	{
		int total = 0;
		for (String skill : endLevel.keySet())
		{
			total += Math.max(0, endLevel(skill) - startLevel(skill));
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
}
