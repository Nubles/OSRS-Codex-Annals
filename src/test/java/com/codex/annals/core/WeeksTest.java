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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Week by week: what is kept, the chapters found in it and the look back at last week. */
public class WeeksTest
{
	private static final Gson GSON = new Gson();

	/** A week starting on the Monday of a date, with the experience and kills given in pairs. */
	private static Week week(LocalDate day, long playedMs, Object... pairs)
	{
		Week w = new Week(Week.mondayOf(day));
		w.playedMs = playedMs;
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			if (pairs[i + 1] instanceof Integer)
			{
				w.kills.put((String) pairs[i], (Integer) pairs[i + 1]);
			}
			else
			{
				w.xp.put((String) pairs[i], (Long) pairs[i + 1]);
			}
		}
		return w;
	}

	@Test
	public void experienceKillsAndTimeGoOnTheirWeek()
	{
		// Thursday 12 March 2026; the week began on Monday 9 March.
		long now = at(2026, 3, 12, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.stat("Slayer", 2_050_000L, 80, now + 60_000, UTC, true);
		annals.killCount("Vorkath", 10, now + 120_000, UTC);
		annals.killCount("Vorkath", 11, now + 240_000, UTC);
		annals.tick(now + 300_000, 600, UTC);
		// The next Monday is a new week.
		annals.stat("Slayer", 2_060_000L, 80, at(2026, 3, 16, 9), UTC, true);
		List<Week> weeks = annals.getWeeks();
		assertEquals(2, weeks.size());
		assertEquals(LocalDate.of(2026, 3, 9), weeks.get(0).getMonday());
		assertEquals(50_000L, (long) weeks.get(0).getXp().get("Slayer"));
		// The first count seen is one kill; the next adds one.
		assertEquals(2, (int) weeks.get(0).getKills().get("Vorkath"));
		assertEquals(600, weeks.get(0).getPlayedMs());
		assertEquals(10_000L, (long) weeks.get(1).getXp().get("Slayer"));
	}

	@Test
	public void experienceTakenAwayOrReadAsNothingIsNoGain()
	{
		long now = at(2026, 3, 12, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.stat("Slayer", 0L, 1, now + 1_000, UTC, true);
		annals.stat("Slayer", 1_900_000L, 79, now + 2_000, UTC, true);
		assertTrue(annals.getWeeks().isEmpty() || annals.getWeeks().get(0).getXp().isEmpty());
	}

	@Test
	public void anOlderSaveIsFilledFromTheDailyReadings()
	{
		long now = at(2026, 3, 2, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		// Four days of readings a day apart, crossing from one week into the next.
		for (int d = 0; d < 9; d++)
		{
			Map<String, Long> xp = new LinkedHashMap<>();
			xp.put("Slayer", 2_000_000L + d * 100_000L);
			xp.put("Mining", 1_000_000L);
			annals.pace.note(LocalDate.of(2026, 3, 2).plusDays(d).toEpochDay(), xp);
		}
		String json = GSON.toJson(annals).replaceAll(",\"weeks\":\\[[^\\]]*\\]", "");
		Annals read = GSON.fromJson(json, Annals.class).repair();
		List<Week> weeks = read.getWeeks();
		assertEquals(2, weeks.size());
		// Seven days of gains from 2 March (Monday to Sunday), one from 9 March.
		assertEquals(700_000L, (long) weeks.get(0).getXp().get("Slayer"));
		assertEquals(100_000L, (long) weeks.get(1).getXp().get("Slayer"));
		assertNull(weeks.get(0).getXp().get("Mining"));
		// A save that already has weeks is never filled again.
		Annals again = GSON.fromJson(GSON.toJson(read), Annals.class).repair();
		assertEquals(700_000L, (long) again.getWeeks().get(0).getXp().get("Slayer"));
	}

	@Test
	public void aBreakAcrossWeeksIsNotPutOnTheWeekBefore()
	{
		long now = at(2026, 3, 2, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		long[][] readings = {
			// Monday, then Thursday of the same week: the gain between is that week's.
			{LocalDate.of(2026, 3, 2).toEpochDay(), 2_000_000L},
			{LocalDate.of(2026, 3, 5).toEpochDay(), 2_300_000L},
			// Then nothing for two weeks: whatever was done meanwhile (on mobile, say) has no week to go on.
			{LocalDate.of(2026, 3, 18).toEpochDay(), 3_300_000L},
			{LocalDate.of(2026, 3, 19).toEpochDay(), 3_350_000L}};
		for (long[] r : readings)
		{
			Map<String, Long> xp = new LinkedHashMap<>();
			xp.put("Slayer", r[1]);
			annals.pace.note(r[0], xp);
		}
		String json = GSON.toJson(annals).replaceAll(",\"weeks\":\\[[^\\]]*\\]", "");
		List<Week> weeks = GSON.fromJson(json, Annals.class).repair().getWeeks();
		assertEquals(2, weeks.size());
		assertEquals(LocalDate.of(2026, 3, 2), weeks.get(0).getMonday());
		assertEquals(300_000L, (long) weeks.get(0).getXp().get("Slayer"));
		assertEquals(50_000L, (long) weeks.get(1).getXp().get("Slayer"));
	}

	@Test
	public void weeksAreCapped()
	{
		Annals annals = begun(at(2020, 1, 6, 10), 80, 2_000_000L);
		for (int i = 0; i < Annals.MAX_WEEKS + 10; i++)
		{
			annals.tick(at(2020, 1, 6, 10) + i * 7L * 86_400_000L, 600, UTC);
		}
		assertEquals(Annals.MAX_WEEKS, annals.getWeeks().size());
	}

	@Test
	public void aWeekIsGivenItsFocus()
	{
		LocalDate d = LocalDate.of(2026, 5, 4);
		assertEquals("Agility", Chapters.focus(week(d, 0, "Agility", 400_000L, "Hitpoints", 900_000L, "Cooking", 100_000L)));
		// Combat experience with plenty of one boss is that boss's week.
		assertEquals("boss:Vorkath", Chapters.focus(week(d, 0, "Ranged", 900_000L, "Hitpoints", 300_000L, "Vorkath", 60)));
		// Combat with Slayer keeping pace is a Slayer week.
		assertEquals("Slayer", Chapters.focus(week(d, 0, "Strength", 600_000L, "Slayer", 150_000L, "Hitpoints", 200_000L)));
		// Nothing standing out, or too little, is no focus.
		assertNull(Chapters.focus(week(d, 0, "Mining", 30_000L)));
		assertNull(Chapters.focus(week(d, 0, "Mining", 100_000L, "Fishing", 90_000L, "Woodcutting", 80_000L)));
		// A non-combat skill wins over a few kills on the side.
		assertEquals("Runecraft", Chapters.focus(week(d, 0, "Runecraft", 500_000L, "Zulrah", 25)));
	}

	@Test
	public void chaptersBridgeOneWeekAwayAndAreNamedForTheirLength()
	{
		LocalDate start = LocalDate.of(2026, 6, 1);
		java.util.List<Week> weeks = new java.util.ArrayList<>();
		// Six weeks: Slayer, Slayer, a week of Agility, Slayer, nothing played, Slayer.
		weeks.add(week(start, 0, "Strength", 600_000L, "Slayer", 150_000L));
		weeks.add(week(start.plusWeeks(1), 0, "Strength", 600_000L, "Slayer", 150_000L));
		weeks.add(week(start.plusWeeks(2), 0, "Agility", 300_000L));
		weeks.add(week(start.plusWeeks(3), 0, "Strength", 600_000L, "Slayer", 150_000L));
		weeks.add(week(start.plusWeeks(5), 0, "Strength", 600_000L, "Slayer", 150_000L));
		// Then two weeks of Zulrah in the autumn, and a lone week of Fishing.
		weeks.add(week(LocalDate.of(2026, 9, 7), 0, "Magic", 500_000L, "Zulrah", 80));
		weeks.add(week(LocalDate.of(2026, 9, 14), 0, "Magic", 500_000L, "Zulrah", 70));
		weeks.add(week(LocalDate.of(2026, 10, 5), 0, "Fishing", 300_000L));
		List<Chapters.Chapter> chapters = Chapters.of(weeks);
		assertEquals(2, chapters.size());
		Chapters.Chapter slayer = chapters.get(0);
		assertEquals("Slayer", slayer.getFocus());
		assertEquals(6, slayer.getWeeks());
		assertEquals(LocalDate.of(2026, 6, 1), slayer.getStart());
		assertEquals(LocalDate.of(2026, 7, 12), slayer.getEnd());
		assertEquals(600_000L, slayer.getXp());
		assertEquals("The Slayer summer", slayer.title());
		Chapters.Chapter zulrah = chapters.get(1);
		assertTrue(zulrah.isBoss());
		assertEquals(150, zulrah.getKills());
		assertEquals("A fortnight of Zulrah", zulrah.title());
		assertTrue(zulrah.in(2026));
	}

	@Test
	public void lastWeekIsToldWithItsLevelsAndAgainstTheUsual()
	{
		long now = at(2026, 3, 2, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		// Three weeks of 100k Slayer, then one of 400k that crosses a level, then today in the week after.
		long slayer = 2_000_000L;
		for (int w = 0; w < 4; w++)
		{
			long gain = w == 3 ? 400_000L : 100_000L;
			slayer += gain;
			annals.stat("Slayer", slayer, Experience.level(slayer), now + w * 7L * 86_400_000L + 3_600_000L, UTC, true);
			annals.tick(now + w * 7L * 86_400_000L + 3_600_000L, 3 * 3_600_000L, UTC);
		}
		annals.killCount("Vorkath", 5, now + 21L * 86_400_000L + 4_000_000L, UTC);
		annals.killCount("Vorkath", 9, now + 21L * 86_400_000L + 5_000_000L, UTC);
		WeekNote note = WeekNote.lastWeek(annals, LocalDate.of(2026, 4, 1), UTC);
		assertNotNull(note);
		assertEquals(LocalDate.of(2026, 3, 23), note.getMonday());
		assertEquals("Slayer", note.getSkills().get(0));
		assertEquals(Experience.level(2_700_000L) - Experience.level(2_300_000L), note.getLevels());
		assertEquals(100_000L, note.getUsualXp());
		assertEquals(3.0, note.againstUsual(), 1e-9);
		assertEquals("Vorkath", note.getBoss());
		assertEquals(5, note.getBossKills());
		assertEquals("3h 0m played, 400k xp (most in Slayer), 5 Vorkath kills, " + note.getLevels() + " levels.", note.line());
	}

	@Test
	public void aQuietWeekHasNoLookBack()
	{
		long now = at(2026, 3, 2, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.tick(now, 60_000, UTC);
		assertNull(WeekNote.lastWeek(annals, LocalDate.of(2026, 3, 10), UTC));
		// A week never kept is none either.
		assertNull(WeekNote.lastWeek(annals, LocalDate.of(2026, 4, 10), UTC));
	}

	@Test
	public void levelsLastWeekAreNotMovedByExperienceGainedAway()
	{
		// 100k Slayer in RuneLite last week, staying level 80; then 900k more on another client before logging in here.
		long now = at(2026, 3, 2, 10);
		Annals annals = begun(now, 80, 2_000_000L);
		annals.stat("Slayer", 2_100_000L, 80, at(2026, 3, 3, 10), UTC, true);
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> levels = new LinkedHashMap<>();
		xp.put("Slayer", 3_000_000L);
		levels.put("Slayer", 84);
		annals.begin(at(2026, 3, 9, 10), UTC, "Zezima", null, xp, levels, 100);
		WeekNote note = WeekNote.lastWeek(annals, LocalDate.of(2026, 3, 9), UTC);
		assertNotNull(note);
		assertEquals(100_000L, note.getWeek().totalXp());
		// 2.0M to 2.1M is no level, whatever was gained since.
		assertEquals(0, note.getLevels());
		// The end of the week survives a save.
		Annals read = GSON.fromJson(GSON.toJson(annals), Annals.class).repair();
		assertEquals(0, WeekNote.lastWeek(read, LocalDate.of(2026, 3, 9), UTC).getLevels());
	}
}
