package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

public class SaveTest
{
	private final Gson gson = new GsonBuilder().create();

	@Test
	public void readsASaveFromTheFirstRelease()
	{
		String old = "{\"begunAt\":1762196400000,\"player\":\"Iron Quill\",\"entries\":["
			+ "{\"kind\":\"BEGIN\",\"at\":1762196400000,\"title\":\"The annals begin\",\"detail\":\"Total level 2,000\",\"itemId\":0,\"value\":0,\"level\":0,\"from\":0},"
			+ "{\"kind\":\"LEVEL\",\"at\":1762200000000,\"title\":\"Slayer 91\",\"detail\":\"Level up\",\"skill\":\"Slayer\",\"itemId\":0,\"value\":0,\"level\":91,\"from\":90},"
			+ "{\"kind\":\"PET\",\"at\":1762200500000,\"title\":\"A pet followed you home\",\"detail\":\"While killing Vorkath\",\"itemId\":0,\"value\":0,\"level\":0,\"from\":0},"
			+ "{\"kind\":\"COLLECTION\",\"at\":1762201000000,\"title\":\"Elder maul\",\"detail\":\"New log slot from Chambers of Xeric\",\"itemId\":21003,\"value\":112000000,\"level\":0,\"from\":0}],"
			+ "\"years\":[{\"year\":2025,\"startXp\":{\"Slayer\":5000000},\"endXp\":{\"Slayer\":5400000},\"days\":[20395],\"playedMs\":3600000,\"kills\":{\"Vorkath\":3},\"clues\":0}],"
			+ "\"kc\":{\"Vorkath\":501},\"levels\":{\"Slayer\":91},\"xp\":{\"Slayer\":5400000},\"combat\":118}";
		Annals annals = gson.fromJson(old, Annals.class).repair();
		assertEquals(4, annals.getEntries().size());
		Entry level = annals.getEntries().get(1);
		assertEquals(91, level.getLevel());
		assertEquals(90, level.getFrom());
		Entry pet = annals.getEntries().get(2);
		assertFalse(pet.isSlot());
		Entry maul = annals.getEntries().get(3);
		assertEquals(21003, maul.getItemId());
		assertEquals(112_000_000L, maul.getValue());

		// Written back, the zeros that meant nothing are gone, and it still reads the same.
		String saved = gson.toJson(annals);
		assertFalse(saved.contains("\"value\":0"));
		assertFalse(saved.contains("\"slot\""));
		Annals again = gson.fromJson(saved, Annals.class).repair();
		assertEquals(112_000_000L, again.getEntries().get(3).getValue());
		assertEquals(90, again.getEntries().get(1).getFrom());
		assertTrue(saved.length() < old.length());
	}
}
