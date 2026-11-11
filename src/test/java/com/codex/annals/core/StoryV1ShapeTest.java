package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Pins the shape of the annals' own story, {@code codexannals/storyV1} (the suite's N1
 * contract), as Codex Home and the others read it: a change here is a change to a contract.
 */
public class StoryV1ShapeTest
{
	private static final Gson GSON = new Gson();
	private static final ZoneId Z = ZoneOffset.UTC;
	private static final long T = 1_793_000_000_000L;

	static Annals annals()
	{
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> lv = new LinkedHashMap<>();
		xp.put("Ranged", 12_000_000L);
		lv.put("Ranged", 98);
		xp.put("Slayer", 1_000_000L);
		lv.put("Slayer", 69);
		Annals a = new Annals();
		a.begin(T, Z, "Iron Quill", new Baseline(T, 167, 13_000_000L, 90, 50, 40, 160, Arrays.asList()), xp, lv, 90);
		a.stat("Ranged", 13_034_431L, 99, T + 60_000, Z, true);
		a.stat("Slayer", 1_100_000L, 70, T + 120_000, Z, true);
		a.killCount("Vorkath", 1, T + 180_000, Z);
		a.drop("Dragonbone necklace", 1, 22111, 12_500_000L, "Vorkath", T + 240_000);
		return a;
	}

	@Test
	public void writesTheContractShape()
	{
		String json = OwnStory.json(GSON, annals());
		assertEquals("{\"version\":1,\"savedAt\":1793000240000,\"moments\":["
			+ "{\"t\":1793000060000,\"kind\":\"level\",\"text\":\"99 Ranged · Your first 99\",\"ref\":\"max:1793000060000\"},"
			+ "{\"t\":1793000180000,\"kind\":\"milestone\",\"text\":\"First Vorkath · Kill count 1\",\"figure\":\"kc 1\",\"ref\":\"boss:1793000180000\"},"
			+ "{\"t\":1793000240000,\"kind\":\"drop\",\"text\":\"Dragonbone necklace · From Vorkath\",\"figure\":\"12.5M\",\"ref\":\"drop:1793000240000\"}]}", json);
		// Read back through the suite's own reader, as Home would.
		assertEquals(3, Story.read(GSON, Story.App.HOME, json).size());
	}

	@Test
	public void keepsTheNewestTwoHundredAndShortText()
	{
		Annals a = annals();
		for (int i = 0; i < 260; i++)
		{
			a.record(new Entry(Entry.Kind.QUEST, T + 300_000 + i, "A quest with a very long name that goes on and on and on, far past what any story line should ever hold " + i,
				"Quest complete and then some more words to make it longer"), T + 300_000 + i, Z);
		}
		OwnStory.Saved s = OwnStory.of(a);
		assertEquals(Story.MAX_MOMENTS, s.moments.size());
		assertEquals(T + 300_000 + 259, s.moments.get(s.moments.size() - 1).t);
		for (OwnStory.Moment m : s.moments)
		{
			assertTrue(m.text.length() <= Story.MAX_TEXT);
		}
	}
}
