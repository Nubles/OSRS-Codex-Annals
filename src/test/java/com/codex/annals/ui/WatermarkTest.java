package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class WatermarkTest
{
	private static final int W = YearCard.WIDTH;
	private static final int H = 700;

	private static BufferedImage page()
	{
		return new BufferedImage(W, 2000, BufferedImage.TYPE_INT_ARGB);
	}

	private static void write(BufferedImage ink, int x, int y, int w, int h)
	{
		Graphics2D g = ink.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(x, y, w, h);
		g.dispose();
	}

	@Test
	public void theMarkKeepsToTheCornerWhenItIsClear()
	{
		BufferedImage ink = page();
		write(ink, 20, 20, W - 40, 300);
		assertEquals(new Point(W - Watermark.SIZE - 16, H - Watermark.SIZE - 26), Watermark.place(ink, W, H));
	}

	@Test
	public void theMarkMovesOffHighlightsWrittenInTheCorner()
	{
		// Highlights written right across the foot of the card, as on a year with four of them.
		BufferedImage ink = page();
		write(ink, 20, 20, W - 40, 200);
		write(ink, 40, H - 260, W - 80, 230);
		Point at = Watermark.place(ink, W, H);
		long old = Watermark.inkIn(ink, W - 196, H - 206, Watermark.SIZE, Watermark.SIZE, H);
		long now = Watermark.inkIn(ink, at.x, at.y, Watermark.SIZE, Watermark.SIZE, H);
		assertTrue("the old corner held ink", old > 0);
		assertTrue(now + " < " + old, now < old);
	}

	@Test
	public void theMarksLinesStopShortOfEveryWord()
	{
		BufferedImage ink = page();
		write(ink, 100, 500, 120, 12);
		boolean[][] busy = Watermark.busy(ink, W, H);
		assertTrue("inside the word", Watermark.near(busy, 150, 506));
		assertTrue("a hair above it", Watermark.near(busy, 150, 498));
		assertFalse("well clear of it", Watermark.near(busy, 150, 470));
		assertFalse("elsewhere on the page", Watermark.near(busy, 300, 100));

		// Engraved over the word, no line of the mark is left on it.
		Point at = new Point(80, 420);
		BufferedImage mark = Watermark.engraved(ink, W, H, at, Color.WHITE, 2);
		for (int y = (500 - at.y) * 2; y < (512 - at.y) * 2; y++)
		{
			for (int x = (100 - at.x) * 2; x < (220 - at.x) * 2; x++)
			{
				assertEquals(0, mark.getRGB(x, y) >>> 24);
			}
		}
		long lines = 0;
		for (int y = 0; y < mark.getHeight(); y++)
		{
			for (int x = 0; x < mark.getWidth(); x++)
			{
				lines += (mark.getRGB(x, y) >>> 24) > 0 ? 1 : 0;
			}
		}
		assertTrue("the rest of the mark is drawn", lines > 500);
	}
}
