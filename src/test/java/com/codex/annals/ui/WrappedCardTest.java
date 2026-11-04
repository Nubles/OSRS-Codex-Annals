package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.codex.annals.core.Annals;
import com.codex.annals.core.Baseline;
import com.codex.annals.core.Wrapped;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/** Every Wrapped card draws, at twice the scale, and the story stacks them all into one picture. */
public class WrappedCardTest
{
	private static final ZoneId UTC = ZoneOffset.UTC;

	@Test
	public void everyCardDrawsAndTheStoryHoldsThemAll()
	{
		long t = java.time.LocalDate.of(2025, 3, 1).atStartOfDay(UTC).toInstant().toEpochMilli();
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> levels = new LinkedHashMap<>();
		xp.put("Fishing", 400_000L);
		levels.put("Fishing", 60);
		Annals a = new Annals();
		a.begin(t, UTC, "Zezima", new Baseline(t, 60, 400_000L, 40, 10, 150, 170, Collections.emptyList()), xp, levels, 40);
		for (int d = 1; d < 6; d++)
		{
			long at = t + d * 86_400_000L;
			a.tick(at, 600, UTC);
			a.stat("Fishing", 400_000L + d * 40_000L, 60 + d, at + 1000, UTC, true);
		}
		Wrapped w = Wrapped.of(a, 2025, UTC);
		for (int i = 0; i < w.getCards().size(); i++)
		{
			BufferedImage card = WrappedCard.render(w, i, i % 2 == 0 ? "Zezima" : null, null, null, UTC, () -> { });
			assertEquals(WrappedCard.WIDTH * WrappedCard.SCALE, card.getWidth());
			assertEquals(WrappedCard.HEIGHT * WrappedCard.SCALE, card.getHeight());
		}
		BufferedImage story = WrappedCard.renderAll(w, "Zezima", null, null, UTC, () -> { });
		int n = w.getCards().size();
		assertTrue(n >= 3);
		assertEquals((WrappedCard.GAP + n * (WrappedCard.HEIGHT + WrappedCard.GAP)) * WrappedCard.SCALE, story.getHeight());
	}

	@Test
	public void aShortYearIsToldInMinutesNotNoHours()
	{
		assertEquals("12", WrappedCard.played(12 * 60_000L)[0]);
		assertEquals("minutes played", WrappedCard.played(12 * 60_000L)[1]);
		assertEquals("1", WrappedCard.played(3_700_000L)[0]);
		assertEquals("hour played", WrappedCard.played(3_700_000L)[1]);
	}
}
