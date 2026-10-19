package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class KeepsakeTest
{
	private static final java.time.ZoneId UTC = AnnalsTest.UTC;

	private static Entry find(Annals annals, Entry.Kind kind)
	{
		for (Entry e : annals.getEntries())
		{
			if (e.getKind() == kind)
			{
				return e;
			}
		}
		return null;
	}

	@Test
	public void tellsTheRoadToA99()
	{
		// Begun on Thursday 12 March; Slayer trained in two weeks, then 99 on Wednesday 25 March.
		Annals annals = AnnalsTest.begun(AnnalsTest.at(2026, 3, 12, 10), 97, 11_000_000L);
		annals.tick(AnnalsTest.at(2026, 3, 12, 11), 3_600_000L, UTC);
		annals.stat("Slayer", 11_900_000L, 98, AnnalsTest.at(2026, 3, 13, 11), UTC, true);
		annals.tick(AnnalsTest.at(2026, 3, 13, 12), 3_600_000L, UTC);
		annals.tick(AnnalsTest.at(2026, 3, 24, 12), 3_600_000L, UTC);
		annals.stat("Slayer", 12_000_000L, 98, AnnalsTest.at(2026, 3, 24, 13), UTC, true);
		annals.stat("Slayer", 13_034_431L, 99, AnnalsTest.at(2026, 3, 25, 13), UTC, true);
		annals.tick(AnnalsTest.at(2026, 3, 25, 14), 3_600_000L, UTC);
		annals.record(new Entry(Entry.Kind.QUEST, AnnalsTest.at(2026, 3, 25, 15), "Dragon Slayer II", "Quest complete"), AnnalsTest.at(2026, 3, 25, 15), UTC);

		Entry max = find(annals, Entry.Kind.MAX);
		Wealth.Saved ledger = new Wealth.Saved(900_000_000L, Arrays.asList(
			new Wealth.Point(AnnalsTest.at(2026, 3, 20, 10), 700_000_000L),
			new Wealth.Point(AnnalsTest.at(2026, 3, 24, 10), 750_000_000L),
			new Wealth.Point(AnnalsTest.at(2026, 3, 26, 10), 800_000_000L)));
		Keepsake k = Keepsake.of(annals, max, ledger, UTC);
		assertNotNull(k);
		assertEquals(LocalDate.of(2026, 3, 25), k.getDay());
		assertEquals("A ninety-nine", k.getKind());
		// 12 March is day 1, so 25 March is day 14; four days were played by then.
		assertEquals(14, k.getDayOfAnnals());
		assertEquals(4, k.getDaysPlayed());
		// Slayer gained in the week of 9 March and the week of 23 March; that last week brought 1.13M.
		assertEquals("Slayer", k.getSkill());
		assertEquals(2, k.getSkillWeeks());
		assertEquals(13_034_431L - 11_900_000L, k.getSkillWeekXp());
		assertEquals(4 * 3_600_000L, k.getPlayedMs());
		// The ledger's last reading before the moment, the day before it, not the one after.
		assertEquals(750_000_000L, k.getWorth());
		// The quest the same day is told beneath it; the 99's own level line is not.
		assertEquals(1, k.getSameDay().size());
		assertEquals("Dragon Slayer II", k.getSameDay().get(0).getTitle());
	}

	@Test
	public void leavesOutWhatItCannotTell()
	{
		Annals annals = AnnalsTest.begun(AnnalsTest.at(2026, 3, 12, 10), 80, 2_000_000L);
		annals.drop("Draconic visage", 1, 11286, 3_000_000L, "Vorkath", AnnalsTest.at(2026, 4, 1, 12));
		Entry drop = find(annals, Entry.Kind.DROP);
		// A ledger reading a week old says nothing about that day.
		Wealth.Saved stale = new Wealth.Saved(1L, Arrays.asList(new Wealth.Point(AnnalsTest.at(2026, 3, 25, 10), 500_000_000L)));
		assertEquals(-1, Keepsake.of(annals, drop, stale, UTC).getWorth());
		assertEquals(-1, Keepsake.of(annals, drop, null, UTC).getWorth());
		// The prologue has no keepsake, and nor do annals not yet begun.
		assertNull(Keepsake.of(annals, find(annals, Entry.Kind.BEGIN), null, UTC));
		assertNull(Keepsake.of(new Annals(), drop, null, UTC));
	}

	@Test
	public void ranksADropAmongTheYearsByValue()
	{
		Annals annals = AnnalsTest.begun(AnnalsTest.at(2026, 3, 12, 10), 80, 2_000_000L);
		annals.drop("Draconic visage", 1, 11286, 3_000_000L, "Vorkath", AnnalsTest.at(2026, 4, 1, 12));
		annals.drop("Dragon warhammer", 1, 13576, 38_000_000L, "Lizardman shaman", AnnalsTest.at(2026, 5, 1, 12));
		annals.drop("Dragon warhammer", 1, 13576, 38_000_000L, "Lizardman shaman", AnnalsTest.at(2026, 6, 1, 12));
		List<Entry> entries = annals.getEntries();
		assertEquals(3, Keepsake.of(annals, entries.get(1), null, UTC).getDropRank());
		// Two equal drops share first place; the second is not called second.
		assertEquals(1, Keepsake.of(annals, entries.get(2), null, UTC).getDropRank());
		assertEquals(1, Keepsake.of(annals, entries.get(3), null, UTC).getDropRank());
		// Its place by time among the year's drops.
		assertEquals(2, Keepsake.of(annals, entries.get(2), null, UTC).getOrdinal());
	}

	@Test
	public void findsTheMomentAgainInAFreshCopy()
	{
		Annals annals = AnnalsTest.begun(AnnalsTest.at(2026, 3, 12, 10), 80, 2_000_000L);
		annals.killCount("Zulrah", 99, AnnalsTest.at(2026, 4, 1, 12), UTC);
		annals.killCount("Zulrah", 100, AnnalsTest.at(2026, 4, 1, 13), UTC);
		Entry kc = find(annals, Entry.Kind.BOSS);
		Gson gson = new Gson();
		Annals copy = gson.fromJson(gson.toJson(annals), Annals.class).repair();
		Entry again = Keepsake.find(copy, kc);
		assertNotNull(again);
		assertEquals("Zulrah 100", again.getTitle());
		Keepsake k = Keepsake.of(copy, again, null, UTC);
		assertEquals("Zulrah", k.getBoss());
		assertEquals("A kill count", k.getKind());
		// Struck since: nothing to find, so the panel shows the year card again.
		copy.remove(again.getAt(), again.getKind(), again.getTitle());
		assertNull(Keepsake.find(copy, kc));
	}

	@Test
	public void namesAndOrdinals()
	{
		assertEquals("Zulrah", Keepsake.bossOf("First Zulrah"));
		assertEquals("Vorkath", Keepsake.bossOf("Vorkath 1,000"));
		assertEquals("Phosani's Nightmare", Keepsake.bossOf("Phosani's Nightmare 50"));
		assertEquals("1st", Keepsake.ordinal(1));
		assertEquals("2nd", Keepsake.ordinal(2));
		assertEquals("3rd", Keepsake.ordinal(3));
		assertEquals("11th", Keepsake.ordinal(11));
		assertEquals("12th", Keepsake.ordinal(12));
		assertEquals("22nd", Keepsake.ordinal(22));
		assertEquals("1,000 x Zulrah's scales", Keepsake.fileName("1,000 x Zulrah's scales"));
		assertEquals("Recipe for Disaster - Freeing King Awowogei", Keepsake.fileName("Recipe for Disaster - Freeing King Awowogei"));
		assertEquals("Dragon warhammer", Keepsake.fileName("Dragon/warhammer?:*"));
		assertEquals("moment", Keepsake.fileName("///"));
	}

	@Test
	public void highlightsTellARepeatedDropOnce()
	{
		Annals annals = AnnalsTest.begun(AnnalsTest.at(2026, 3, 12, 10), 80, 2_000_000L);
		for (int m = 4; m <= 7; m++)
		{
			annals.drop("Dragon warhammer", 1, 13576, 38_000_000L, "Lizardman shaman", AnnalsTest.at(2026, m, 1, 12));
		}
		annals.record(new Entry(Entry.Kind.QUEST, AnnalsTest.at(2026, 3, 20, 12), "Dragon Slayer II", "Quest complete"), AnnalsTest.at(2026, 3, 20, 12), UTC);
		Recap r = Recap.of(annals, 2026, UTC);
		List<Entry> shown = r.getHighlightsByDate(6);
		assertEquals(2, shown.size());
		// In the order they happened: the quest in March, then the first hammer in April.
		assertEquals("Dragon Slayer II", shown.get(0).getTitle());
		assertEquals("Dragon warhammer", shown.get(1).getTitle());
		assertEquals(AnnalsTest.at(2026, 4, 1, 12), shown.get(1).getAt());
		assertEquals(4, r.timesOf(shown.get(1)));
		assertEquals(1, r.timesOf(shown.get(0)));
		assertTrue(r.getHighlights().get(0).getKind() == Entry.Kind.DROP);
	}
}
