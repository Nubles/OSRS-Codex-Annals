package com.codex.annals.ui;

import com.codex.annals.core.Chapters;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * One chapter of the account's story on a gilt thread: its name, the weeks it spanned and what it
 * came to, with the skill's icon or a boss's ember mark.
 */
class ChapterRow extends JComponent
{
	private static final int H = 38;

	private final Chapters.Chapter chapter;
	private final BufferedImage icon;
	private final boolean first;
	private final boolean last;

	ChapterRow(Chapters.Chapter chapter, BufferedImage icon, boolean first, boolean last)
	{
		this.chapter = chapter;
		this.icon = icon;
		this.first = first;
		this.last = last;
		setOpaque(false);
		setPreferredSize(new Dimension(10, H));
		setToolTipText(chapter.title() + ": " + span(chapter) + ", " + figure(chapter)
			+ (chapter.getPlayedMs() > 0 ? ", " + Format.hours(chapter.getPlayedMs()) + " played" : ""));
	}

	static String span(Chapters.Chapter c)
	{
		String start = Format.shortDay(c.getStart());
		String end = Format.shortDay(c.getEnd());
		return start + " to " + end + (c.getEnd().getYear() != c.getStart().getYear() ? " " + c.getEnd().getYear() : "");
	}

	static String figure(Chapters.Chapter c)
	{
		return c.isBoss() ? Format.plural(c.getKills(), "kill", "kills") : Format.amount(c.getXp()) + " xp";
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		Color tint = chapter.isBoss() ? Theme.KILLS : Theme.XP;

		// The thread, and a bead for this chapter.
		int cx = 9;
		int cy = 13;
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 150));
		g.drawLine(cx, first ? cy : 0, cx, last ? cy : H);
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(cx - 9, cy - 9, 18, 18));
		g.setColor(Theme.alpha(tint, 200));
		g.draw(new Ellipse2D.Float(cx - 9, cy - 9, 18, 18));
		if (icon != null)
		{
			double scale = Math.min(1, 13.0 / Math.max(icon.getWidth(), icon.getHeight()));
			int iw = (int) Math.round(icon.getWidth() * scale);
			int ih = (int) Math.round(icon.getHeight() * scale);
			g.drawImage(icon, cx - iw / 2, cy - ih / 2, iw, ih, null);
		}
		else
		{
			Path2D mark = new Path2D.Float();
			mark.moveTo(cx, cy - 4);
			mark.lineTo(cx + 4, cy);
			mark.lineTo(cx, cy + 4);
			mark.lineTo(cx - 4, cy);
			mark.closePath();
			g.setColor(tint);
			g.fill(mark);
		}

		int tx = 26;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String fig = figure(chapter);
		int fw = small.stringWidth(fig);
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(chapter.title(), body, w - tx - 2), tx, body.getAscent() + 2, Theme.TEXT);
		g.setFont(Theme.small());
		int base = body.getHeight() + small.getAscent() + 3;
		g.setColor(tint);
		g.drawString(fig, w - fw, base);
		g.setColor(Theme.TEXT_MUTED);
		// The weeks go with the dates when they fit; the name already says how long most chapters ran.
		int room = w - tx - fw - 8;
		String dates = Format.shortDay(chapter.getStart()) + " to " + Format.shortDay(chapter.getEnd());
		String full = dates + "  ·  " + chapter.getWeeks() + " weeks";
		g.drawString(small.stringWidth(full) <= room ? full : Ui.clip(dates, small, room), tx, base);
		g.dispose();
	}
}
