package com.codex.annals.ui;

import com.codex.annals.core.Today;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The day so far, at the top of the timeline: time played, experience and kills, where the
 * experience went, and how much of the usual day of play it already is. The panel keeps one and
 * fills it again on every refresh, so the figures move without the whole timeline being rebuilt.
 */
class TodayCard extends JPanel
{
	private final Card card = new Card(new BorderLayout());
	private String shown;

	TodayCard()
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		card.accent(Theme.GOLD_BRIGHT);
		setVisible(false);
	}

	/**
	 * @param today the day so far, or null to hide the card
	 * @return whether anything shown changed
	 */
	boolean show(Today today)
	{
		String key = key(today);
		if (key.equals(shown))
		{
			return false;
		}
		shown = key;
		removeAll();
		card.removeAll();
		if (today == null || today.isEmpty())
		{
			setVisible(false);
			revalidate();
			return true;
		}
		add(new SectionHeader("Today", "so far"), BorderLayout.NORTH);
		StackPanel lines = new StackPanel(3, 0);
		boolean kills = today.getKills() > 0;
		lines.add(new WeekFigures(
			new String[]{Format.duration(today.getPlayedMs()), Format.amount(today.getXp()), kills ? Format.number(today.getKills()) : Format.number(today.getLevels())},
			new String[]{"played", "experience", kills ? "kills" : "levels"},
			new Color[]{Theme.GOLD_BRIGHT, Theme.XP, kills ? Theme.KILLS : Theme.GREEN}));
		List<String> skills = today.getSkills();
		if (!skills.isEmpty())
		{
			String most = skills.size() == 1 ? skills.get(0)
				: skills.size() == 2 ? skills.get(0) + " and " + skills.get(1)
				: skills.get(0) + ", " + skills.get(1) + " and " + skills.get(2);
			lines.add(Ui.wrapped("Most experience in " + most + ".", Theme.small(), Theme.TEXT_MUTED, Views.WIDTH - 30));
		}
		List<String> parts = new ArrayList<>();
		if (kills && today.getBoss() != null)
		{
			parts.add(today.getBoss() + " " + Format.number(today.getBossKills()));
		}
		if (kills && today.getLevels() > 0)
		{
			parts.add(Format.plural(today.getLevels(), "level", "levels"));
		}
		if (today.getPets() > 0)
		{
			parts.add(Format.plural(today.getPets(), "pet", "pets"));
		}
		if (today.getSlots() > 0)
		{
			parts.add(Format.plural(today.getSlots(), "log slot", "log slots"));
		}
		if (today.getDrops() > 0)
		{
			parts.add(Format.plural(today.getDrops(), "notable drop", "notable drops"));
		}
		if (today.getQuests() > 0)
		{
			parts.add(Format.plural(today.getQuests(), "quest", "quests"));
		}
		if (today.getDiaries() > 0)
		{
			parts.add(Format.plural(today.getDiaries(), "diary", "diaries"));
		}
		if (!parts.isEmpty())
		{
			lines.add(Ui.wrapped(String.join("  ·  ", parts), Theme.small(), Theme.TEXT, Views.WIDTH - 30));
		}
		double share = today.ofUsual();
		if (!Double.isNaN(share) && today.getXp() > 0)
		{
			long pct = Math.round(share * 100);
			String text = pct >= 100
				? "Past your usual day of " + Format.amount(today.getUsualXp()) + " experience."
				: pct + "% of your usual day's " + Format.amount(today.getUsualXp()) + " experience.";
			lines.add(new UsualBar(share));
			lines.add(Ui.wrapped(text, Theme.small(), pct >= 100 ? Theme.GREEN : Theme.TEXT_MUTED, Views.WIDTH - 30));
		}
		card.add(lines, BorderLayout.CENTER);
		card.setBorder(new EmptyBorder(6, 10, 6, 8));
		add(card, BorderLayout.CENTER);
		setVisible(true);
		revalidate();
		repaint();
		return true;
	}

	/** Everything the card shows, so an unchanged day is not laid out again. */
	private static String key(Today t)
	{
		if (t == null || t.isEmpty())
		{
			return "none";
		}
		return t.getDay() + "/" + t.getPlayedMs() / 60_000 + "/" + t.getXp() + "/" + t.getLevels() + "/" + t.getKills() + "/" + t.getBoss()
			+ "/" + t.getSkills() + "/" + t.getSlots() + "/" + t.getPets() + "/" + t.getDrops() + "/" + t.getQuests() + "/" + t.getDiaries()
			+ "/" + t.getUsualXp();
	}

	/** A thin bar: how much of the usual day's experience is done, gold up to it and green past it. */
	private static final class UsualBar extends JComponent
	{
		private final double share;

		UsualBar(double share)
		{
			this.share = share;
			setOpaque(false);
			setPreferredSize(new Dimension(10, 8));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth() - 2;
			int y = 2;
			g.setColor(Theme.INK_DEEP);
			g.fill(new RoundRectangle2D.Float(0, y, w, 4, 4, 4));
			float fill = (float) Math.max(0, Math.min(1, share)) * w;
			g.setColor(share >= 1 ? Theme.GREEN : Theme.GOLD);
			if (fill > 0)
			{
				g.fill(new RoundRectangle2D.Float(0, y, Math.max(4, fill), 4, 4, 4));
			}
			g.dispose();
		}
	}
}
