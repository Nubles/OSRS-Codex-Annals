package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * Sailing, the 24th skill (released 19 November 2025), is a skill like any other in the annals:
 * its 99 counts among the 99s, the max total is 24 x 99 = 2,376, and the max cape waits on it.
 * The skills are RuneLite's own list, so the annals follow the game without a list of their own.
 */
public class SailingTest
{
	private static final ZoneId UTC = ZoneOffset.UTC;
	private static final long DAY = 86_400_000L;
	private static final long XP_99 = 13_034_431L;

	/** An account with every skill at 99 except the ones named, which stand at the level given. */
	private static Annals account(long now, Map<String, Integer> below)
	{
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> levels = new LinkedHashMap<>();
		List<String> maxed = new ArrayList<>();
		int total = 0;
		for (Skill skill : Skill.values())
		{
			int level = below.getOrDefault(skill.getName(), 99);
			levels.put(skill.getName(), level);
			xp.put(skill.getName(), level >= 99 ? XP_99 : Experience.forLevel(level));
			total += level;
			if (level >= 99)
			{
				maxed.add(skill.getName());
			}
		}
		Annals annals = new Annals();
		annals.begin(now, UTC, "Zezima", new Baseline(now, total, 0, 126, 300, 150, 170, maxed), xp, levels, 126);
		return annals;
	}

	@Test
	public void runeLiteKnowsTwentyFourSkillsAndTheMaxTotalIs2376()
	{
		// The OSRS wiki's Total level page: "The current maximum for members is 2,376".
		assertEquals(24, Skill.values().length);
		assertEquals("Sailing", Skill.SAILING.getName());
		assertEquals(2_376, Skill.values().length * Experience.MAX_LEVEL);
	}

	@Test
	public void ninetyNineSailingMaxesTheAccount()
	{
		long now = AnnalsTest.at(2026, 6, 1, 12);
		Annals annals = account(now, Collections.singletonMap("Sailing", 98));
		assertEquals(2_375, annals.totalLevel());

		List<Entry> written = annals.stat("Sailing", XP_99, 99, now + 60_000, UTC, true);
		assertEquals(Entry.Kind.MAX, written.get(0).getKind());
		assertEquals("99 Sailing", written.get(0).getTitle());
		assertEquals("Every skill at 99", written.get(0).getDetail());
		assertEquals("Sailing", written.get(0).getSkill());
		Entry max = written.get(1);
		assertEquals(Entry.Kind.TOTAL, max.getKind());
		assertEquals("Total level 2,376", max.getTitle());
		assertEquals(Annals.MAX_TOTAL_DETAIL, max.getDetail());
	}

	@Test
	public void theOldMaxOf2277IsNoLongerTheHighestTotal()
	{
		// Every skill that was in the game before Sailing at 99 bar Hunter, and Sailing untouched.
		long now = AnnalsTest.at(2026, 6, 1, 12);
		Map<String, Integer> below = new LinkedHashMap<>();
		below.put("Hunter", 98);
		below.put("Sailing", 1);
		Annals annals = account(now, below);

		List<Entry> written = annals.stat("Hunter", XP_99, 99, now + 60_000, UTC, true);
		assertEquals(1, written.size());
		assertEquals("99 Hunter", written.get(0).getTitle());
		assertEquals("Your 23rd 99", written.get(0).getDetail());
		// The 23 skills from before Sailing at 99 (the old max of 2,277) and Sailing at 1.
		assertEquals(2_278, annals.totalLevel());
		for (Entry e : annals.getEntries())
		{
			assertFalse("2,278 is no max total: Sailing is still to come", Annals.MAX_TOTAL_DETAIL.equals(e.getDetail()));
		}
	}

	@Test
	public void theMaxCapeWaitsOnSailing()
	{
		long start = AnnalsTest.at(2026, 3, 1, 12);
		Annals annals = account(start, Collections.singletonMap("Sailing", 70));
		long xp = Experience.forLevel(70);
		for (int d = 1; d <= 28; d++)
		{
			long now = start + d * DAY;
			annals.tick(now, 600, UTC);
			xp += 100_000;
			annals.stat("Sailing", xp, Experience.level(xp), now + 3_600_000L, UTC, true);
		}
		LocalDate today = Instant.ofEpochMilli(start + 28 * DAY).atZone(UTC).toLocalDate();
		Ahead ahead = Ahead.of(annals, today, UTC);
		assertFalse(ahead.isMaxed());
		assertEquals(1, ahead.getNineties().size());
		Ahead.Mark sailing = ahead.getNineties().get(0);
		assertEquals("99 Sailing", sailing.getTitle());
		assertNotNull(sailing.getDate());
		// Sailing is the only skill left, so the max cape falls the day it reaches 99.
		assertEquals(sailing.getDate(), ahead.getCape().getDate());
		assertTrue(ahead.getTotal().getTitle().startsWith("Total level "));
	}
}
