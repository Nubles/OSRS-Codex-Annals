package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

/** What kind of player a year made you, from where its time and experience went. */
public class ArchetypeTest
{
	private static final long HOUR = 3_600_000L;

	private static Archetype of(long craft, long combat, long slayer, long hours, int kills, int slots, int pets, int quests, int diaries, int clues)
	{
		return Archetype.of(Archetype.Figures.of(craft, combat, slayer, hours * HOUR, kills, slots, pets, quests, diaries, clues));
	}

	@Test
	public void aYearOfTheCraftIsTheSkillers()
	{
		assertSame(Archetype.THE_SKILLER, of(9_000_000, 1_000_000, 0, 300, 0, 0, 0, 0, 0, 0));
	}

	@Test
	public void aYearOfCombatIsTheWarriors()
	{
		assertSame(Archetype.THE_WARRIOR, of(1_000_000, 9_000_000, 0, 300, 100, 0, 0, 0, 0, 0));
	}

	@Test
	public void slayerOverAFifteenthOfTheExperienceIsTheSlayers()
	{
		// 30% Slayer is twice its line; 60% combat is just under the warrior's.
		assertSame(Archetype.THE_SLAYER, of(1_000_000, 6_000_000, 3_000_000, 300, 100, 0, 0, 0, 0, 0));
		// 10% is not enough: the combat carries the year.
		assertSame(Archetype.THE_WARRIOR, of(500_000, 8_500_000, 1_000_000, 300, 100, 0, 0, 0, 0, 0));
	}

	@Test
	public void killsAnHourPlayedMakeTheBossHunter()
	{
		// 3,000 kills in 300 hours is 10 an hour, twice the line.
		assertSame(Archetype.THE_BOSS_HUNTER, of(1_000_000, 9_000_000, 0, 300, 3_000, 0, 0, 0, 0, 0));
		// The same kills spread over 1,000 hours is three an hour: a warrior's year.
		assertSame(Archetype.THE_WARRIOR, of(1_000_000, 9_000_000, 0, 1_000, 3_000, 0, 0, 0, 0, 0));
		// Under an hour played tells nothing of kills an hour.
		assertEquals(0, Archetype.THE_BOSS_HUNTER.score(Archetype.Figures.of(0, 0, 0, HOUR / 2, 50, 0, 0, 0, 0, 0)), 0);
	}

	@Test
	public void slotsAndPetsMakeTheCollectorAndPetsCountForMore()
	{
		assertSame(Archetype.THE_COLLECTOR, of(5_000_000, 5_000_000, 0, 300, 100, 40, 0, 0, 0, 0));
		// 21 slots and 3 pets: 21 + 9 = 30, just on the line, and 60% craft is short of the skiller's.
		assertSame(Archetype.THE_COLLECTOR, of(6_000_000, 4_000_000, 0, 300, 100, 21, 3, 0, 0, 0));
		// One slot fewer reaches no line: the craft had the most experience.
		assertSame(Archetype.THE_SKILLER, of(6_000_000, 4_000_000, 0, 300, 100, 20, 3, 0, 0, 0));
	}

	@Test
	public void questsDiariesAndCluesHaveTheirOwnKinds()
	{
		assertSame(Archetype.THE_ADVENTURER, of(4_000_000, 4_000_000, 0, 300, 0, 0, 0, 30, 6, 0));
		assertSame(Archetype.THE_TREASURE_HUNTER, of(4_000_000, 4_000_000, 0, 300, 0, 0, 0, 0, 0, 200));
	}

	@Test
	public void theFurthestPastItsLineWinsAndTheRarerKindOnATie()
	{
		// Collector 60/30 = 2.0 against Treasure Hunter 150/75 = 2.0: the collector is listed first.
		assertSame(Archetype.THE_COLLECTOR, of(9_000_000, 1_000_000, 0, 300, 0, 60, 0, 0, 0, 150));
		// Treasure Hunter 300/75 = 4.0 beats the collector's 2.0.
		assertSame(Archetype.THE_TREASURE_HUNTER, of(9_000_000, 1_000_000, 0, 300, 0, 60, 0, 0, 0, 300));
	}

	@Test
	public void aYearReachingNoLineIsToldByWhereMostExperienceWent()
	{
		// 55% craft and 45% combat: neither line, the craft has more.
		assertSame(Archetype.THE_SKILLER, of(5_500_000, 4_500_000, 0, 300, 0, 0, 0, 0, 0, 0));
		assertSame(Archetype.THE_WARRIOR, of(4_000_000, 5_000_000, 1_000_000, 300, 0, 0, 0, 0, 0, 0));
		assertSame(Archetype.THE_WANDERER, of(0, 0, 0, 3, 0, 0, 0, 0, 0, 0));
	}

	@Test
	public void theReasonSaysWhy()
	{
		Archetype.Figures f = Archetype.Figures.of(7_100_000, 2_900_000, 0, 300 * HOUR, 0, 0, 0, 0, 0, 0);
		assertEquals("71% of your experience went to skills of the craft", Archetype.THE_SKILLER.reason(f));
		f = Archetype.Figures.of(0, 0, 0, 0, 0, 12, 2, 0, 0, 0);
		assertEquals("12 log slots and 2 pets this year", Archetype.THE_COLLECTOR.reason(f));
		f = Archetype.Figures.of(0, 0, 0, 300 * HOUR, 3_000, 0, 0, 0, 0, 0);
		assertEquals("10.0 kills for every hour played", Archetype.THE_BOSS_HUNTER.reason(f));
	}

	@Test
	public void everySkillOfTheGameIsSortedSailingAmongThem()
	{
		// RuneLite's own list of skills: each one is combat, Slayer or a skill of the craft, and Sailing is the craft's.
		for (Skill skill : Skill.values())
		{
			String name = skill.getName();
			int places = (Archetype.COMBAT.contains(name) ? 1 : 0) + (Archetype.SLAYER.equals(name) ? 1 : 0) + (Archetype.SKILLING.contains(name) ? 1 : 0);
			assertEquals(name, 1, places);
		}
		assertTrue(Archetype.SKILLING.contains(Skill.SAILING.getName()));
		assertEquals("Sailing", Skill.SAILING.getName());
	}
}
