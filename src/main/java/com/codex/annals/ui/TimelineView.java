package com.codex.annals.ui;

import com.codex.annals.core.Annals;
import com.codex.annals.core.Baseline;
import com.codex.annals.core.Entry;
import com.codex.annals.core.Memories;
import com.codex.annals.core.MonthNote;
import com.codex.annals.core.Timeline;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The Timeline tab: every entry of the account's history, newest first, in chapters by month,
 * ending with the prologue: where the account stood the day the annals began.
 */
final class TimelineView
{
	static final String[] CHAPTERS = {"All", "Skills", "Feats", "Loot", "Bosses"};

	private TimelineView()
	{
	}

	/**
	 * @param limit how many rows to show before the button that shows more
	 * @param more called to show more rows
	 */
	static JComponent build(Annals annals, ZoneId zone, Segmented chapters, int limit, Runnable more,
		ItemIcons items, Icons icons, Runnable onLoaded, Consumer<Entry> strike)
	{
		StackPanel stack = new StackPanel(6, 8);
		stack.add(chapters);
		if (annals == null || !annals.isBegun())
		{
			stack.add(Views.note("The annals are waiting", "Log in and Codex Annals begins your account's history: every level, quest, diary, "
				+ "collection log slot, pet, first kill and notable drop, written down with its day. Each year becomes a Year in Gielinor card to share."));
			return stack;
		}

		Entry.Chapter chapter = Entry.Chapter.values()[chapters.getSelected()];
		List<Timeline.Row> rows = Timeline.rows(annals.getEntries(), chapter, zone);
		if (rows.isEmpty())
		{
			String text = chapter == Entry.Chapter.LOOT
				? "Pets, notable drops and clue milestones are written down as they happen. New collection log slots need the game's "
				+ "own chat line: turn on Collection log, New addition notification in the game's settings."
				: "Entries of this kind are written down as they happen. Keep playing and this chapter fills.";
			stack.add(Views.note("Nothing here yet", text));
			return stack;
		}

		Map<YearMonth, List<Entry>> byMonth = new HashMap<>();
		if (chapter == Entry.Chapter.ALL)
		{
			memories(stack, annals, zone, items, icons, onLoaded);
			for (Entry e : annals.getEntries())
			{
				byMonth.computeIfAbsent(YearMonth.from(dayOf(e.getAt(), zone)), k -> new ArrayList<>()).add(e);
			}
		}

		int shown = Math.min(limit, rows.size());
		int i = 0;
		while (i < shown)
		{
			YearMonth month = YearMonth.from(rows.get(i).getDate());
			int end = i;
			while (end < rows.size() && YearMonth.from(rows.get(end).getDate()).equals(month))
			{
				end++;
			}
			int count = 0;
			for (int k = i; k < end; k++)
			{
				count += Math.max(1, rows.get(k).getFolded());
			}
			stack.add(new SectionHeader(Format.month(rows.get(i).getDate()), Format.plural(count, "entry", "entries")));
			String note = byMonth.containsKey(month) ? MonthNote.of(byMonth.get(month)) : null;
			if (note != null)
			{
				JLabel line = Ui.label(note, Theme.small(), Theme.GOLD_DIM);
				line.setBorder(new EmptyBorder(0, 14, 0, 0));
				line.setToolTipText(Format.month(rows.get(i).getDate()) + ": " + note);
				stack.add(line);
			}
			JPanel run = new JPanel();
			run.setOpaque(false);
			run.setLayout(new BoxLayout(run, BoxLayout.Y_AXIS));
			int stop = Math.min(end, shown);
			for (int k = i; k < stop; k++)
			{
				Timeline.Row row = rows.get(k);
				EntryRow line = new EntryRow(row.getEntry(), row.getDate(), Views.icon(row.getEntry(), items, icons, onLoaded), k == i, k == stop - 1);
				if (row.getFolded() == 0)
				{
					line.strikeWith(strike);
				}
				line.setAlignmentX(JComponent.LEFT_ALIGNMENT);
				run.add(line);
			}
			stack.add(run);
			i = stop;
		}

		if (shown < rows.size())
		{
			JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			holder.setOpaque(false);
			holder.add(new CodexButton("Show older entries", false, more));
			stack.add(holder);
		}
		else if (chapter == Entry.Chapter.ALL && annals.getBaseline() != null)
		{
			stack.add(new SectionHeader("Prologue", Format.since(annals.getBegunAt())));
			stack.add(prologue(annals.getBaseline(), dayOf(annals.getBegunAt(), zone)));
		}
		return stack;
	}

	/** On this day: what was written on today's date in earlier years, and a month and six months ago. */
	private static void memories(StackPanel stack, Annals annals, ZoneId zone, ItemIcons items, Icons icons, Runnable onLoaded)
	{
		List<Memories.Memory> memories = Memories.of(annals, LocalDate.now(zone), zone);
		if (memories.isEmpty())
		{
			return;
		}
		stack.add(new SectionHeader("On this day", Format.shortDay(LocalDate.now(zone))));
		Card card = new Card(new BorderLayout());
		card.accent(Theme.GOLD);
		card.setBorder(new EmptyBorder(6, 10, 4, 8));
		JPanel run = new JPanel();
		run.setOpaque(false);
		run.setLayout(new BoxLayout(run, BoxLayout.Y_AXIS));
		String last = null;
		for (int k = 0; k < memories.size(); k++)
		{
			Memories.Memory m = memories.get(k);
			if (!m.getWhen().equals(last))
			{
				last = m.getWhen();
				JLabel when = Ui.label(last, Theme.small(), Theme.GOLD);
				when.setAlignmentX(JComponent.LEFT_ALIGNMENT);
				run.add(when);
			}
			boolean alone = k + 1 >= memories.size() || !memories.get(k + 1).getWhen().equals(m.getWhen());
			boolean opens = k == 0 || !memories.get(k - 1).getWhen().equals(m.getWhen());
			Entry e = m.getEntry();
			EntryRow row = new EntryRow(e, dayOf(e.getAt(), zone), Views.icon(e, items, icons, onLoaded), opens, alone);
			row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			run.add(row);
		}
		card.add(run, BorderLayout.CENTER);
		stack.add(card);
	}

	private static LocalDate dayOf(long at, ZoneId zone)
	{
		return Instant.ofEpochMilli(at).atZone(zone).toLocalDate();
	}

	/** Where the account stood when the annals began, since nothing before can be dated. */
	static Card prologue(Baseline b, LocalDate day)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.label("Before the annals", Theme.body(), Theme.GOLD), BorderLayout.NORTH);
		StringBuilder text = new StringBuilder("On " + Format.shortDay(day) + " " + day.getYear() + " the account stood at total level "
			+ Format.number(b.getTotalLevel()) + " with " + Format.amount(b.getTotalXp()) + " experience, combat level " + b.getCombat());
		if (b.getQuestsTotal() > 0)
		{
			text.append(", and ").append(b.getQuestsDone()).append(" of ").append(b.getQuestsTotal()).append(" quests done");
		}
		text.append('.');
		List<String> maxed = b.getMaxed();
		if (!maxed.isEmpty())
		{
			text.append(maxed.size() == 1 ? " Already 99: " : " Already " + maxed.size() + " skills at 99: ").append(String.join(", ", maxed)).append('.');
		}
		text.append(" Everything above was written down as it happened.");
		card.add(Ui.wrapped(text.toString(), Theme.small(), Theme.TEXT_MUTED, Views.WIDTH - 30), BorderLayout.CENTER);
		return card;
	}
}
