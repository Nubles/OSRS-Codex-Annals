package com.codex.annals.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

/**
 * The year chooser once the annals hold more years than fit side by side: the chosen year in the
 * middle of an engraved strip, an arrow to the year before on the left and the year after on the right.
 */
class YearStepper extends JComponent
{
	private static final int ARROW = 30;

	private final List<Integer> years;
	private final int index;
	private int hover;

	/**
	 * @param years newest first
	 * @param onChange called with the index of the year chosen
	 */
	YearStepper(List<Integer> years, int index, IntConsumer onChange)
	{
		this.years = years;
		this.index = index;
		setFont(Theme.small());
		setPreferredSize(new Dimension(10, 22));
		setToolTipText("");
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int side = side(e.getX());
				setCursor(Cursor.getPredefinedCursor(target(side) >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
				if (side != hover)
				{
					hover = side;
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = 0;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				int to = target(side(e.getX()));
				if (to >= 0)
				{
					onChange.accept(to);
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	/** -1 over the left arrow, 1 over the right one, 0 between. */
	private int side(int x)
	{
		return x < ARROW ? -1 : x >= getWidth() - ARROW ? 1 : 0;
	}

	/** The index an arrow leads to, or -1. Left goes back in time, to an older year. */
	private int target(int side)
	{
		int to = side < 0 ? index + 1 : side > 0 ? index - 1 : -1;
		return side != 0 && to >= 0 && to < years.size() ? to : -1;
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		int to = target(side(e.getX()));
		return to < 0 ? years.size() + " years in the annals" : "Show " + years.get(to);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D strip = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(strip);
		RoundRectangle2D lit = new RoundRectangle2D.Float(ARROW, 2, w - ARROW * 2, h - 4, 6, 6);
		g.setColor(Theme.CARD_TOP);
		g.fill(lit);
		g.setColor(Theme.alpha(Theme.GOLD, 120));
		g.draw(lit);

		g.setFont(getFont());
		FontMetrics fm = g.getFontMetrics();
		String year = String.valueOf(years.get(index));
		String place = (years.size() - index) + " of " + years.size();
		int ty = (h + fm.getAscent()) / 2 - 1;
		int yw = fm.stringWidth(year);
		int pw = fm.stringWidth(place);
		int x = (w - yw - 8 - pw) / 2;
		Ui.shadowed(g, year, x, ty, Theme.GOLD_BRIGHT);
		Ui.shadowed(g, place, x + yw + 8, ty, Theme.TEXT_FAINT);

		arrow(g, ARROW / 2f, h / 2f, -1, target(-1) >= 0, hover < 0);
		arrow(g, w - ARROW / 2f, h / 2f, 1, target(1) >= 0, hover > 0);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(strip);
		g.dispose();
	}

	private static void arrow(Graphics2D g, float cx, float cy, int dir, boolean enabled, boolean hovered)
	{
		Path2D p = new Path2D.Float();
		p.moveTo(cx - 3 * dir, cy - 5);
		p.lineTo(cx + 3 * dir, cy);
		p.lineTo(cx - 3 * dir, cy + 5);
		p.closePath();
		Color c = !enabled ? Theme.alpha(Theme.TEXT_FAINT, 90) : hovered ? Theme.GOLD_BRIGHT : Theme.GOLD;
		g.setColor(c);
		g.fill(p);
	}
}
