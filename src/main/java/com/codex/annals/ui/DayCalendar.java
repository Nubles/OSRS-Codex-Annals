package com.codex.annals.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JComponent;

/**
 * Every day of a year as a small square, a month to a block, four months to a line: gilded where
 * the account was played, teal along the streak still running, an empty socket where it was not,
 * and nothing at all for days still to come or from before the annals began.
 */
class DayCalendar extends JComponent
{
	private static final int COLUMNS = 4;
	private static final int GAP = 8;
	private static final int LABEL = 12;
	private static final DateTimeFormatter TIP = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);

	private final int year;
	private final Set<Long> played;
	private final Set<Long> streak = new HashSet<>();
	private final long today;
	private final long first;

	/**
	 * @param days days played, as epoch days
	 * @param today today as an epoch day; later days are not drawn
	 * @param first the day the annals began, as an epoch day; earlier days are not drawn
	 * @param streak how many days back from today, or from yesterday, the running streak goes
	 */
	DayCalendar(int year, List<Long> days, long today, long first, int streak)
	{
		this.year = year;
		this.played = new HashSet<>(days);
		this.today = today;
		this.first = first;
		long day = played.contains(today) ? today : today - 1;
		for (int i = 0; i < streak; i++)
		{
			this.streak.add(day - i);
		}
		setOpaque(false);
		setPreferredSize(new Dimension(10, height(cell(Views.WIDTH - 30))));
		setToolTipText("");
	}

	/** The side of one day's square, with a pixel between squares, for a given width. */
	private static int cell(int width)
	{
		int month = (width - GAP * (COLUMNS - 1)) / COLUMNS;
		return Math.max(3, (month + 1) / 7 - 1);
	}

	private static int height(int cell)
	{
		return 3 * (LABEL + 6 * (cell + 1)) + 2 * GAP;
	}

	@Override
	public Dimension getPreferredSize()
	{
		int w = getWidth() > 0 ? getWidth() : Views.WIDTH - 30;
		return new Dimension(10, height(cell(w)));
	}

	/** Where a month's block starts. */
	private int[] origin(int month, int cell)
	{
		int monthW = 7 * (cell + 1);
		int spare = getWidth() - COLUMNS * monthW;
		int gap = Math.max(2, spare / (COLUMNS - 1));
		int col = month % COLUMNS;
		int row = month / COLUMNS;
		return new int[]{col * (monthW + gap), row * (LABEL + 6 * (cell + 1) + GAP)};
	}

	/** The day under a point, or null. */
	private LocalDate dayAt(int x, int y)
	{
		int cell = cell(getWidth());
		for (int m = 0; m < 12; m++)
		{
			int[] o = origin(m, cell);
			LocalDate start = LocalDate.of(year, m + 1, 1);
			int lead = start.getDayOfWeek().getValue() - 1;
			int cx = (x - o[0]) / (cell + 1);
			int cy = (y - o[1] - LABEL) / (cell + 1);
			if (x < o[0] || y < o[1] + LABEL || cx >= 7 || cy >= 6)
			{
				continue;
			}
			int index = cy * 7 + cx - lead;
			if (index >= 0 && index < start.lengthOfMonth())
			{
				return start.plusDays(index);
			}
		}
		return null;
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		LocalDate day = dayAt(e.getX(), e.getY());
		if (day == null || day.toEpochDay() > today || day.toEpochDay() < first)
		{
			return null;
		}
		long d = day.toEpochDay();
		return TIP.format(day) + (played.contains(d) ? streak.contains(d) ? ": played, on your streak" : ": played" : d == today ? ": not played yet" : ": not played");
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		int cell = cell(getWidth());
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		for (int m = 0; m < 12; m++)
		{
			int[] o = origin(m, cell);
			LocalDate start = LocalDate.of(year, m + 1, 1);
			boolean any = false;
			int lead = start.getDayOfWeek().getValue() - 1;
			for (int i = 0; i < start.lengthOfMonth(); i++)
			{
				long d = start.toEpochDay() + i;
				if (d > today || d < first)
				{
					continue;
				}
				any = true;
				int slot = lead + i;
				int x = o[0] + (slot % 7) * (cell + 1);
				int y = o[1] + LABEL + (slot / 7) * (cell + 1);
				Color c = streak.contains(d) ? Theme.TEAL : played.contains(d) ? Theme.GOLD : Theme.STROKE;
				g.setColor(c);
				g.fillRect(x, y, cell, cell);
				if (d == today)
				{
					g.setColor(Theme.GOLD_BRIGHT);
					g.drawRect(x - 1, y - 1, cell + 1, cell + 1);
				}
			}
			String name = start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
			g.setColor(any ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
			g.drawString(name, o[0], o[1] + fm.getAscent() - 2);
		}
		g.dispose();
	}
}
