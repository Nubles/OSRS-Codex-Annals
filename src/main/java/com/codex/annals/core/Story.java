package com.codex.annals.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One account story: the moments every Codex plugin writes under its own {@code storyV1} key,
 * read here and woven into the annals so each moment is told once. A moment the annals already
 * hold (a 99, a log slot, a first kill) gains a second clause from the plugin that knows more;
 * one they do not hold (a personal best, a close call) becomes a line of its own. A death is
 * Codex Guardian's to tell: the same death from another plugin is folded into Guardian's line.
 *
 * <p>The shape read is the suite's N1 contract:
 * {@code {"version":1,"savedAt":ms,"moments":[{"t":ms,"kind":"pb","text":"...","figure":"1:02","ref":"..."}]}},
 * newest last, at most 200, text at most 120 characters. Anything else reads as no moments.
 */
public final class Story
{
	public static final String KEY = "storyV1";
	public static final int MAX_MOMENTS = 200;
	public static final int MAX_TEXT = 120;
	/** The kinds of moment the contract names; any other reads as a note. */
	static final Set<String> KINDS = new HashSet<>(Arrays.asList("pb", "drop", "death", "closeCall", "milestone", "level", "goal", "run", "note"));
	/** How far apart a moment and an annals entry may be and still be the same moment. */
	static final long MATCH_MS = 5 * 60_000L;
	/** How far apart two plugins' moments may be and still be one moment. */
	static final long SAME_MS = 2 * 60_000L;
	/** Two deaths this close together are one death, told by whichever plugin owns deaths. */
	static final long SAME_DEATH_MS = 3 * 60_000L;

	/** The Codex plugins that may tell moments, in the order an owner is preferred. */
	public enum App
	{
		GUARDIAN("codexguardian", "Guardian"),
		BOSS_PILOT("codexbosspilot", "Boss Pilot"),
		FIGHT_CAVES("codexfightcavespilot", "Fight Caves"),
		PLUNDER("codexplunderpilot", "Plunder Pilot"),
		PEST_CONTROL("codexpestcontrol", "Pest Control"),
		MINIGAMES("codexminigamepilot", "Minigame Pilot"),
		ARCHIVE("codexarchive", "Archive Splits"),
		LOG_INSIGHT("codexloginsight", "Log Insight"),
		CHRONICLE("codexsessionchronicle", "Chronicle"),
		NAVIGATOR("codexnavigator", "Navigator"),
		LEDGER("codexledger", "Ledger"),
		GEAR_ROADMAP("codexgearroadmap", "Gear Roadmap"),
		MONEY_PILOT("codexmoneypilot", "Money Pilot"),
		QUARTERMASTER("codexquartermaster", "Quartermaster"),
		BANK_STEWARD("codexbanksteward", "Bank Steward"),
		PROVISIONER("codexprovisioner", "Provisioner"),
		ROUNDS("codexrounds", "Rounds"),
		WAYFINDER("codexwayfinder", "Wayfinder"),
		NOMAD("codexnomad", "Nomad"),
		IRONWORKS("codexironworks", "Ironworks"),
		AUTOPILOT("codexautopilot", "Autopilot"),
		HOME("codexhome", "Home");

		private final String group;
		private final String label;

		App(String group, String label)
		{
			this.group = group;
			this.label = label;
		}

		public String getGroup()
		{
			return group;
		}

		public String getLabel()
		{
			return label;
		}

		/** Lower comes first: the plugin that owns a kind of moment, then the suite's order. */
		int rank(String kind)
		{
			boolean owner = (this == GUARDIAN && ("death".equals(kind) || "closeCall".equals(kind) || "run".equals(kind)))
				|| (this == LOG_INSIGHT && "drop".equals(kind))
				|| (this == NAVIGATOR && "goal".equals(kind))
				|| (this == CHRONICLE && "level".equals(kind));
			return owner ? 0 : ordinal() + 1;
		}
	}

	/** One moment as another plugin told it. */
	public static final class Moment
	{
		private final App app;
		private final long t;
		private final String kind;
		private final String text;
		private final String figure;
		private final String ref;

		public Moment(App app, long t, String kind, String text, String figure, String ref)
		{
			this.app = app;
			this.t = t;
			this.kind = KINDS.contains(kind) ? kind : "note";
			this.text = text;
			this.figure = figure;
			this.ref = ref;
		}

		public App getApp()
		{
			return app;
		}

		public long getT()
		{
			return t;
		}

		public String getKind()
		{
			return kind;
		}

		public String getText()
		{
			return text;
		}

		/** A short figure that goes with it, such as "1:02" or "212 kc", or null. */
		public String getFigure()
		{
			return figure;
		}

		/** The telling plugin's own name for what it is about, or null. */
		public String getRef()
		{
			return ref;
		}

		boolean isDeath()
		{
			return "death".equals(kind);
		}
	}

	private Story()
	{
	}

	/**
	 * Reads one plugin's story. Absent, empty, malformed, a later version, or moments without a
	 * time or text all read as nothing; it never throws. The newest {@link #MAX_MOMENTS} are kept.
	 */
	public static List<Moment> read(Gson gson, App app, String raw)
	{
		JsonObject o = Json.object(gson, raw);
		if (o == null || !Json.versionOne(o))
		{
			return Collections.emptyList();
		}
		JsonArray list = Json.array(o, "moments");
		if (list == null)
		{
			return Collections.emptyList();
		}
		List<Moment> out = new ArrayList<>();
		for (JsonElement e : list)
		{
			if (e == null || !e.isJsonObject())
			{
				continue;
			}
			JsonObject m = e.getAsJsonObject();
			long t = Json.whole(m, "t", 0);
			String text = Json.cut(Json.text(m, "text"), MAX_TEXT);
			if (t <= 0 || text == null || text.isEmpty())
			{
				continue;
			}
			String kind = Json.text(m, "kind");
			out.add(new Moment(app, t, kind, text, Json.cut(Json.text(m, "figure"), 24), Json.cut(Json.text(m, "ref"), 80)));
		}
		out.sort(Comparator.comparingLong(Moment::getT));
		return out.size() > MAX_MOMENTS ? new ArrayList<>(out.subList(out.size() - MAX_MOMENTS, out.size())) : out;
	}

	/**
	 * The annals with every plugin's moments woven in, oldest first. Entries the annals hold are
	 * copied when a moment adds a clause to them, so the list may be kept and drawn freely.
	 */
	public static List<Entry> weave(List<Entry> own, List<Moment> moments)
	{
		List<Entry> out = new ArrayList<>(own);
		if (moments == null || moments.isEmpty())
		{
			return out;
		}
		for (Moment m : once(moments))
		{
			int at = match(out, m);
			if (at >= 0)
			{
				Entry e = out.get(at);
				String clause = clause(e, m);
				if (clause != null && e.getClause() == null)
				{
					Entry copy = e.copy();
					copy.clause = clause;
					copy.clauseApp = m.getApp().getLabel();
					out.set(at, copy);
				}
				continue;
			}
			out.add(entry(m));
		}
		out.sort(Comparator.comparingLong(Entry::getAt));
		return out;
	}

	/** The moments with any moment two plugins both told kept once, from the plugin that owns its kind. */
	static List<Moment> once(List<Moment> moments)
	{
		List<Moment> sorted = new ArrayList<>(moments);
		sorted.removeIf(m -> m == null || m.getApp() == null);
		sorted.sort(Comparator.<Moment>comparingInt(m -> m.getApp().rank(m.getKind())).thenComparingLong(Moment::getT));
		List<Moment> kept = new ArrayList<>();
		for (Moment m : sorted)
		{
			boolean told = false;
			for (Moment k : kept)
			{
				if (same(k, m))
				{
					told = true;
					break;
				}
			}
			if (!told)
			{
				kept.add(m);
			}
		}
		kept.sort(Comparator.comparingLong(Moment::getT));
		return kept;
	}

	static boolean same(Moment a, Moment b)
	{
		if (a.getApp() == b.getApp())
		{
			return false;
		}
		long dt = Math.abs(a.getT() - b.getT());
		if (a.isDeath() && b.isDeath())
		{
			return dt <= SAME_DEATH_MS;
		}
		if (!a.getKind().equals(b.getKind()) || dt > SAME_MS)
		{
			return false;
		}
		Set<String> x = words(a.getText());
		Set<String> y = words(b.getText());
		if (x.isEmpty() || y.isEmpty())
		{
			return false;
		}
		if (x.containsAll(y) || y.containsAll(x))
		{
			return true;
		}
		Set<String> both = new HashSet<>(x);
		both.retainAll(y);
		Set<String> either = new HashSet<>(x);
		either.addAll(y);
		return both.size() * 2 >= either.size();
	}

	/** The annals entry this moment is about, or -1. Deaths are never in the annals' own lines. */
	static int match(List<Entry> entries, Moment m)
	{
		if (m.isDeath() || "closeCall".equals(m.getKind()))
		{
			return -1;
		}
		Set<String> said = words(m.getText());
		int best = -1;
		long bestDt = Long.MAX_VALUE;
		for (int i = 0; i < entries.size(); i++)
		{
			Entry e = entries.get(i);
			long dt = Math.abs(e.getAt() - m.getT());
			if (dt > MATCH_MS || dt >= bestDt || e.isMoment() || !fits(m.getKind(), e.getKind()))
			{
				continue;
			}
			Set<String> title = words(e.getTitle());
			if (!title.isEmpty() && said.containsAll(title))
			{
				best = i;
				bestDt = dt;
			}
		}
		return best;
	}

	/** Whether a kind of moment can be about a kind of annals entry. */
	static boolean fits(String moment, Entry.Kind kind)
	{
		switch (moment)
		{
			case "drop":
				return kind == Entry.Kind.PET || kind == Entry.Kind.COLLECTION || kind == Entry.Kind.DROP || kind == Entry.Kind.CLUE;
			case "level":
				return kind == Entry.Kind.MAX || kind == Entry.Kind.LEVEL || kind == Entry.Kind.XP || kind == Entry.Kind.TOTAL || kind == Entry.Kind.COMBAT;
			default:
				return kind != Entry.Kind.BEGIN;
		}
	}

	/**
	 * The second clause a moment adds to the entry it is about: what it says beyond the entry's
	 * own name, with its figure; null when it says nothing more, so it is simply told once.
	 */
	static String clause(Entry e, Moment m)
	{
		String text = m.getText();
		String title = e.getTitle();
		if (text.toLowerCase(Locale.ENGLISH).startsWith(title.toLowerCase(Locale.ENGLISH)))
		{
			text = text.substring(title.length()).replaceFirst("^[\\s\\u00b7,:;\\-\\u2013\\u2014.]+", "");
		}
		Set<String> extra = words(text);
		extra.removeAll(words(title));
		String figure = m.getFigure();
		if (extra.isEmpty())
		{
			text = "";
		}
		if (figure != null && !figure.isEmpty() && !text.toLowerCase(Locale.ENGLISH).contains(figure.toLowerCase(Locale.ENGLISH)))
		{
			text = text.isEmpty() ? figure : text + " · " + figure;
		}
		return text.isEmpty() ? null : text;
	}

	/** A moment no annals line holds, as a line of its own. */
	static Entry entry(Moment m)
	{
		Entry.Kind kind;
		Entry.Chapter chapter;
		switch (m.getKind())
		{
			case "death":
				kind = Entry.Kind.DEATH;
				chapter = Entry.Chapter.ALL;
				break;
			case "closeCall":
				kind = Entry.Kind.CLOSE_CALL;
				chapter = Entry.Chapter.ALL;
				break;
			case "pb":
				kind = Entry.Kind.PB;
				chapter = Entry.Chapter.BOSSES;
				break;
			case "drop":
				kind = Entry.Kind.FIND;
				chapter = Entry.Chapter.LOOT;
				break;
			case "level":
				kind = Entry.Kind.MOMENT;
				chapter = Entry.Chapter.SKILLS;
				break;
			case "milestone":
			case "goal":
				kind = Entry.Kind.MOMENT;
				chapter = Entry.Chapter.FEATS;
				break;
			default:
				kind = Entry.Kind.MOMENT;
				chapter = Entry.Chapter.ALL;
		}
		// "Draconic visage · 1 in 5,000 · luckier than 92%": the name leads the line, the rest goes beneath it.
		String text = m.getText();
		String title = text;
		String rest = "";
		int dot = text.indexOf(" \u00b7 ");
		if (dot > 0)
		{
			title = text.substring(0, dot).trim();
			rest = text.substring(dot + 3).trim();
		}
		String figure = m.getFigure();
		if (figure != null && !figure.isEmpty() && !rest.toLowerCase(Locale.ENGLISH).contains(figure.toLowerCase(Locale.ENGLISH)))
		{
			rest = rest.isEmpty() ? figure : figure + " \u00b7 " + rest;
		}
		String detail = m.getApp().getLabel() + (rest.isEmpty() ? "" : " \u00b7 " + rest);
		Entry e = new Entry(kind, m.getT(), title, detail);
		e.app = m.getApp().getLabel();
		e.ref = m.getRef();
		e.chapter = chapter;
		return e;
	}

	/** Words that say nothing about which moment it is. */
	private static final Set<String> QUIET = new HashSet<>(Arrays.asList(
		"a", "an", "the", "of", "at", "in", "on", "to", "and", "your", "you", "first", "new", "kc", "kill", "count", "level", "reached",
		"from", "with", "for", "by", "is", "was", "x"));

	/** The words of a line that name what it is about, lower case, numbers without their commas. */
	static Set<String> words(String text)
	{
		Set<String> out = new LinkedHashSet<>();
		if (text == null)
		{
			return out;
		}
		String plain = text.toLowerCase(Locale.ENGLISH).replaceAll("(?<=\\d),(?=\\d{3})", "");
		for (String w : plain.split("[^a-z0-9']+"))
		{
			String word = w.replace("'", "");
			if (!word.isEmpty() && !QUIET.contains(word))
			{
				out.add(word);
			}
		}
		return out;
	}
}
