package com.codex.annals.ui;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Annals' mark: an open chronicle on a gilt binding, a ribbon marking the page, a teal star above. */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		BasicStroke edge = new BasicStroke(Math.max(0.6f, 0.55f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		// The binding, a little wider than the pages.
		Path2D cover = new Path2D.Float();
		cover.moveTo(x + 0.4f * s, y + 6.2f * s);
		cover.lineTo(x + 0.4f * s, y + 13.6f * s);
		cover.quadTo(x + 4.4f * s, y + 12.6f * s, x + 8 * s, y + 14.6f * s);
		cover.quadTo(x + 11.6f * s, y + 12.6f * s, x + 15.6f * s, y + 13.6f * s);
		cover.lineTo(x + 15.6f * s, y + 6.2f * s);
		cover.closePath();
		g.setPaint(new GradientPaint(x, y + 6 * s, Theme.GOLD, x, y + 15 * s, Theme.GOLD_DIM));
		g.fill(cover);
		g.setColor(Theme.INK_DEEP);
		g.setStroke(edge);
		g.draw(cover);

		// Two pages curving up from the spine.
		for (int side = -1; side <= 1; side += 2)
		{
			float sx = side;
			Path2D page = new Path2D.Float();
			page.moveTo(x + 8 * s, y + 13.4f * s);
			page.quadTo(x + (8 + sx * 3.4f) * s, y + 11.4f * s, x + (8 + sx * 6.8f) * s, y + 12.4f * s);
			page.lineTo(x + (8 + sx * 6.8f) * s, y + 5.2f * s);
			page.quadTo(x + (8 + sx * 3.4f) * s, y + 4.2f * s, x + 8 * s, y + 6 * s);
			page.closePath();
			g.setPaint(new GradientPaint(x + 8 * s, y, Theme.mix(Theme.TEXT, Theme.GOLD_BRIGHT, 0.35),
				x + (8 + sx * 7) * s, y, Theme.mix(Theme.GOLD_BRIGHT, Theme.GOLD_DIM, 0.35)));
			g.fill(page);
			g.setColor(Theme.INK_DEEP);
			g.draw(page);

			// Lines of writing on each page.
			g.setColor(Theme.alpha(Theme.INK_DEEP, 150));
			for (int line = 0; line < 3; line++)
			{
				float ly = 7.4f + line * 1.6f;
				float from = 8 + sx * 1.4f;
				float to = 8 + sx * (line == 2 ? 4.2f : 5.6f);
				g.drawLine(Math.round(x + from * s), Math.round(y + ly * s), Math.round(x + to * s), Math.round(y + (ly - 0.3f) * s));
			}
		}

		// The ribbon, hanging from the spine below the binding.
		Path2D ribbon = new Path2D.Float();
		ribbon.moveTo(x + 9.2f * s, y + 9 * s);
		ribbon.lineTo(x + 10.6f * s, y + 9 * s);
		ribbon.lineTo(x + 10.6f * s, y + 15.8f * s);
		ribbon.lineTo(x + 9.9f * s, y + 15 * s);
		ribbon.lineTo(x + 9.2f * s, y + 15.8f * s);
		ribbon.closePath();
		g.setPaint(new GradientPaint(x, y + 9 * s, Theme.BLOOD, x, y + 16 * s, Theme.BLOOD_DEEP));
		g.fill(ribbon);

		// A teal star over the book, for the moment being written down.
		float cx = x + 8 * s;
		float cy = y + 2.2f * s;
		float r = 2.2f * s;
		float w = 0.55f * s;
		Path2D star = new Path2D.Float();
		star.moveTo(cx, cy - r);
		star.lineTo(cx + w, cy - w);
		star.lineTo(cx + r, cy);
		star.lineTo(cx + w, cy + w);
		star.lineTo(cx, cy + r);
		star.lineTo(cx - w, cy + w);
		star.lineTo(cx - r, cy);
		star.lineTo(cx - w, cy - w);
		star.closePath();
		g.setColor(Theme.alpha(Theme.TEAL, 90));
		g.fill(new java.awt.geom.Ellipse2D.Float(cx - r * 0.8f, cy - r * 0.8f, r * 1.6f, r * 1.6f));
		g.setColor(Theme.TEAL);
		g.fill(star);
	}

	/**
	 * The mark as a fine engraving: its outlines only, in one colour, for a watermark. Filled
	 * shapes faded to a few percent read as a smudge on the page; thin lines stay crisp at any
	 * strength.
	 */
	public static void engrave(Graphics2D g, float x, float y, float size, java.awt.Color color)
	{
		float s = size / 16f;
		g.setColor(color);
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		Path2D cover = new Path2D.Float();
		cover.moveTo(x + 0.4f * s, y + 6.2f * s);
		cover.lineTo(x + 0.4f * s, y + 13.6f * s);
		cover.quadTo(x + 4.4f * s, y + 12.6f * s, x + 8 * s, y + 14.6f * s);
		cover.quadTo(x + 11.6f * s, y + 12.6f * s, x + 15.6f * s, y + 13.6f * s);
		cover.lineTo(x + 15.6f * s, y + 6.2f * s);
		g.draw(cover);
		for (int side = -1; side <= 1; side += 2)
		{
			Path2D page = new Path2D.Float();
			page.moveTo(x + 8 * s, y + 13.4f * s);
			page.quadTo(x + (8 + side * 3.4f) * s, y + 11.4f * s, x + (8 + side * 6.8f) * s, y + 12.4f * s);
			page.lineTo(x + (8 + side * 6.8f) * s, y + 5.2f * s);
			page.quadTo(x + (8 + side * 3.4f) * s, y + 4.2f * s, x + 8 * s, y + 6 * s);
			page.closePath();
			g.draw(page);
			for (int line = 0; line < 3; line++)
			{
				float ly = 7.4f + line * 1.6f;
				float from = 8 + side * 1.4f;
				float to = 8 + side * (line == 2 ? 4.2f : 5.6f);
				g.draw(new java.awt.geom.Line2D.Float(x + from * s, y + ly * s, x + to * s, y + (ly - 0.3f) * s));
			}
		}
		float cx = x + 8 * s;
		float cy = y + 2.2f * s;
		float r = 2.2f * s;
		float w = 0.55f * s;
		Path2D star = new Path2D.Float();
		star.moveTo(cx, cy - r);
		star.lineTo(cx + w, cy - w);
		star.lineTo(cx + r, cy);
		star.lineTo(cx + w, cy + w);
		star.lineTo(cx, cy + r);
		star.lineTo(cx - w, cy + w);
		star.lineTo(cx - r, cy);
		star.lineTo(cx - w, cy - w);
		star.closePath();
		g.draw(star);
	}
}
