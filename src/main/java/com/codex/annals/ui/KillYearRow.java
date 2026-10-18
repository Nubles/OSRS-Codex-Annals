package com.codex.annals.ui;

import com.codex.annals.core.Recap;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** One boss, raid or chest across the year: its kills, the game's count after them, and a bar for its share. */
class KillYearRow extends JComponent
{
	private final Recap.KillYear line;
	private final double share;

	KillYearRow(Recap.KillYear line, double share)
	{
		this.line = line;
		this.share = share;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 24));
		setToolTipText(line.getName() + ": " + Format.number(line.getKills()) + " this year"
			+ (line.getKillCount() > 0 ? ", kill count " + Format.number(line.getKillCount()) : ""));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		paint(g, line, share, 0, 0, getWidth(), true);
		g.dispose();
	}

	static void paint(Graphics2D g, Recap.KillYear line, double share, int x, int y, int w, boolean showCount)
	{
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		int base = y + fm.getAscent();
		Path2D mark = new Path2D.Float();
		float my = base - fm.getAscent() / 2f + 1;
		mark.moveTo(x + 3, my - 3);
		mark.lineTo(x + 6, my);
		mark.lineTo(x + 3, my + 3);
		mark.lineTo(x, my);
		mark.closePath();
		g.setColor(Theme.mix(Theme.GOLD_DIM, Theme.KILLS, Math.max(0.2, share)));
		g.fill(mark);

		String count = Format.number(line.getKills());
		String kc = showCount && line.getKillCount() > 0 ? "KC " + Format.number(line.getKillCount()) : "";
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int kcw = kc.isEmpty() ? 0 : small.stringWidth(kc) + 8;
		g.setFont(Theme.body());
		int cw = fm.stringWidth(count);
		Ui.shadowed(g, count, x + w - kcw - cw, base, Theme.KILLS);
		Ui.shadowed(g, Ui.clip(line.getName(), fm, w - kcw - cw - 18), x + 12, base, Theme.TEXT);
		if (!kc.isEmpty())
		{
			g.setFont(Theme.small());
			Ui.shadowed(g, kc, x + w - kcw + 8, base, Theme.TEXT_FAINT);
		}
		int by = base + 4;
		int bw = w - 12;
		g.setColor(Theme.INK_DEEP);
		g.fill(new RoundRectangle2D.Float(x + 12, by, bw, 3, 3, 3));
		g.setColor(Theme.alpha(Theme.KILLS, 200));
		g.fill(new RoundRectangle2D.Float(x + 12, by, (float) Math.max(3, bw * Math.max(0, Math.min(1, share))), 3, 3, 3));
	}
}
