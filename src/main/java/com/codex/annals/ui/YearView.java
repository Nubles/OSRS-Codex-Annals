package com.codex.annals.ui;

import com.codex.annals.core.Entry;
import com.codex.annals.core.Recap;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** The Year tab: one year told in full, from its headline down to the records it set. */
final class YearView
{
	private static final int SKILLS = 6;
	private static final int BOSSES = 6;

	private YearView()
	{
	}

	/**
	 * @param years the year chooser, or null when only one year is kept
	 * @param currentMonth the month still being lived when this is the current year, or -1
	 */
	static JComponent build(Recap r, JComponent years, int currentMonth, ZoneId zone, ItemIcons items, Icons icons, Runnable onLoaded)
	{
		StackPanel stack = new StackPanel(8, 8);
		if (years != null)
		{
			stack.add(years);
		}
		if (r == null)
		{
			stack.add(Views.note("No year yet", "Your year begins the first time you log in with Codex Annals on. Everything you do after that is counted here."));
			return stack;
		}

		Card hero = new Card(new BorderLayout());
		hero.highlighted(true);
		JPanel lines = new JPanel();
		lines.setOpaque(false);
		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		JLabel year = Ui.label(String.valueOf(r.getYear()), Theme.title().deriveFont(Theme.title().getSize2D() * 2), Theme.GOLD_BRIGHT);
		lines.add(year);
		lines.add(Ui.label(r.getHeadline(), Theme.body(), Theme.TEXT));
		lines.add(Ui.label(Format.hours(r.getPlayedMs()) + " across " + Format.plural(r.getDaysPlayed(), "day", "days"), Theme.small(), Theme.TEXT_MUTED));
		hero.add(lines, BorderLayout.CENTER);
		stack.add(hero);

		JPanel tiles = new JPanel(new GridLayout(2, 2, 6, 6));
		tiles.setOpaque(false);
		tiles.add(new StatTile(Format.number(r.getDaysPlayed()), "Days played",
			r.getLongestStreak() > 1 ? r.getLongestStreak() + " in a row" : "", Theme.TEAL));
		tiles.add(new StatTile("+" + Format.number(r.getLevelsGained()), "Levels", Format.xp(r.getXpGained()) + " xp", Theme.GREEN));
		tiles.add(new StatTile(Format.number(r.getCollection()), "Log slots", r.getPets() > 0 ? Format.plural(r.getPets(), "pet", "pets") : "new this year", Theme.GOLD));
		tiles.add(new StatTile(Format.number(r.getQuests()), "Quests", Format.plural(r.getDiaries(), "diary", "diaries"), Theme.FEAT));
		stack.add(tiles);

		int busiest = r.busiestMonth();
		stack.add(new SectionHeader("Months", busiest >= 0 ? "busiest " + Format.monthName(busiest) : null));
		stack.add(new MonthStrip(r.getMonthDays(), r.getMonthEntries(), currentMonth));

		List<Recap.SkillYear> skills = r.getSkills();
		if (!skills.isEmpty())
		{
			stack.add(new SectionHeader("Skills", skills.size() > SKILLS ? skills.size() + " trained" : null));
			long top = skills.get(0).getGained();
			for (int i = 0; i < skills.size() && i < SKILLS; i++)
			{
				Recap.SkillYear s = skills.get(i);
				stack.add(new SkillYearRow(s, icons == null ? null : icons.skill(s.getSkill()), s.getGained() / (double) top));
			}
		}

		List<Recap.KillYear> bosses = r.getBosses();
		if (!bosses.isEmpty())
		{
			stack.add(new SectionHeader("Bosses", Format.plural(r.getKills(), "kill", "kills")));
			int top = bosses.get(0).getKills();
			for (int i = 0; i < bosses.size() && i < BOSSES; i++)
			{
				stack.add(new KillYearRow(bosses.get(i), bosses.get(i).getKills() / (double) top));
			}
		}

		List<Entry> highlights = r.getHighlights();
		if (!highlights.isEmpty())
		{
			stack.add(new SectionHeader("Highlights"));
			JPanel run = new JPanel();
			run.setOpaque(false);
			run.setLayout(new BoxLayout(run, BoxLayout.Y_AXIS));
			for (int i = 0; i < highlights.size(); i++)
			{
				Entry e = highlights.get(i);
				EntryRow row = new EntryRow(e, Instant.ofEpochMilli(e.getAt()).atZone(zone).toLocalDate(), Views.icon(e, items, icons, onLoaded),
					i == 0, i == highlights.size() - 1);
				row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
				run.add(row);
			}
			stack.add(run);
		}

		stack.add(new SectionHeader("Records"));
		Card records = new Card(new GridLayout(0, 1, 0, 3));
		records.add(record("Longest streak", Format.plural(r.getLongestStreak(), "day", "days")));
		records.add(record("Busiest month", busiest >= 0 ? Format.monthName(busiest) + ", " + Format.plural(r.getMonthDays()[busiest], "day", "days") : "-"));
		Entry drop = r.getBiggestDrop();
		records.add(record("Biggest drop", drop == null ? "-" : Format.coins(drop.getValue())));
		if (drop != null)
		{
			records.add(Ui.label(drop.getTitle(), Theme.small(), Theme.GOLD));
		}
		records.add(record("Clue scrolls", Format.number(r.getClues())));
		records.add(record("Combat tasks", Format.number(r.getTasks())));
		stack.add(records);
		return stack;
	}

	private static JPanel record(String label, String value)
	{
		JPanel line = new JPanel(new BorderLayout());
		line.setOpaque(false);
		line.add(Ui.label(label, Theme.small(), Theme.TEXT_MUTED), BorderLayout.WEST);
		line.add(Ui.label(value, Theme.small(), Theme.TEXT), BorderLayout.EAST);
		return line;
	}
}
