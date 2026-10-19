package com.codex.annals.ui;

import com.codex.annals.core.Chapters;
import com.codex.annals.core.Entry;
import com.codex.annals.core.Recap;
import com.codex.annals.core.Wealth;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
	private static final int HIGHLIGHTS = 6;

	private YearView()
	{
	}

	/**
	 * @param before the year before, to set this one against, or null
	 * @param wealth the year's worth from Codex Ledger, or null when it is not kept
	 * @param years the year chooser, or null when only one year is kept
	 * @param currentMonth the month still being lived when this is the current year, or -1
	 */
	static JComponent build(Recap r, Recap before, Wealth wealth, List<Chapters.Chapter> chapters, JComponent years, int currentMonth,
		ZoneId zone, ItemIcons items, Icons icons, Runnable onLoaded, java.util.function.Consumer<Entry> keep)
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
		stack.add(new MonthStrip(r.getMonthDays(), r.getMonthEntries(), currentMonth, r.getFirstMonth()));

		// Every day of the year, and the streak still running when it is this year.
		long today = LocalDate.now(zone).toEpochDay();
		long first = r.getBegunAt() > 0 ? Instant.ofEpochMilli(r.getBegunAt()).atZone(zone).toLocalDate().toEpochDay() : Long.MIN_VALUE;
		List<Long> days = new ArrayList<>(r.getDays());
		if (before != null)
		{
			days.addAll(before.getDays());
		}
		int streak = currentMonth >= 0 ? Recap.currentStreak(days, today) : 0;
		stack.add(new SectionHeader("Days", streak > 1 ? streak + " in a row now" : r.getLongestStreak() > 1 ? "best run " + r.getLongestStreak() : null));
		Card calendar = new Card(new BorderLayout());
		calendar.add(new DayCalendar(r.getYear(), r.getDays(), today, first, streak), BorderLayout.CENTER);
		stack.add(calendar);

		if (chapters != null && !chapters.isEmpty())
		{
			// The stretches of weeks the year was spent on, oldest first, as the story reads.
			stack.add(new SectionHeader("Chapters", Format.plural(chapters.size(), "chapter", "chapters")));
			JPanel run = new JPanel();
			run.setOpaque(false);
			run.setLayout(new BoxLayout(run, BoxLayout.Y_AXIS));
			for (int i = 0; i < chapters.size(); i++)
			{
				Chapters.Chapter c = chapters.get(i);
				ChapterRow row = new ChapterRow(c, c.isBoss() || icons == null ? null : icons.skill(c.getFocus()), i == 0, i == chapters.size() - 1);
				row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
				run.add(row);
			}
			stack.add(run);
		}

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

		// The most telling moments, told in the order they came, each drop once with its count.
		List<Entry> highlights = r.getHighlightsByDate(HIGHLIGHTS);
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
					i == 0, i == highlights.size() - 1).times(r.timesOf(e));
				row.menu(keep, null);
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
		if (drop != null)
		{
			records.add(record("Biggest drop", Format.coins(drop.getValue())));
			// The item under its value, on the value's side, so the label column stays one word deep.
			JLabel item = Ui.label(Ui.clip(drop.getTitle(), records.getFontMetrics(Theme.small()), Views.WIDTH - 24), Theme.small(), Theme.GOLD);
			item.setHorizontalAlignment(JLabel.RIGHT);
			item.setToolTipText(drop.getTitle());
			records.add(item);
		}
		// A row of noughts says nothing; only what the year holds is told.
		if (r.getClues() > 0)
		{
			records.add(record("Clue scrolls", Format.number(r.getClues())));
		}
		if (r.getTasks() > 0)
		{
			records.add(record("Combat tasks", Format.number(r.getTasks())));
		}
		if (wealth != null)
		{
			records.add(record(wealth.isPartial() ? "Worth since " + Format.shortDay(Instant.ofEpochMilli(wealth.getFrom()).atZone(zone).toLocalDate())
				: "Worth", Format.signedCoins(wealth.change())));
		}
		stack.add(records);
		if (wealth != null)
		{
			// Beneath the card: a line that wraps would stretch every row of the records with it.
			stack.add(Ui.wrapped("Worth went from " + Format.coins(wealth.getStart()) + " to " + Format.coins(wealth.getEnd())
				+ ", as Codex Ledger counted it.", Theme.small(), Theme.TEXT_FAINT, Views.WIDTH - 10));
		}
		if (before != null)
		{
			compare(stack, r, before, currentMonth >= 0);
		}
		return stack;
	}

	/** The year set against the one before it, each figure with how much it rose or fell. */
	private static void compare(StackPanel stack, Recap r, Recap before, boolean current)
	{
		stack.add(new SectionHeader("Against " + before.getYear(), current ? r.getYear() + " so far" : null));
		Card card = new Card(new GridLayout(0, 1, 0, 3));
		card.add(versus("Hours played", r.getPlayedMs() / 3_600_000L, before.getPlayedMs() / 3_600_000L, false));
		card.add(versus("Days played", r.getDaysPlayed(), before.getDaysPlayed(), false));
		card.add(versus("Levels", r.getLevelsGained(), before.getLevelsGained(), false));
		card.add(versus("Experience", r.getXpGained(), before.getXpGained(), true));
		card.add(versus("Log slots", r.getCollection(), before.getCollection(), false));
		card.add(versus("Boss kills", r.getKills(), before.getKills(), false));
		card.add(versus("Quests", r.getQuests(), before.getQuests(), false));
		stack.add(card);
		if (before.getFirstMonth() > 0)
		{
			// The annals began part way through the year before, so it holds only some of that year.
			stack.add(Ui.wrapped(before.getYear() + " was only kept from " + Format.monthName(before.getFirstMonth()) + ".",
				Theme.small(), Theme.TEXT_FAINT, Views.WIDTH - 10));
		}
	}

	/** "Levels  41  +12": this year's figure and how far it is from the year before's. */
	private static JPanel versus(String label, long now, long then, boolean xp)
	{
		JPanel line = new JPanel(new BorderLayout(6, 0));
		line.setOpaque(false);
		line.add(Ui.label(label, Theme.small(), Theme.TEXT_MUTED), BorderLayout.WEST);
		JPanel right = new JPanel(new BorderLayout(6, 0));
		right.setOpaque(false);
		right.add(Ui.label(xp ? Format.amount(now) : Format.number(now), Theme.small(), Theme.TEXT), BorderLayout.CENTER);
		long diff = now - then;
		String change = diff == 0 ? "same" : (diff > 0 ? "+" : "-") + (xp ? Format.amount(Math.abs(diff)) : Format.number(Math.abs(diff)));
		JLabel delta = Ui.label(change, Theme.small(), diff > 0 ? Theme.GREEN : diff < 0 ? Theme.EMBER : Theme.TEXT_FAINT);
		delta.setHorizontalAlignment(JLabel.RIGHT);
		delta.setPreferredSize(new java.awt.Dimension(44, delta.getPreferredSize().height));
		right.add(delta, BorderLayout.EAST);
		line.add(right, BorderLayout.EAST);
		line.setToolTipText(label + ": " + (xp ? Format.amount(now) : Format.number(now)) + ", against " + (xp ? Format.amount(then) : Format.number(then)));
		return line;
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
