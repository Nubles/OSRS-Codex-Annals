package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import org.junit.Test;

public class RecapTest
{
	@Test
	public void tellsTheYear()
	{
		long now = AnnalsTest.at(2026, 3, 12, 10);
		Annals annals = AnnalsTest.begun(now, 98, 12_000_000L);
		annals.tick(now, 600, AnnalsTest.UTC);
		annals.tick(AnnalsTest.at(2026, 3, 13, 10), 600, AnnalsTest.UTC);
		annals.tick(AnnalsTest.at(2026, 3, 14, 10), 600, AnnalsTest.UTC);
		annals.tick(AnnalsTest.at(2026, 7, 1, 10), 600, AnnalsTest.UTC);
		annals.stat("Slayer", 13_100_000L, 99, AnnalsTest.at(2026, 7, 1, 11), AnnalsTest.UTC, true);
		annals.killCount("Vorkath", 1, AnnalsTest.at(2026, 7, 1, 12), AnnalsTest.UTC);
		annals.drop("Draconic visage", 1, 11286, 3_000_000L, "Vorkath", AnnalsTest.at(2026, 7, 1, 12));

		Recap r = Recap.of(annals, 2026, AnnalsTest.UTC);
		assertEquals("The year of 99 Slayer", r.getHeadline());
		assertEquals(4, r.getDaysPlayed());
		assertEquals(3, r.getLongestStreak());
		assertEquals(2, r.busiestMonth());
		assertEquals(1, r.getLevelsGained());
		assertEquals(1_100_000L, r.getXpGained());
		assertEquals("Slayer", r.getSkills().get(0).getSkill());
		assertEquals("Draconic visage", r.getBiggestDrop().getTitle());
		assertEquals(Entry.Kind.MAX, r.getHighlights().get(0).getKind());
		assertEquals(Arrays.asList(2026), Recap.years(annals));
	}
}
