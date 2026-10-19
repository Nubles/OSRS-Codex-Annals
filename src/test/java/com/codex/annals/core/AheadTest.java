package com.codex.annals.core;

import static com.codex.annals.core.AnnalsTest.UTC;
import static com.codex.annals.core.AnnalsTest.at;
import static com.codex.annals.core.AnnalsTest.begun;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AheadTest
{
	private static final long DAY = 86_400_000L;

	@Test
	public void theExperienceTableMatchesTheGame()
	{
		assertEquals(0, Experience.forLevel(1));
		assertEquals(83, Experience.forLevel(2));
		assertEquals(1_986_068, Experience.forLevel(80));
		assertEquals(13_034_431, Experience.forLevel(99));
		assertEquals(98, Experience.level(13_034_430));
		assertEquals(99, Experience.level(200_000_000));
	}

	/** Slayer at 50k a day for four weeks; Mining never moves. */
	private static Annals fourWeeks(long start)
	{
		Annals annals = begun(start, 80, 2_000_000L);
		long xp = 2_000_000L;
		for (int d = 1; d <= 28; d++)
		{
			long now = start + d * DAY;
			annals.tick(now, 600, UTC);
			xp += 50_000;
			annals.stat("Slayer", xp, Experience.level(xp), now + 3_600_000L, UTC, true);
		}
		return annals;
	}

	@Test
	public void forecastsEach99AtTheRecentPace()
	{
		long start = at(2026, 3, 1, 12);
		Annals annals = fourWeeks(start);
		LocalDate today = LocalDate.of(2026, 3, 29);
		Ahead ahead = Ahead.of(annals, today, UTC);
		assertTrue(ahead.hasPace());
		assertTrue(ahead.isRecent());
		assertEquals(28, ahead.getDays());
		Ahead.Mark slayer = ahead.getNineties().get(0);
		assertEquals("99 Slayer", slayer.getTitle());
		assertEquals(50_000, slayer.getPerDay(), 1);
		// 13,034,431 - 3,400,000 at 50k a day: 193 days.
		assertEquals(today.plusDays(193), slayer.getDate());
		// Mining never moves, so there is no max cape at this pace.
		assertEquals(List.of("Mining"), ahead.getStalled());
		assertNull(ahead.getCape().getDate());
		assertFalse(ahead.isMaxed());
	}

	@Test
	public void thePaceIsTheLastFourWeeksEvenWithDaysOff()
	{
		// Played every other day for eight weeks: the 29 readings kept reach back 56 days, but
		// the pace is the last four weeks' alone, as the page says.
		long start = at(2026, 3, 1, 12);
		Annals annals = begun(start, 80, 2_000_000L);
		long xp = 2_000_000L;
		for (int d = 2; d <= 56; d += 2)
		{
			long now = start + d * DAY;
			annals.tick(now, 600, UTC);
			xp += 100_000;
			annals.stat("Slayer", xp, Experience.level(xp), now + 3_600_000L, UTC, true);
		}
		LocalDate today = LocalDate.of(2026, 3, 1).plusDays(57);
		annals.tick(start + 57 * DAY, 600, UTC);
		Ahead ahead = Ahead.of(annals, today, UTC);
		assertTrue(ahead.isRecent());
		assertEquals(28, ahead.getDays());
		// Fourteen days played in the last 28, 100k each: 50k a day.
		assertEquals(50_000, ahead.getNineties().get(0).getPerDay(), 1);
	}

	@Test
	public void tooEarlyForAPace()
	{
		long start = at(2026, 3, 1, 12);
		Annals annals = begun(start, 80, 2_000_000L);
		annals.tick(start + 2 * DAY, 600, UTC);
		Ahead ahead = Ahead.of(annals, LocalDate.of(2026, 3, 3), UTC);
		assertFalse(ahead.hasPace());
		assertTrue(ahead.getNineties().isEmpty());
	}

	@Test
	public void fallsBackOnTheYearSoFar()
	{
		long start = at(2026, 1, 1, 12);
		Annals annals = begun(start, 80, 2_000_000L);
		annals.stat("Slayer", 2_600_000L, 82, at(2026, 3, 1, 12), UTC, true);
		annals.stat("Mining", 1_600_000L, 74, at(2026, 3, 1, 12), UTC, true);
		// The readings of the last weeks were lost; the year still knows where it began.
		annals.pace = new Pace();
		annals.tick(at(2026, 3, 1, 13), 600, UTC);
		Ahead ahead = Ahead.of(annals, LocalDate.of(2026, 3, 1), UTC);
		assertFalse(ahead.isRecent());
		assertEquals(60, ahead.getDays());
		assertEquals(10_000, ahead.getNineties().get(0).getPerDay(), 1);
		// Both moving: the cape falls when the slower one gets there.
		assertNotNull(ahead.getCape().getDate());
		assertEquals(ahead.getNineties().get(ahead.getNineties().size() - 1).getDate(), ahead.getCape().getDate());
		assertNotNull(ahead.getTotal());
		// Two skills top out at 198.
		assertEquals("Total level 198", ahead.getTotal().getTitle());
	}

	@Test
	public void forecastsTheNextKillCountMilestone()
	{
		long start = at(2026, 1, 1, 12);
		Annals annals = begun(start, 80, 2_000_000L);
		annals.killCount("Vorkath", 40, at(2026, 1, 1, 13), UTC);
		annals.killCount("Vorkath", 89, at(2026, 1, 30, 13), UTC);
		annals.tick(at(2026, 1, 30, 14), 600, UTC);
		Ahead ahead = Ahead.of(annals, LocalDate.of(2026, 1, 30), UTC);
		Ahead.Mark vorkath = ahead.getKills().get(0);
		assertEquals("Vorkath 100", vorkath.getTitle());
		// 50 kills over 30 days, 11 to go: 7 days.
		assertEquals(LocalDate.of(2026, 2, 6), vorkath.getDate());
	}

	@Test
	public void aFewDaysIntoTheYearIsTooEarlyForAKillPace()
	{
		// Four weeks of skill pace from last year, then two kills on the first day of the new one.
		Annals annals = fourWeeks(at(2025, 12, 4, 12));
		annals.killCount("Vorkath", 40, at(2025, 12, 31, 13), UTC);
		annals.killCount("Vorkath", 42, at(2026, 1, 1, 13), UTC);
		annals.tick(at(2026, 1, 2, 14), 600, UTC);
		Ahead ahead = Ahead.of(annals, LocalDate.of(2026, 1, 2), UTC);
		assertTrue(ahead.hasPace());
		// One kill a day over two days would put Vorkath 50 a week off: too little to go on.
		assertTrue(ahead.getKills().isEmpty());
		// A week in, the year's kills are a pace.
		annals.killCount("Vorkath", 47, at(2026, 1, 7, 13), UTC);
		assertEquals("Vorkath 50", Ahead.of(annals, LocalDate.of(2026, 1, 7), UTC).getKills().get(0).getTitle());
	}

	@Test
	public void monthEndsAndLeapDaysAreRememberedOnceEach()
	{
		long start = at(2024, 1, 10, 12);
		Annals annals = begun(start, 98, 12_000_000L);
		annals.record(new Entry(Entry.Kind.QUEST, at(2024, 2, 29, 18), "Monkey Madness II", "Quest complete"), at(2024, 2, 29, 18), UTC);
		annals.record(new Entry(Entry.Kind.QUEST, at(2025, 2, 28, 18), "Song of the Elves", "Quest complete"), at(2025, 2, 28, 18), UTC);
		// A leap day comes back round on the last day of February in the years between.
		List<Memories.Memory> feb28 = Memories.of(annals, LocalDate.of(2025, 2, 28), UTC);
		assertEquals(1, feb28.size());
		assertEquals("Monkey Madness II", feb28.get(0).getEntry().getTitle());
		// A month on from 28 February is 28 March only, not the 29th, 30th and 31st as well.
		assertEquals(1, Memories.of(annals, LocalDate.of(2025, 3, 28), UTC).size());
		assertTrue(Memories.of(annals, LocalDate.of(2025, 3, 30), UTC).isEmpty());
		assertTrue(Memories.of(annals, LocalDate.of(2025, 3, 31), UTC).isEmpty());
		// Six months on from 29 February 2024 is 29 August, not the 30th and 31st too.
		assertEquals(1, Memories.of(annals, LocalDate.of(2024, 8, 29), UTC).size());
		assertTrue(Memories.of(annals, LocalDate.of(2024, 8, 31), UTC).isEmpty());
	}

	@Test
	public void remembersThisDayInEarlierYears()
	{
		long start = at(2025, 3, 1, 12);
		Annals annals = begun(start, 98, 12_000_000L);
		annals.stat("Slayer", 13_100_000L, 99, at(2025, 10, 19, 20), UTC, true);
		annals.stat("Mining", 1_100_000L, 71, at(2025, 10, 20, 21), UTC, true);
		annals.record(new Entry(Entry.Kind.QUEST, at(2026, 9, 19, 18), "Dragon Slayer II", "Quest complete"), at(2026, 9, 19, 18), UTC);
		List<Memories.Memory> memories = Memories.of(annals, LocalDate.of(2026, 10, 19), UTC);
		assertEquals(2, memories.size());
		assertEquals("One year ago", memories.get(0).getWhen());
		assertEquals("99 Slayer", memories.get(0).getEntry().getTitle());
		assertEquals("A month ago", memories.get(1).getWhen());
		assertEquals("99 Slayer", Memories.anniversary(memories).getEntry().getTitle());
		assertTrue(Memories.of(annals, LocalDate.of(2026, 10, 18), UTC).isEmpty());
	}

	@Test
	public void aMonthInOneLine()
	{
		List<Entry> month = new ArrayList<>();
		month.add(new Entry(Entry.Kind.MAX, 1, "99 Slayer", ""));
		Entry level = new Entry(Entry.Kind.LEVEL, 2, "Mining 71 to 74", "");
		level.level = 74;
		level.from = 71;
		month.add(level);
		month.add(new Entry(Entry.Kind.COLLECTION, 3, "Dragon warhammer", ""));
		month.add(new Entry(Entry.Kind.QUEST, 4, "Monkey Madness II", ""));
		assertEquals("1 99, 1 log slot, 1 quest", MonthNote.of(month));
		month.remove(0);
		month.remove(1);
		assertNull(MonthNote.of(month));
	}

	@Test
	public void readsTheYearsWorthFromTheLedger()
	{
		List<Wealth.Point> history = new ArrayList<>();
		history.add(new Wealth.Point(at(2025, 12, 31, 20), 500_000_000L));
		history.add(new Wealth.Point(at(2026, 6, 1, 20), 700_000_000L));
		Wealth.Saved saved = new Wealth.Saved(812_000_000L, history);
		Wealth now = Wealth.of(saved, 2026, UTC, true);
		assertEquals(500_000_000L, now.getStart());
		assertEquals(312_000_000L, now.change());
		assertFalse(now.isPartial());
		Wealth last = Wealth.of(saved, 2025, UTC, false);
		assertTrue(last.isPartial());
		assertEquals(0, last.change());
		assertNull(Wealth.of(saved, 2024, UTC, false));
		assertNull(Wealth.of(new Wealth.Saved(), 2026, UTC, true));
		// A ledger last kept in 2025 says nothing of 2026: its total is that old worth, not today's.
		List<Wealth.Point> stopped = new ArrayList<>();
		stopped.add(new Wealth.Point(at(2025, 3, 1, 20), 400_000_000L));
		stopped.add(new Wealth.Point(at(2025, 4, 1, 20), 450_000_000L));
		assertNull(Wealth.of(new Wealth.Saved(450_000_000L, stopped), 2026, UTC, true));
	}

	@Test
	public void paceSurvivesBeingSavedAndRead()
	{
		Annals annals = fourWeeks(at(2026, 3, 1, 12));
		com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();
		Annals read = gson.fromJson(gson.toJson(annals), Annals.class).repair();
		assertEquals(Pace.DAYS + 1, read.pace.days.size());
		Annals old = gson.fromJson("{\"begunAt\":1}", Annals.class).repair();
		assertNotNull(old.pace);
	}
}
