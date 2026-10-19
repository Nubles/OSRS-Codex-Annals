package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import com.codex.annals.core.Entry;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SearchTest
{
	@Test
	public void findsByNameDetailAndCaseFree()
	{
		Entry pet = new Entry(Entry.Kind.PET, 1, "Vorki", "Pet from Vorkath");
		Entry kc = new Entry(Entry.Kind.BOSS, 2, "Vorkath 500", "Kill count milestone");
		Entry quest = new Entry(Entry.Kind.QUEST, 3, "Dragon Slayer II", "Quest complete");
		Entry noDetail = new Entry(Entry.Kind.DROP, 4, "Dragon warhammer", null);
		List<Entry> all = Arrays.asList(pet, kc, quest, noDetail);
		assertEquals(Arrays.asList(pet, kc), TimelineView.matching(all, "vorkath"));
		assertEquals(Arrays.asList(quest, noDetail), TimelineView.matching(all, "dragon"));
		assertEquals(Arrays.asList(quest), TimelineView.matching(all, "quest complete"));
		assertEquals(0, TimelineView.matching(all, "zulrah").size());
	}
}
