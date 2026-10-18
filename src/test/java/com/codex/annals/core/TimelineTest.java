package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.junit.Test;

public class TimelineTest
{
	@Test
	public void foldsADayOfLevels()
	{
		long now = AnnalsTest.at(2026, 3, 12, 10);
		Annals annals = AnnalsTest.begun(now, 60, 300_000L);
		annals.stat("Slayer", 340_000L, 61, now + 1000, AnnalsTest.UTC, true);
		annals.stat("Slayer", 380_000L, 62, now + 2000, AnnalsTest.UTC, true);
		annals.stat("Slayer", 430_000L, 63, now + 3000, AnnalsTest.UTC, true);
		annals.record(new Entry(Entry.Kind.QUEST, now + 4000, "Dragon Slayer II", "Quest complete"), now, AnnalsTest.UTC);
		annals.stat("Mining", 1_100_000L, 71, now + 5000, AnnalsTest.UTC, true);
		annals.stat("Slayer", 480_000L, 64, now + 6000, AnnalsTest.UTC, true);

		List<Timeline.Row> rows = Timeline.rows(annals.getEntries(), Entry.Chapter.ALL, AnnalsTest.UTC);
		// Newest first: Mining + Slayer folded, the quest, three Slayer levels folded, the opening.
		assertEquals(4, rows.size());
		assertEquals("2 levels", rows.get(0).getEntry().getTitle());
		assertEquals("Slayer 64, Mining 71", rows.get(0).getEntry().getDetail());
		assertEquals("Dragon Slayer II", rows.get(1).getEntry().getTitle());
		assertEquals("Slayer 60 to 63", rows.get(2).getEntry().getTitle());
		assertEquals(3, rows.get(2).getFolded());

		List<Timeline.Row> feats = Timeline.rows(annals.getEntries(), Entry.Chapter.FEATS, AnnalsTest.UTC);
		assertEquals(1, feats.size());
	}
}
