package com.codex.annals.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
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

	/**
	 * Whether a point of the page is near ink: in a cell holding any, or a cell beside one. The
	 * mark draws nowhere that is.
	 */
	static boolean[][] busy(BufferedImage ink, int width, int height)
	{
		int cols = (width + CELL - 1) / CELL;
		int rows = (height + CELL - 1) / CELL;
		boolean[][] inked = new boolean[rows][cols];
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				inked[r][c] = inkIn(ink, c * CELL, r * CELL, CELL, CELL, height) > 0;
			}
		}
		boolean[][] near = new boolean[rows][cols];
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				for (int dr = -1; dr <= 1 && !near[r][c]; dr++)
				{
					for (int dc = -1; dc <= 1; dc++)
					{
						int rr = r + dr;
						int cc = c + dc;
						if (rr >= 0 && rr < rows && cc >= 0 && cc < cols && inked[rr][cc])
						{
							near[r][c] = true;
							break;
						}
					}
				}
			}
		}
		return near;
	}

	/** Whether a point of the page, in card units, is near ink. */
	static boolean near(boolean[][] busy, double x, double y)
	{
		int r = (int) Math.floor(y / CELL);
		int c = (int) Math.floor(x / CELL);
		return r >= 0 && c >= 0 && r < busy.length && c < busy[r].length && busy[r][c];
	}

	/**
	 * The mark engraved at a place, drawn at the card's scale, with every line near ink taken out,
	 * ready to lay on the page at {@code at}, {@link #SIZE} card units square.
	 */
	static BufferedImage engraved(BufferedImage ink, int width, int height, Point at, Color color, int scale)
	{
		int px = SIZE * scale;
		BufferedImage mark = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mark.createGraphics();
		Theme.smooth(g);
		g.scale(scale, scale);
		Sigil.engrave(g, 0, 0, SIZE, color);
		g.dispose();
		boolean[][] busy = busy(ink, width, height);
		for (int y = 0; y < px; y++)
		{
			for (int x = 0; x < px; x++)
			{
				if (near(busy, at.x + x / (double) scale, at.y + y / (double) scale))
				{
					mark.setRGB(x, y, 0);
				}
			}
		}
		return mark;
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
