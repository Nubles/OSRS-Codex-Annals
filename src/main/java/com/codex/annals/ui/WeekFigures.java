package com.codex.annals.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** Up to three figures side by side, each large over a small label: "6h 12m / played". */
class WeekFigures extends JComponent
{
	private final String[] values;
	private final String[] labels;
	private final Color[] colors;

	WeekFigures(String[] values, String[] labels, Color[] colors)
	{
		this.values = values;
		this.labels = labels;
		this.colors = colors;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 34));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int n = values.length;
		int col = getWidth() / Math.max(1, n);
		for (int i = 0; i < n; i++)
		{
			int x = i * col;
			g.setFont(Theme.title());
			FontMetrics bold = g.getFontMetrics();
			Ui.shadowed(g, Ui.clip(values[i], bold, col - 4), x, bold.getAscent(), colors[i]);
			g.setFont(Theme.small());
			FontMetrics small = g.getFontMetrics();
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(Ui.clip(labels[i], small, col - 4), x, bold.getHeight() + small.getAscent());
			if (i > 0)
			{
				g.setColor(Theme.STROKE_SOFT);
				g.drawLine(x - 6, 3, x - 6, getHeight() - 3);
			}
		}
		g.dispose();
	}
}
