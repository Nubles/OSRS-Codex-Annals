package com.codex.annals.core;

import static com.codex.annals.core.AnnalsTest.UTC;
import static com.codex.annals.core.AnnalsTest.at;
import static com.codex.annals.core.AnnalsTest.begun;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.Test;

/** The day so far at the top of the timeline, and the turn of the day. */
public class TodayTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void theDaySoFarCountsTimeExperienceLevelsAndKills()
	{
		long now = at(2026, 3, 12, 9);
		// Slayer 80 starts at 1,986,068 xp; 2,192,818 is level 81.
		Annals annals = begun(now, 80, 2_000_000L);
		for (int i = 1; i <= 100; i++)
		{
			annals.tick(now + i * 600L, 600, UTC);
		}
		annals.stat("Slayer", 2_200_000L, 81, now + 70_000, UTC, true);
		annals.stat("Mining", 1_010_000L, 70, now + 80_000, UTC, true);
		annals.killCount("Vorkath", 10, now + 90_000, UTC);
		annals.killCount("Vorkath", 12, now + 95_000, UTC);
		annals.killCount("Zulrah", 5, now + 99_000, UTC);

		Today today = Today.of(annals, LocalDate.of(2026, 3, 12), UTC);
		assertNotNull(today);
		assertEquals(60_000, today.getPlayedMs());
		assertEquals(210_000, today.getXp());
		assertEquals(Arrays.asList("Slayer", "Mining"), today.getSkills());
		assertEquals(1, today.getLevels());
		// The first count seen is one kill, the next adds what it moved by.
		assertEquals(4, today.getKills());
		assertEquals("Vorkath", today.getBoss());
		assertEquals(3, today.getBossKills());
		// Three days of play are needed before there is a usual day to set this one against.
		assertEquals(-1, today.getUsualXp());
		assertTrue(Double.isNaN(today.ofUsual()));
	}

	@Test
	public void justAfterMidnightTheNewDayHasNothingYet()
	{
		long now = at(2026, 3, 12, 22);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.tick(now + 600, 600, UTC);
		annals.stat("Slayer", 2_050_000L, 80, now + 60_000, UTC, true);
		assertNotNull(Today.of(annals, LocalDate.of(2026, 3, 12), UTC));
		// Midnight passes with the client open but nothing read yet: the card goes, it does not carry yesterday over.
		assertNull(Today.of(annals, LocalDate.of(2026, 3, 13), UTC));
	}

	@Test
	public void aChangeThatOpensTheDayCountsTowardIt()
	{
		long now = at(2026, 3, 12, 22);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.tick(now + 600, 600, UTC);
		// The first thing after midnight is an experience drop, before any tick of the new day.
		annals.stat("Slayer", 2_004_000L, 80, at(2026, 3, 13, 0) + 5_000, UTC, true);
		Today today = Today.of(annals, LocalDate.of(2026, 3, 13), UTC);
		assertNotNull(today);
		assertEquals(4_000, today.getXp());
	}

	@Test
	public void theUsualDayIsTakenFromTheDaysPlayedBefore()
	{
		Annals annals = begun(at(2026, 3, 9, 9), 80, 2_000_000L);
		long xp = 2_000_000L;
		// Three days of 100k each, a day off, then today.
		for (int day : new int[]{9, 10, 11, 13})
		{
			long start = at(2026, 3, day, 9);
			annals.tick(start, 600, UTC);
			xp += 100_000;
			annals.stat("Slayer", xp, 82, start + 3_600_000, UTC, true);
		}
		Today today = Today.of(annals, LocalDate.of(2026, 3, 13), UTC);
		assertNotNull(today);
		assertEquals(100_000, today.getUsualXp());
		assertEquals(100_000, today.getXp());
		assertEquals(1.0, today.ofUsual(), 1e-9);
	}

	@Test
	public void theDayOutlivesASave()
	{
		long now = at(2026, 3, 12, 9);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.tick(now + 600, 600, UTC);
		annals.killCount("Vorkath", 10, now + 1_000, UTC);
		Annals read = GSON.fromJson(GSON.toJson(annals), Annals.class).repair();
		Today today = Today.of(read, LocalDate.of(2026, 3, 12), UTC);
		assertNotNull(today);
		assertEquals(600, today.getPlayedMs());
		assertEquals(1, today.getKills());
	}

	@Test
	public void aReadingKeptBeforeDaysWereTimedStillReads()
	{
		String old = "{\"begunAt\":1773306000000,\"player\":\"Zezima\",\"xp\":{\"Slayer\":2000000},\"levels\":{\"Slayer\":80},"
			+ "\"pace\":{\"days\":[{\"d\":20524,\"xp\":{\"Slayer\":1900000}}]}}";
		Annals read = GSON.fromJson(old, Annals.class).repair();
		Today today = Today.of(read, LocalDate.ofEpochDay(20524), UTC);
		assertNotNull(today);
		assertEquals(0, today.getPlayedMs());
		assertEquals(0, today.getKills());
		assertEquals(100_000, today.getXp());
	}

	@Test
	public void midnightIsFoundInTheLocalZoneOnShortDays()
	{
		ZoneId london = ZoneId.of("Europe/London");
		// The clocks went forward on 29 March 2026: that day is 23 hours long.
		long start = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, london).toInstant().toEpochMilli();
		assertEquals(23 * 3_600_000L, Today.untilTomorrow(start, london));
		long beforeMidnight = ZonedDateTime.of(2026, 3, 28, 23, 59, 59, 0, london).toInstant().toEpochMilli();
		assertEquals(1_000, Today.untilTomorrow(beforeMidnight, london));
	}
}
