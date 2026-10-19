package com.codex.annals.core;

import static com.codex.annals.core.AnnalsTest.UTC;
import static com.codex.annals.core.AnnalsTest.at;
import static com.codex.annals.core.AnnalsTest.begun;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GoalsAndDaysTest
{
	private static final long DAY = 86_400_000L;
	private static final Gson GSON = new Gson();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = GoalsAndDaysTest.class.getResourceAsStream(name))
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; )
			{
				out.write(buf, 0, n);
			}
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	/** Slayer at 50k a day for four weeks, an hour of play a day; Mining never moves. */
	private static Annals fourWeeks(long start)
	{
		Annals annals = begun(start, 80, 2_000_000L);
		long xp = 2_000_000L;
		for (int d = 1; d <= 28; d++)
		{
			long now = start + d * DAY;
			annals.tick(now, 3_600_000L, UTC);
			xp += 50_000;
			annals.stat("Slayer", xp, Experience.level(xp), now + 3_600_000L, UTC, true);
		}
		return annals;
	}

	private static String board(String goals)
	{
		return "{\"version\":1,\"goals\":[" + goals + "]}";
	}

	@Test
	public void readsTheBoardNavigatorWrites() throws IOException
	{
		List<Goals.Goal> goals = Goals.read(GSON, fixture("navigator-goalBoard.json"));
		assertEquals(2, goals.size());
		Goals.Goal ds2 = goals.get(0);
		assertEquals("quest:Dragon Slayer II", ds2.getKey());
		assertEquals("Dragon Slayer II", ds2.getTitle());
		assertFalse(ds2.isComplete());
		assertEquals(109.5, ds2.getHoursLeft(), 1e-9);
		assertEquals(Integer.valueOf(70), ds2.getSkills().get("Smithing"));
		assertEquals(34, ds2.getQuests());
		assertEquals("Start Shield of Arrav", ds2.getNext());
		assertEquals("Varrock Hard Diary", goals.get(1).getTitle());
	}

	@Test
	public void rubbishOrNoBoardIsNoGoals()
	{
		assertTrue(Goals.read(GSON, null).isEmpty());
		assertTrue(Goals.read(GSON, "nope{").isEmpty());
		assertTrue(Goals.read(GSON, "{\"goals\":7}").isEmpty());
		List<Goals.Goal> odd = Goals.read(GSON, board("{\"key\":\"k\",\"title\":\"T\",\"skills\":{\"Slayer\":\"lots\",\"Magic\":200},\"progress\":4},5,{\"title\":\"no key\"}"));
		assertEquals(1, odd.size());
		assertTrue(odd.get(0).getSkills().isEmpty());
		assertEquals(1, odd.get(0).getProgress(), 1e-9);
	}

	@Test
	public void aGoalIsDatedByTheLevelItNeedsAtThatSkillsPace()
	{
		Annals annals = fourWeeks(at(2026, 3, 1, 12));
		LocalDate today = LocalDate.of(2026, 3, 29);
		Ahead ahead = Ahead.of(annals, today, UTC);
		List<Goals.Goal> goals = Goals.read(GSON, board(
			"{\"key\":\"a\",\"title\":\"Slayer goal\",\"hoursLeft\":20,\"skills\":{\"Slayer\":90}},"
				+ "{\"key\":\"b\",\"title\":\"Mining goal\",\"hoursLeft\":5,\"skills\":{\"Mining\":80,\"Slayer\":90}},"
				+ "{\"key\":\"c\",\"title\":\"Quests only\",\"hoursLeft\":10,\"skills\":{},\"quests\":[\"X\"]},"
				+ "{\"key\":\"d\",\"title\":\"Done\",\"complete\":true}"));
		List<Goals.Forecast> f = Goals.forecast(goals, ahead, today, annals.hoursPerDay(today, UTC));
		assertEquals("a finished goal is not dated", 3, f.size());
		// Slayer 90 is 5,346,332; at 3,400,000 and 50k a day that is 39 days.
		assertEquals(today.plusDays(39), f.get(0).getDate());
		assertEquals("Slayer", f.get(0).getLast());
		// Mining is not moving, so the goal has no date however near its Slayer level is.
		assertNull(f.get(1).getDate());
		assertEquals(Collections.singletonList("Mining"), f.get(1).getStalled());
		// No levels: the hours of work at the hours really played, about an hour a day.
		assertEquals(1.0, annals.hoursPerDay(today, UTC), 0.05);
		assertEquals(today.plusDays(10), f.get(2).getDate());
	}

	@Test
	public void aLevelReachedSinceTheBoardWasWrittenNoLongerDatesIt()
	{
		Annals annals = fourWeeks(at(2026, 3, 1, 12));
		LocalDate today = LocalDate.of(2026, 3, 29);
		Ahead ahead = Ahead.of(annals, today, UTC);
		// Slayer is past 80 already; the board still lists it.
		List<Goals.Forecast> f = Goals.forecast(Goals.read(GSON, board("{\"key\":\"a\",\"title\":\"A\",\"hoursLeft\":3,\"skills\":{\"Slayer\":80}}")),
			ahead, today, 1.0);
		assertFalse(f.get(0).hasLevelsLeft());
		assertEquals(today.plusDays(3), f.get(0).getDate());
		// Without any pace yet, nothing is dated by levels.
		List<Goals.Forecast> early = Goals.forecast(Goals.read(GSON, board("{\"key\":\"a\",\"title\":\"A\",\"skills\":{\"Slayer\":90}}")),
			Ahead.of(begun(at(2026, 3, 28, 12), 80, 2_000_000L), today, UTC), today, 0);
		assertNull(early.get(0).getDate());
	}

	@Test
	public void hoursADayCountDaysOff()
	{
		long start = at(2026, 3, 1, 12);
		Annals annals = begun(start, 80, 2_000_000L);
		// Two hours every other day for four weeks: an hour a day.
		for (int d = 1; d <= 28; d += 2)
		{
			annals.tick(start + d * DAY, 2 * 3_600_000L, UTC);
		}
		LocalDate today = LocalDate.of(2026, 3, 29);
		double perDay = annals.hoursPerDay(today, UTC);
		assertTrue(perDay > 0.8 && perDay < 1.2);
		// Less than a week in, nothing is said.
		Annals fresh = begun(at(2026, 3, 27, 12), 80, 2_000_000L);
		fresh.tick(at(2026, 3, 28, 12), 3_600_000L, UTC);
		assertEquals(0, fresh.hoursPerDay(today, UTC), 1e-9);
	}

	@Test
	public void theStreakRunsBackFromTodayOrYesterday()
	{
		long today = LocalDate.of(2026, 3, 29).toEpochDay();
		List<Long> days = Arrays.asList(today - 9, today - 3, today - 2, today - 1);
		assertEquals("today not played yet: the streak still stands", 3, Recap.currentStreak(days, today));
		assertEquals(4, Recap.currentStreak(Arrays.asList(today - 3, today - 2, today - 1, today), today));
		assertEquals("a day missed ends it", 0, Recap.currentStreak(Arrays.asList(today - 5, today - 2), today));
		assertEquals(0, Recap.currentStreak(Collections.emptyList(), today));
	}
}
