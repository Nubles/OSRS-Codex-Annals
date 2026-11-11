package com.codex.annals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * One account story: other plugins' N1 moments read fail-safe, woven so each moment is told
 * once, and the annals' own story written in the same shape. The fixtures are written to the
 * contract in CONTRACTS-NEW.md (N1), since the writers land in their own plugins at the same time.
 */
public class StoryTest
{
	private static final Gson GSON = new Gson();
	private static final long T = 1_793_000_000_000L;

	static String fixture(String name)
	{
		try (InputStream in = StoryTest.class.getResourceAsStream("/com/codex/annals/core/" + name))
		{
			byte[] all = new byte[1 << 16];
			int n = 0;
			for (int r; (r = in.read(all, n, all.length - n)) > 0; )
			{
				n += r;
			}
			return new String(all, 0, n, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	static List<Story.Moment> all()
	{
		List<Story.Moment> all = new ArrayList<>();
		all.addAll(Story.read(GSON, Story.App.LOG_INSIGHT, fixture("story-loginsight.json")));
		all.addAll(Story.read(GSON, Story.App.GUARDIAN, fixture("story-guardian.json")));
		all.addAll(Story.read(GSON, Story.App.CHRONICLE, fixture("story-chronicle.json")));
		all.addAll(Story.read(GSON, Story.App.BOSS_PILOT, fixture("story-bosspilot.json")));
		return all;
	}

	@Test
	public void readsTheContractShape()
	{
		List<Story.Moment> li = Story.read(GSON, Story.App.LOG_INSIGHT, fixture("story-loginsight.json"));
		assertEquals(2, li.size());
		Story.Moment fang = li.get(1);
		assertEquals(T + 20_000, fang.getT());
		assertEquals("drop", fang.getKind());
		assertEquals("Tanzanite fang · 212 kc · luckier than 66% of players", fang.getText());
		assertEquals("212 kc", fang.getFigure());
		assertEquals("12922", fang.getRef());
		// Newest last, whatever order they were written in.
		List<Story.Moment> g = Story.read(GSON, Story.App.GUARDIAN, fixture("story-guardian.json"));
		assertEquals(Arrays.asList("closeCall", "milestone", "death"), Arrays.asList(g.get(0).getKind(), g.get(1).getKind(), g.get(2).getKind()));
	}

	@Test
	public void anythingElseReadsAsNoMoments()
	{
		for (String bad : new String[]{null, "", "  ", "null", "[]", "{", "5", "\"story\"", "{\"version\":2,\"moments\":[{\"t\":5,\"text\":\"x\"}]}",
			"{\"version\":\"two\",\"moments\":[{\"t\":5,\"text\":\"x\"}]}", "{\"version\":1,\"moments\":{}}", "{\"version\":1,\"moments\":[null,1,[],\"x\"]}",
			"{\"version\":1,\"moments\":[{\"t\":-5,\"text\":\"x\"},{\"t\":5},{\"t\":5,\"text\":\"  \"},{\"text\":\"x\"}]}"})
		{
			assertTrue(String.valueOf(bad), Story.read(GSON, Story.App.HOME, bad).isEmpty());
		}
	}

	@Test
	public void bendsWhatItCanAndCapsTheRest()
	{
		StringBuilder many = new StringBuilder("{\"version\":1,\"moments\":[");
		for (int i = 1; i <= 250; i++)
		{
			many.append(i > 1 ? "," : "").append("{\"t\":").append(i).append(",\"kind\":\"sneeze\",\"text\":\"").append(i).append("\",\"figure\":").append(i * 2).append('}');
		}
		many.append("]}");
		List<Story.Moment> read = Story.read(GSON, Story.App.HOME, many.toString());
		assertEquals(Story.MAX_MOMENTS, read.size());
		assertEquals(51, read.get(0).getT());
		assertEquals("note", read.get(0).getKind());
		assertEquals("102", read.get(0).getFigure());
		char[] longText = new char[400];
		Arrays.fill(longText, 'a');
		Story.Moment cut = Story.read(GSON, Story.App.HOME, "{\"moments\":[{\"t\":9,\"kind\":\"pb\",\"text\":\"" + new String(longText) + "\"}]}").get(0);
		assertTrue(cut.getText().length() <= Story.MAX_TEXT);
	}

	private static List<Entry> own()
	{
		List<Entry> own = new ArrayList<>();
		own.add(new Entry(Entry.Kind.MAX, T - 5_000_000, "99 Ranged", "Your 3rd 99").skill("Ranged", 99));
		own.add(new Entry(Entry.Kind.LEVEL, T + 2_000_010, "Slayer 70", "Level up").skill("Slayer", 70));
		own.add(new Entry(Entry.Kind.COLLECTION, T + 1_000, "Tanzanite fang", "New log slot from Zulrah").item(12922, 3_200_000));
		return own;
	}

	@Test
	public void eachMomentIsToldOnce()
	{
		List<Entry> woven = Story.weave(own(), all());
		// The fang: one line, the annals', with Log Insight's clause.
		List<Entry> fangs = named(woven, "Tanzanite fang");
		assertEquals(1, fangs.size());
		assertEquals("Log Insight", fangs.get(0).getClauseApp());
		assertEquals("212 kc · luckier than 66% of players", fangs.get(0).getClause());
		// The 99 gains Chronicle's session.
		Entry ranged = named(woven, "99 Ranged").get(0);
		assertEquals("Chronicle", ranged.getClauseApp());
		assertEquals("in a 3h 10m session at 412k/hr", ranged.getClause());
		// Guardian's "Reached 70 Slayer" says nothing the annals' line does not: absorbed, no clause.
		Entry slayer = named(woven, "Slayer 70").get(0);
		assertNull(slayer.getClause());
		assertTrue(named(woven, "Reached 70 Slayer").isEmpty());
		// One death, Guardian's, though Chronicle told it too.
		int deaths = 0;
		for (Entry e : woven)
		{
			if (e.getKind() == Entry.Kind.DEATH)
			{
				deaths++;
				assertEquals("Guardian", e.getApp());
				assertEquals("Died at Vorkath, to a dragonfire bomb", e.getTitle());
			}
		}
		assertEquals(1, deaths);
		// Lines of their own: the warhammer (no annals line), the close call, the personal best.
		Entry hammer = named(woven, "Dragon warhammer").get(0);
		assertEquals(Entry.Kind.FIND, hammer.getKind());
		assertEquals("Log Insight · 412 kc · 1 in 5,000 · luckier than 41% of players", hammer.getDetail());
		Entry pb = named(woven, "Vorkath personal best").get(0);
		assertEquals(Entry.Kind.PB, pb.getKind());
		assertEquals("Boss Pilot · 1:02", pb.getDetail());
		assertTrue(pb.in(Entry.Chapter.BOSSES));
		assertTrue(!pb.in(Entry.Chapter.LOOT));
		assertEquals(Entry.Kind.CLOSE_CALL, named(woven, "Close call at Ice trolls, 6 hp").get(0).getKind());
		// Oldest first, and the annals' own lines are not changed in place.
		for (int i = 1; i < woven.size(); i++)
		{
			assertTrue(woven.get(i - 1).getAt() <= woven.get(i).getAt());
		}
		assertNull(own().get(2).getClause());
		assertEquals(3 + 4, woven.size());
	}

	@Test
	public void withoutAnyStoryTheAnnalsAreUnchanged()
	{
		List<Entry> own = own();
		assertEquals(own, Story.weave(own, null));
		assertEquals(own, Story.weave(own, new ArrayList<>()));
	}

	@Test
	public void guardiansJournalStandsInForItsStory()
	{
		List<Story.Moment> journal = GuardianJournal.moments(GSON, fixture("guardian-journal.json"));
		assertEquals(3, journal.size());
		assertEquals("closeCall", journal.get(0).getKind());
		assertEquals("Survived a close call: Down to 6/99 hitpoints at Ice trolls, against Ice troll runt", journal.get(0).getText());
		assertEquals("a safe death", journal.get(1).getFigure());
		assertEquals("death", journal.get(2).getKind());
		for (String bad : new String[]{null, "", "{", "[]", "{\"entries\":5}", "{\"entries\":[null,{\"at\":\"x\"},{\"type\":\"DEATH\"}]}"})
		{
			assertTrue(GuardianJournal.moments(GSON, bad).isEmpty());
		}
	}

	@Test
	public void wordsIgnoreCommasAndQuietWords()
	{
		assertEquals(new java.util.LinkedHashSet<>(Arrays.asList("vorkath", "1000")), Story.words("Your first Vorkath kill count: 1,000"));
	}

	private static List<Entry> named(List<Entry> entries, String title)
	{
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries)
		{
			if (e.getTitle().equals(title))
			{
				out.add(e);
			}
		}
		return out;
	}
}
