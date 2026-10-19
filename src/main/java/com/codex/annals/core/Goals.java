package com.codex.annals.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The goals pinned in Codex Navigator, read from the board it publishes for other plugins under
 * {@link #KEY} of its config group, and dated at the pace the annals know: each level a goal
 * still needs, at the pace that skill is really trained, and the hours of work Navigator counts,
 * against the hours a day really played. Read only; without Navigator there are no goals.
 *
 * <p>The board's shape, as written by Navigator:
 * {@code {"version":1,"focus":key,"goals":[{"key","type","title","complete","progress","next","hoursLeft","skills":{"Agility":60},"quests":[...]}]}}
 */
public final class Goals
{
	public static final String GROUP = "codexnavigator";
	public static final String KEY = "goalBoard";

	private Goals()
	{
	}

	/** One goal on the board. */
	public static final class Goal
	{
		private final String key;
		private final String title;
		private final boolean complete;
		private final double progress;
		private final double hoursLeft;
		private final String next;
		private final Map<String, Integer> skills;
		private final int quests;

		Goal(String key, String title, boolean complete, double progress, double hoursLeft, String next, Map<String, Integer> skills, int quests)
		{
			this.key = key;
			this.title = title;
			this.complete = complete;
			this.progress = progress;
			this.hoursLeft = hoursLeft;
			this.next = next;
			this.skills = skills;
			this.quests = quests;
		}

		public String getKey()
		{
			return key;
		}

		public String getTitle()
		{
			return title;
		}

		public boolean isComplete()
		{
			return complete;
		}

		/** 0 to 1. */
		public double getProgress()
		{
			return progress;
		}

		/** About how many hours of training and questing Navigator counts are left. */
		public double getHoursLeft()
		{
			return hoursLeft;
		}

		/** Navigator's next step in plain words, or null. */
		public String getNext()
		{
			return next;
		}

		/** The levels it still needs, by skill name, as the level to reach ("Agility": 60). */
		public Map<String, Integer> getSkills()
		{
			return skills;
		}

		/** Unfinished quests on the way. */
		public int getQuests()
		{
			return quests;
		}
	}

	/** When a goal falls at the pace the account keeps. */
	public static final class Forecast
	{
		private final Goal goal;
		private final LocalDate levels;
		private final String last;
		private final List<String> stalled;
		private final LocalDate allIn;
		/** Some level it needs is not reached yet. */
		private final boolean levelsLeft;

		Forecast(Goal goal, LocalDate levels, String last, List<String> stalled, LocalDate allIn, boolean levelsLeft)
		{
			this.levelsLeft = levelsLeft;
			this.goal = goal;
			this.levels = levels;
			this.last = last;
			this.stalled = stalled;
			this.allIn = allIn;
		}

		public Goal getGoal()
		{
			return goal;
		}

		/** When the last level it needs comes at each skill's own pace; null when one is stalled or it needs none. */
		public LocalDate getLevels()
		{
			return levels;
		}

		/** The skill whose level comes last, or null. */
		public String getLast()
		{
			return last;
		}

		/** Skills it needs that are not moving fast enough to say, in the board's order. */
		public List<String> getStalled()
		{
			return stalled;
		}

		/** When the hours of work left are done if every hour played went to it; null without a pace of play. */
		public LocalDate getAllIn()
		{
			return allIn;
		}

		/** The date to show: the levels' while any is still to reach, else the hours'. */
		public LocalDate getDate()
		{
			return levelsLeft ? levels : allIn;
		}

		/** Whether a level it needs is still to reach; the board can lag a level just gained. */
		public boolean hasLevelsLeft()
		{
			return levelsLeft;
		}
	}

	/** The goals on a board, in its order; empty when there is none or it cannot be read. */
	public static List<Goal> read(Gson gson, String json)
	{
		List<Goal> out = new ArrayList<>();
		if (json == null || json.trim().isEmpty())
		{
			return out;
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || !root.isJsonObject() || !(root.getAsJsonObject().get("goals") instanceof com.google.gson.JsonArray))
			{
				return out;
			}
			for (JsonElement e : root.getAsJsonObject().getAsJsonArray("goals"))
			{
				if (!e.isJsonObject())
				{
					continue;
				}
				JsonObject g = e.getAsJsonObject();
				String key = string(g, "key");
				String title = string(g, "title");
				if (key == null || title == null)
				{
					continue;
				}
				Map<String, Integer> skills = new LinkedHashMap<>();
				JsonElement s = g.get("skills");
				if (s != null && s.isJsonObject())
				{
					for (Map.Entry<String, JsonElement> l : s.getAsJsonObject().entrySet())
					{
						Double level = number(l.getValue());
						if (level != null && level >= 1 && level <= Experience.MAX_LEVEL)
						{
							skills.put(l.getKey(), level.intValue());
						}
					}
				}
				JsonElement q = g.get("quests");
				int quests = q != null && q.isJsonArray() ? q.getAsJsonArray().size() : 0;
				JsonElement done = g.get("complete");
				boolean complete = done != null && done.isJsonPrimitive() && done.getAsJsonPrimitive().isBoolean() && done.getAsBoolean();
				Double progress = number(g.get("progress"));
				Double hours = number(g.get("hoursLeft"));
				out.add(new Goal(key, title, complete, progress == null ? 0 : Math.max(0, Math.min(1, progress)),
					hours == null ? 0 : Math.max(0, hours), string(g, "next"), Collections.unmodifiableMap(skills), quests));
			}
		}
		catch (RuntimeException e)
		{
			// Navigator's board is only read; one that does not read is no board at all.
			out.clear();
		}
		return out;
	}

	/**
	 * Dates every goal not yet complete.
	 *
	 * @param hoursPerDay hours played a day lately, days off included; 0 when not known
	 */
	public static List<Forecast> forecast(List<Goal> goals, Ahead ahead, LocalDate today, double hoursPerDay)
	{
		List<Forecast> out = new ArrayList<>();
		for (Goal g : goals)
		{
			if (g.complete)
			{
				continue;
			}
			LocalDate latest = null;
			String last = null;
			List<String> stalled = new ArrayList<>();
			boolean left = false;
			for (Map.Entry<String, Integer> need : g.skills.entrySet())
			{
				LocalDate d = ahead == null ? null : ahead.whenLevel(need.getKey(), need.getValue());
				if (d != null && !d.isAfter(today))
				{
					// Reached since the board was written.
					continue;
				}
				left = true;
				if (d == null)
				{
					stalled.add(need.getKey());
				}
				else if (latest == null || d.isAfter(latest))
				{
					latest = d;
					last = need.getKey();
				}
			}
			LocalDate allIn = null;
			if (g.hoursLeft > 0 && hoursPerDay > 0)
			{
				double days = Math.ceil(g.hoursLeft / hoursPerDay);
				allIn = days > Ahead.HORIZON_DAYS ? null : today.plusDays(Math.max(1, (long) days));
			}
			out.add(new Forecast(g, stalled.isEmpty() ? latest : null, last, Collections.unmodifiableList(stalled), allIn, left));
		}
		return out;
	}

	private static String string(JsonObject o, String key)
	{
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
	}

	private static Double number(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return null;
		}
		double d = e.getAsDouble();
		return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
	}
}
