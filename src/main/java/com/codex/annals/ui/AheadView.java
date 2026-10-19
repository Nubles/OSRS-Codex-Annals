package com.codex.annals.ui;

import com.codex.annals.core.Ahead;
import com.codex.annals.core.Goals;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** The Ahead tab: when each 99, the next total level, the max cape and the next kill counts fall at the pace the account keeps. */
final class AheadView
{
	private static final int NINETIES = 6;
	private static final int OTHERS = 4;
	private static final int GOALS = 4;
	private static final long MAX_99 = 13_034_431L;
	private static final DateTimeFormatter SOON = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter LATER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);

	private AheadView()
	{
	}

	/**
	 * @param xp each skill's experience now, for how far along each 99 is
	 */
	static JComponent build(Ahead ahead, LocalDate today, Map<String, Long> xp, Icons icons, List<Goals.Forecast> goals)
	{
		StackPanel stack = new StackPanel(8, 8);
		if (ahead == null)
		{
			stack.add(Views.note("Nothing ahead yet", "Once the annals have begun, this page says when each 99, the next total level, "
				+ "the max cape and your next kill count milestones fall at the pace you keep."));
			return stack;
		}
		if (!ahead.hasPace())
		{
			stack.add(Views.note("A week to find your pace", "Forecasts appear once the annals have seen a week of your play. "
				+ "The pace counts every day, days off included, so the dates are honest ones."));
			return stack;
		}

		Card hero = new Card(new BorderLayout(0, 2));
		hero.highlighted(true);
		Ahead.Mark cape = ahead.getCape();
		String headline;
		String sub;
		if (ahead.isMaxed())
		{
			headline = "Maxed";
			sub = "Every skill is 99. Only 200M and kill counts lie ahead.";
		}
		else if (cape != null && cape.getDate() != null)
		{
			headline = "Max cape " + later(cape.getDate());
			sub = "around " + FULL.format(cape.getDate()) + ", if every skill keeps its pace";
		}
		else
		{
			List<String> stalled = ahead.getStalled();
			List<String> far = ahead.getFar();
			headline = "No max cape at this pace";
			String still = stalled.isEmpty() ? null : names(stalled) + (stalled.size() == 1 ? " has" : " have") + " not moved";
			String slow = far.isEmpty() ? null : names(far) + (far.size() == 1 ? " is" : " are") + " more than ten years off";
			sub = (still != null && slow != null ? still + "; " + slow : still != null ? still : slow) + ".";
		}
		JPanel words = new JPanel();
		words.setOpaque(false);
		words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
		words.add(Ui.label("At your pace", Theme.small(), Theme.TEXT_MUTED));
		words.add(Ui.label(headline, Theme.title(), Theme.GOLD_BRIGHT));
		hero.add(words, BorderLayout.NORTH);
		hero.add(Ui.wrapped(sub, Theme.small(), Theme.TEXT_MUTED, Views.WIDTH - 30), BorderLayout.CENTER);
		stack.add(hero);

		if (!goals.isEmpty())
		{
			stack.add(new SectionHeader("Your goals", "from Codex Navigator"));
			for (int i = 0; i < goals.size() && i < GOALS; i++)
			{
				stack.add(goal(goals.get(i), today, icons));
			}
		}

		List<Ahead.Mark> nineties = ahead.getNineties();
		if (!nineties.isEmpty())
		{
			stack.add(new SectionHeader("Next 99s", nineties.size() > NINETIES ? nineties.size() + " on the way" : null));
			for (int i = 0; i < nineties.size() && i < NINETIES; i++)
			{
				Ahead.Mark m = nineties.get(i);
				long now = xp.getOrDefault(m.getSkill(), 0L);
				stack.add(row(m, today, icons, Theme.GOLD, now / (double) MAX_99, Format.amount(Math.round(m.getPerDay())) + " xp a day, "
					+ Format.amount(m.getRemaining()) + " to go"));
			}
		}
		Ahead.Mark total = ahead.getTotal();
		if (total != null && total.getDate() != null)
		{
			stack.add(new SectionHeader("Total level"));
			stack.add(row(total, today, icons, Theme.GREEN, -1, Format.plural(total.getRemaining(), "level", "levels") + " to go"));
		}
		List<Ahead.Mark> beyond = ahead.getBeyond();
		if (!beyond.isEmpty())
		{
			stack.add(new SectionHeader("200M"));
			for (int i = 0; i < beyond.size() && i < OTHERS; i++)
			{
				Ahead.Mark m = beyond.get(i);
				stack.add(row(m, today, icons, Theme.TEAL, (200_000_000L - m.getRemaining()) / 200_000_000.0,
					Format.amount(Math.round(m.getPerDay())) + " xp a day, " + Format.amount(m.getRemaining()) + " to go"));
			}
		}
		List<Ahead.Mark> kills = ahead.getKills();
		if (!kills.isEmpty())
		{
			stack.add(new SectionHeader("Kill counts"));
			for (int i = 0; i < kills.size() && i < OTHERS; i++)
			{
				Ahead.Mark m = kills.get(i);
				String pace = m.getPerDay() >= 1 ? trim(m.getPerDay()) + " a day"
					: m.getPerDay() * 7 >= 1 ? trim(m.getPerDay() * 7) + " a week" : "one every " + Math.round(1 / m.getPerDay()) + " days";
				stack.add(row(m, today, icons, Theme.KILLS, -1, pace + ", " + Format.number(m.getRemaining()) + " to go"));
			}
		}
		String basis = ahead.isRecent() ? "the last " + ahead.getDays() + " days" : "this year so far (" + ahead.getDays() + " days)";
		stack.add(Ui.wrapped("Paced on " + basis + ", every day counted whether you played or not. Kill counts go by this year's kills."
				+ (goals.isEmpty() ? " Goals you pin in Codex Navigator are dated here too." : ""),
			Theme.small(), Theme.TEXT_FAINT, Views.WIDTH - 10));
		return stack;
	}

	/**
	 * A goal: dated by the levels it needs at each skill's own pace, or, when it needs none, by
	 * its hours of work against the hours a day really played.
	 */
	private static AheadRow goal(Goals.Forecast f, LocalDate today, Icons icons)
	{
		Goals.Goal g = f.getGoal();
		String hours = g.getHoursLeft() > 0 ? hours(g.getHoursLeft()) + " of work" : null;
		String note;
		String tip = g.getTitle() + ".";
		if (!f.getStalled().isEmpty())
		{
			String skill = f.getStalled().get(0);
			note = skill + " " + g.getSkills().get(skill) + (f.getStalled().size() > 1 ? " and more" : "") + " not moving";
			tip += " " + names(f.getStalled()) + (f.getStalled().size() == 1 ? " is" : " are") + " not being trained fast enough to date.";
		}
		else if (f.getLast() != null)
		{
			note = f.getLast() + " " + g.getSkills().get(f.getLast()) + (g.getSkills().size() > 1 ? " comes last" : "");
			tip += " The levels it needs come by around " + FULL.format(f.getLevels()) + " at the pace you train them.";
		}
		else
		{
			note = hours != null ? hours : g.getQuests() > 0 ? Format.plural(g.getQuests(), "quest", "quests") + " left" : "almost there";
			if (f.getAllIn() != null)
			{
				tip += " About " + hours(g.getHoursLeft()) + " of work: around " + FULL.format(f.getAllIn()) + " if all your play went to it.";
			}
		}
		if (f.getLast() != null && hours != null)
		{
			tip += " Codex Navigator counts about " + hours + " of work left.";
		}
		if (g.getNext() != null)
		{
			tip += " Next: " + g.getNext() + ".";
		}
		LocalDate date = f.getDate();
		String when = date != null ? when(date, today) : f.getStalled().isEmpty() ? "-" : "no date";
		java.awt.image.BufferedImage icon = f.getLast() != null && icons != null ? icons.skill(f.getLast())
			: !f.getStalled().isEmpty() && icons != null ? icons.skill(f.getStalled().get(0)) : null;
		return new AheadRow(icon, g.getTitle(), note, when, date != null ? Theme.GOLD : Theme.TEXT_FAINT, g.getProgress(), tip);
	}

	/** "14h", "1.5h", "under an hour". */
	private static String hours(double h)
	{
		return h < 1 ? "under an hour" : trim(h) + "h";
	}

	private static AheadRow row(Ahead.Mark m, LocalDate today, Icons icons, java.awt.Color color, double progress, String note)
	{
		return new AheadRow(m.getSkill() == null || icons == null ? null : icons.skill(m.getSkill()), m.getTitle(), note,
			when(m.getDate(), today), color, progress, m.getTitle() + ": around " + FULL.format(m.getDate()) + ". " + note + ".");
	}

	/** "in 4 days", "14 Mar" within two months, "Mar 2027" beyond. */
	static String when(LocalDate date, LocalDate today)
	{
		long days = ChronoUnit.DAYS.between(today, date);
		if (days <= 1)
		{
			return "tomorrow";
		}
		if (days < 7)
		{
			return "in " + days + " days";
		}
		return days <= 60 ? SOON.format(date) : LATER.format(date);
	}

	private static String later(LocalDate date)
	{
		return LATER.format(date);
	}

	private static String trim(double n)
	{
		String text = String.format(Locale.ENGLISH, "%.1f", n);
		return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
	}

	private static String names(List<String> skills)
	{
		if (skills.size() == 1)
		{
			return skills.get(0);
		}
		if (skills.size() <= 3)
		{
			return String.join(", ", skills.subList(0, skills.size() - 1)) + " and " + skills.get(skills.size() - 1);
		}
		return skills.get(0) + ", " + skills.get(1) + " and " + (skills.size() - 2) + " more";
	}
}
