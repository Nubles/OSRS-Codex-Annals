package com.codex.annals.ui;

import com.codex.annals.core.Ahead;
import com.codex.annals.core.Annals;
import com.codex.annals.core.Entry;
import com.codex.annals.core.Recap;
import com.codex.annals.core.Wealth;
import com.codex.annals.core.Year;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Codex Annals side panel: the crest, then four pages. Timeline is the account's whole
 * history, Year is one year told in full, Ahead is when the next milestones fall at the pace the
 * account keeps, and Card is that year as a picture to save or copy.
 */
public class AnnalsPanel extends PluginPanel
{
	private static final String[] TABS = {"timeline", "year", "ahead", "card"};
	private static final int YEAR = 1;
	private static final int AHEAD = 2;
	private static final int CARD = 3;
	private static final int PAGE = 120;
	/** Up to this many years sit side by side as tabs; beyond it the chooser steps from year to year. */
	private static final int YEAR_TABS = 4;

	private final ItemIcons items;
	private final Icons icons;
	private final CardActions actions;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Timeline", "Year", "Ahead", "Card");
	private final Segmented chapters = new Segmented(false, TimelineView.CHAPTERS);
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane timeline = scroller();
	private final JScrollPane year = scroller();
	private final JScrollPane ahead = scroller();
	private final JScrollPane card = scroller();
	/** Codex Ledger's saved worth, when that plugin is kept too. */
	private Wealth.Saved wealth;

	private Annals annals;
	private ZoneId zone = ZoneId.systemDefault();
	private String playerName;
	private List<Integer> years = Collections.emptyList();
	private int yearIndex;
	private Consumer<Entry> strike;
	/** The annals changed while the panel was out of sight; rebuild when it is opened. */
	private boolean stale;
	private int limit = PAGE;
	private boolean showName = true;
	private int tab;
	private String cardNote;
	private BufferedImage cardImage;
	private boolean refreshPending;

	public AnnalsPanel(ItemIcons items, Icons icons, CardActions actions)
	{
		super(false);
		this.items = items;
		this.icons = icons;
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.setBorder(new EmptyBorder(8, 8, 2, 8));
		tabHolder.add(tabs, BorderLayout.CENTER);
		north.add(tabHolder, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		content.setOpaque(false);
		content.add(timeline, TABS[0]);
		content.add(year, TABS[1]);
		content.add(ahead, TABS[2]);
		content.add(card, TABS[3]);
		add(content, BorderLayout.CENTER);

		tabs.onChange(this::showTab);
		chapters.onChange(i ->
		{
			limit = PAGE;
			rebuildTimeline();
			SwingUtilities.invokeLater(() -> timeline.getVerticalScrollBar().setValue(0));
		});
		rebuildAll();
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		return scroll;
	}

	/**
	 * Called on the Swing thread whenever the annals change.
	 *
	 * @param annals a copy the panel may keep, or null when no account is loaded
	 */
	public void update(Annals annals, ZoneId zone, String playerName)
	{
		this.annals = annals;
		this.zone = zone;
		this.playerName = playerName;
		List<Integer> latest = annals == null ? Collections.emptyList() : Recap.years(annals);
		if (!latest.equals(years))
		{
			Integer chosen = yearIndex < years.size() ? years.get(yearIndex) : null;
			years = latest;
			yearIndex = Math.max(0, chosen == null ? 0 : years.indexOf(chosen));
		}
		// Building every row costs real time; while the panel is out of sight, wait until it is opened.
		if (!isShowing())
		{
			stale = true;
			return;
		}
		rebuildAll();
	}

	@Override
	public void onActivate()
	{
		if (stale)
		{
			rebuildAll();
		}
	}

	/** Called on the Swing thread with an entry the player chose to strike from the annals. */
	public void onStrike(Consumer<Entry> strike)
	{
		this.strike = strike;
	}

	/**
	 * A fresh year chooser for a page, or null when only one year is kept. Each page gets its
	 * own, since a Swing component can only sit in one place at a time.
	 */
	private JComponent yearChooser()
	{
		if (years.size() < 2)
		{
			return null;
		}
		IntConsumer choose = i ->
		{
			yearIndex = i;
			if (tab == CARD)
			{
				rebuildCard();
			}
			else
			{
				rebuildYear();
			}
		};
		if (years.size() > YEAR_TABS)
		{
			return new YearStepper(years, yearIndex, choose);
		}
		String[] labels = new String[years.size()];
		for (int i = 0; i < labels.length; i++)
		{
			labels[i] = String.valueOf(years.get(i));
		}
		Segmented chooser = new Segmented(false, labels);
		chooser.setSelected(yearIndex, false);
		chooser.onChange(choose);
		return chooser;
	}

	public void setShowName(boolean showName)
	{
		this.showName = showName;
		if (tab == CARD)
		{
			rebuildCard();
		}
	}

	/** A line under the card's buttons, such as where it was saved. */
	public void cardNote(String note)
	{
		cardNote = note;
		rebuildCard();
	}

	/** Opens one tab, for previews and tests. */
	public void showTab(int index)
	{
		tab = index;
		tabs.setSelected(index, false);
		cards.show(content, TABS[index]);
		if (index == CARD)
		{
			cardNote = null;
			rebuildCard();
		}
		else if (index == AHEAD)
		{
			rebuildAhead();
		}
		else if (index == YEAR)
		{
			// The chosen year may have changed on the card page.
			rebuildYear();
		}
	}

	/** Picks a chapter of the timeline, for previews and tests. */
	public void showChapter(int index)
	{
		chapters.setSelected(index, true);
	}

	private void rebuildAll()
	{
		stale = false;
		Year thisYear = annals == null ? null : annals.getYear(LocalDate.now(zone).getYear());
		crest.update(playerName != null ? playerName : annals == null ? null : annals.getPlayer(),
			annals == null ? 0 : annals.getBegunAt(),
			annals == null ? 0 : annals.getEntries().size(),
			thisYear == null ? 0 : thisYear.getDays().size(),
			thisYear == null ? 0 : thisYear.getPlayedMs());
		rebuildTimeline();
		if (tab == YEAR)
		{
			rebuildYear();
		}
		else if (tab == AHEAD)
		{
			rebuildAhead();
		}
		else if (tab == CARD)
		{
			rebuildCard();
		}
	}

	/** Codex Ledger's saved worth, or null when it is not kept; shown at the next rebuild. */
	public void setWealth(Wealth.Saved wealth)
	{
		this.wealth = wealth;
	}

	private Wealth wealthOf(Recap r)
	{
		return r == null ? null : Wealth.of(wealth, r.getYear(), zone, r.getYear() == LocalDate.now(zone).getYear());
	}

	private void rebuildAhead()
	{
		LocalDate today = LocalDate.now(zone);
		Ahead forecast = annals == null ? null : Ahead.of(annals, today, zone);
		replace(ahead, AheadView.build(forecast, today, annals == null ? Collections.emptyMap() : annals.getExperience(), icons));
	}

	/** What the timeline was last built from; when only the clock moved, it is left as it is. */
	private String timelineKey;

	private void rebuildTimeline()
	{
		String key = timelineKey();
		if (key.equals(timelineKey))
		{
			return;
		}
		timelineKey = key;
		replace(timeline, TimelineView.build(annals, zone, chapters, limit, () ->
		{
			limit += PAGE;
			rebuildTimeline();
		}, items, icons, this::refreshLater, strike));
	}

	private String timelineKey()
	{
		long sum = 0;
		if (annals != null)
		{
			for (Entry e : annals.getEntries())
			{
				sum = sum * 31 + (e.getAt() ^ e.getTitle().hashCode() ^ e.getItemId() ^ (e.getDetail() == null ? 0 : e.getDetail().hashCode()));
			}
		}
		return (annals == null ? "none" : annals.getEntries().size() + ":" + sum) + "/" + chapters.getSelected() + "/" + limit + "/" + zone + "/" + LocalDate.now(zone);
	}

	private Recap recap()
	{
		if (annals == null || years.isEmpty())
		{
			return null;
		}
		return Recap.of(annals, years.get(Math.min(yearIndex, years.size() - 1)), zone);
	}

	private void rebuildYear()
	{
		Recap r = recap();
		LocalDate today = LocalDate.now(zone);
		int current = r != null && r.getYear() == today.getYear() ? today.getMonthValue() - 1 : -1;
		Recap before = r == null || annals == null ? null : Recap.of(annals, r.getYear() - 1, zone);
		replace(year, YearView.build(r, before, wealthOf(r), yearChooser(), current, zone, items, icons, this::refreshLater));
	}

	private void rebuildCard()
	{
		StackPanel stack = new StackPanel(8, 8);
		JComponent chooser = yearChooser();
		if (chooser != null)
		{
			stack.add(chooser);
		}
		Recap r = recap();
		if (r == null)
		{
			stack.add(Views.note("Nothing to share yet", "Once the annals have begun, your Year in Gielinor card appears here, ready to save or copy. It grows all year."));
			replace(card, stack);
			return;
		}
		int shownYear = r.getYear();
		cardImage = YearCard.render(r, wealthOf(r), showName, items, icons, this::refreshLater);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
		buttons.setOpaque(false);
		buttons.add(new CodexButton("Save card", true, () -> actions.save(cardImage, shownYear)));
		buttons.add(new CodexButton("Copy to clipboard", false, () -> actions.copy(cardImage)));
		boolean thisYear = shownYear == LocalDate.now(zone).getYear();
		stack.add(new SectionHeader("Year in Gielinor", thisYear ? "so far" : String.valueOf(shownYear)));
		stack.add(new CardPreview(cardImage));
		stack.add(buttons);
		String note = cardNote != null ? cardNote : "Save card asks where to put the picture. Copy puts it on the clipboard, ready to paste.";
		stack.add(Ui.wrapped(note, Theme.small(), cardNote != null ? Theme.TEAL : Theme.TEXT_FAINT, Views.WIDTH - 10));
		replace(card, stack);
	}

	/** Item pictures arrive after the first drawing; redraw once they are in. */
	private void refreshLater()
	{
		if (refreshPending)
		{
			return;
		}
		refreshPending = true;
		SwingUtilities.invokeLater(() ->
		{
			refreshPending = false;
			if (tab == CARD)
			{
				rebuildCard();
			}
			else
			{
				timeline.repaint();
				year.repaint();
			}
		});
	}

	private static void replace(JScrollPane scroll, JComponent view)
	{
		int position = scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(view);
		scroll.revalidate();
		SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(position));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(Theme.INK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
