package com.codex.annals.ui;

import com.codex.annals.core.Entry;
import com.codex.annals.core.Keepsake;
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
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A keepsake: one moment of the annals on a gilt page of its own, the way a guild would letter a
 * certificate. Its picture in a medallion, its name large, the day it came, and three figures from
 * the road to it, with what else that day held. Drawn at twice the panel's scale, as the year card is.
 */
public final class KeepsakeCard
{
	private static final int PAD = 20;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
	private static final int MEDAL = 31;

	private KeepsakeCard()
	{
	}

	/** Renders the keepsake; an icon still loading is left out and onLoaded runs when it arrives. */
	public static BufferedImage render(Keepsake k, String player, long begunAt, ItemIcons items, Icons skills, Runnable onLoaded)
	{
		BufferedImage icon = Views.icon(k.getEntry(), items, skills, onLoaded);
		BufferedImage scratch = new BufferedImage(YearCard.WIDTH, 1000, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = scratch.createGraphics();
		int height = content(sg, k, player, begunAt, icon, items, skills, onLoaded);
		sg.dispose();

		BufferedImage image = new BufferedImage(YearCard.WIDTH * YearCard.SCALE, height * YearCard.SCALE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.scale(YearCard.SCALE, YearCard.SCALE);
		Theme.smooth(g);
		YearCard.frame(g, height);
		content(g, k, player, begunAt, icon, items, skills, onLoaded);
		g.dispose();
		return image;
	}

	private static int content(Graphics2D g, Keepsake k, String player, long begunAt, BufferedImage icon, ItemIcons items, Icons skills,
		Runnable onLoaded)
	{
		Theme.smooth(g);
		Entry e = k.getEntry();
		Color tint = Theme.entry(e.getKind());
		int x = PAD;
		int w = YearCard.WIDTH - PAD * 2;
		int y = PAD + 4;

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		Sigil.paint(g, x, y - 2, 14);
		g.setColor(Theme.GOLD);
		g.drawString("CODEX ANNALS", x + 19, y + small.getAscent() - 1);
		String corner = "A KEEPSAKE";
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(corner, x + w - small.stringWidth(corner), y + small.getAscent() - 1);
		y += small.getHeight() + 8;

		// The medallion, with the moment's picture at the game's own size so its pixels stay true.
		int cx = x + w / 2;
		int cy = y + MEDAL + 4;
		medallion(g, cx, cy, tint, icon, e.getKind());
		y = cy + MEDAL + 14;

		String kind = k.getKind().toUpperCase(Locale.ENGLISH);
		g.setFont(Theme.small());
		spaced(g, kind, cx, y + small.getAscent(), Theme.mix(Theme.GOLD, tint, 0.35));
		y += small.getHeight() + 2;

		// The name as large as it will go: double where it fits, so the pixel font stays crisp.
		Font big = Theme.title();
		for (float scale : new float[]{2f, 1.5f, 1f})
		{
			big = Theme.title().deriveFont(Theme.title().getSize2D() * scale);
			if (g.getFontMetrics(big).stringWidth(e.getTitle()) <= w)
			{
				break;
			}
		}
		g.setFont(big);
		FontMetrics bm = g.getFontMetrics();
		String title = Ui.clip(e.getTitle(), bm, w);
		int tw = bm.stringWidth(title);
		int base = y + bm.getAscent() - Math.round(bm.getDescent() * 0.4f);
		g.setColor(Color.BLACK);
		g.drawString(title, cx - tw / 2 + 1, base + 2);
		g.setPaint(new GradientPaint(0, base - bm.getAscent() + bm.getDescent(), Theme.GOLD_BRIGHT, 0, base, Theme.GOLD));
		g.drawString(title, cx - tw / 2, base);
		y = base + 6;

		String detail = EntryRow.detail(e);
		if (detail != null && !detail.isEmpty())
		{
			g.setFont(Theme.body());
			FontMetrics body = g.getFontMetrics();
			centered(g, Ui.clip(detail, body, w), cx, y + body.getAscent(), Theme.TEXT);
			y += body.getHeight();
		}
		g.setFont(Theme.small());
		String when = DATE.format(k.getDay());
		if (player != null && !player.isEmpty())
		{
			when = player + EntryRow.DOT + when;
		}
		centered(g, Ui.clip(when, small, w), cx, y + small.getAscent() + 1, Theme.TEXT_MUTED);
		y += small.getHeight() + 10;
		rule(g, x, y, w);
		y += 10;

		List<String[]> tiles = tiles(k);
		List<Color> colors = new ArrayList<>();
		for (String[] t : tiles)
		{
			colors.add("worth".equals(t[3]) ? Theme.GOLD_BRIGHT : "road".equals(t[3]) ? tint : Theme.TEAL);
		}
		int gap = 6;
		int tileW = (w - gap * 2) / 3;
		int rowW = tileW * tiles.size() + gap * (tiles.size() - 1);
		int tx = x + (w - rowW) / 2;
		int tileH = 54;
		for (int i = 0; i < tiles.size(); i++)
		{
			String[] t = tiles.get(i);
			int tileX = tx + i * (tileW + gap);
			StatTile.paint(g, t[0], t[1], t[2], colors.get(i), tileX, y, i == 2 ? x + w - tileX : tileW, tileH);
		}
		if (!tiles.isEmpty())
		{
			y += tileH + 10;
		}

		if (!k.getSameDay().isEmpty())
		{
			g.setFont(Theme.small());
			SectionHeader.paint(g, "ALSO THAT DAY", null, x, y, w, 20);
			y += 24;
			for (Entry other : k.getSameDay())
			{
				Color c = Theme.entry(other.getKind());
				EntryRow.node(g, other.getKind(), x + 6, y + 6, c);
				int ox = x + 20;
				int edge = x + w;
				BufferedImage pic = Views.icon(other, items, skills, onLoaded);
				if (pic != null)
				{
					int box = 22;
					double scale = Math.min(1, box / (double) Math.max(pic.getWidth(), pic.getHeight()));
					int iw = (int) Math.round(pic.getWidth() * scale);
					int ih = (int) Math.round(pic.getHeight() * scale);
					g.drawImage(pic, edge - box + (box - iw) / 2, y - 4 + (box - ih) / 2, iw, ih, null);
					edge -= box + 6;
				}
				g.setFont(Theme.body());
				FontMetrics fm = g.getFontMetrics();
				String od = EntryRow.detail(other);
				Ui.shadowed(g, Ui.clip(other.getTitle(), fm, edge - ox), ox, y + fm.getAscent() - 3, Theme.mix(Theme.TEXT, c, 0.3));
				if (od != null)
				{
					g.setFont(Theme.small());
					g.setColor(Theme.TEXT_MUTED);
					g.drawString(Ui.clip(od, small, edge - ox), ox, y + fm.getAscent() + small.getAscent() - 3);
				}
				y += 28;
			}
			y += 2;
		}

		rule(g, x, y, w);
		y += 8;
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString("Codex Annals for RuneLite", x, y + small.getAscent());
		if (begunAt > 0)
		{
			String since = "Kept since " + Format.since(begunAt);
			g.drawString(since, x + w - small.stringWidth(since), y + small.getAscent());
		}
		y += small.getHeight();
		return y + PAD;
	}

	/**
	 * The three figures of the card, each {value, label, note, colour role}: the day of the annals,
	 * the road (a skill's weeks, a boss's week, the year's place), then the worth that day or the
	 * time played by then. A figure the annals cannot tell is left out rather than shown as nought.
	 */
	static List<String[]> tiles(Keepsake k)
	{
		List<String[]> out = new ArrayList<>();
		if (k.getDayOfAnnals() > 0)
		{
			out.add(new String[]{Format.number(k.getDayOfAnnals()), "Day",
				k.getDaysPlayed() > 0 ? Format.plural(k.getDaysPlayed(), "day", "days") + " played" : "of the annals", "day"});
		}
		String[] road = road(k);
		if (road != null)
		{
			out.add(road);
		}
		if (k.getWorth() > 0)
		{
			out.add(new String[]{Format.coins(k.getWorth()), "Worth", "that day", "worth"});
		}
		else if (k.getPlayedMs() > 0)
		{
			out.add(new String[]{Format.hours(k.getPlayedMs()), "Played", "by that week", "day"});
		}
		if (out.size() < 3 && k.getWorth() > 0 && k.getPlayedMs() > 0)
		{
			out.add(new String[]{Format.hours(k.getPlayedMs()), "Played", "by that week", "day"});
		}
		return out;
	}

	private static String[] road(Keepsake k)
	{
		Entry e = k.getEntry();
		String year = String.valueOf(k.getDay().getYear());
		if (k.getSkill() != null && k.getSkillWeeks() > 0)
		{
			// The experience of the week it came: the last push.
			String note = k.getSkillWeekXp() > 0 ? Format.xp(k.getSkillWeekXp()) + " xp" : k.getSkill();
			return new String[]{Format.number(k.getSkillWeeks()), k.getSkillWeeks() == 1 ? "Week trained" : "Weeks trained", note, "road"};
		}
		if (k.getBoss() != null && k.getBossWeekKills() > 0)
		{
			return new String[]{Format.number(k.getBossWeekKills()), "That week", k.getBossWeekKills() == 1 ? "kill" : "kills", "road"};
		}
		if (k.getDropRank() == 1)
		{
			return new String[]{"1st", "Of " + year, "biggest drop", "road"};
		}
		if (k.getDropRank() > 1)
		{
			return new String[]{Keepsake.ordinal(k.getDropRank()), "Of " + year, "by value", "road"};
		}
		String noun = noun(e.getKind());
		if (noun != null && k.getOrdinal() > 0)
		{
			return new String[]{Keepsake.ordinal(k.getOrdinal()), "Of " + year, noun, "road"};
		}
		return null;
	}

	private static String noun(Entry.Kind kind)
	{
		switch (kind)
		{
			case PET:
				return "pet";
			case QUEST:
				return "quest";
			case DIARY:
				return "diary";
			case TASK:
				return "combat task";
			case COLLECTION:
				return "log slot";
			case MAX:
				return "99";
			case DROP:
				return "big drop";
			default:
				return null;
		}
	}

	/** A gilt medallion: a soft glow, a double ring with twelve studs, and the picture inside. */
	private static void medallion(Graphics2D g, int cx, int cy, Color tint, BufferedImage icon, Entry.Kind kind)
	{
		int r = MEDAL;
		g.setPaint(new RadialGradientPaint(cx, cy, r * 2.2f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(tint, 70), Theme.alpha(tint, 0)}));
		g.fill(new Ellipse2D.Float(cx - r * 2.2f, cy - r * 2.2f, r * 4.4f, r * 4.4f));
		g.setPaint(new RadialGradientPaint(cx, cy - r * 0.3f, r, new float[]{0f, 1f},
			new Color[]{Theme.mix(Theme.CARD_TOP, tint, 0.18), Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(1.6f));
		g.setPaint(new GradientPaint(0, cy - r, Theme.GOLD_BRIGHT, 0, cy + r, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(0.8f));
		g.setColor(Theme.alpha(Theme.GOLD, 120));
		g.draw(new Ellipse2D.Float(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8));
		for (int i = 0; i < 12; i++)
		{
			double a = Math.PI * 2 * i / 12;
			float sx = (float) (cx + Math.cos(a) * (r + 5));
			float sy = (float) (cy + Math.sin(a) * (r + 5));
			stud(g, sx, sy, i % 3 == 0 ? 2.6f : 1.6f, i % 3 == 0 ? Theme.GOLD : Theme.GOLD_DIM);
		}
		if (icon != null)
		{
			int box = 40;
			int size = Math.max(icon.getWidth(), icon.getHeight());
			// A skill's small icon doubles, pixel for pixel; an item's picture keeps its own size.
			double scale = size * 2 <= box ? 2 : Math.min(1, box / (double) size);
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
			Graphics2D big = (Graphics2D) g.create();
			big.translate(cx, cy);
			big.scale(2.6, 2.6);
			EntryRow.node(big, kind, 0, 0, tint);
			big.dispose();
		}
	}

	private static void stud(Graphics2D g, float x, float y, float r, Color color)
	{
		Path2D d = new Path2D.Float();
		d.moveTo(x, y - r);
		d.lineTo(x + r, y);
		d.lineTo(x, y + r);
		d.lineTo(x - r, y);
		d.closePath();
		g.setColor(color);
		g.fill(d);
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

	private static void centered(Graphics2D g, String text, int cx, int baseline, Color color)
	{
		FontMetrics fm = g.getFontMetrics();
		Ui.shadowed(g, text, cx - fm.stringWidth(text) / 2, baseline, color);
	}

	private static void rule(Graphics2D g, int x, int y, int w)
	{
		g.setStroke(new BasicStroke(1f));
		g.setPaint(new GradientPaint(x, 0, Theme.alpha(Theme.GOLD, 0), x + w / 2f, 0, Theme.alpha(Theme.GOLD, 150), true));
		g.drawLine(x, y, x + w, y);
		stud(g, x + w / 2f, y, 3, Theme.GOLD);
	}
}
