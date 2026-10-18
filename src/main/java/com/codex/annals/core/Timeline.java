package com.codex.annals.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The annals as the timeline shows them: newest first, in one chapter, with a day's ordinary
 * levels folded into a single line so a long grind does not bury the rest.
 */
public final class Timeline
{
	/** One line on the timeline: an entry, or a day's levels folded together. */
	public static final class Row
	{
		private final Entry entry;
		private final LocalDate date;
		private final int folded;

		Row(Entry entry, LocalDate date, int folded)
		{
			this.entry = entry;
			this.date = date;
			this.folded = folded;
		}

		public Entry getEntry()
		{
			return entry;
		}

		public LocalDate getDate()
		{
			return date;
		}

		/** How many level entries this row stands for, or 0 for a single entry. */
		public int getFolded()
		{
			return folded;
		}
	}

	private Timeline()
	{
	}

	public static List<Row> rows(List<Entry> written, Entry.Chapter chapter, ZoneId zone)
	{
		// Written in the order they happened, unless the computer's clock was put back; the timeline goes by the clock.
		List<Entry> entries = new ArrayList<>(written);
		entries.sort(Comparator.comparingLong(Entry::getAt));
		List<Row> rows = new ArrayList<>();
		int i = entries.size() - 1;
		while (i >= 0)
		{
			Entry e = entries.get(i);
			if (!e.in(chapter))
			{
				i--;
				continue;
			}
			LocalDate date = Instant.ofEpochMilli(e.at).atZone(zone).toLocalDate();
			if (e.getKind() != Entry.Kind.LEVEL)
			{
				rows.add(new Row(e, date, 0));
				i--;
				continue;
			}
			// Gather every level of the day that sits next to this one in the chapter shown;
			// anything else shown in between ends the run, so a pet is never folded away.
			List<Entry> day = new ArrayList<>();
			int j = i;
			while (j >= 0 && Instant.ofEpochMilli(entries.get(j).at).atZone(zone).toLocalDate().equals(date))
			{
				Entry next = entries.get(j);
				if (next.getKind() == Entry.Kind.LEVEL)
				{
					day.add(next);
				}
				else if (next.in(chapter))
				{
					break;
				}
				j--;
			}
			i = j;
			if (day.size() == 1)
			{
				rows.add(new Row(e, date, 0));
				continue;
			}
			rows.add(new Row(fold(day), date, day.size()));
		}
		return rows;
	}

	/** One line for a day of levels: "Mining 61 to 64", or "6 levels" naming where each skill ended. */
	static Entry fold(List<Entry> newestFirst)
	{
		Map<String, int[]> bySkill = new LinkedHashMap<>();
		for (Entry e : newestFirst)
		{
			String skill = e.getSkill() == null ? e.getTitle() : e.getSkill();
			int[] range = bySkill.get(skill);
			int from = fromLevel(e);
			if (range == null)
			{
				bySkill.put(skill, new int[]{from, e.level});
			}
			else
			{
				range[0] = Math.min(range[0], from);
				range[1] = Math.max(range[1], e.level);
			}
		}
		Entry newest = newestFirst.get(0);
		Entry folded = new Entry(Entry.Kind.LEVEL, newest.at, null, null);
		int levels = 0;
		for (int[] range : bySkill.values())
		{
			levels += range[1] - range[0];
		}
		if (bySkill.size() == 1)
		{
			Map.Entry<String, int[]> only = bySkill.entrySet().iterator().next();
			folded.title = only.getKey() + " " + only.getValue()[0] + " to " + only.getValue()[1];
			folded.detail = levels + " levels in a day";
			folded.skill = only.getKey();
			folded.level = only.getValue()[1];
			return folded;
		}
		folded.title = levels + " levels";
		StringBuilder detail = new StringBuilder();
		for (Map.Entry<String, int[]> s : bySkill.entrySet())
		{
			if (detail.length() > 0)
			{
				detail.append(", ");
			}
			detail.append(s.getKey()).append(' ').append(s.getValue()[1]);
		}
		folded.detail = detail.toString();
		folded.skill = bySkill.keySet().iterator().next();
		folded.level = bySkill.values().iterator().next()[1];
		return folded;
	}

	/** The level an entry rose from; older saves without it rose by one. */
	private static int fromLevel(Entry e)
	{
		return e.from > 0 ? e.from : e.level - 1;
	}
}
