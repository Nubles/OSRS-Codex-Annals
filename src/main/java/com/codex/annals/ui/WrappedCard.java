package com.codex.annals.ui;

import com.codex.annals.core.Archetype;
import com.codex.annals.core.DropRates;
import com.codex.annals.core.Entry;
import com.codex.annals.core.Recap;
import com.codex.annals.core.Wrapped;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The cards of Gielinor Wrapped: each one figure of the year on a gilt page of its own, a story
 * format (four by five) drawn at twice the panel's scale so the game's pixel fonts stay crisp, and
 * the whole story stacked into one tall picture to save or share.
 */
public final class WrappedCard
{
	static final int WIDTH = YearCard.WIDTH;
	static final int HEIGHT = 450;
	static final int SCALE = 2;
	/** The space between cards in the tall picture of the whole story. */
	static final int GAP = 14;
	private static final int PAD = 22;
	private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);
	private static final String LETTERS = "JFMAMJJASOND";

	private WrappedCard()
	{
	}

	/** One card of the story, at twice the scale. */
	public static BufferedImage render(Wrapped w, int index, String player, ItemIcons items, Icons skills, ZoneId zone, Runnable onLoaded)
	{
		BufferedImage image = new BufferedImage(WIDTH * SCALE, HEIGHT * SCALE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.scale(SCALE, SCALE);
		paint(g, w, index, player, items, skills, zone, onLoaded);
		g.dispose();
		return image;
	}

	/** The whole story as one tall picture, every card in order on a dark sheet. */
	public static BufferedImage renderAll(Wrapped w, String player, ItemIcons items, Icons skills, ZoneId zone, Runnable onLoaded)
	{
		int n = w.getCards().size();
		int height = GAP + n * (HEIGHT + GAP);
		int width = WIDTH + GAP * 2;
		BufferedImage image = new BufferedImage(width * SCALE, height * SCALE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.scale(SCALE, SCALE);
		g.setColor(Theme.INK_DEEP);
		g.fillRect(0, 0, width, height);
		for (int i = 0; i < n; i++)
		{
			Graphics2D one = (Graphics2D) g.create();
			one.translate(GAP, GAP + i * (HEIGHT + GAP));
			paint(one, w, i, player, items, skills, zone, onLoaded);
			one.dispose();
		}
		g.dispose();
		return image;
	}

	static void paint(Graphics2D g, Wrapped w, int index, String player, ItemIcons items, Icons skills, ZoneId zone, Runnable onLoaded)
	{
		Theme.smooth(g);
		Wrapped.Card card = w.getCards().get(index);
		Color accent = accent(w, card);
		int top = PAD + 34;
		// The card's words are laid out once on their own: to centre them between the header and the
		// footer, and so the engraved mark keeps clear of them.
		BufferedImage measure = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D mg = measure.createGraphics();
		Theme.smooth(mg);
		int end = body(mg, card, w, player, items, skills, zone, top, () -> { });
		mg.dispose();
		int shift = Math.max(0, (HEIGHT - PAD - 34 - end) / 2);
		BufferedImage ink = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig = ink.createGraphics();
		Theme.smooth(ig);
		header(ig, w, index);
		body(ig, card, w, player, items, skills, zone, top + shift, () -> { });
		footer(ig, w, index, player);
		ig.dispose();

		YearCard.frame(g, HEIGHT, ink);
		// Each card glows in its own colour behind its figure, so paging through them reads as turning a page.
		java.awt.Shape outside = g.getClip();
		g.clip(new RoundRectangle2D.Float(0, 0, WIDTH, HEIGHT, 18, 18));
		g.setPaint(new RadialGradientPaint(WIDTH / 2f, top + shift + 110, WIDTH * 0.62f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(accent, 46), Theme.alpha(accent, 0)}));
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setClip(outside);
		header(g, w, index);
		body(g, card, w, player, items, skills, zone, top + shift, onLoaded);
		footer(g, w, index, player);
	}

	/** Draws a card's own figures from a height down; returns where they end. */
	private static int body(Graphics2D g, Wrapped.Card card, Wrapped w, String player, ItemIcons items, Icons skills, ZoneId zone, int top,
		Runnable onLoaded)
	{
		switch (card)
		{
			case OPENING:
				return opening(g, w, player, top);
			case SKILL:
				return skill(g, w, skills, top);
			case DROP:
				return drop(g, w, items, skills, zone, top, onLoaded);
			case DAYS:
				return days(g, w, top);
			case ARCHETYPE:
				return archetype(g, w, top);
			case MONTHS:
				return months(g, w, top);
			default:
				return closing(g, w, player, top);
		}
	}

	static Color accent(Wrapped w, Wrapped.Card card)
	{
		switch (card)
		{
			case SKILL:
				return Theme.GREEN;
			case DROP:
				return w.getDrop() != null && w.getDrop().getKind() == Entry.Kind.PET ? Theme.VIOLET : Theme.GOLD_BRIGHT;
			case DAYS:
				return Theme.TEAL;
			case ARCHETYPE:
				return Theme.EMBER;
			case MONTHS:
				return Theme.TEAL;
			default:
				return Theme.GOLD;
		}
	}

	private static void header(Graphics2D g, Wrapped w, int index)
	{
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int y = PAD + 2;
		Sigil.paint(g, PAD, y - 2, 14);
		g.setColor(Theme.GOLD);
		g.drawString("GIELINOR WRAPPED", PAD + 19, y + small.getAscent() - 1);
		String right = w.getYear() + "  " + (index + 1) + "/" + w.getCards().size();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(right, WIDTH - PAD - small.stringWidth(right), y + small.getAscent() - 1);
	}

	private static void footer(Graphics2D g, Wrapped w, int index, String player)
	{
		int n = w.getCards().size();
		int y = HEIGHT - PAD - 2;
		float step = 12;
		float x0 = WIDTH / 2f - step * (n - 1) / 2f;
		for (int i = 0; i < n; i++)
		{
			diamond(g, x0 + i * step, y - 14, i == index ? 3.5f : 2.2f, i == index ? Theme.GOLD_BRIGHT : Theme.alpha(Theme.GOLD_DIM, 170));
		}
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_FAINT);
		g.drawString("Codex Annals", PAD, y);
		if (player != null)
		{
			String name = Ui.clip(player, small, WIDTH / 2 - PAD - 30);
			g.drawString(name, WIDTH - PAD - small.stringWidth(name), y);
		}
	}

	// ---- the cards ------------------------------------------------------------------------

	private static int opening(Graphics2D g, Wrapped w, String player, int y)
	{
		Recap r = w.getRecap();
		int cx = WIDTH / 2;
		g.setFont(Theme.small());
		y += 18;
		spaced(g, player != null ? "THE YEAR OF " + player.toUpperCase(Locale.ENGLISH) : "YOUR YEAR IN GIELINOR", cx, y, Theme.TEXT_MUTED);
		y += 22;
		Font huge = scaled(Theme.title(), 5);
		g.setFont(huge);
		FontMetrics hm = g.getFontMetrics();
		String year = String.valueOf(r.getYear());
		int base = y + hm.getAscent() - 18;
		int yx = cx - hm.stringWidth(year) / 2;
		g.setColor(Color.BLACK);
		g.drawString(year, yx + 2, base + 3);
		g.setPaint(new GradientPaint(0, base - hm.getAscent() + 20, Theme.GOLD_BRIGHT, 0, base, Theme.GOLD_DIM));
		g.drawString(year, yx, base);
		y = base + 22;
		g.setFont(Theme.body());
		centred(g, Ui.clip(r.getHeadline(), g.getFontMetrics(), WIDTH - PAD * 2), cx, y, Theme.GOLD_BRIGHT);
		y += 16;
		rule(g, PAD + 30, y, WIDTH - PAD * 2 - 60);
		y += 18;

		String[][] figures = {
			played(r.getPlayedMs()),
			{Format.number(r.getDaysPlayed()), r.getDaysPlayed() == 1 ? "day logged in" : "days logged in"},
			{Format.amount(r.getXpGained()), "experience gained"},
			{Format.number(r.getLevelsGained()), r.getLevelsGained() == 1 ? "level gained" : "levels gained"},
		};
		Color[] colors = {Theme.GOLD_BRIGHT, Theme.TEAL, Theme.TEAL, Theme.GREEN};
		int colW = (WIDTH - PAD * 2 - 12) / 2;
		int rowH = 66;
		for (int i = 0; i < figures.length; i++)
		{
			int x = PAD + (i % 2) * (colW + 12);
			int ty = y + (i / 2) * (rowH + 10);
			tile(g, figures[i][0], figures[i][1], colors[i], x, ty, colW, rowH);
		}
		y += rowH * 2 + 10 + 22;
		g.setFont(Theme.small());
		centred(g, "Page through to see how it went.", cx, y, Theme.TEXT_FAINT);
		return y;
	}

	private static int skill(Graphics2D g, Wrapped w, Icons skills, int y)
	{
		Recap.SkillYear top = w.getTopSkill();
		int cx = WIDTH / 2;
		g.setFont(Theme.small());
		y += 18;
		spaced(g, "YOUR SKILL OF THE YEAR", cx, y, Theme.TEXT_MUTED);
		y += 56;
		medallion(g, cx, y, Theme.GREEN, skills == null ? null : skills.skill(top.getSkill()), 30);
		y += 76;
		g.setFont(scaled(Theme.title(), 3));
		FontMetrics big = g.getFontMetrics();
		centred(g, Ui.clip(top.getSkill(), big, WIDTH - PAD * 2), cx, y, Theme.TEXT);
		y += 22;
		g.setFont(Theme.title());
		String gained = Format.xp(top.getGained()) + " xp";
		centred(g, gained, cx, y, Theme.GREEN);
		y += 15;
		g.setFont(Theme.small());
		String share = Math.round(w.topSkillShare() * 100) + "% of all your experience this year";
		String levels = top.getEndLevel() > top.getStartLevel() ? "Level " + top.getStartLevel() + " to " + top.getEndLevel() + ", " + Format.lower(share)
			: share.substring(0, 1).toUpperCase(Locale.ENGLISH) + share.substring(1);
		centred(g, Ui.clip(levels, g.getFontMetrics(), WIDTH - PAD * 2), cx, y, Theme.TEXT_MUTED);
		y += 22;
		Wrapped.Climb climb = w.getClimb();
		if (climb != null)
		{
			rule(g, PAD + 30, y, WIDTH - PAD * 2 - 60);
			y += 22;
			g.setFont(Theme.small());
			spaced(g, "BIGGEST CLIMB IN A DAY", cx, y, Theme.TEXT_MUTED);
			y += 28;
			g.setFont(scaled(Theme.title(), 2));
			String span = climb.getSkill() + "  " + climb.getFrom() + " > " + climb.getTo();
			FontMetrics fm = g.getFontMetrics();
			span = fm.stringWidth(span) > WIDTH - PAD * 2 ? climb.getFrom() + " > " + climb.getTo() : span;
			centred(g, span, cx, y, Theme.GOLD_BRIGHT);
			y += 18;
			g.setFont(Theme.small());
			String when = Format.plural(climb.levels(), "level", "levels") + " on " + WEEKDAY.format(climb.getDay());
			centred(g, when, cx, y, Theme.TEXT_MUTED);
			return y;
		}
		else
		{
			// Without a level written down, the rest of the year's skills stand under the first.
			List<Recap.SkillYear> rest = w.getRecap().getSkills();
			int shown = 0;
			for (int i = 1; i < rest.size() && shown < 3; i++, shown++)
			{
				g.setFont(Theme.small());
				centred(g, rest.get(i).getSkill() + "  " + Format.xp(rest.get(i).getGained()), cx, y + shown * 15, Theme.TEXT_MUTED);
			}
			return y + Math.max(0, shown - 1) * 15;
		}
	}

	private static int drop(Graphics2D g, Wrapped w, ItemIcons items, Icons skills, ZoneId zone, int y, Runnable onLoaded)
	{
		Entry e = w.getDrop();
		DropRates.Rate rate = w.getRate();
		boolean pet = e.getKind() == Entry.Kind.PET;
		Color tint = pet ? Theme.VIOLET : Theme.GOLD_BRIGHT;
		int cx = WIDTH / 2;
		g.setFont(Theme.small());
		y += 18;
		spaced(g, rate != null ? "THE RAREST DROP OF YOUR YEAR" : pet ? "A PET CAME HOME" : "THE FINEST DROP OF YOUR YEAR", cx, y, Theme.TEXT_MUTED);
		y += 60;
		medallion(g, cx, y, tint, Views.icon(e, items, skills, onLoaded), 34);
		y += 80;
		g.setFont(scaled(Theme.title(), 2));
		FontMetrics big = g.getFontMetrics();
		String title = e.getTitle();
		if (big.stringWidth(title) > WIDTH - PAD * 2)
		{
			g.setFont(Theme.title());
			big = g.getFontMetrics();
		}
		centred(g, Ui.clip(title, big, WIDTH - PAD * 2), cx, y, Theme.TEXT);
		y += 18;
		g.setFont(Theme.small());
		LocalDate day = Instant.ofEpochMilli(e.getAt()).atZone(zone).toLocalDate();
		String source = rate != null ? rate.getSource() : sourceOf(e);
		String when = WEEKDAY.format(day) + (source != null ? ", from " + source : "");
		centred(g, Ui.clip(when, g.getFontMetrics(), WIDTH - PAD * 2), cx, y, Theme.TEXT_MUTED);
		if (rate != null)
		{
			y += 50;
			g.setFont(scaled(Theme.title(), 3));
			centred(g, rate.oneIn(), cx, y, tint);
			y += 18;
			g.setFont(Theme.small());
			centred(g, "Drop rate " + rate.getText() + " on the OSRS Wiki", cx, y, Theme.TEXT_FAINT);
		}
		if (e.getValue() > 0)
		{
			y += 26;
			g.setFont(Theme.title());
			centred(g, "Worth " + Format.coins(e.getValue()), cx, y, Theme.GOLD);
		}
		return y;
	}

	private static String sourceOf(Entry e)
	{
		String d = e.getDetail();
		if (d == null)
		{
			return null;
		}
		if (d.startsWith("From "))
		{
			return d.substring(5);
		}
		if (d.startsWith("Pet from "))
		{
			return d.substring(9);
		}
		int at = d.indexOf(" from ");
		return d.startsWith("New log slot") && at > 0 ? d.substring(at + 6) : null;
	}

	private static int days(Graphics2D g, Wrapped w, int y)
	{
		Recap r = w.getRecap();
		int cx = WIDTH / 2;
		Wrapped.Day busy = w.getBusiest();
		y += 18;
		if (busy != null)
		{
			g.setFont(Theme.small());
			spaced(g, "YOUR BIGGEST DAY", cx, y, Theme.TEXT_MUTED);
			y += 30;
			g.setFont(scaled(Theme.title(), 2));
			centred(g, WEEKDAY.format(busy.getDate()), cx, y, Theme.TEXT);
			y += 16;
			g.setFont(Theme.small());
			centred(g, busy.getCount() + " moments written down", cx, y, Theme.TEAL);
			y += 12;
			g.setFont(Theme.body());
			FontMetrics fm = g.getFontMetrics();
			int widest = 0;
			for (Entry e : busy.getBest())
			{
				widest = Math.max(widest, fm.stringWidth(Ui.clip(e.getTitle(), fm, WIDTH - PAD * 2 - 24)));
			}
			// One block of lines, centred as a whole, so the markers stand in a column.
			int left = cx - (widest + 14) / 2;
			for (Entry e : busy.getBest())
			{
				y += 16;
				Color c = Theme.entry(e.getKind());
				EntryRow.node(g, e.getKind(), left + 4, y - 4, c);
				Ui.shadowed(g, Ui.clip(e.getTitle(), fm, WIDTH - PAD * 2 - 24), left + 14, y, Theme.mix(Theme.TEXT, c, 0.3));
			}
			y += 22;
			rule(g, PAD + 30, y, WIDTH - PAD * 2 - 60);
			y += 22;
		}
		g.setFont(Theme.small());
		spaced(g, "LONGEST STREAK", cx, y, Theme.TEXT_MUTED);
		y += 40;
		g.setFont(scaled(Theme.title(), 3));
		String streak = Format.plural(r.getLongestStreak(), "day", "days");
		centred(g, streak, cx, y, Theme.TEAL);
		y += 16;
		g.setFont(Theme.small());
		LocalDate start = w.getStreakStart();
		String span = start == null ? "in a row" : "in a row, " + Format.shortDay(start) + " to " + Format.shortDay(start.plusDays(r.getLongestStreak() - 1));
		centred(g, span, cx, y, Theme.TEXT_MUTED);
		y += 14;
		return calendar(g, w, PAD, y, WIDTH - PAD * 2);
	}

	/** Every day of the year as a dot: gold where played, teal along the longest streak. */
	private static int calendar(Graphics2D g, Wrapped w, int x, int y, int width)
	{
		int height = 56;
		int year = w.getYear();
		LocalDate first = LocalDate.of(year, 1, 1);
		int lead = first.getDayOfWeek().getValue() - 1;
		int weeks = (lead + first.lengthOfYear() + 6) / 7;
		float cell = Math.min(width / (float) weeks, height / 7f);
		float dot = Math.max(2, cell - 1.2f);
		float left = x + (width - cell * weeks) / 2;
		Set<Long> played = new HashSet<>(w.getRecap().getDays());
		LocalDate s = w.getStreakStart();
		long from = s == null ? Long.MAX_VALUE : s.toEpochDay();
		long to = s == null ? Long.MIN_VALUE : from + w.getRecap().getLongestStreak() - 1;
		for (int d = 0; d < first.lengthOfYear(); d++)
		{
			int slot = lead + d;
			float px = left + (slot / 7) * cell;
			float py = y + (slot % 7) * cell;
			long day = first.toEpochDay() + d;
			Color c = day >= from && day <= to ? Theme.TEAL : played.contains(day) ? Theme.GOLD : Theme.alpha(Theme.STROKE, 150);
			g.setColor(c);
			g.fill(new RoundRectangle2D.Float(px, py, dot, dot, 1.5f, 1.5f));
		}
		// The month letters under the week each month begins in.
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int base = Math.round(y + cell * 7) + fm.getAscent() + 2;
		for (int m = 0; m < 12; m++)
		{
			int slot = lead + LocalDate.of(year, m + 1, 1).getDayOfYear() - 1;
			String letter = LETTERS.substring(m, m + 1);
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(letter, Math.round(left + (slot / 7) * cell + cell * 2 - fm.stringWidth(letter) / 2f), base);
		}
		return base;
	}

	private static int archetype(Graphics2D g, Wrapped w, int y)
	{
		Archetype a = w.getArchetype();
		int cx = WIDTH / 2;
		g.setFont(Theme.small());
		y += 18;
		spaced(g, "THIS YEAR YOU WERE", cx, y, Theme.TEXT_MUTED);
		y += 70;
		emblem(g, cx, y, Theme.EMBER);
		y += 96;
		g.setFont(scaled(Theme.title(), 3));
		FontMetrics big = g.getFontMetrics();
		if (big.stringWidth(a.getTitle()) > WIDTH - PAD * 2)
		{
			g.setFont(scaled(Theme.title(), 2));
			big = g.getFontMetrics();
		}
		centred(g, a.getTitle(), cx, y, Theme.GOLD_BRIGHT);
		y += 20;
		g.setFont(Theme.body());
		for (String line : wrap(a.getMotto(), g.getFontMetrics(), WIDTH - PAD * 2 - 20))
		{
			centred(g, line, cx, y, Theme.TEXT);
			y += 15;
		}
		y += 2;
		g.setFont(Theme.small());
		centred(g, Ui.clip(w.getArchetypeReason(), g.getFontMetrics(), WIDTH - PAD * 2), cx, y, Theme.TEXT_MUTED);
		y += 18;
		double[] shares = w.getShares();
		if (shares[0] + shares[1] + shares[2] > 0)
		{
			shareBar(g, shares, PAD + 10, y, WIDTH - PAD * 2 - 20);
			y += 22;
		}
		return y;
	}

	/** Where the experience went: the craft, combat and Slayer, as one bar with its legend under it. */
	private static void shareBar(Graphics2D g, double[] shares, int x, int y, int w)
	{
		Color[] colors = {Theme.GREEN, Theme.EMBER, Theme.VIOLET};
		String[] names = {"Skilling", "Combat", "Slayer"};
		int h = 8;
		g.setColor(Theme.INK_DEEP);
		g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
		java.awt.Shape clip = g.getClip();
		g.clip(new RoundRectangle2D.Float(x, y, w, h, h, h));
		float at = x;
		for (int i = 0; i < 3; i++)
		{
			float part = (float) (w * shares[i]);
			g.setPaint(new GradientPaint(0, y, colors[i], 0, y + h, Theme.mix(colors[i], Theme.INK, 0.45)));
			g.fill(new java.awt.geom.Rectangle2D.Float(at, y, part, h));
			at += part;
		}
		g.setClip(clip);
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 160));
		g.draw(new RoundRectangle2D.Float(x, y, w, h, h, h));
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		List<String> parts = new ArrayList<>();
		List<Color> partColors = new ArrayList<>();
		for (int i = 0; i < 3; i++)
		{
			if (shares[i] >= 0.005)
			{
				parts.add(names[i] + " " + Math.round(shares[i] * 100) + "%");
				partColors.add(colors[i]);
			}
		}
		int total = 0;
		for (String p : parts)
		{
			total += fm.stringWidth(p) + 10;
		}
		total += 12 * Math.max(0, parts.size() - 1) - 0;
		int lx = x + (w - total) / 2;
		int ly = y + h + 14;
		for (int i = 0; i < parts.size(); i++)
		{
			g.setColor(partColors.get(i));
			g.fill(new Ellipse2D.Float(lx, ly - 6, 6, 6));
			g.setColor(Theme.TEXT_MUTED);
			g.drawString(parts.get(i), lx + 10, ly);
			lx += fm.stringWidth(parts.get(i)) + 10 + 12;
		}
	}

	private static int months(Graphics2D g, Wrapped w, int y)
	{
		Recap r = w.getRecap();
		int cx = WIDTH / 2;
		long[] xp = w.getMonthXp();
		int[] days = r.getMonthDays();
		boolean byXp = w.bestMonth() >= 0;
		double[] values = new double[12];
		for (int m = 0; m < 12; m++)
		{
			values[m] = byXp ? xp[m] : days[m];
		}
		int best = 0;
		for (int m = 1; m < 12; m++)
		{
			best = values[m] > values[best] ? m : best;
		}
		g.setFont(Theme.small());
		y += 18;
		spaced(g, "MONTH BY MONTH", cx, y, Theme.TEXT_MUTED);
		y += 30;
		g.setFont(scaled(Theme.title(), 2));
		centred(g, Format.monthName(best), cx, y, Theme.TEXT);
		y += 16;
		g.setFont(Theme.small());
		String what = byXp ? "your biggest month, " + Format.xp(xp[best]) + " xp" : "your busiest month, " + Format.plural(days[best], "day", "days") + " played";
		centred(g, what, cx, y, Theme.TEAL);
		y += 18;
		int chartX = PAD + 4;
		int chartW = WIDTH - PAD * 2 - 8;
		int chartH = 118;
		sparkline(g, values, best, r.getFirstMonth(), chartX, y, chartW, chartH);
		y += chartH + 20;
		// Days played each month under it, as small bars, so the time and the experience can be read together.
		int max = 1;
		for (int d : days)
		{
			max = Math.max(max, d);
		}
		float step = chartW / 11f;
		for (int m = 0; m < 12; m++)
		{
			float mx = chartX + m * step;
			float bh = days[m] == 0 ? 1 : Math.max(2, 26f * days[m] / max);
			g.setColor(days[m] == 0 ? Theme.alpha(Theme.STROKE, 160) : Theme.alpha(Theme.TEAL, m == best ? 230 : 150));
			g.fill(new RoundRectangle2D.Float(mx - 4, y + 26 - bh, 8, bh, 3, 3));
		}
		y += 40;
		g.setFont(Theme.small());
		centred(g, byXp ? "Line: experience. Bars: days played." : "Days played each month", cx, y, Theme.TEXT_FAINT);
		return y;
	}

	/** The year's months as a gilt line over a soft fill, the best month marked, the month letters beneath. */
	private static void sparkline(Graphics2D g, double[] values, int best, int first, int x, int y, int w, int h)
	{
		double max = 0;
		for (double v : values)
		{
			max = Math.max(max, v);
		}
		max = Math.max(1, max);
		int letters = 12;
		int ch = h - letters;
		float step = w / 11f;
		g.setStroke(new BasicStroke(1f));
		for (int i = 0; i <= 3; i++)
		{
			float gy = y + ch * i / 3f;
			g.setColor(Theme.alpha(Theme.STROKE, i == 3 ? 160 : 70));
			g.drawLine(x, Math.round(gy), x + w, Math.round(gy));
		}
		Path2D line = new Path2D.Float();
		Path2D area = new Path2D.Float();
		float[] px = new float[12];
		float[] py = new float[12];
		for (int m = 0; m < 12; m++)
		{
			px[m] = x + m * step;
			py[m] = (float) (y + ch - (ch - 6) * values[m] / max);
		}
		line.moveTo(px[0], py[0]);
		area.moveTo(px[0], y + ch);
		area.lineTo(px[0], py[0]);
		for (int m = 1; m < 12; m++)
		{
			// A gentle curve through each month, so the line reads as a year and not a stair.
			float mx = (px[m - 1] + px[m]) / 2;
			line.curveTo(mx, py[m - 1], mx, py[m], px[m], py[m]);
			area.curveTo(mx, py[m - 1], mx, py[m], px[m], py[m]);
		}
		area.lineTo(px[11], y + ch);
		area.closePath();
		g.setPaint(new GradientPaint(0, y, Theme.alpha(Theme.GOLD, 120), 0, y + ch, Theme.alpha(Theme.GOLD, 0)));
		g.fill(area);
		g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(new GradientPaint(x, 0, Theme.GOLD_DIM, x + w, 0, Theme.GOLD_BRIGHT));
		g.draw(line);
		g.setStroke(new BasicStroke(1f));
		for (int m = 0; m < 12; m++)
		{
			if (m == best)
			{
				g.setPaint(new RadialGradientPaint(px[m], py[m], 12, new float[]{0f, 1f},
					new Color[]{Theme.alpha(Theme.GOLD_BRIGHT, 150), Theme.alpha(Theme.GOLD_BRIGHT, 0)}));
				g.fill(new Ellipse2D.Float(px[m] - 12, py[m] - 12, 24, 24));
				diamond(g, px[m], py[m], 4.5f, Theme.GOLD_BRIGHT);
			}
			else if (values[m] > 0)
			{
				g.setColor(Theme.INK_DEEP);
				g.fill(new Ellipse2D.Float(px[m] - 2.5f, py[m] - 2.5f, 5, 5));
				g.setColor(Theme.GOLD);
				g.draw(new Ellipse2D.Float(px[m] - 2.5f, py[m] - 2.5f, 5, 5));
			}
		}
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		for (int m = 0; m < 12; m++)
		{
			String letter = LETTERS.substring(m, m + 1);
			g.setColor(m == best ? Theme.GOLD_BRIGHT : m < first ? Theme.alpha(Theme.TEXT_FAINT, 110) : Theme.TEXT_FAINT);
			g.drawString(letter, Math.round(px[m] - fm.stringWidth(letter) / 2f), y + h + 2);
		}
	}

	private static int closing(Graphics2D g, Wrapped w, String player, int y)
	{
		Recap r = w.getRecap();
		int cx = WIDTH / 2;
		g.setFont(Theme.small());
		y += 18;
		spaced(g, "AND THAT WAS", cx, y, Theme.TEXT_MUTED);
		y += 46;
		g.setFont(scaled(Theme.title(), 3));
		centred(g, "Your " + r.getYear(), cx, y, Theme.GOLD_BRIGHT);
		y += 22;
		int[] counts = {r.getMaxes(), r.getQuests(), r.getDiaries(), r.getCollection(), r.getPets(), r.getClues(), r.getKills()};
		String[] ones = {"99", "quest", "diary", "log slot", "pet", "clue", "boss kill"};
		String[] many = {"99s", "quests", "diaries", "log slots", "pets", "clues", "boss kills"};
		Color[] colors = {Theme.GOLD_BRIGHT, Theme.FEAT, Theme.FEAT, Theme.TEAL, Theme.VIOLET, Theme.GOLD, Theme.EMBER};
		List<Integer> kept = new ArrayList<>();
		for (int i = 0; i < counts.length; i++)
		{
			if (counts[i] > 0)
			{
				kept.add(i);
			}
		}
		int cols = kept.size() <= 4 ? 2 : 3;
		int gap = 8;
		int cellW = (WIDTH - PAD * 2 - gap * (cols - 1)) / cols;
		int cellH = 62;
		int rows = (kept.size() + cols - 1) / cols;
		for (int i = 0; i < kept.size(); i++)
		{
			int k = kept.get(i);
			int row = i / cols;
			int inRow = Math.min(cols, kept.size() - row * cols);
			int rowLeft = PAD + (WIDTH - PAD * 2 - (inRow * cellW + (inRow - 1) * gap)) / 2;
			int x = rowLeft + (i % cols) * (cellW + gap);
			tile(g, Format.number(counts[k]), counts[k] == 1 ? ones[k] : many[k], colors[k], x, y + row * (cellH + gap), cellW, cellH);
		}
		y += rows * (cellH + gap) + 22;
		g.setFont(Theme.body());
		centred(g, "See you in " + (r.getYear() + 1) + (player != null ? ", " + player : "") + ".", cx, y, Theme.TEXT);
		y += 16;
		g.setFont(Theme.small());
		centred(g, "Kept by Codex Annals since " + Format.since(r.getBegunAt()), cx, y, Theme.TEXT_FAINT);
		return y;
	}

	/** Hours played and their label; under an hour, minutes, so a short year never reads "0 hours". */
	static String[] played(long ms)
	{
		long hours = ms / 3_600_000;
		if (hours >= 1)
		{
			return new String[]{Format.number(hours), hours == 1 ? "hour played" : "hours played"};
		}
		long minutes = ms / 60_000;
		return new String[]{Format.number(minutes), minutes == 1 ? "minute played" : "minutes played"};
	}

	// ---- pieces -----------------------------------------------------------------------------

	/** A big figure on a dark plate with its colour along the top: the opening and closing cards' tiles. */
	private static void tile(Graphics2D g, String value, String label, Color color, int x, int y, int w, int h)
	{
		RoundRectangle2D shape = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, 10, 10);
		g.setPaint(new GradientPaint(0, y, Theme.alpha(Theme.CARD_TOP, 230), 0, y + h, Theme.alpha(Theme.INK_DEEP, 230)));
		g.fill(shape);
		g.setColor(Theme.alpha(color, 120));
		g.draw(shape);
		g.setPaint(new GradientPaint(x, 0, Theme.alpha(color, 0), x + w / 2f, 0, Theme.alpha(color, 220), true));
		g.fillRect(x + 8, y + 1, w - 16, 2);
		Font font = scaled(Theme.title(), h >= 60 ? 2 : 1.5f);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		if (fm.stringWidth(value) > w - 10)
		{
			g.setFont(Theme.title());
			fm = g.getFontMetrics();
		}
		Ui.shadowed(g, value, x + (w - fm.stringWidth(value)) / 2, y + h / 2 + fm.getAscent() / 2 - 6, color);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		String l = Ui.clip(label, small, w - 8);
		g.drawString(l, x + (w - small.stringWidth(l)) / 2, y + h - 7);
	}

	/** A gilt medallion with a soft glow, twelve studs and the picture inside. */
	private static void medallion(Graphics2D g, int cx, int cy, Color tint, BufferedImage icon, int r)
	{
		ring(g, cx, cy, tint, r);
		if (icon != null)
		{
			int box = r + 14;
			int size = Math.max(icon.getWidth(), icon.getHeight());
			double scale = size * 2 <= box ? 2 : Math.min(1.4, box / (double) size);
			int iw = (int) Math.round(icon.getWidth() * scale);
			int ih = (int) Math.round(icon.getHeight() * scale);
			Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(icon, cx - iw / 2, cy - ih / 2, iw, ih, null);
			if (old != null)
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
			}
		}
		else
		{
			Sigil.paint(g, cx - r * 0.55f, cy - r * 0.55f, r * 1.1f);
		}
	}

	/** The archetype's emblem: the Codex sigil large in a double ring of studs, glowing in the card's colour. */
	private static void emblem(Graphics2D g, int cx, int cy, Color tint)
	{
		int r = 40;
		g.setStroke(new BasicStroke(0.8f));
		for (int i = 0; i < 24; i++)
		{
			double a = Math.PI * 2 * i / 24;
			float inner = r + 9;
			float outer = r + (i % 2 == 0 ? 17 : 13);
			g.setColor(Theme.alpha(Theme.GOLD, i % 2 == 0 ? 150 : 90));
			g.drawLine(Math.round((float) (cx + Math.cos(a) * inner)), Math.round((float) (cy + Math.sin(a) * inner)),
				Math.round((float) (cx + Math.cos(a) * outer)), Math.round((float) (cy + Math.sin(a) * outer)));
		}
		ring(g, cx, cy, tint, r);
		Sigil.paint(g, cx - 24, cy - 24, 48);
	}

	private static void ring(Graphics2D g, int cx, int cy, Color tint, int r)
	{
		g.setPaint(new RadialGradientPaint(cx, cy, r * 2.4f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(tint, 80), Theme.alpha(tint, 0)}));
		g.fill(new Ellipse2D.Float(cx - r * 2.4f, cy - r * 2.4f, r * 4.8f, r * 4.8f));
		g.setPaint(new RadialGradientPaint(cx, cy - r * 0.3f, r, new float[]{0f, 1f},
			new Color[]{Theme.mix(Theme.CARD_TOP, tint, 0.2), Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(1.8f));
		g.setPaint(new GradientPaint(0, cy - r, Theme.GOLD_BRIGHT, 0, cy + r, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(0.8f));
		g.setColor(Theme.alpha(Theme.GOLD, 120));
		g.draw(new Ellipse2D.Float(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8));
		for (int i = 0; i < 12; i++)
		{
			double a = Math.PI * 2 * i / 12;
			diamond(g, (float) (cx + Math.cos(a) * (r + 5)), (float) (cy + Math.sin(a) * (r + 5)), i % 3 == 0 ? 2.6f : 1.6f,
				i % 3 == 0 ? Theme.GOLD : Theme.GOLD_DIM);
		}
	}

	/** The game's pixel font at a whole multiple, so its pixels stay square. */
	static Font scaled(Font font, float times)
	{
		return font.deriveFont(font.getSize2D() * times);
	}

	/** Small capitals with a little air between them, centred. */
	private static void spaced(Graphics2D g, String text, int cx, int baseline, Color color)
	{
		FontMetrics fm = g.getFontMetrics();
		int track = 1;
		int width = fm.stringWidth(text) + track * Math.max(0, text.length() - 1);
		int px = cx - width / 2;
		g.setColor(color);
		for (int i = 0; i < text.length(); i++)
		{
			String ch = String.valueOf(text.charAt(i));
			g.drawString(ch, px, baseline);
			px += fm.stringWidth(ch) + track;
		}
	}

	private static void centred(Graphics2D g, String text, int cx, int baseline, Color color)
	{
		FontMetrics fm = g.getFontMetrics();
		Ui.shadowed(g, text, cx - fm.stringWidth(text) / 2, baseline, color);
	}

	/** Words in lines no wider than a width. */
	static List<String> wrap(String text, FontMetrics fm, int width)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String next = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(next) > width && line.length() > 0)
			{
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(next);
			}
		}
		if (line.length() > 0)
		{
			lines.add(line.toString());
		}
		return lines;
	}

	private static void rule(Graphics2D g, int x, int y, int w)
	{
		g.setStroke(new BasicStroke(1f));
		g.setPaint(new GradientPaint(x, 0, Theme.alpha(Theme.GOLD, 0), x + w / 2f, 0, Theme.alpha(Theme.GOLD, 150), true));
		g.drawLine(x, y, x + w, y);
		diamond(g, x + w / 2f, y, 3, Theme.GOLD);
	}

	private static void diamond(Graphics2D g, float cx, float cy, float r, Color color)
	{
		Path2D d = new Path2D.Float();
		d.moveTo(cx, cy - r);
		d.lineTo(cx + r, cy);
		d.lineTo(cx, cy + r);
		d.lineTo(cx - r, cy);
		d.closePath();
		g.setColor(color);
		g.fill(d);
	}
}
