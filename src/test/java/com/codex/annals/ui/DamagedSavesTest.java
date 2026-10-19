package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.codex.annals.core.Annals;
import com.codex.annals.core.Baseline;
import com.codex.annals.core.Entry;
import com.codex.annals.core.Goals;
import com.codex.annals.core.Keepsake;
import com.codex.annals.core.Recap;
import com.codex.annals.core.Wealth;
import com.google.gson.Gson;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/**
 * Every saved key Annals reads, damaged every way a save can be: its own annals, Codex Ledger's
 * ledger and Codex Navigator's goal board. Empty, cut short, the wrong shape, figures below zero
 * or too big, a later version's, or not JSON at all: none may throw, leave the panel or a card
 * unable to draw, or cost the player what still reads.
 */
public class DamagedSavesTest
{
	private static final Gson GSON = new Gson();
	private static final ZoneId Z = ZoneOffset.UTC;
	private static final String[] SKILLS = {"Attack", "Strength", "Defence", "Ranged", "Slayer", "Agility", "Runecraft", "Farming"};
	private static final long DAY = 86_400_000L;

	/** About two hundred days of an account's play, through the real methods. */
	static Annals history(long end, int days)
	{
		Random rnd = new Random(5);
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> lv = new LinkedHashMap<>();
		for (String s : SKILLS)
		{
			xp.put(s, 3_000_000L + rnd.nextInt(9_000_000));
			lv.put(s, level(xp.get(s)));
		}
		Annals a = new Annals();
		long t0 = end - days * DAY;
		a.begin(t0, Z, "Iron Quill", new Baseline(t0, 1800, 90_000_000L, 110, 200, 120, 160, Arrays.asList("Attack")), xp, lv, 110);
		int vork = 100;
		for (int d = 1; d <= days; d++)
		{
			long t = t0 + d * DAY;
			for (int k = 0; k < 20; k++)
			{
				a.tick(t + k * 120_000, 120_000, Z);
			}
			for (String s : SKILLS)
			{
				long nx = xp.get(s) + rnd.nextInt(60_000);
				xp.put(s, nx);
				a.stat(s, nx, level(nx), t + 3_000_000, Z, true);
			}
			if (d % 3 == 0)
			{
				vork += 7;
				a.killCount("Vorkath", vork, t + 3_100_000, Z);
			}
			if (d % 40 == 7)
			{
				a.drop("Dragon warhammer", 1, 13576, 38_000_000L, "Lizardman shaman", t + 3_200_000);
			}
			if (d % 50 == 9)
			{
				a.record(new Entry(Entry.Kind.QUEST, t + 3_300_000, "Desert Treasure II", "Quest complete"), t + 3_300_000, Z);
			}
			if (d == days / 2)
			{
				a.record(new Entry(Entry.Kind.PET, t + 3_400_000, "Vorki", "Pet from Vorkath"), t + 3_400_000, Z);
			}
		}
		return a;
	}

	static int level(long xp)
	{
		return Math.max(1, Math.min(99, net.runelite.api.Experience.getLevelForXp((int) Math.min(Integer.MAX_VALUE, xp))));
	}

	/** A save damaged every way we can think of. */
	static List<String> damaged(String save)
	{
		List<String> out = new ArrayList<>();
		out.add(null);
		out.add("");
		out.add("   ");
		out.add("null");
		out.add("{");
		out.add("}");
		out.add("[]");
		out.add("[1,2,3]");
		out.add("5");
		out.add("\"a save\"");
		out.add("true");
		out.add("{\u0000￿}{][");
		out.add("﻿" + save);
		out.add(save.replaceFirst("^\\{", "{\"version\":99,\"shelf\":{\"a\":[1,{\"b\":2}]},"));
		for (String n : new String[]{"99999999999999999999", "-5", "-9223372036854775808", "9223372036854775807", "2147483648", "1e308", "0.5", "\"many\"", "null", "{}", "[]"})
		{
			out.add(save.replaceAll("(?<=[:\\[,])-?\\d+(\\.\\d+)?(?=[,\\]}])", n));
			out.add(save.replaceFirst("(?<=[:\\[,])-?\\d+(\\.\\d+)?(?=[,\\]}])", n));
		}
		out.add(save.replaceAll("(?<=:)\"[^\"]*\"", "7"));
		out.add(save.replaceAll("(?<=:)\"[^\"]*\"", "\"\""));
		out.add(save.replaceAll("(?<=:)\"[^\"]*\"", "\"NOT_A_THING\""));
		out.add(save.replaceAll("\\[", "{").replaceAll("]", "}"));
		out.add(save.replaceAll("(?<=:)\\{", "[").replaceAll("}(?=[,}])", "]"));
		for (int cut = 1; cut < 24; cut++)
		{
			out.add(save.substring(0, save.length() * cut / 24));
		}
		return out;
	}

	private static String ledger(long end, int points)
	{
		StringBuilder s = new StringBuilder("{\"total\":836600000,\"since\":1,\"history\":[");
		for (int i = points; i >= 0; i--)
		{
			s.append(i == points ? "" : ",").append("{\"t\":").append(end - i * (400 * DAY / Math.max(1, points))).append(",\"v\":").append(500_000_000L + (points - i) * 840L).append("}");
		}
		return s.append("]}").toString();
	}

	private static final String BOARD = "{\"version\":1,\"focus\":\"quest:Dragon Slayer II\",\"goals\":["
		+ "{\"key\":\"quest:Dragon Slayer II\",\"type\":\"quest\",\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.62,\"next\":\"Train Slayer to 18\",\"hoursLeft\":14.5,\"skills\":{\"Slayer\":90,\"Agility\":70},\"quests\":[\"Dream Mentor\"]},"
		+ "{\"key\":\"skill:Agility 99\",\"type\":\"skill\",\"title\":\"Agility 99\",\"complete\":false,\"progress\":0.4,\"hoursLeft\":30,\"skills\":{\"Agility\":99}},"
		+ "{\"key\":\"cape:Quest cape\",\"type\":\"cape\",\"title\":\"Quest cape\",\"complete\":true,\"progress\":1}]}";

	@Test
	public void theAnnalsOwnSaveSurvivesAnyDamage()
	{
		long now = System.currentTimeMillis();
		Annals good = history(now, 120);
		String save = GSON.toJson(good);
		Wealth.Saved worth = Wealth.read(GSON, ledger(now, 60));
		List<Goals.Goal> goals = Goals.read(GSON, BOARD);
		for (String bad : damaged(save))
		{
			Annals.Loaded read = Annals.read(GSON, bad);
			assertNotNull(String.valueOf(bad), read.annals);
			draw(read.annals, worth, goals, bad);
			assertFalse(String.valueOf(bad), Annals.read(GSON, GSON.toJson(read.annals)).damaged);
		}
	}

	@Test
	public void codexLedgersSaveSurvivesAnyDamage()
	{
		long now = System.currentTimeMillis();
		Annals a = history(now, 60);
		for (String bad : damaged(ledger(now, 40)))
		{
			Wealth.Saved worth = Wealth.read(GSON, bad);
			draw(a, worth, null, bad);
		}
	}

	@Test
	public void codexNavigatorsBoardSurvivesAnyDamage()
	{
		long now = System.currentTimeMillis();
		Annals a = history(now, 60);
		for (String bad : damaged(BOARD))
		{
			List<Goals.Goal> goals = Goals.read(GSON, bad);
			assertNotNull(goals);
			draw(a, null, goals, bad);
		}
	}

	@Test
	public void oneDamagedFigureNeverCostsTheRestOfTheAnnals()
	{
		long now = System.currentTimeMillis();
		Annals good = history(now, 120);
		String save = GSON.toJson(good);
		// The account's combat level is no longer a number: every entry, year and week must still read.
		String bad = save.replaceFirst("\"combat\":\\d+", "\"combat\":\"high\"");
		Annals.Loaded read = Annals.read(GSON, bad);
		assertTrue(read.damaged);
		assertEquals(good.getEntries().size(), read.annals.getEntries().size());
		assertEquals(good.getYears().size(), read.annals.getYears().size());
		assertEquals(good.getWeeks().size(), read.annals.getWeeks().size());
		assertEquals(good.getBegunAt(), read.annals.getBegunAt());
		// One bad entry costs that entry only.
		String oneBad = save.replaceFirst("\"kind\":\"[A-Z]+\",\"at\":\\d+", "\"kind\":\"LEVEL\",\"at\":\"soon\"");
		Annals.Loaded entry = Annals.read(GSON, oneBad);
		assertEquals(good.getEntries().size() - 1, entry.annals.getEntries().size());
		// A save cut off part way through its entries keeps those before the cut.
		int cut = nth(save, "{\"kind\"", 30);
		Annals.Loaded part = Annals.read(GSON, save.substring(0, cut));
		assertTrue(part.damaged);
		assertEquals(29, part.annals.getEntries().size());
		assertTrue(part.annals.isBegun());
		// The day the annals began is damaged: they are still begun, on their first entry's day, so
		// the next login does not begin them a second time over the history kept.
		Annals.Loaded undated = Annals.read(GSON, save.replaceFirst("\"begunAt\":\\d+", "\"begunAt\":\"once\""));
		assertTrue(undated.annals.isBegun());
		assertEquals(good.getBegunAt(), undated.annals.getBegunAt());
		assertEquals(good.getEntries().size(), undated.annals.getEntries().size());
		assertFalse(Annals.read(GSON, save).damaged);
		assertFalse(Annals.read(GSON, null).damaged);
	}

	@Test
	public void longHistoriesAreQuickOnTheClientThread()
	{
		long now = System.currentTimeMillis();
		// Four years of play; a ledger kept every few minutes for a year, 12,000 readings.
		Annals a = history(now, 4 * 365);
		String save = GSON.toJson(a);
		String ledger = ledger(now, 12_000);
		Annals.Loaded read = Annals.read(GSON, save);
		assertFalse(read.damaged);
		for (int i = 0; i < 3; i++)
		{
			Annals.read(GSON, save);
			GSON.fromJson(GSON.toJson(a), Annals.class).repair();
			Wealth.read(GSON, ledger);
		}
		long load = time(() -> Annals.read(GSON, save));
		// The panel's copy after a change, made only while the panel is open.
		long copy = time(() -> GSON.fromJson(GSON.toJson(a), Annals.class).repair());
		long worth = time(() -> Wealth.read(GSON, ledger));
		System.out.println("Four years of annals (" + save.length() / 1024 + " KB): load " + load / 1000 + "us, copy " + copy / 1000
			+ "us; 12,000 ledger readings " + worth / 1000 + "us");
		assertTrue("copy " + copy / 1000 + "us", copy < 60_000_000L);
		assertTrue("ledger " + worth / 1000 + "us", worth < 20_000_000L);
		draw(a, Wealth.read(GSON, ledger), Goals.read(GSON, BOARD), "four years");

		// A save holding far more entries than the annals keep, from a damaged or hand-edited file, is trimmed on reading.
		StringBuilder many = new StringBuilder("{\"begunAt\":" + (now - 400 * DAY) + ",\"entries\":[");
		for (int i = 0; i < 12_000; i++)
		{
			many.append(i == 0 ? "" : ",").append("{\"kind\":\"LEVEL\",\"at\":").append(now - 400 * DAY + i * 2_000_000L)
				.append(",\"title\":\"Slayer ").append(2 + i % 97).append("\",\"skill\":\"Slayer\",\"level\":").append(2 + i % 97).append("}");
		}
		Annals huge = Annals.read(GSON, many.append("]}").toString()).annals;
		assertTrue(huge.getEntries().size() + " entries", huge.getEntries().size() <= 2000);
		draw(huge, null, null, "huge");
	}

	private static long time(Runnable r)
	{
		long best = Long.MAX_VALUE;
		for (int i = 0; i < 5; i++)
		{
			long t0 = System.nanoTime();
			r.run();
			best = Math.min(best, System.nanoTime() - t0);
		}
		return best;
	}

	private static int nth(String s, String what, int n)
	{
		int at = -1;
		for (int i = 0; i < n; i++)
		{
			at = s.indexOf(what, at + 1);
		}
		return at;
	}

	/** The panel on every tab, the year card of every year and a keepsake of every kind of moment. */
	static void draw(Annals a, Wealth.Saved worth, List<Goals.Goal> goals, String why)
	{
		ItemIcons items = (id, r) -> null;
		Icons icons = name -> null;
		AnnalsPanel p = new AnnalsPanel(items, icons, new CardActions()
		{
			public void save(BufferedImage c, String n)
			{
			}

			public void copy(BufferedImage c)
			{
			}
		});
		p.setWealth(worth);
		p.setGoals(goals == null ? new ArrayList<>() : goals);
		p.update(a.isBegun() ? a : null, Z, "Iron Quill");
		p.onActivate();
		for (int tab = 0; tab < 4; tab++)
		{
			p.showTab(tab);
			paint(p);
		}
		if (!a.isBegun())
		{
			return;
		}
		for (int year : Recap.years(a))
		{
			Recap r = Recap.of(a, year, Z);
			assertNotNull(why, YearCard.render(r, Wealth.of(worth, year, Z, year == LocalDate.now(Z).getYear()), true, items, icons, () -> { }));
		}
		java.util.Set<Entry.Kind> seen = java.util.EnumSet.noneOf(Entry.Kind.class);
		for (Entry e : a.getEntries())
		{
			if (seen.add(e.getKind()))
			{
				Keepsake k = Keepsake.of(a, e, worth, Z);
				if (k != null)
				{
					assertNotNull(why, KeepsakeCard.render(k, "Iron Quill", a.getBegunAt(), items, icons, () -> { }));
					p.keep(e);
					paint(p);
				}
			}
		}
	}

	private static void paint(Component c)
	{
		c.setSize(225, 2400);
		layout(c);
		BufferedImage img = new BufferedImage(225, 2400, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		c.paint(g);
		g.dispose();
	}

	private static void layout(Component c)
	{
		c.doLayout();
		if (c instanceof Container)
		{
			for (Component k : ((Container) c).getComponents())
			{
				layout(k);
			}
		}
	}
}
