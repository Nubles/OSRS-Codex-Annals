package com.codex.annals.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * What kind of player a year made you, judged from where its time and experience went: the
 * skills it was spent on, the kills an hour played, the log slots and pets, the quests and
 * diaries and the clues. Each kind scores how far the year went past the line that earns it;
 * the highest score wins, and a year that earns none is told by where most of its experience went.
 */
public enum Archetype
{
	THE_COLLECTOR("The Collector", "Every slot a story; the log fills because you keep going back."),
	THE_TREASURE_HUNTER("The Treasure Hunter", "A spade in one hand and a clue in the other, all year long."),
	THE_BOSS_HUNTER("The Boss Hunter", "Your year was told in kill counts."),
	THE_SLAYER("The Slayer", "One more task, always. The task came first."),
	THE_ADVENTURER("The Adventurer", "Quests read, diaries signed: you went to see Gielinor."),
	THE_SKILLER("The Skiller", "Logs, ores and laps: the long, quiet grind was yours."),
	THE_WARRIOR("The Warrior", "Steel first, questions later: your experience went to the fight."),
	THE_WANDERER("The Wanderer", "A year of a little of everything, and no hurry.");

	/** The combat skills; Slayer is told apart, and every other skill, Sailing among them, is a skill of the craft. */
	static final Set<String> COMBAT = new HashSet<>(Arrays.asList("Attack", "Strength", "Defence", "Ranged", "Magic", "Hitpoints", "Prayer"));
	static final String SLAYER = "Slayer";
	/**
	 * The skills of the craft as the game names them, for telling a skiller's year. Any skill not
	 * named in either list (one the game adds later) counts as a skill of the craft too.
	 */
	static final Set<String> SKILLING = new HashSet<>(Arrays.asList("Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking",
		"Crafting", "Smithing", "Mining", "Herblore", "Agility", "Thieving", "Farming", "Runecraft", "Hunter", "Construction", "Sailing"));

	/** The lines each kind must reach: the share of experience, kills an hour played, slots, feats and clues. */
	static final double SKILLER_SHARE = 0.65;
	static final double WARRIOR_SHARE = 0.65;
	static final double SLAYER_SHARE = 0.15;
	static final double KILLS_AN_HOUR = 5;
	static final int COLLECTION = 30;
	/** A pet counts as this many log slots: rarer, and more remembered. */
	static final int PET_WEIGHT = 3;
	static final int FEATS = 20;
	/** A diary counts as this many quests. */
	static final int DIARY_WEIGHT = 2;
	static final int CLUES = 75;

	private final String title;
	private final String motto;

	Archetype(String title, String motto)
	{
		this.title = title;
		this.motto = motto;
	}

	public String getTitle()
	{
		return title;
	}

	public String getMotto()
	{
		return motto;
	}

	/** The year's figures an archetype is judged from. */
	public static final class Figures
	{
		long skillingXp;
		long combatXp;
		long slayerXp;
		long playedMs;
		int kills;
		int collection;
		int pets;
		int quests;
		int diaries;
		int clues;

		Figures()
		{
		}

		/** For tests and previews: the year's figures as plain numbers. */
		public static Figures of(long skillingXp, long combatXp, long slayerXp, long playedMs, int kills, int collection, int pets,
			int quests, int diaries, int clues)
		{
			Figures f = new Figures();
			f.skillingXp = skillingXp;
			f.combatXp = combatXp;
			f.slayerXp = slayerXp;
			f.playedMs = playedMs;
			f.kills = kills;
			f.collection = collection;
			f.pets = pets;
			f.quests = quests;
			f.diaries = diaries;
			f.clues = clues;
			return f;
		}

		long totalXp()
		{
			return skillingXp + combatXp + slayerXp;
		}

		static Figures of(Recap r)
		{
			Figures f = new Figures();
			for (Recap.SkillYear s : r.getSkills())
			{
				if (SLAYER.equals(s.getSkill()))
				{
					f.slayerXp += s.getGained();
				}
				else if (COMBAT.contains(s.getSkill()))
				{
					f.combatXp += s.getGained();
				}
				else
				{
					f.skillingXp += s.getGained();
				}
			}
			f.playedMs = r.getPlayedMs();
			f.kills = r.getKills();
			f.collection = r.getCollection();
			f.pets = r.getPets();
			f.quests = r.getQuests();
			f.diaries = r.getDiaries();
			f.clues = r.getClues();
			return f;
		}
	}

	/** How far a year went past the line for this kind: 1 is just on it, 2 twice it, 0 nothing at all. */
	double score(Figures f)
	{
		long total = f.totalXp();
		switch (this)
		{
			case THE_COLLECTOR:
				return (f.collection + f.pets * (double) PET_WEIGHT) / COLLECTION;
			case THE_TREASURE_HUNTER:
				return f.clues / (double) CLUES;
			case THE_BOSS_HUNTER:
				// Kills an hour of everything played, so a long year of a little bossing is not a boss hunter's.
				return f.playedMs < 3_600_000L ? 0 : f.kills / (f.playedMs / 3_600_000.0) / KILLS_AN_HOUR;
			case THE_SLAYER:
				return total <= 0 ? 0 : f.slayerXp / (double) total / SLAYER_SHARE;
			case THE_ADVENTURER:
				return (f.quests + f.diaries * (double) DIARY_WEIGHT) / FEATS;
			case THE_SKILLER:
				return total <= 0 ? 0 : f.skillingXp / (double) total / SKILLER_SHARE;
			case THE_WARRIOR:
				return total <= 0 ? 0 : f.combatXp / (double) total / WARRIOR_SHARE;
			default:
				return 0;
		}
	}

	/**
	 * The archetype of a year. The kind that went furthest past its line wins, the rarer kinds
	 * first on a tie (they are listed first); a year that reached no line is the skiller's or the
	 * warrior's by where most of its experience went, and a year with nothing is the wanderer's.
	 */
	public static Archetype of(Figures f)
	{
		Archetype best = null;
		double top = 1;
		for (Archetype a : values())
		{
			double s = a.score(f);
			if (s >= top && (best == null || s > top))
			{
				best = a;
				top = s;
			}
		}
		if (best != null)
		{
			return best;
		}
		long total = f.totalXp();
		if (total <= 0)
		{
			return THE_WANDERER;
		}
		return f.skillingXp >= f.combatXp + f.slayerXp ? THE_SKILLER : THE_WARRIOR;
	}

	public static Archetype of(Recap r)
	{
		return of(Figures.of(r));
	}

	/** The plain reason a year is this kind, for under the title: "71% of your experience went to skills of the craft". */
	public String reason(Figures f)
	{
		long total = Math.max(1, f.totalXp());
		switch (this)
		{
			case THE_COLLECTOR:
				return f.pets > 0 ? plural(f.collection, "log slot", "log slots") + " and " + plural(f.pets, "pet", "pets") + " this year"
					: plural(f.collection, "new log slot", "new log slots") + " this year";
			case THE_TREASURE_HUNTER:
				return plural(f.clues, "clue scroll", "clue scrolls") + " opened this year";
			case THE_BOSS_HUNTER:
				return String.format(Locale.ENGLISH, "%.1f kills for every hour played", f.kills / Math.max(1, f.playedMs / 3_600_000.0));
			case THE_SLAYER:
				return percent(f.slayerXp, total) + " of your experience was Slayer";
			case THE_ADVENTURER:
				return plural(f.quests, "quest", "quests") + " and " + plural(f.diaries, "diary", "diaries") + " this year";
			case THE_SKILLER:
				return percent(f.skillingXp, total) + " of your experience went to skills of the craft";
			case THE_WARRIOR:
				return percent(f.combatXp, total) + " of your experience went to combat";
			default:
				return "No one thing claimed the year";
		}
	}

	public String reason(Recap r)
	{
		return reason(Figures.of(r));
	}

	private static String percent(long part, long whole)
	{
		return Math.round(100.0 * part / whole) + "%";
	}

	private static String plural(long n, String one, String many)
	{
		return String.format(Locale.ENGLISH, "%,d", n) + " " + (n == 1 ? one : many);
	}
}
