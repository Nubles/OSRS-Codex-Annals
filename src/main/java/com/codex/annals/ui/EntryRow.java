package com.codex.annals.ui;

import com.codex.annals.core.Entry;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

/**
 * One line of the annals: the day, a node on a gilt thread coloured by the kind of entry, and
 * what happened, with the item or skill it belongs to. Rows in a run join their threads.
 */
class EntryRow extends JComponent
{
	static final int HEIGHT = 34;
	private static final int DATE_W = 36;

	private final Entry entry;
	private final LocalDate date;
	private final BufferedImage icon;
	private final boolean first;
	private final boolean last;

	EntryRow(Entry entry, LocalDate date, BufferedImage icon, boolean first, boolean last)
	{
		this.entry = entry;
		this.date = date;
		this.icon = icon;
		this.first = first;
		this.last = last;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		String detail = detail(entry);
		setToolTipText(Format.date(entry.getAt()) + ": " + entry.getTitle() + (detail == null ? "" : ", " + detail));
	}

	/**
	 * Lets the player strike this line from the annals with a right click, after saying yes.
	 *
	 * @param strike what to do with the entry, or null to leave the row as it is
	 */
	void strikeWith(Consumer<Entry> strike)
	{
		if (strike == null || entry.getKind() == Entry.Kind.BEGIN)
		{
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem item = new JMenuItem("Strike from the annals");
		item.addActionListener(e ->
		{
			int answer = JOptionPane.showConfirmDialog(this,
				"Strike \"" + entry.getTitle() + "\" of " + Format.date(entry.getAt()) + " from your annals?\nThis cannot be undone.",
				"Codex Annals", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
			{
				strike.accept(entry);
			}
		});
		menu.add(item);
		setComponentPopupMenu(menu);
	}

	/** The detail line, led by a drop's value so it survives when the line is cut short. */
	static String detail(Entry e)
	{
		if (e.getValue() > 0 && e.getDetail() != null)
		{
			String detail = e.getDetail();
			return Format.coins(e.getValue()) + ", " + Format.lower(detail);
		}
		return e.getDetail();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		Color color = Theme.entry(entry.getKind());
		int nx = DATE_W + 6;
		int ny = 10;

		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 140));
		if (!first)
		{
			g.drawLine(nx, 0, nx, ny);
		}
		if (!last)
		{
			g.drawLine(nx, ny, nx, h);
		}
		node(g, entry.getKind(), nx, ny, color);

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		Ui.shadowed(g, Format.shortDay(date), 0, ny + small.getAscent() / 2 - 1, Theme.TEXT_FAINT);

		int tx = nx + 10;
		int right = w;
		if (icon != null)
		{
			int box = 26;
			double scale = Math.min(1, box / (double) Math.max(icon.getWidth(), icon.getHeight()));
			int iw = (int) Math.round(icon.getWidth() * scale);
			int ih = (int) Math.round(icon.getHeight() * scale);
			g.drawImage(icon, w - box + (box - iw) / 2, Math.max(0, (h - ih) / 2 - 2), iw, ih, null);
			right = w - box - 4;
		}
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(entry.getTitle(), fm, right - tx), tx, ny + fm.getAscent() / 2 - 1, Theme.mix(Theme.TEXT, color, 0.25));
		String detail = detail(entry);
		if (detail != null)
		{
			g.setFont(Theme.small());
			Ui.shadowed(g, Ui.clip(detail, small, right - tx), tx, ny + fm.getAscent() / 2 + small.getHeight(), Theme.TEXT_MUTED);
		}
		g.dispose();
	}

	/** Whether an entry gets the ringed node: a pet, a 99, the highest total. */
	static boolean grand(Entry.Kind kind)
	{
		return kind == Entry.Kind.PET || kind == Entry.Kind.MAX;
	}

	/** The node: a diamond for most entries, a ringed diamond for the grand ones. Shared with the card. */
	static void node(Graphics2D g, Entry.Kind kind, float x, float y, Color color)
	{
		boolean grand = grand(kind);
		if (grand)
		{
			g.setColor(Theme.alpha(color, 60));
			g.fill(new Ellipse2D.Float(x - 7, y - 7, 14, 14));
			g.setColor(Theme.alpha(color, 160));
			g.draw(new Ellipse2D.Float(x - 6, y - 6, 12, 12));
		}
		float r = grand ? 4.5f : kind == Entry.Kind.LEVEL ? 3f : 4f;
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(x, y - r);
		diamond.lineTo(x + r, y);
		diamond.lineTo(x, y + r);
		diamond.lineTo(x - r, y);
		diamond.closePath();
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(x - r - 1.5f, y - r - 1.5f, r * 2 + 3, r * 2 + 3));
		g.setColor(color);
		g.fill(diamond);
		if (kind == Entry.Kind.BEGIN)
		{
			g.setColor(Theme.INK_DEEP);
			g.fill(new Ellipse2D.Float(x - 1.5f, y - 1.5f, 3, 3));
		}
	}
}
