package com.codex.annals.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * One milestone ahead: its skill's icon (or a gilt diamond), its name, the pace under it, the day
 * it falls on the right, and how far along the way it is.
 */
class AheadRow extends JComponent
{
	private static final int ICON = 20;

	private final BufferedImage icon;
	private final String title;
	private final String note;
	private final String when;
	private final Color color;
	private final double progress;

	/** @param progress 0 to 1 for a bar, or below 0 for none */
	AheadRow(BufferedImage icon, String title, String note, String when, Color color, double progress, String tip)
	{
		this.icon = icon;
		this.title = title;
		this.note = note;
		this.when = when;
		this.color = color;
		this.progress = progress;
		setOpaque(false);
		setPreferredSize(new Dimension(10, progress >= 0 ? 38 : 32));
		setToolTipText(tip);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		if (icon != null)
		{
			double scale = Math.min(1, ICON / (double) Math.max(icon.getWidth(), icon.getHeight()));
			int iw = (int) Math.round(icon.getWidth() * scale);
			int ih = (int) Math.round(icon.getHeight() * scale);
			g.drawImage(icon, (ICON - iw) / 2, 4 + (ICON - ih) / 2, iw, ih, null);
		}
		else
		{
			float cx = ICON / 2f;
			float cy = 13;
			Path2D d = new Path2D.Float();
			d.moveTo(cx, cy - 5);
			d.lineTo(cx + 5, cy);
			d.lineTo(cx, cy + 5);
			d.lineTo(cx - 5, cy);
			d.closePath();
			g.setColor(color);
			g.fill(d);
		}
		int tx = ICON + 6;
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		int base = fm.getAscent() + 1;
		int ww = fm.stringWidth(when);
		Ui.shadowed(g, when, w - ww, base, color);
		Ui.shadowed(g, Ui.clip(title, fm, w - tx - ww - 6), tx, base, Theme.TEXT);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(note, small, w - tx), tx, base + small.getAscent() + 2);
		if (progress >= 0)
		{
			int by = getHeight() - 5;
			int bw = w - tx;
			g.setColor(Theme.INK_DEEP);
			g.fill(new RoundRectangle2D.Float(tx, by, bw, 3, 3, 3));
			float fill = (float) Math.max(3, bw * Math.min(1, progress));
			g.setPaint(new GradientPaint(tx, 0, Theme.mix(color, Theme.INK, 0.45), tx + bw, 0, color));
			g.fill(new RoundRectangle2D.Float(tx, by, fill, 3, 3, 3));
		}
		g.dispose();
	}
}
