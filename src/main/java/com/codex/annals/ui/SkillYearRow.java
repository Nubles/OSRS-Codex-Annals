package com.codex.annals.ui;

import com.codex.annals.core.Recap;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/** One skill's year: its icon and name, the levels it rose through, the experience, and its share of the whole. */
class SkillYearRow extends JComponent
{
	private final Recap.SkillYear line;
	private final BufferedImage icon;
	private final double share;

	SkillYearRow(Recap.SkillYear line, BufferedImage icon, double share)
	{
		this.line = line;
		this.icon = icon;
		this.share = share;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 30));
		setToolTipText(line.getSkill() + ": " + Format.number(line.getGained()) + " xp this year");
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		paint(g, line, icon, share, 0, 0, getWidth());
		g.dispose();
	}

	/** Draws the row at a place and width; the card uses the same drawing. */
	static void paint(Graphics2D g, Recap.SkillYear line, BufferedImage icon, double share, int x, int y, int w)
	{
		paint(g, line, icon, share, x, y, w, true);
	}

	/** Whether the line's levels fit beside its name and experience at this width. */
	static boolean levelsFit(Graphics2D g, Recap.SkillYear line, int w)
	{
		FontMetrics fm = g.getFontMetrics(Theme.body());
		FontMetrics small = g.getFontMetrics(Theme.small());
		int gw = fm.stringWidth(Format.xp(line.getGained()));
		int lx = 26 + fm.stringWidth(line.getSkill()) + 6;
		return lx + small.stringWidth(levels(line)) < w - gw - 6;
	}

	private static String levels(Recap.SkillYear line)
	{
		return line.getEndLevel() > line.getStartLevel() ? line.getStartLevel() + " > " + line.getEndLevel() : String.valueOf(line.getEndLevel());
	}

	/** @param levels whether to show the levels it rose through, where they fit */
	static void paint(Graphics2D g, Recap.SkillYear line, BufferedImage icon, double share, int x, int y, int w, boolean levels)
	{
		int iconBox = 20;
		if (icon != null)
		{
			double scale = Math.min(1, iconBox / (double) Math.max(icon.getWidth(), icon.getHeight()));
			int iw = (int) Math.round(icon.getWidth() * scale);
			int ih = (int) Math.round(icon.getHeight() * scale);
			g.drawImage(icon, x + (iconBox - iw) / 2, y + 2 + (iconBox - ih) / 2, iw, ih, null);
		}
		int tx = x + iconBox + 6;
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		int base = y + fm.getAscent() + 1;
		String gained = Format.xp(line.getGained());
		int gw = fm.stringWidth(gained);
		Ui.shadowed(g, gained, x + w - gw, base, Theme.XP);
		String rose = levels(line);
		int nameRoom = w - (tx - x) - gw - 8;
		String name = Ui.clip(line.getSkill(), fm, nameRoom);
		Ui.shadowed(g, name, tx, base, Theme.TEXT);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int lx = tx + fm.stringWidth(name) + 6;
		if (levels && lx + small.stringWidth(rose) < x + w - gw - 6)
		{
			Ui.shadowed(g, rose, lx, base, line.getEndLevel() > line.getStartLevel() ? Theme.GREEN : Theme.TEXT_FAINT);
		}

		int by = y + 22;
		int bw = w - (tx - x);
		RoundRectangle2D track = new RoundRectangle2D.Float(tx, by, bw, 4, 4, 4);
		g.setColor(Theme.INK_DEEP);
		g.fill(track);
		float fill = (float) Math.max(4, bw * Math.max(0, Math.min(1, share)));
		g.setPaint(new GradientPaint(tx, 0, Theme.mix(Theme.TEAL, Theme.INK, 0.35), tx + bw, 0, Theme.TEAL));
		g.fill(new RoundRectangle2D.Float(tx, by, fill, 4, 4, 4));
	}
}
