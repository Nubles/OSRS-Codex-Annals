package com.codex.annals.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The annals' own milestones in the suite's shared story shape (N1), saved as
 * {@code codexannals/storyV1} so Codex Home and others can tell them: 99s, totals, quests,
 * diaries, log slots, pets, first kills and kill count milestones, notable drops. Ordinary levels
 * are left out. Newest last, at most {@link Story#MAX_MOMENTS}.
 */
public final class OwnStory
{
	/** The saved shape. Field order is the order written. */
	static final class Saved
	{
		int version = 1;
		long savedAt;
		List<Moment> moments = new ArrayList<>();
	}

	static final class Moment
	{
		long t;
		String kind;
		String text;
		String figure;
		String ref;
	}

	private OwnStory()
	{
	}

	/** The story of these annals, as saved; {@code savedAt} is the newest moment's time, so an unchanged story saves the same text. */
	static Saved of(Annals annals)
	{
		Saved s = new Saved();
		List<Moment> newestFirst = new ArrayList<>();
		List<Entry> entries = annals.getEntries();
		for (int i = entries.size() - 1; i >= 0 && newestFirst.size() < Story.MAX_MOMENTS; i--)
		{
			Moment m = moment(entries.get(i));
			if (m != null)
			{
				newestFirst.add(m);
			}
		}
		Collections.reverse(newestFirst);
		s.moments = newestFirst;
		s.savedAt = newestFirst.isEmpty() ? annals.getBegunAt() : newestFirst.get(newestFirst.size() - 1).t;
		return s;
	}

	/** The story as the JSON saved under {@code storyV1}. */
	public static String json(com.google.gson.Gson gson, Annals annals)
	{
		return gson.toJson(of(annals));
	}

	static Moment moment(Entry e)
	{
		String kind;
		String figure = null;
		switch (e.getKind())
		{
			case MAX:
			case XP:
			case TOTAL:
			case COMBAT:
				kind = "level";
				break;
			case PET:
			case COLLECTION:
			case DROP:
				kind = "drop";
				figure = e.getValue() > 0 ? coins(e.getValue()) : null;
				break;
			case QUEST:
			case DIARY:
			case TASK:
			case CLUE:
				kind = "milestone";
				break;
			case BOSS:
				kind = "milestone";
				figure = e.getLevel() > 0 ? "kc " + e.getLevel() : null;
				break;
			default:
				return null;
		}
		Moment m = new Moment();
		m.t = e.getAt();
		m.kind = kind;
		String detail = e.getDetail();
		String text = e.getTitle() + (detail == null || detail.isEmpty() ? "" : " · " + detail);
		m.text = Json.cut(text, Story.MAX_TEXT);
		m.figure = figure;
		m.ref = e.getKind().name().toLowerCase(Locale.ENGLISH) + ":" + e.getAt();
		return m;
	}

	/** "38M", "1.2B", "450K": the short coin figure the suite uses. */
	static String coins(long gp)
	{
		if (gp >= 1_000_000_000L)
		{
			return trim(gp / 1_000_000_000.0) + "B";
		}
		if (gp >= 1_000_000L)
		{
			return trim(gp / 1_000_000.0) + "M";
		}
		if (gp >= 1_000L)
		{
			return gp / 1_000 + "K";
		}
		return String.valueOf(gp);
	}

	private static String trim(double v)
	{
		String s = String.format(Locale.ENGLISH, "%.1f", v);
		return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
	}
}
