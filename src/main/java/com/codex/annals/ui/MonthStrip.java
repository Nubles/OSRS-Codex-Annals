package com.codex.annals.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * The year as twelve gilded tiles, one a month, lit by how many days were played in it.
 * The busiest month burns brightest; hovering names the month and what it held.
 */
class MonthStrip extends JComponent
{
	private static final String LETTERS = "JFMAMJJASOND";
	static final int HEIGHT = 40;

	private final int[] days;
	private final int[] entries;
	private final int current;
	private int hover = -1;

	/**
	 * @param current the month still being lived, 0 for January, or -1 for a finished year
	 */
	MonthStrip(int[] days, int[] entries, int current)
	{
		this.days = days;
		this.entries = entries;
		this.current = current;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setToolTipText("");
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int index = Math.min(11, Math.max(0, e.getX() * 12 / Math.max(1, getWidth())));
				if (index != hover)
				{
					hover = index;
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = -1;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		int m = Math.min(11, Math.max(0, e.getX() * 12 / Math.max(1, getWidth())));
		return Format.monthName(m) + ": " + Format.plural(days[m], "day", "days") + " played, "
			+ Format.plural(entries[m], "entry", "entries");
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		paint(g, days, current, hover, 0, 0, getWidth(), getHeight());
		g.dispose();
	}

	/** Draws the strip in a box; the card uses the same drawing. */
	static void paint(Graphics2D g, int[] days, int current, int hover, int x, int y, int w, int h)
	{
		int max = 1;
		for (int d : days)
		{
			max = Math.max(max, d);
		}
		int best = -1;
		for (int m = 0; m < 12 && best < 0; m++)
		{
			best = days[m] == max && days[m] > 0 ? m : -1;
		}
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int gap = 3;
		int labelH = fm.getAscent() + 3;
		float cell = (w - gap * 11) / 12f;
		int ch = h - labelH;
		for (int m = 0; m < 12; m++)
		{
			float cx = x + m * (cell + gap);
			double t = days[m] / (double) max;
			RoundRectangle2D tile = new RoundRectangle2D.Float(cx, y, cell, ch, 5, 5);
			Color top;
			Color bottom;
			if (days[m] == 0)
			{
				top = Theme.INK_DEEP;
				bottom = Theme.INK_DEEP;
			}
			else
			{
				Color lit = m == best ? Theme.GOLD_BRIGHT : Theme.mix(Theme.GOLD_DIM, Theme.GOLD, t);
				top = Theme.mix(Theme.CARD, lit, 0.25 + 0.75 * t);
				bottom = Theme.mix(Theme.INK_DEEP, lit, 0.15 + 0.45 * t);
			}
			g.setPaint(new GradientPaint(0, y, top, 0, y + ch, bottom));
			g.fill(tile);
			g.setColor(m == hover ? Theme.alpha(Theme.GOLD_BRIGHT, 200) : m == current ? Theme.alpha(Theme.TEAL, 200)
				: Theme.alpha(Theme.STROKE, days[m] == 0 ? 140 : 90));
			g.draw(tile);
			if (days[m] > 0)
			{
				g.setColor(Theme.alpha(Color.WHITE, 40));
				g.drawLine(Math.round(cx + 3), y + 1, Math.round(cx + cell - 3), y + 1);
			}
			String letter = LETTERS.substring(m, m + 1);
			g.setColor(m == best ? Theme.GOLD : m == hover ? Theme.TEXT : Theme.TEXT_FAINT);
			g.drawString(letter, Math.round(cx + (cell - fm.stringWidth(letter)) / 2), y + h - 1);
		}
	}
}
