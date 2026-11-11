package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import com.codex.annals.core.Annals;
import com.codex.annals.core.Baseline;
import com.codex.annals.core.Wrapped;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Drawing never clears the caller's clip: a cleared clip lets whatever is drawn next spill over
 * everything around it. Each drawing that clips its own shapes must give back the clip it found.
 */
public class ClipTest
{
	private static Graphics2D clipped(BufferedImage img, Rectangle clip)
	{
		Graphics2D g = img.createGraphics();
		g.setClip(clip);
		return g;
	}

	@Test
	public void theCardsPageGivesBackTheClip()
	{
		BufferedImage img = new BufferedImage(YearCard.WIDTH, 400, BufferedImage.TYPE_INT_ARGB);
		Rectangle clip = new Rectangle(3, 4, 200, 300);
		Graphics2D g = clipped(img, clip);
		YearCard.frame(g, 400);
		assertEquals(clip, g.getClip().getBounds());
		YearCard.frame(g, 400, new BufferedImage(YearCard.WIDTH, 400, BufferedImage.TYPE_INT_ARGB));
		assertEquals(clip, g.getClip().getBounds());
		g.dispose();
	}

	@Test
	public void theBarChartGivesBackTheClip()
	{
		BarChart chart = new BarChart(Arrays.asList(new BarChart.Bar("J", "January", 5), new BarChart.Bar("F", "February", 9)), Theme.GOLD, i -> { });
		chart.setSize(180, 60);
		BufferedImage img = new BufferedImage(180, 60, BufferedImage.TYPE_INT_ARGB);
		Rectangle clip = new Rectangle(1, 2, 150, 50);
		Graphics2D g = clipped(img, clip);
		chart.paint(g);
		assertEquals(clip, g.getClip().getBounds());
		g.dispose();
	}

	@Test
	public void anAccentedCardGivesBackTheClip()
	{
		Card card = new Card(new BorderLayout());
		card.accent(Theme.TEAL);
		card.setSize(200, 60);
		BufferedImage img = new BufferedImage(200, 60, BufferedImage.TYPE_INT_ARGB);
		Rectangle clip = new Rectangle(2, 2, 120, 40);
		Graphics2D g = clipped(img, clip);
		card.paint(g);
		assertEquals(clip, g.getClip().getBounds());
		g.dispose();
	}

	@Test
	public void everyWrappedCardGivesBackTheClip()
	{
		long t = LocalDate.of(2025, 3, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> levels = new LinkedHashMap<>();
		xp.put("Fishing", 400_000L);
		levels.put("Fishing", 60);
		Annals a = new Annals();
		a.begin(t, ZoneOffset.UTC, "Zezima", new Baseline(t, 60, 400_000L, 40, 10, 150, 170, Collections.emptyList()), xp, levels, 40);
		for (int d = 1; d < 6; d++)
		{
			long at = t + d * 86_400_000L;
			a.tick(at, 600, ZoneOffset.UTC);
			a.stat("Fishing", 400_000L + d * 40_000L, 60 + d, at + 1000, ZoneOffset.UTC, true);
		}
		Wrapped w = Wrapped.of(a, 2025, ZoneOffset.UTC);
		for (int i = 0; i < w.getCards().size(); i++)
		{
			BufferedImage img = new BufferedImage(WrappedCard.WIDTH, WrappedCard.HEIGHT, BufferedImage.TYPE_INT_ARGB);
			Rectangle clip = new Rectangle(5, 6, 300, 200);
			Graphics2D g = clipped(img, clip);
			WrappedCard.paint(g, w, i, "Zezima", null, null, ZoneOffset.UTC, () -> { });
			assertEquals(clip, g.getClip().getBounds());
			g.dispose();
		}
	}
}
