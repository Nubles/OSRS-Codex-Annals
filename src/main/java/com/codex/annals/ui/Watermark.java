package com.codex.annals.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

/**
 * Where a card's engraved mark goes, and where it must not: found from the card's content drawn
 * once on its own, so the mark sits in the emptiest corner it can and its lines stop short of
 * every word, figure and tile, never running behind them.
 */
final class Watermark
{
	/** The mark's size on the page. */
	static final int SIZE = 180;
	/** Ink is looked for in cells this size, and the mark kept a cell clear of it. */
	private static final int CELL = 4;
	private static final int INSET = 16;

	private Watermark()
	{
	}

	/**
	 * The mark's top left: the bottom right corner when it is clear, else of the places down the
	 * right and left edges the one with least ink under it, the lowest first on a tie.
	 */
	static Point place(BufferedImage ink, int width, int height)
	{
		int x0 = width - SIZE - INSET;
		int bottom = height - SIZE - 26;
		Point best = new Point(x0, bottom);
		long least = Long.MAX_VALUE;
		for (int x : new int[]{x0, INSET})
		{
			for (int y = bottom; y >= INSET; y -= 20)
			{
				long n = inkIn(ink, x, y, SIZE, SIZE, height);
				if (n < least)
				{
					least = n;
					best = new Point(x, y);
				}
			}
		}
		return best;
	}

	/** The page less every cell holding ink and the cells around it: where the mark may draw. */
	static Area clear(BufferedImage ink, int width, int height)
	{
		Area free = new Area(new Rectangle(0, 0, width, height));
		int cols = (width + CELL - 1) / CELL;
		int rows = (Math.min(height, ink.getHeight()) + CELL - 1) / CELL;
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				if (inkIn(ink, c * CELL, r * CELL, CELL, CELL, height) > 0)
				{
					free.subtract(new Area(new Rectangle((c - 1) * CELL, (r - 1) * CELL, CELL * 3, CELL * 3)));
				}
			}
		}
		return free;
	}

	/** Pixels with any ink in a box of the content image. */
	static long inkIn(BufferedImage ink, int x, int y, int w, int h, int height)
	{
		long n = 0;
		int x1 = Math.min(ink.getWidth(), x + w);
		int y1 = Math.min(Math.min(ink.getHeight(), height), y + h);
		for (int py = Math.max(0, y); py < y1; py++)
		{
			for (int px = Math.max(0, x); px < x1; px++)
			{
				n += (ink.getRGB(px, py) >>> 24) > 8 ? 1 : 0;
			}
		}
		return n;
	}
}
