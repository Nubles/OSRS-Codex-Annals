package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** The wiki's drop rates are given only to the drops they are for. */
public class DropRatesTest
{
	private static Entry drop(String title, String detail)
	{
		return new Entry(Entry.Kind.DROP, 1, title, detail);
	}

	@Test
	public void aDropIsMatchedByItsItemAndWhereItCameFrom()
	{
		DropRates.Rate r = DropRates.of(drop("Tanzanite fang", "From Zulrah"));
		assertNotNull(r);
		assertEquals("2 × 1/1,024", r.getText());
		assertEquals(2 / 1024.0, r.getChance(), 1e-12);
		assertEquals("1 in 512", r.oneIn());
		// A log slot names its source after "from".
		assertEquals("1/5,000", DropRates.of(new Entry(Entry.Kind.COLLECTION, 1, "Draconic visage", "New log slot from Vorkath")).getText());
		// A stack is the same item.
		assertEquals("1/3,000", DropRates.of(drop("1 x Dragon warhammer", "From Lizardman shaman")).getText());
	}

	@Test
	public void anItemFromSomewhereElseOrNowhereSaidGetsNoRate()
	{
		// Draconic visages also drop elsewhere, at other rates; only Vorkath's is known here.
		assertNull(DropRates.of(drop("Draconic visage", "From Skeletal Wyvern")));
		assertNull(DropRates.of(drop("Draconic visage", "Notable drop")));
		assertNull(DropRates.of(new Entry(Entry.Kind.COLLECTION, 1, "Draconic visage", "New collection log slot")));
		assertNull(DropRates.of(drop("Twisted bow", "From Chambers of Xeric")));
		assertNull(DropRates.of(new Entry(Entry.Kind.QUEST, 1, "Vorki", "From Vorkath")));
		assertNull(DropRates.of(null));
	}

	@Test
	public void aPetIsKnownByItsNameWhateverItsCase()
	{
		Entry pet = new Entry(Entry.Kind.PET, 1, "Pet Snakeling", "Pet from Zulrah");
		DropRates.Rate r = DropRates.of(pet);
		assertEquals("1/4,000", r.getText());
		assertEquals("Zulrah", r.getSource());
		assertEquals("1/2,015.75", DropRates.of(new Entry(Entry.Kind.PET, 1, "Scorpia's offspring", null)).getText());
		assertEquals("1 in 2,016", DropRates.of(new Entry(Entry.Kind.PET, 1, "Scorpia's offspring", null)).oneIn());
		// Named after its follower when the game gave no collection log line.
		assertEquals("1/4,000", DropRates.of(new Entry(Entry.Kind.PET, 1, "Snakeling", "Pet from Zulrah")).getText());
		assertEquals("1/5,000", DropRates.of(new Entry(Entry.Kind.PET, 1, "General Graardor Jr.", null)).getText());
		// A pet whose rate hangs on the mode or a task is left without one.
		assertNull(DropRates.of(new Entry(Entry.Kind.PET, 1, "Youngllef", "Pet from The Gauntlet")));
		assertNull(DropRates.of(new Entry(Entry.Kind.PET, 1, ChatParser.UNNAMED_PET, "Pet from Vorkath")));
	}

	@Test
	public void everyRateIsAChanceAndListedOnce()
	{
		Set<String> seen = new HashSet<>();
		for (DropRates.Rate r : DropRates.all())
		{
			assertTrue(r.getItem(), r.getChance() > 0 && r.getChance() < 1);
			assertTrue(r.getItem(), seen.add(r.getItem().toLowerCase() + "/" + r.getSource().toLowerCase()));
			assertTrue(r.getText(), r.getText().matches("(2 × )?\\d+/[\\d,.]+"));
		}
	}

	@Test
	public void sourcesAreReadFromTheDetail()
	{
		assertEquals("Zulrah", DropRates.source("From Zulrah"));
		assertEquals("Vorkath", DropRates.source("New log slot from Vorkath"));
		assertNull(DropRates.source("Notable drop"));
		assertEquals("Tanzanite fang", DropRates.item("2 x Tanzanite fang"));
		assertEquals("Ring of 3 x stuff", DropRates.item("Ring of 3 x stuff"));
	}
}
