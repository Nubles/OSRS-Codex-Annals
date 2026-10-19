package com.codex.annals.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A month of the timeline in one line: what it holds, the biggest things first, such as
 * "2 99s, 14 levels, 3 log slots".
 */
public final class MonthNote
{
	/** Parts written at most, so the line fits the side panel. */
	static final int PARTS = 3;

	private MonthNote()
	{
	}

	/** The line for a month's entries, or null when it holds too little to be worth one. */
	public static String of(List<Entry> month)
	{
		int[] count = new int[Entry.Kind.values().length];
		int levels = 0;
		for (Entry e : month)
		{
			count[e.getKind().ordinal()]++;
			if (e.getKind() == Entry.Kind.LEVEL)
			{
				levels += Math.max(1, e.getLevel() - (e.getFrom() > 0 ? e.getFrom() : e.getLevel() - 1));
			}
			else if (e.getKind() == Entry.Kind.MAX)
			{
				levels++;
			}
		}
		if (month.size() < 3)
		{
			return null;
		}
		List<String> parts = new ArrayList<>();
		part(parts, count[Entry.Kind.PET.ordinal()], "pet", "pets");
		part(parts, count[Entry.Kind.MAX.ordinal()], "99", "99s");
		part(parts, count[Entry.Kind.COLLECTION.ordinal()], "log slot", "log slots");
		part(parts, count[Entry.Kind.DIARY.ordinal()], "diary", "diaries");
		part(parts, count[Entry.Kind.QUEST.ordinal()], "quest", "quests");
		part(parts, levels, "level", "levels");
		part(parts, count[Entry.Kind.BOSS.ordinal()], "kill milestone", "kill milestones");
		part(parts, count[Entry.Kind.DROP.ordinal()], "big drop", "big drops");
		part(parts, count[Entry.Kind.TASK.ordinal()], "combat task", "combat tasks");
		return parts.isEmpty() ? null : String.join(", ", parts.subList(0, Math.min(PARTS, parts.size())));
	}

	private static void part(List<String> parts, int n, String one, String many)
	{
		if (n > 0)
		{
			parts.add(n + " " + (n == 1 ? one : many));
		}
	}
}
