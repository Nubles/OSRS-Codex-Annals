package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import com.codex.annals.core.Entry;
import com.codex.annals.core.Pets;
import org.junit.Test;

public class PicturesTest
{
	@Test
	public void picturesPetsQuestsAndDiaries()
	{
		assertEquals(21992, Views.item(new Entry(Entry.Kind.PET, 1, "Vorki", "Pet from Vorkath")));
		// The follower's name can come in another case than the item's.
		assertEquals(12921, Pets.item("Pet snakeling"));
		assertEquals(0, Views.item(new Entry(Entry.Kind.PET, 1, "A pet", "The rarest drop of all")));
		assertEquals(Views.QUEST_ITEM, Views.item(new Entry(Entry.Kind.QUEST, 1, "Desert Treasure II", "Quest complete")));
		assertEquals(Views.DIARY_ITEM, Views.item(new Entry(Entry.Kind.DIARY, 1, "Hard Kandarin Diary", "Achievement diary")));
		assertEquals(0, Views.item(new Entry(Entry.Kind.BOSS, 1, "Zulrah 100", "Kill count milestone")));
	}

	@Test
	public void tellsAnOld99ByItsPlace()
	{
		assertEquals("Your 7th 99", EntryRow.detail(new Entry(Entry.Kind.MAX, 1, "99 Slayer", "99 number 7")));
		assertEquals("Your 22nd 99", EntryRow.detail(new Entry(Entry.Kind.MAX, 1, "99 Hunter", "99 number 22")));
		assertEquals("Your first 99", EntryRow.detail(new Entry(Entry.Kind.MAX, 1, "99 Hunter", "Your first 99")));
	}
}
