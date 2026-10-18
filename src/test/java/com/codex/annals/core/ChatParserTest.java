package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ChatParserTest
{
	@Test
	public void readsKillCounts()
	{
		ChatParser.KillCount kc = ChatParser.killCount("Your Vorkath kill count is: 1,412.");
		assertEquals("Vorkath", kc.getName());
		assertEquals(1412, kc.getCount());

		kc = ChatParser.killCount("Your completed Theatre of Blood count is: 25.");
		assertEquals("Theatre of Blood", kc.getName());

		kc = ChatParser.killCount("Your Barrows chest count is: 300.");
		assertEquals("Barrows chest", kc.getName());

		assertNull(ChatParser.killCount("You have a funny feeling like you're being followed."));
	}

	@Test
	public void agilityLapsAreNotKills()
	{
		assertNull(ChatParser.killCount("Your Ardougne Rooftop lap count is: 512."));
		assertNull(ChatParser.killCount("Your Prifddinas Agility Course lap count is: 1,024."));
		assertEquals("Gauntlet", ChatParser.killCount("Your Gauntlet completion count is: 12.").getName());
		assertEquals("Wintertodt", ChatParser.killCount("Your subdued Wintertodt count is: 300.").getName());
	}

	@Test
	public void readsEntries()
	{
		Entry e = ChatParser.entry("New item added to your collection log: Draconic visage", 1, null);
		assertEquals(Entry.Kind.COLLECTION, e.getKind());
		assertEquals("Draconic visage", e.getTitle());

		e = ChatParser.entry("You have a funny feeling like you're being followed.", 1, "Vorkath");
		assertEquals(Entry.Kind.PET, e.getKind());
		assertEquals("Pet from Vorkath", e.getDetail());

		e = ChatParser.entry("Congratulations, you've completed a quest: Dragon Slayer II", 1, null);
		assertEquals(Entry.Kind.QUEST, e.getKind());
		assertEquals("Dragon Slayer II", e.getTitle());

		e = ChatParser.entry("Congratulations! You have completed all of the hard tasks in the Varrock area. Speak to Toby to claim your reward.", 1, null);
		assertEquals(Entry.Kind.DIARY, e.getKind());
		assertEquals("Varrock hard diary", e.getTitle());

		e = ChatParser.entry("Congratulations, you've completed an elite combat task: Faithless Crypt Run (5 points).", 1, null);
		assertEquals(Entry.Kind.TASK, e.getKind());
		assertEquals("Faithless Crypt Run", e.getTitle());
		assertEquals("Elite combat task", e.getDetail());

		e = ChatParser.entry("You have completed 100 medium Treasure Trails.", 1, null);
		assertEquals(Entry.Kind.CLUE, e.getKind());
		assertEquals("Medium clue number 100", e.getTitle());

		assertNull(ChatParser.entry("Welcome to Old School RuneScape.", 1, null));
	}

	@Test
	public void clueMilestones()
	{
		assertTrue(ChatParser.clueMilestone(1));
		assertTrue(ChatParser.clueMilestone(25));
		assertTrue(ChatParser.clueMilestone(300));
		assertFalse(ChatParser.clueMilestone(12));
		assertFalse(ChatParser.clueMilestone(150));
	}
}
