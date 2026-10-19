package com.codex.annals.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RadialGradientPaint;
import java.awt.geom.Path2D;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** The crest at the top of the panel: the open chronicle, the player, and the ledger of what the annals hold. */
public class Crest extends JPanel
{
	private final JLabel title = Ui.label("Codex Annals", Theme.title(), Theme.GOLD_BRIGHT);
	private final JLabel subtitle = Ui.label("Log in to open the annals", Theme.small(), Theme.TEXT_MUTED);
	private final Ledger entries = new Ledger("Entries");
	private final Ledger days = new Ledger("Days this year");
	private final Ledger hours = new Ledger("Played this year");

	public Crest()
	{
		super(new BorderLayout(10, 0));
		setOpaque(false);
		setBorder(new EmptyBorder(12, 10, 12, 10));

		JPanel markHolder = new JPanel(new BorderLayout());
		markHolder.setOpaque(false);
		markHolder.add(new Mark(), BorderLayout.NORTH);
		add(markHolder, BorderLayout.WEST);

		JPanel right = new JPanel();
		right.setOpaque(false);
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		title.setAlignmentX(LEFT_ALIGNMENT);
		subtitle.setAlignmentX(LEFT_ALIGNMENT);
		right.add(title);
		right.add(subtitle);
		JPanel ledgers = new JPanel(new GridLayout(3, 1, 0, 2));
		ledgers.setOpaque(false);
		ledgers.setBorder(new EmptyBorder(4, 0, 0, 0));
		ledgers.add(entries);
		ledgers.add(days);
		ledgers.add(hours);
		ledgers.setAlignmentX(LEFT_ALIGNMENT);
		right.add(ledgers);
		add(right, BorderLayout.CENTER);
		update(null, 0, 0, 0, 0);
	}

	/**
	 * @param begunAt when the annals began, or 0 if they have not
	 */
	public void update(String player, long begunAt, int entryCount, int daysThisYear, long playedThisYear)
	{
		title.setText(player != null ? player : "Codex Annals");
		if (begunAt <= 0)
		{
			subtitle.setText(player == null ? "Log in to open the annals" : "Opening the annals...");
			entries.set("-");
			days.set("-");
			hours.set("-");
			return;
		}
		subtitle.setText("Kept since " + Format.since(begunAt));
		entries.set(Format.number(entryCount));
		days.set(Format.number(daysThisYear));
		hours.set(Format.played(playedThisYear));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.GOLD_DIM, 0.18), 0, h, Theme.INK));
		g.fillRect(0, 0, w, h);
		int y = h - 2;
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.GOLD, 0), w / 2f, 0, Theme.alpha(Theme.GOLD, 150), true));
		g.drawLine(0, y, w, y);
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(w / 2f, y - 3);
		diamond.lineTo(w / 2f + 3, y);
		diamond.lineTo(w / 2f, y + 3);
		diamond.lineTo(w / 2f - 3, y);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);
		g.dispose();
		super.paintComponent(graphics);
	}

	/** The open chronicle in a gilt ring. */
	private static final class Mark extends JComponent
	{
		private static final int SIZE = 64;

		Mark()
		{
			setPreferredSize(new Dimension(SIZE, SIZE));
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setPaint(new RadialGradientPaint(SIZE / 2f, SIZE / 2f, SIZE / 2f, new float[]{0f, 1f},
				new Color[]{Theme.alpha(Theme.GOLD, 60), Theme.alpha(Theme.GOLD, 0)}));
			g.fillOval(0, 0, SIZE, SIZE);
			g.setColor(Theme.INK_DEEP);
			g.fillOval(4, 4, SIZE - 8, SIZE - 8);
			g.setColor(Theme.alpha(Theme.GOLD, 180));
			g.drawOval(4, 4, SIZE - 9, SIZE - 9);
			g.setColor(Theme.alpha(Theme.GOLD_DIM, 140));
			g.drawOval(7, 7, SIZE - 15, SIZE - 15);
			Sigil.paint(g, 14, 13, SIZE - 28);
			g.dispose();
		}
	}

	/** One labelled value. */
	private static final class Ledger extends JPanel
	{
		private final JLabel value = Ui.label("-", Theme.small(), Theme.GOLD);

		Ledger(String label)
		{
			super(new BorderLayout());
			setOpaque(false);
			add(Ui.label(label, Theme.small(), Theme.TEXT_MUTED), BorderLayout.WEST);
			add(value, BorderLayout.EAST);
			setPreferredSize(new Dimension(10, 15));
		}

		void set(String text)
		{
			value.setText(text);
		}
	}
}
