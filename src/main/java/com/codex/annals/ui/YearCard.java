package com.codex.annals.ui;

import com.codex.annals.core.Entry;
import com.codex.annals.core.Recap;
import com.codex.annals.core.Wealth;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Year in Gielinor card: a whole year on one gilt-framed page, drawn at twice the panel's
 * scale so the game's pixel fonts stay crisp wherever it is posted.
 */
public final class YearCard
{
	static final int WIDTH = 360;
	static final int SCALE = 2;
	private static final int PAD = 20;
	private static final int SKILLS = 4;
	private static final int BOSSES = 4;
	private static final int HIGHLIGHTS = 4;

	private YearCard()
	{
	}

	/** Renders the card; icons still loading are left out and onLoaded runs when one arrives. */
	public static BufferedImage render(Recap r, Wealth wealth, boolean showName, ItemIcons items, Icons skills, Runnable onLoaded)
	{
		BufferedImage scratch = new BufferedImage(WIDTH, 2000, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = scratch.createGraphics();
		int height = content(sg, r, wealth, showName, items, skills, onLoaded);
		sg.dispose();

		BufferedImage image = new BufferedImage(WIDTH * SCALE, height * SCALE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.scale(SCALE, SCALE);
		Theme.smooth(g);
		frame(g, height);
		content(g, r, wealth, showName, items, skills, onLoaded);
		g.dispose();
		return image;
	}

	/** The gilt page both cards are drawn on. */
	static void frame(Graphics2D g, int h)
	{
		int w = WIDTH;
		RoundRectangle2D page = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.CARD_TOP, 0.5), 0, h, Theme.INK_DEEP));
		g.fill(page);
		g.setClip(page);
		g.setPaint(new RadialGradientPaint(w / 2f, 70, w * 0.75f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(Theme.GOLD, 58), Theme.alpha(Theme.GOLD, 0)}));
		g.fillRect(0, 0, w, h);

		// A faint grain, the same every time, so the page reads as vellum rather than a flat fill.
		Random grain = new Random(11);
		for (int i = 0; i < w * h / 26; i++)
		{
			g.setColor(Theme.alpha(grain.nextBoolean() ? Theme.GOLD_BRIGHT : Color.BLACK, 6 + grain.nextInt(8)));
			g.fillRect(grain.nextInt(w), grain.nextInt(h), 1, 1);
		}

		// The mark engraved faintly in the corner: lines only, so it never muddies what sits over it.
		Sigil.engrave(g, w - 196, h - 206, 180, Theme.alpha(Theme.GOLD, 16));
		g.setClip(null);

		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 190));
		g.draw(new RoundRectangle2D.Float(4.5f, 4.5f, w - 9, h - 9, 14, 14));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 120));
		g.draw(new RoundRectangle2D.Float(8.5f, 8.5f, w - 17, h - 17, 10, 10));
		float[][] corners = {{14, 14}, {w - 14, 14}, {14, h - 14}, {w - 14, h - 14}};
		for (float[] c : corners)
		{
			diamond(g, c[0], c[1], 3.5f, Theme.GOLD);
		}
	}

	/** "+26", or a plain "0" for a year with no level yet, rather than "+0". */
	static String levels(Recap r)
	{
		return r.getLevelsGained() > 0 ? "+" + Format.number(r.getLevelsGained()) : "0";
	}

	/** The experience under the levels, or "none yet" for a year with none, rather than "+0 xp". */
	static String levelsNote(Recap r)
	{
		return r.getXpGained() > 0 ? Format.xp(r.getXpGained()) + " xp" : "none yet";
	}

	/** Lays out and draws everything inside the frame; returns the page height it needs. */
	private static int content(Graphics2D g, Recap r, Wealth wealth, boolean showName, ItemIcons items, Icons skills, Runnable onLoaded)
	{
		Theme.smooth(g);
		int x = PAD;
		int w = WIDTH - PAD * 2;
		int y = PAD + 4;

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		Sigil.paint(g, x, y - 2, 14);
		g.setColor(Theme.GOLD);
		g.drawString("CODEX ANNALS", x + 19, y + small.getAscent() - 1);
		String kind = "A YEAR IN GIELINOR";
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(kind, x + w - small.stringWidth(kind), y + small.getAscent() - 1);
		y += small.getHeight() + 10;

		// The year itself, large, in gilt.
		Font huge = Theme.title().deriveFont(Theme.title().getSize2D() * 4);
		g.setFont(huge);
		FontMetrics hm = g.getFontMetrics();
		String year = String.valueOf(r.getYear());
		int yw = hm.stringWidth(year);
		int yx = x + (w - yw) / 2;
		int base = y + hm.getAscent() - 12;
		g.setColor(Color.BLACK);
		g.drawString(year, yx + 2, base + 2);
		g.setPaint(new GradientPaint(0, base - hm.getAscent() + 14, Theme.GOLD_BRIGHT, 0, base, Theme.GOLD_DIM));
		g.drawString(year, yx, base);
		ornament(g, x, yx - 10, base - hm.getAscent() / 2 + 6);
		ornament(g, yx + yw + 10, x + w, base - hm.getAscent() / 2 + 6);
		y = base + 8;

		if (showName && r.getPlayer() != null)
		{
			g.setFont(Theme.title());
			FontMetrics bold = g.getFontMetrics();
			centered(g, Ui.clip(r.getPlayer(), bold, w), x, w, y + bold.getAscent(), Theme.TEXT);
			y += bold.getHeight();
		}
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		centered(g, Ui.clip(r.getHeadline(), body, w), x, w, y + body.getAscent(), Theme.GOLD_BRIGHT);
		y += body.getHeight();
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_MUTED);
		// The days are the first tile just below, so this line tells the hours alone.
		String when = Format.hours(r.getPlayedMs()) + " played";
		g.drawString(when, x + (w - small.stringWidth(when)) / 2, y + small.getAscent());
		y += small.getHeight() + 8;
		rule(g, x, y, w);
		y += 10;

		int tileW = (w - 12) / 3;
		int tileH = 54;
		StatTile.paint(g, Format.number(r.getDaysPlayed()), "Days played",
			r.getLongestStreak() > 1 ? "best run " + r.getLongestStreak() : "", Theme.TEAL, x, y, tileW, tileH);
		StatTile.paint(g, levels(r), "Levels", levelsNote(r), Theme.GREEN, x + tileW + 6, y, tileW, tileH);
		List<Recap.KillYear> bosses = r.getBosses();
		StatTile.paint(g, Format.number(r.getKills()), "Boss kills", bosses.isEmpty() ? "none yet" : bosses.get(0).getName(), Theme.KILLS,
			x + (tileW + 6) * 2, y, w - (tileW + 6) * 2, tileH);
		y += tileH + 8;

		y = tallies(g, r, wealth, x, y, w) + 10;

		header(g, "Months", r.busiestMonth() >= 0 ? "busiest " + Format.monthName(r.busiestMonth()) : null, x, y, w);
		y += 24;
		MonthStrip.paint(g, r.getMonthDays(), -1, -1, r.getFirstMonth(), x, y, w, MonthStrip.HEIGHT);
		y += MonthStrip.HEIGHT + 10;

		List<Recap.SkillYear> skillLines = r.getSkills();
		boolean twoColumns = !skillLines.isEmpty() && !bosses.isEmpty();
		int colW = twoColumns ? (w - 14) / 2 : w;
		int top = y;
		int left = top;
		if (!skillLines.isEmpty())
		{
			header(g, "Skills", null, x, top, colW);
			left += 24;
			long most = skillLines.get(0).getGained();
			int shown = Math.min(SKILLS, skillLines.size());
			// Levels go on every line or none: one row with them and three without reads as a mistake.
			boolean levels = true;
			for (int i = 0; i < shown; i++)
			{
				levels &= SkillYearRow.levelsFit(g, skillLines.get(i), colW);
			}
			for (int i = 0; i < shown; i++)
			{
				Recap.SkillYear line = skillLines.get(i);
				SkillYearRow.paint(g, line, skills == null ? null : skills.skill(line.getSkill()), line.getGained() / (double) most, x, left, colW, levels);
				left += 30;
			}
		}
		int right = top;
		if (!bosses.isEmpty())
		{
			int bx = twoColumns ? x + colW + 14 : x;
			int by = twoColumns ? top : left + 4;
			header(g, "Bosses", null, bx, by, colW);
			by += 24;
			int most = bosses.get(0).getKills();
			for (int i = 0; i < bosses.size() && i < BOSSES; i++)
			{
				// Side by side there is no room for the game's count as well; the name matters more.
				KillYearRow.paint(g, bosses.get(i), bosses.get(i).getKills() / (double) most, bx, by, colW, !twoColumns);
				by += 30;
			}
			right = by;
		}
		y = Math.max(left, right) + 4;

		List<Entry> highlights = r.getHighlightsByDate(HIGHLIGHTS);
		if (!highlights.isEmpty())
		{
			header(g, "Highlights", null, x, y, w);
			y += 24;
			for (int i = 0; i < highlights.size() && i < HIGHLIGHTS; i++)
			{
				Entry e = highlights.get(i);
				Color color = Theme.entry(e.getKind());
				EntryRow.node(g, e.getKind(), x + 6, y + 7, color);
				int tx = x + 20;
				int edge = x + w;
				BufferedImage icon = Views.icon(e, items, skills, onLoaded);
				if (icon != null)
				{
					int box = 26;
					double scale = Math.min(1, box / (double) Math.max(icon.getWidth(), icon.getHeight()));
					int iw = (int) Math.round(icon.getWidth() * scale);
					int ih = (int) Math.round(icon.getHeight() * scale);
					g.drawImage(icon, edge - box + (box - iw) / 2, y - 4 + (box - ih) / 2, iw, ih, null);
					edge -= box + 6;
				}
				String date = Format.shortDay(java.time.Instant.ofEpochMilli(e.getAt()).atZone(java.time.ZoneId.systemDefault()).toLocalDate());
				g.setFont(Theme.body());
				FontMetrics fm = g.getFontMetrics();
				Ui.shadowed(g, Ui.clip(e.getTitle(), fm, edge - tx), tx, y + fm.getAscent() - 2, Theme.mix(Theme.TEXT, color, 0.3));
				g.setFont(Theme.small());
				String detail = EntryRow.detail(e, r.timesOf(e));
				String line = detail == null ? date : date + EntryRow.DOT + detail;
				g.setColor(Theme.TEXT_MUTED);
				g.drawString(Ui.clip(line, small, edge - tx), tx, y + fm.getAscent() + small.getAscent());
				y += 32;
			}
		}

		y += 2;
		rule(g, x, y, w);
		y += 8;
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString("Codex Annals for RuneLite", x, y + small.getAscent());
		String since = "Kept since " + Format.since(r.getBegunAt());
		g.drawString(since, x + w - small.stringWidth(since), y + small.getAscent());
		y += small.getHeight();
		return y + PAD;
	}

	/** A row of small medallions for the year's quests, diaries, log slots, pets and clues. */
	private static int tallies(Graphics2D g, Recap r, Wealth wealth, int x, int y, int w)
	{
		int[] counts = {r.getQuests(), r.getDiaries(), r.getCollection(), r.getPets(), r.getClues()};
		String[] ones = {"quest", "diary", "log slot", "pet", "clue"};
		String[] many = {"quests", "diaries", "log slots", "pets", "clues"};
		Color[] all = {Theme.FEAT, Theme.FEAT, Theme.TEAL, Theme.VIOLET, Theme.GOLD};
		// A row of noughts says nothing on a card meant to be shared; only what the year holds is shown.
		List<String[]> kept = new ArrayList<>();
		List<Color> keptColors = new ArrayList<>();
		for (int i = 0; i < counts.length; i++)
		{
			if (counts[i] > 0)
			{
				kept.add(new String[]{Format.number(counts[i]), counts[i] == 1 ? ones[i] : many[i]});
				keptColors.add(all[i]);
			}
		}
		if (wealth != null && wealth.change() != 0 && !wealth.isPartial())
		{
			// Worth from Codex Ledger, when it kept the whole year.
			kept.add(new String[]{Format.signedCompact(wealth.change()), "worth"});
			keptColors.add(wealth.change() > 0 ? Theme.GOLD_BRIGHT : Theme.EMBER);
		}
		if (kept.isEmpty())
		{
			return y - 10;
		}
		String[][] items = kept.toArray(new String[0][]);
		Color[] colors = keptColors.toArray(new Color[0]);
		int gap = 4;
		// Medallions keep the size five would have, centred, so one alone does not stretch across the page.
		int slots = Math.max(5, items.length);
		float cell = (w - gap * (slots - 1)) / (float) slots;
		x += Math.round((w - (cell * items.length + gap * (items.length - 1))) / 2);
		int h = 34;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		for (int i = 0; i < items.length; i++)
		{
			float cx = x + i * (cell + gap);
			RoundRectangle2D shape = new RoundRectangle2D.Float(cx + 0.5f, y + 0.5f, cell - 1, h - 1, 8, 8);
			g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
			g.fill(shape);
			g.setColor(Theme.alpha(colors[i], 110));
			g.draw(shape);
			g.setFont(Theme.title());
			FontMetrics bold = g.getFontMetrics();
			String value = items[i][0];
			Ui.shadowed(g, value, Math.round(cx + (cell - bold.stringWidth(value)) / 2), y + bold.getAscent() + 1, colors[i]);
			g.setFont(Theme.small());
			g.setColor(Theme.TEXT_MUTED);
			String label = items[i][1];
			g.drawString(label, Math.round(cx + (cell - small.stringWidth(label)) / 2), y + h - 4);
		}
		return y + h;
	}

	private static void centered(Graphics2D g, String text, int x, int w, int baseline, Color color)
	{
		FontMetrics fm = g.getFontMetrics();
		Ui.shadowed(g, text, x + (w - fm.stringWidth(text)) / 2, baseline, color);
	}

	/** A gilt line ending in a diamond, either side of the year. */
	private static void ornament(Graphics2D g, int from, int to, int y)
	{
		if (to - from < 12)
		{
			return;
		}
		boolean towardRight = from < WIDTH / 2;
		g.setStroke(new BasicStroke(1f));
		g.setPaint(new GradientPaint(from, 0, Theme.alpha(Theme.GOLD, towardRight ? 0 : 170), to, 0, Theme.alpha(Theme.GOLD, towardRight ? 170 : 0)));
		g.drawLine(from, y, to, y);
		diamond(g, towardRight ? to : from, y, 3, Theme.GOLD);
	}

	private static void header(Graphics2D g, String text, String trailing, int x, int y, int w)
	{
		g.setFont(Theme.small());
		SectionHeader.paint(g, text.toUpperCase(), trailing, x, y, w, 20);
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
