package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class AnnalsTest
{
	static final ZoneId UTC = ZoneOffset.UTC;

	static long at(int year, int month, int day, int hour)
	{
		return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, UTC).toInstant().toEpochMilli();
	}

	static Annals begun(long now, int slayer, long slayerXp)
	{
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> levels = new LinkedHashMap<>();
		xp.put("Slayer", slayerXp);
		levels.put("Slayer", slayer);
		xp.put("Mining", 1_000_000L);
		levels.put("Mining", 70);
		Annals annals = new Annals();
		annals.begin(now, UTC, "Zezima", new Baseline(now, slayer + 70, slayerXp + 1_000_000L, 100, 200, 150, 170, Collections.emptyList()),
			xp, levels, 100);
		return annals;
	}

	@Test
	public void beginsOnce()
	{
		long now = at(2026, 3, 12, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		assertTrue(annals.isBegun());
		assertEquals(1, annals.getEntries().size());
		assertEquals(Entry.Kind.BEGIN, annals.getEntries().get(0).getKind());
		List<Entry> again = annals.begin(now + 1000, UTC, "Zezima", annals.getBaseline(), Collections.emptyMap(), Collections.emptyMap(), 100);
		assertTrue(again.isEmpty());
		assertEquals(now, annals.getBegunAt());
	}

	@Test
	public void writesLevelsAnd99s()
	{
		long now = at(2026, 3, 12, 10);
		Annals annals = begun(now, 98, 12_000_000L);
		List<Entry> written = annals.stat("Slayer", 13_034_431L, 99, now + 60_000, UTC, true);
		assertEquals(Entry.Kind.MAX, written.get(0).getKind());
		assertEquals("99 Slayer", written.get(0).getTitle());
		assertEquals("Your first 99", written.get(0).getDetail());

		written = annals.stat("Mining", 1_100_000L, 71, now + 120_000, UTC, true);
		assertEquals("Mining 71", written.get(0).getTitle());

		// Only round numbers when every level is not wanted.
		assertTrue(annals.stat("Mining", 1_200_000L, 72, now + 180_000, UTC, false).isEmpty());
	}

	@Test
	public void writesTotalAndExperienceMilestones()
	{
		long now = at(2026, 3, 12, 10);
		Annals annals = begun(now, 29, 49_990_000L);
		List<Entry> written = annals.stat("Slayer", 50_010_000L, 30, now + 1, UTC, true);
		boolean total = false;
		boolean xp = false;
		for (Entry e : written)
		{
			total |= e.getTitle().equals("Total level 100");
			xp |= e.getTitle().equals("50M Slayer");
		}
		// 30 + 70 = 100 is under the 500 floor for total milestones.
		assertTrue(!total);
		assertTrue(xp);
		// The account total was already past 50M, so only the skill crossed a mark.
		assertEquals(1, written.size() - 1);
	}

	@Test
	public void countsKillsAndKeepsMilestones()
	{
		long now = at(2026, 5, 1, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		Entry first = annals.killCount("Vorkath", 1, now, UTC);
		assertEquals("First Vorkath", first.getTitle());
		for (int kc = 2; kc < 50; kc++)
		{
			assertNull(annals.killCount("Vorkath", kc, now, UTC));
		}
		Entry fifty = annals.killCount("Vorkath", 50, now, UTC);
		assertEquals("Vorkath 50", fifty.getTitle());
		assertEquals(50, (int) annals.getYear(2026).getKills().get("Vorkath"));
		// The same count twice is one kill, not two.
		annals.killCount("Vorkath", 50, now, UTC);
		assertEquals(50, (int) annals.getYear(2026).getKills().get("Vorkath"));
	}

	@Test
	public void startsANewYearFromTheLastReadings()
	{
		long now = at(2026, 12, 31, 23);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.stat("Slayer", 2_500_000L, 81, now + 1000, UTC, true);
		long next = at(2027, 1, 1, 0) + 5000;
		annals.stat("Slayer", 2_600_000L, 81, next, UTC, true);
		Year y2027 = annals.getYear(2027);
		assertNotNull(y2027);
		assertEquals(100_000L, y2027.gained("Slayer"));
		assertEquals(500_000L, annals.getYear(2026).gained("Slayer"));
	}

	@Test
	public void keepsPlayTimeAndDays()
	{
		long now = at(2026, 6, 1, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		for (int i = 0; i < 100; i++)
		{
			annals.tick(now + i * 600L, 600, UTC);
		}
		annals.tick(at(2026, 6, 2, 10), 600, UTC);
		Year y = annals.getYear(2026);
		assertEquals(101 * 600L, y.getPlayedMs());
		assertEquals(2, y.getDays().size());
	}

	@Test
	public void compactsOrdinaryLevelsFirst()
	{
		long now = at(2026, 1, 1, 10);
		Annals annals = begun(now, 1, 0L);
		annals.record(new Entry(Entry.Kind.PET, now, "A pet followed you home", null), now, UTC);
		long xp = 0;
		for (int i = 0; i < Annals.MAX_ENTRIES + 50; i++)
		{
			annals.stat("Slayer", ++xp, 2 + i % 90, now + i, UTC, true);
			annals.levels.put("Slayer", 1);
		}
		assertEquals(Annals.MAX_ENTRIES, annals.getEntries().size());
		assertEquals(Entry.Kind.BEGIN, annals.getEntries().get(0).getKind());
		assertEquals(Entry.Kind.PET, annals.getEntries().get(1).getKind());
	}

	@Test
	public void clueCountsEveryOneButKeepsMilestones()
	{
		long now = at(2026, 1, 1, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		assertNull(annals.record(ChatParser.entry("You have completed 12 hard Treasure Trails.", now, null), now, UTC));
		assertNotNull(annals.record(ChatParser.entry("You have completed 25 hard Treasure Trails.", now, null), now, UTC));
		assertEquals(2, annals.getYear(2026).getClues());
	}

	@Test
	public void milestonesOfTotalExperience()
	{
		assertEquals(100_000_000L, Annals.totalXpMilestone(99_000_000L, 101_000_000L));
		assertEquals(1_500_000_000L, Annals.totalXpMilestone(1_499_000_000L, 1_500_100_000L));
		assertEquals(0, Annals.totalXpMilestone(101_000_000L, 102_000_000L));
		assertEquals("1.5B", Annals.shortAmount(1_500_000_000L));
	}

	@Test
	public void aDropThatFillsALogSlotIsOneLine()
	{
		long now = at(2026, 7, 21, 20);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.record(ChatParser.entry("New item added to your collection log: Elder maul", now, null), now, UTC);
		Entry merged = annals.drop("Elder maul", 1, 21003, 112_000_000L, "Chambers of Xeric", now + 2000);
		assertEquals(Entry.Kind.COLLECTION, merged.getKind());
		assertEquals("New log slot from Chambers of Xeric", merged.getDetail());
		assertEquals(2, annals.getEntries().size());

		annals.drop("Dragon warhammer", 1, 13576, 31_000_000L, "Lizardman shaman", now + 60_000);
		Entry slot = annals.record(ChatParser.entry("New item added to your collection log: Dragon warhammer", now + 61_000, null), now + 61_000, UTC);
		assertEquals(Entry.Kind.COLLECTION, slot.getKind());
		assertEquals(31_000_000L, slot.getValue());
		assertEquals(3, annals.getEntries().size());
		assertEquals("Elder maul", Recap.of(annals, 2026, UTC).getBiggestDrop().getTitle());
	}
}
