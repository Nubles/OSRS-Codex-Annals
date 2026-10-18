package com.codex.annals.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the game's own messages for the things the annals keep. It takes the message with its
 * colour tags already removed and answers with what it found, or null.
 */
public final class ChatParser
{
	private static final Pattern KILL_COUNT = Pattern.compile(
		"^Your (?:completed |subdued )?(.+?) (?:kill |success |completion )?count is: ?([\\d,]+)\\b");
	private static final Pattern COLLECTION = Pattern.compile("^New item added to your collection log: (.+?)\\.?$");
	private static final Pattern QUEST = Pattern.compile("^Congratulations,? you've completed a quest: (.+?)\\.?$");
	private static final Pattern DIARY = Pattern.compile(
		"^Congratulations! You have completed all of the (\\w+) tasks in the (.+?) area\\b");
	private static final Pattern TASK = Pattern.compile(
		"^Congratulations, you've completed an? (\\w+) combat task: (.+?)(?: \\(\\d+ points?\\))?\\.?$");
	private static final Pattern CLUE = Pattern.compile("^You have completed ([\\d,]+) (\\w+) Treasure Trails?\\.?$");
	/** The pet that comes out to follow; the one that goes to the backpack is the other line. */
	public static final String PET_FOLLOWS = "You have a funny feeling like you're being followed";
	private static final String[] PET_LINES = {
		PET_FOLLOWS,
		"You feel something weird sneaking into your backpack",
	};
	/** What a pet is called until the game names it. */
	public static final String UNNAMED_PET = "A pet followed you home";

	/** A kill count the game announced. */
	public static final class KillCount
	{
		private final String name;
		private final int count;

		KillCount(String name, int count)
		{
			this.name = name;
			this.count = count;
		}

		public String getName()
		{
			return name;
		}

		public int getCount()
		{
			return count;
		}
	}

	private ChatParser()
	{
	}

	public static KillCount killCount(String message)
	{
		Matcher m = KILL_COUNT.matcher(message);
		if (!m.find())
		{
			return null;
		}
		String name = m.group(1);
		// Agility courses count their laps the same way; a lap is not a kill and would drown out every boss.
		if (name.endsWith(" lap") || name.endsWith(" Lap"))
		{
			return null;
		}
		try
		{
			return new KillCount(name, Integer.parseInt(m.group(2).replace(",", "")));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * The entry the message tells of, or null.
	 *
	 * @param recentKill the boss whose kill count was just announced, to say where a pet came from
	 */
	public static Entry entry(String message, long now, String recentKill)
	{
		for (String line : PET_LINES)
		{
			if (message.startsWith(line))
			{
				return new Entry(Entry.Kind.PET, now, UNNAMED_PET,
					recentKill == null ? "The rarest drop of all" : "Pet from " + recentKill);
			}
		}
		Matcher m = COLLECTION.matcher(message);
		if (m.find())
		{
			return new Entry(Entry.Kind.COLLECTION, now, m.group(1), "New collection log slot");
		}
		m = QUEST.matcher(message);
		if (m.find())
		{
			return new Entry(Entry.Kind.QUEST, now, m.group(1), "Quest complete");
		}
		m = DIARY.matcher(message);
		if (m.find())
		{
			return new Entry(Entry.Kind.DIARY, now, m.group(2) + " " + m.group(1).toLowerCase() + " diary", "Achievement diary complete");
		}
		m = TASK.matcher(message);
		if (m.find())
		{
			return new Entry(Entry.Kind.TASK, now, m.group(2), capital(m.group(1)) + " combat task");
		}
		m = CLUE.matcher(message);
		if (m.find())
		{
			int count;
			try
			{
				count = Integer.parseInt(m.group(1).replace(",", ""));
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			// Every clue is counted for the year, but only the first and the round numbers are written down.
			Entry clue = new Entry(Entry.Kind.CLUE, now, capital(m.group(2)) + " clue number " + m.group(1),
				count == 1 ? "Your first of this tier" : "Treasure Trails milestone");
			clue.level = count;
			return clue;
		}
		return null;
	}

	/** Whether a clue count is worth its own line: the first, then 10, 25, 50, 100 and every hundred after. */
	public static boolean clueMilestone(int count)
	{
		return count == 1 || count == 10 || count == 25 || count == 50 || (count >= 100 && count % 100 == 0);
	}

	static String capital(String word)
	{
		return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
	}
}
