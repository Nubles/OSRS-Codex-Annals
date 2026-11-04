package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Gielinor Wrapped: when it opens, which cards a year is dealt, and the figures on them. */
public class WrappedTest
{
	private static final java.time.ZoneId UTC = AnnalsTest.UTC;

	@Test
	public void opensOnTheFirstOfDecemberAndAnyTimeForYearsGoneBy()
	{
		assertFalse(Wrapped.isOpen(2026, LocalDate.of(2026, 11, 30)));
		assertTrue(Wrapped.isOpen(2026, LocalDate.of(2026, 12, 1)));
		assertTrue(Wrapped.isOpen(2026, LocalDate.of(2026, 12, 31)));
		assertTrue("a year gone by opens any day", Wrapped.isOpen(2025, LocalDate.of(2026, 3, 4)));
		assertFalse("a year not yet begun is not open", Wrapped.isOpen(2027, LocalDate.of(2026, 12, 15)));
		assertEquals(LocalDate.of(2026, 12, 1), Wrapped.opensOn(2026));
	}

	@Test
	public void aYearWithNothingButTimeGetsOnlyTheOpeningCard()
	{
		long now = AnnalsTest.at(2026, 3, 12, 10);
		Annals annals = AnnalsTest.begun(now, 80, 2_000_000L);
		annals.tick(now, 600, UTC);
		Wrapped w = Wrapped.of(annals, 2026, UTC);
		assertEquals(Collections.singletonList(Wrapped.Card.OPENING), w.getCards());
		assertNull("no year, no story", Wrapped.of(annals, 2019, UTC));
		assertNull(Wrapped.of(null, 2026, UTC));
	}

	@Test
	public void aFullYearIsDealtEveryCardInOrder()
	{
		Annals annals = fullYear();
		Wrapped w = Wrapped.of(annals, 2026, UTC);
		assertEquals(Arrays.asList(Wrapped.Card.OPENING, Wrapped.Card.SKILL, Wrapped.Card.DROP, Wrapped.Card.DAYS,
			Wrapped.Card.ARCHETYPE, Wrapped.Card.MONTHS, Wrapped.Card.CLOSING), w.getCards());
		assertEquals("Gielinor Wrapped 2026", w.fileName());
	}

	@Test
	public void theRarestDropIsTheOneWithTheSmallestWikiChanceNotTheDearest()
	{
		Annals annals = fullYear();
		Wrapped w = Wrapped.of(annals, 2026, UTC);
		// The warhammer is worth more, but the visage is 1/5,000 against its 1/3,000.
		assertEquals("Draconic visage", w.getDrop().getTitle());
		assertEquals("1/5,000", w.getRate().getText());
		assertEquals("1 in 5,000", w.getRate().oneIn());
	}

	@Test
	public void withoutAWikiRateAPetComesBeforeTheDearestDrop()
	{
		long t = AnnalsTest.at(2026, 2, 1, 10);
		Annals annals = AnnalsTest.begun(t, 80, 2_000_000L);
		annals.tick(t, 600, UTC);
		annals.drop("Dragon claws", 1, 13652, 50_000_000L, "Chambers of Xeric", t + 1000);
		Wrapped w = Wrapped.of(annals, 2026, UTC);
		assertEquals("Dragon claws", w.getDrop().getTitle());
		assertNull("no rate is guessed", w.getRate());
		// A pet the table has no single rate for (TzRek-Jad's depends on the task).
		annals.record(new Entry(Entry.Kind.PET, t + 5000, "Tzrek-jad", "Pet from TzTok-Jad"), t + 5000, UTC);
		w = Wrapped.of(annals, 2026, UTC);
		assertEquals("Tzrek-jad", w.getDrop().getTitle());
		assertNull(w.getRate());
	}

	@Test
	public void theBiggestClimbIsOneSkillOnOneDay()
	{
		long day = AnnalsTest.at(2026, 6, 14, 9);
		Annals annals = AnnalsTest.begun(day, 60, 273_742L);
		annals.tick(day, 600, UTC);
		// Mining 70 to 73 in three levels on the 14th; Slayer 60 to 64 that same day across two sittings.
		annals.stat("Mining", 1_100_000L, 71, day + 1_000, UTC, true);
		annals.stat("Mining", 1_200_000L, 72, day + 2_000, UTC, true);
		annals.stat("Mining", 1_300_000L, 73, day + 3_000, UTC, true);
		annals.stat("Slayer", 400_000L, 62, day + 4_000, UTC, true);
		annals.stat("Slayer", 600_000L, 64, day + 3_600_000L * 8, UTC, true);
		// And Slayer 64 to 66 the next day: a different day, so not part of the climb.
		annals.stat("Slayer", 700_000L, 66, day + 86_400_000L, UTC, true);
		Wrapped.Climb c = Wrapped.of(annals, 2026, UTC).getClimb();
		assertEquals("Slayer", c.getSkill());
		assertEquals(60, c.getFrom());
		assertEquals(64, c.getTo());
		assertEquals(4, c.levels());
		assertEquals(LocalDate.of(2026, 6, 14), c.getDay());
	}

	@Test
	public void aClimbIsReadFromTheLevelsEvenWhenOnlyEveryTenthIsKept()
	{
		long day = AnnalsTest.at(2026, 6, 14, 9);
		Entry level = new Entry(Entry.Kind.LEVEL, day, "Agility 62 to 70", "Level up").skill("Agility", 70);
		level.from = 62;
		Entry max = new Entry(Entry.Kind.MAX, day + 1000, "99 Cooking", "Your first 99").skill("Cooking", 99);
		Wrapped.Climb c = Wrapped.climb(Arrays.asList(level, max), UTC);
		assertEquals("Agility", c.getSkill());
		assertEquals(8, c.levels());
		// A lone 99 came from 98.
		c = Wrapped.climb(Collections.singletonList(max), UTC);
		assertEquals(98, c.getFrom());
		assertNull(Wrapped.climb(Collections.emptyList(), UTC));
	}

	@Test
	public void theBusiestDayHasTheMostWrittenDownAndTheMoreTellingOnATie()
	{
		long a = AnnalsTest.at(2026, 4, 1, 10);
		long b = AnnalsTest.at(2026, 4, 2, 10);
		List<Entry> entries = Arrays.asList(
			new Entry(Entry.Kind.LEVEL, a, "Mining 71", "Level up").skill("Mining", 71),
			new Entry(Entry.Kind.LEVEL, a + 1, "Mining 72", "Level up").skill("Mining", 72),
			new Entry(Entry.Kind.PET, b, "Vorki", "Pet from Vorkath"),
			new Entry(Entry.Kind.QUEST, b + 1, "Dragon Slayer II", "Quest complete"),
			new Entry(Entry.Kind.LEVEL, AnnalsTest.at(2026, 4, 3, 10), "Mining 73", "Level up"));
		Wrapped.Day d = Wrapped.busiest(entries, UTC);
		assertEquals(LocalDate.of(2026, 4, 2), d.getDate());
		assertEquals(2, d.getCount());
		assertEquals("Vorki", d.getBest().get(0).getTitle());
		assertNull("one entry a day makes no busiest day", Wrapped.busiest(entries.subList(4, 5), UTC));
	}

	@Test
	public void theLongestStreakStartsWhereItsRunBegan()
	{
		long jan1 = LocalDate.of(2026, 1, 1).toEpochDay();
		List<Long> days = Arrays.asList(jan1, jan1 + 1, jan1 + 5, jan1 + 6, jan1 + 7, jan1 + 9);
		assertEquals(LocalDate.of(2026, 1, 6), Wrapped.streakStart(days, Recap.streak(days)));
		assertNull(Wrapped.streakStart(Collections.emptyList(), 0));
	}

	@Test
	public void monthsAreReadFromTheWeeksByTheirThursday()
	{
		Annals annals = fullYear();
		Wrapped w = Wrapped.of(annals, 2026, UTC);
		long[] xp = w.getMonthXp();
		long sum = 0;
		for (long v : xp)
		{
			sum += v;
		}
		assertTrue(sum > 0);
		assertEquals("the most experience went in in August", 7, w.bestMonth());
		// The week of Monday 29 December 2025 has its Thursday on 1 January: it is 2026's.
		Week straddling = new Week(LocalDate.of(2025, 12, 29).toEpochDay());
		assertEquals(2026, straddling.getMonday().with(java.time.DayOfWeek.THURSDAY).getYear());
	}

	@Test
	public void theArchetypeComesFromTheYear()
	{
		Wrapped w = Wrapped.of(fullYear(), 2026, UTC);
		assertSame(Archetype.THE_SLAYER, w.getArchetype());
		double[] shares = w.getShares();
		assertEquals(1, shares[0] + shares[1] + shares[2], 1e-9);
		assertTrue(w.getArchetypeReason().endsWith("of your experience was Slayer"));
	}

	/** A year of Slayer and Mining, with a pet-less run of drops, levels, quests, a streak and the months between. */
	static Annals fullYear()
	{
		long start = AnnalsTest.at(2026, 1, 5, 18);
		Annals annals = AnnalsTest.begun(start, 80, 2_000_000L);
		long slayer = 2_000_000L;
		long mining = 1_000_000L;
		int slayerLevel = 80;
		for (int d = 0; d < 300; d += d < 40 ? 1 : 3)
		{
			long t = start + d * 86_400_000L;
			annals.tick(t, 600, UTC);
			// August is the busy month: three times the Slayer.
			LocalDate date = LocalDate.ofEpochDay(t / 86_400_000L);
			slayer += date.getMonthValue() == 8 ? 300_000 : 30_000;
			mining += 4_000;
			int level = Math.min(99, 80 + (int) ((slayer - 2_000_000L) / 800_000L));
			annals.stat("Slayer", slayer, level, t + 1000, UTC, true);
			annals.stat("Mining", mining, 70, t + 2000, UTC, true);
			slayerLevel = level;
		}
		long t = AnnalsTest.at(2026, 7, 1, 12);
		annals.drop("Dragon warhammer", 1, 13576, 38_000_000L, "Lizardman shaman", t);
		annals.drop("Draconic visage", 1, 11286, 3_000_000L, "Vorkath", t + 1000);
		annals.record(new Entry(Entry.Kind.QUEST, t + 2000, "Dragon Slayer II", "Quest complete"), t + 2000, UTC);
		annals.killCount("Vorkath", 1, t + 3000, UTC);
		assertNotNull(annals.getYear(2026));
		assertTrue(slayerLevel > 80);
		return annals;
	}
}
