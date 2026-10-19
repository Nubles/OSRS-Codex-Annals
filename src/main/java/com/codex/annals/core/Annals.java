package com.codex.annals.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The account's whole history as Codex Annals keeps it: every entry since the annals began,
 * a ledger per calendar year, and the last reading of every skill and kill count. It is plain
 * data so it saves as one blob, and every change goes through the methods here.
 */
public class Annals
{
	/** More than this and the least telling old entries make room; ordinary levels go first. */
	static final int MAX_ENTRIES = 2000;
	/** Five years of weeks; the oldest go first. */
	static final int MAX_WEEKS = 260;
	private static final int[] KC_MILESTONES = {50, 100, 250, 500, 1000, 2000, 5000, 10000};
	private static final long[] SKILL_XP = {50_000_000L, 100_000_000L, 200_000_000L};
	private static final long MAX_XP = 200_000_000L;
	/** How close a drop and its collection log message must be to count as one. */
	private static final long MERGE_MS = 30_000;
	/** The detail of the entry for the highest total level, the day the account maxed. */
	static final String MAX_TOTAL_DETAIL = "The highest total there is";

	long begunAt;
	String player;
	Baseline baseline;
	/** Oldest first. */
	List<Entry> entries = new ArrayList<>();
	/** Oldest first. */
	List<Year> years = new ArrayList<>();
	Map<String, Integer> kc = new LinkedHashMap<>();
	Map<String, Integer> levels = new LinkedHashMap<>();
	Map<String, Long> xp = new LinkedHashMap<>();
	int combat;
	/** Where the skills stood at the start of each recent day, for the pace. */
	Pace pace = new Pace();
	/** Week by week, oldest first, for the chapters and the weekly look back; null in a save from before weeks were kept. */
	List<Week> weeks;
	/** The Monday of the last week told in chat at login, so it is told once per account. */
	long toldWeek;

	public boolean isBegun()
	{
		return begunAt > 0;
	}

	public long getBegunAt()
	{
		return begunAt;
	}

	public String getPlayer()
	{
		return player;
	}

	public Baseline getBaseline()
	{
		return baseline;
	}

	/** Every entry, oldest first. Read only. */
	public List<Entry> getEntries()
	{
		return Collections.unmodifiableList(entries);
	}

	/** Every year kept, oldest first. Read only. */
	public List<Year> getYears()
	{
		return Collections.unmodifiableList(years);
	}

	public Year getYear(int year)
	{
		for (Year y : years)
		{
			if (y.year == year)
			{
				return y;
			}
		}
		return null;
	}

	/** Each skill's experience as last read. Read only. */
	public Map<String, Long> getExperience()
	{
		return Collections.unmodifiableMap(xp);
	}

	public Map<String, Integer> getKillCounts()
	{
		return Collections.unmodifiableMap(kc);
	}

	/** Makes sure every field exists after Gson has read an older or damaged save. */
	public Annals repair()
	{
		if (entries == null)
		{
			entries = new ArrayList<>();
		}
		entries.removeIf(e -> e == null);
		for (Entry e : entries)
		{
			e.compact();
		}
		if (years == null)
		{
			years = new ArrayList<>();
		}
		years.removeIf(y -> y == null);
		for (Year y : years)
		{
			y.repair();
		}
		if (kc == null)
		{
			kc = new LinkedHashMap<>();
		}
		if (levels == null)
		{
			levels = new LinkedHashMap<>();
		}
		if (xp == null)
		{
			xp = new LinkedHashMap<>();
		}
		if (pace == null)
		{
			pace = new Pace();
		}
		pace.repair();
		if (weeks == null)
		{
			// A save from before weeks were kept: the last few weeks can still be told from the daily readings.
			weeks = new ArrayList<>();
			backfill();
		}
		weeks.removeIf(w -> w == null);
		for (Week w : weeks)
		{
			w.repair();
		}
		weeks.sort((a, b) -> Long.compare(a.w, b.w));
		return this;
	}

	/**
	 * Picks up the account once its stats can be read. The first time, the annals begin and the
	 * baseline is written down; afterwards it only takes the current stats in quietly, since
	 * levels gained with the plugin off have no day to put them on.
	 *
	 * @return the entries written, at most the opening one
	 */
	public List<Entry> begin(long now, ZoneId zone, String player, Baseline start, Map<String, Long> xpNow, Map<String, Integer> levelsNow, int combatNow)
	{
		List<Entry> written = new ArrayList<>();
		this.player = player;
		combat = combatNow;
		Year year = year(now, zone);
		for (Map.Entry<String, Long> e : xpNow.entrySet())
		{
			String skill = e.getKey();
			long read = e.getValue();
			Long known = xp.get(skill);
			if (read <= 0 && known != null && known > 0)
			{
				// A skill the game has not sent yet reads as nothing; taking it in would make its real
				// value, when it arrives, look like a lifetime of levels gained in one tick.
				continue;
			}
			Integer level = levelsNow.get(skill);
			xp.put(skill, read);
			year.startXp.putIfAbsent(skill, read);
			year.endXp.put(skill, read);
			if (level != null)
			{
				levels.put(skill, level);
				year.startLevel.putIfAbsent(skill, level);
				year.endLevel.put(skill, level);
			}
		}
		pace.note(Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay(), xp);
		if (!isBegun())
		{
			begunAt = now;
			baseline = start;
			String detail = "Total level " + number(start.getTotalLevel()) + ", combat " + start.getCombat();
			if (start.getQuestPoints() > 0)
			{
				detail += ", " + start.getQuestPoints() + " quest points";
			}
			written.add(add(new Entry(Entry.Kind.BEGIN, now, "The annals begin", detail)));
		}
		return written;
	}

	/**
	 * Hours played a day over about the last four weeks, days off included, from the weeks kept;
	 * this year's so far until the annals have a week of play to go on. 0 when nothing is known.
	 */
	public double hoursPerDay(LocalDate today, ZoneId zone)
	{
		long begun = begunAt > 0 ? Instant.ofEpochMilli(begunAt).atZone(zone).toLocalDate().toEpochDay() : today.toEpochDay();
		// Whole weeks: from the Monday of the week four weeks back, or the day the annals began.
		long monday = Week.mondayOf(today.minusDays(Pace.DAYS - 1));
		long start = Math.max(monday, Math.min(begun, today.toEpochDay()));
		long ms = 0;
		if (weeks != null)
		{
			for (Week w : weeks)
			{
				if (w.w >= monday && w.w <= today.toEpochDay())
				{
					ms += w.playedMs;
				}
			}
		}
		long days = today.toEpochDay() - start + 1;
		if (weeks != null && days >= Ahead.MIN_DAYS)
		{
			return ms / 3_600_000.0 / days;
		}
		Year year = getYear(today.getYear());
		if (year == null)
		{
			return 0;
		}
		long yearStart = Math.max(today.withDayOfYear(1).toEpochDay(), Math.min(begun, today.toEpochDay()));
		long yearDays = today.toEpochDay() - yearStart + 1;
		return yearDays < Ahead.MIN_DAYS ? 0 : year.playedMs / 3_600_000.0 / yearDays;
	}

	/** Every week kept, oldest first. Read only. */
	public List<Week> getWeeks()
	{
		return weeks == null ? Collections.emptyList() : Collections.unmodifiableList(weeks);
	}

	/** The Monday of the last week told in chat, as an epoch day. */
	public long getToldWeek()
	{
		return toldWeek;
	}

	/** Notes that the week beginning on that Monday has been told in chat. */
	public void told(long monday)
	{
		toldWeek = Math.max(toldWeek, monday);
	}

	/** The week a moment falls in, begun if it is new. */
	Week week(long now, ZoneId zone)
	{
		if (weeks == null)
		{
			weeks = new ArrayList<>();
		}
		long monday = Week.mondayOf(Instant.ofEpochMilli(now).atZone(zone).toLocalDate());
		Week last = weeks.isEmpty() ? null : weeks.get(weeks.size() - 1);
		if (last != null && last.w == monday)
		{
			return last;
		}
		for (Week w : weeks)
		{
			if (w.w == monday)
			{
				return w;
			}
		}
		Week week = new Week(monday);
		weeks.add(week);
		weeks.sort((a, b) -> Long.compare(a.w, b.w));
		while (weeks.size() > MAX_WEEKS)
		{
			weeks.remove(0);
		}
		return week;
	}

	/**
	 * Fills the weeks from the daily readings the pace keeps, for a save from before weeks were
	 * kept: the experience between one day's first reading and the next is put on the first day's week,
	 * unless the two readings are days apart in different weeks.
	 */
	private void backfill()
	{
		List<Pace.Snap> days = pace.days;
		for (int i = 0; i + 1 < days.size(); i++)
		{
			Pace.Snap from = days.get(i);
			Pace.Snap to = days.get(i + 1);
			if (to.d - from.d > 1 && Week.mondayOf(java.time.LocalDate.ofEpochDay(to.d)) != Week.mondayOf(java.time.LocalDate.ofEpochDay(from.d)))
			{
				// A break across weeks: what was done in it cannot be put on any one week.
				continue;
			}
			Week week = null;
			for (Map.Entry<String, Long> e : to.xp.entrySet())
			{
				Long before = from.xp.get(e.getKey());
				if (before == null || before <= 0 || e.getValue() <= before)
				{
					continue;
				}
				if (week == null)
				{
					week = weekOn(Week.mondayOf(java.time.LocalDate.ofEpochDay(from.d)));
				}
				week.xp.merge(e.getKey(), e.getValue() - before, Long::sum);
				week.end.merge(e.getKey(), e.getValue(), Math::max);
			}
		}
	}

	private Week weekOn(long monday)
	{
		for (Week w : weeks)
		{
			if (w.w == monday)
			{
				return w;
			}
		}
		Week week = new Week(monday);
		weeks.add(week);
		weeks.sort((a, b) -> Long.compare(a.w, b.w));
		return week;
	}

	/** A skill's experience or level changed. */
	public List<Entry> stat(String skill, long xpNow, int levelNow, long now, ZoneId zone, boolean everyLevel)
	{
		List<Entry> written = new ArrayList<>();
		if (!isBegun())
		{
			return written;
		}
		Long xpBefore = xp.get(skill);
		Integer levelBefore = levels.get(skill);
		if (xpNow <= 0 && xpBefore != null && xpBefore > 0)
		{
			// Not a real reading: experience is never lost outside of a world that takes it, and never all of it.
			return written;
		}
		// The day's first reading is where the skills stood before this change, so today's experience counts it.
		pace.note(Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay(), xp);
		Year year = year(now, zone);
		year.startXp.putIfAbsent(skill, xpBefore == null ? xpNow : xpBefore);
		year.startLevel.putIfAbsent(skill, levelBefore == null ? levelNow : levelBefore);
		year.endXp.put(skill, xpNow);
		year.endLevel.put(skill, levelNow);
		if (xpBefore != null && xpNow > xpBefore)
		{
			Week week = week(now, zone);
			week.xp.merge(skill, xpNow - xpBefore, Long::sum);
			week.end.put(skill, xpNow);
		}
		xp.put(skill, xpNow);
		levels.put(skill, levelNow);
		if (xpBefore == null || levelBefore == null)
		{
			// A skill seen for the first time, such as one added to the game: nothing to compare with.
			return written;
		}
		if (xpNow < xpBefore)
		{
			// Experience taken away, as a Deadman world does on death: follow it quietly, it is no milestone.
			return written;
		}

		int totalBefore = totalLevel() - levelNow + levelBefore;
		if (levelNow > levelBefore)
		{
			if (levelNow >= 99 && levelBefore < 99)
			{
				int maxed = 0;
				for (int level : levels.values())
				{
					maxed += level >= 99 ? 1 : 0;
				}
				String detail = maxed == 1 ? "Your first 99" : maxed == levels.size() ? "Every skill at 99" : "99 number " + maxed;
				written.add(add(new Entry(Entry.Kind.MAX, now, "99 " + skill, detail).skill(skill, 99)));
			}
			else if (everyLevel || levelNow % 10 == 0)
			{
				String title = levelNow - levelBefore > 1 ? skill + " " + levelBefore + " to " + levelNow : skill + " " + levelNow;
				Entry level = new Entry(Entry.Kind.LEVEL, now, title, "Level up").skill(skill, levelNow);
				level.from = levelBefore;
				written.add(add(level));
			}
			int totalNow = totalLevel();
			int maxTotal = levels.size() * 99;
			if (totalNow >= maxTotal && totalBefore < maxTotal)
			{
				written.add(add(new Entry(Entry.Kind.TOTAL, now, "Total level " + number(totalNow), MAX_TOTAL_DETAIL)));
			}
			else if (totalNow / 100 > totalBefore / 100 && totalNow >= 500)
			{
				int milestone = totalNow / 100 * 100;
				written.add(add(new Entry(Entry.Kind.TOTAL, now, "Total level " + number(milestone), "Total level milestone")));
			}
		}

		for (long mark : SKILL_XP)
		{
			if (xpBefore < mark && xpNow >= mark)
			{
				String detail = mark == MAX_XP ? "The most experience a skill can hold" : "Experience milestone";
				written.add(add(new Entry(Entry.Kind.XP, now, (mark / 1_000_000) + "M " + skill, detail).skill(skill, levelNow)));
			}
		}
		long totalXpNow = totalXp();
		long totalXpBefore = totalXpNow - xpNow + xpBefore;
		long mark = totalXpMilestone(totalXpBefore, totalXpNow);
		if (mark > 0)
		{
			written.add(add(new Entry(Entry.Kind.XP, now, shortAmount(mark) + " total experience", "Experience milestone")));
		}
		return written;
	}

	/** The local player's combat level, read each tick; only round numbers and the maximum are written. */
	public Entry combat(int level, long now)
	{
		if (!isBegun() || level <= combat)
		{
			if (level > 0 && combat == 0)
			{
				combat = level;
			}
			return null;
		}
		int before = combat;
		combat = level;
		if (before == 0)
		{
			return null;
		}
		if (level >= 126)
		{
			return add(new Entry(Entry.Kind.COMBAT, now, "Combat level 126", "The highest combat level there is"));
		}
		if (level / 10 > before / 10 && level >= 50)
		{
			return add(new Entry(Entry.Kind.COMBAT, now, "Combat level " + (level / 10 * 10), "Combat milestone"));
		}
		return null;
	}

	/** The game announced a kill count. The year counts every kill; the annals keep the first and the round numbers. */
	public Entry killCount(String name, int count, long now, ZoneId zone)
	{
		if (!isBegun() || count <= 0)
		{
			return null;
		}
		Integer before = kc.put(name, count);
		int gained = before == null || count <= before || count - before > 500 ? 1 : count - before;
		if (before != null && count == before)
		{
			return null;
		}
		year(now, zone).kills.merge(name, gained, Integer::sum);
		week(now, zone).kills.merge(name, gained, Integer::sum);
		long day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay();
		pace.note(day, xp);
		Pace.Snap today = pace.on(day);
		if (today != null)
		{
			if (today.k == null)
			{
				today.k = new LinkedHashMap<>();
			}
			today.k.merge(name, gained, Integer::sum);
		}
		if (count == 1)
		{
			return add(new Entry(Entry.Kind.BOSS, now, "First " + name, "Kill count 1").skill(null, 1));
		}
		for (int milestone : KC_MILESTONES)
		{
			if (count == milestone || (before != null && before < milestone && count > milestone))
			{
				return add(new Entry(Entry.Kind.BOSS, now, name + " " + number(milestone), "Kill count milestone").skill(null, milestone));
			}
		}
		return null;
	}

	/**
	 * A single drop worth telling of. The detail names where it came from; its value goes with it.
	 * A drop that also filled a collection log slot is one line, not two: the slot takes the drop's value.
	 */
	public Entry drop(String item, int quantity, int itemId, long value, String source, long now)
	{
		if (!isBegun())
		{
			return null;
		}
		Entry slot = recent(Entry.Kind.COLLECTION, item, now);
		if (slot != null)
		{
			slot.item(itemId, value);
			slot.detail = source == null ? "New collection log slot" : "New log slot from " + source;
			return slot;
		}
		String title = quantity > 1 ? number(quantity) + " x " + item : item;
		return add(new Entry(Entry.Kind.DROP, now, title, source == null ? "Notable drop" : "From " + source).item(itemId, value));
	}

	/** An entry of a kind about an item written in the last moments, or null. */
	private Entry recent(Entry.Kind kind, String title, long now)
	{
		for (int i = entries.size() - 1; i >= 0; i--)
		{
			Entry e = entries.get(i);
			if (now - e.at > MERGE_MS)
			{
				return null;
			}
			if (e.getKind() == kind && e.getTitle().equalsIgnoreCase(title))
			{
				return e;
			}
		}
		return null;
	}

	/** Files an entry read from the chat box. Clues are all counted, but only the milestones are kept. */
	public Entry record(Entry entry, long now, ZoneId zone)
	{
		if (!isBegun() || entry == null)
		{
			return null;
		}
		if (entry.getKind() == Entry.Kind.COLLECTION)
		{
			Entry pet = unslottedPet(now);
			if (pet != null)
			{
				// The game names a new pet in the collection log in the same breath as it arrives.
				pet.title = entry.getTitle();
				pet.slot = Boolean.TRUE;
				return pet;
			}
			Entry drop = recent(Entry.Kind.DROP, entry.getTitle(), now);
			if (drop != null)
			{
				drop.kind = Entry.Kind.COLLECTION;
				drop.detail = drop.detail.startsWith("From ") ? "New log slot from " + drop.detail.substring(5) : entry.detail;
				return drop;
			}
		}
		if (entry.getKind() == Entry.Kind.CLUE)
		{
			year(now, zone).clues++;
			if (!ChatParser.clueMilestone(entry.getLevel()))
			{
				return null;
			}
		}
		return add(entry);
	}

	/** How soon after a pet its collection log line must come to be about that pet. */
	static final long PET_SLOT_MS = 1_800;
	/** How long after a pet arrives its follower may still name it. */
	static final long PET_NAME_MS = 10_000;

	/** A pet written a moment ago whose collection log line has not come yet, or null. */
	private Entry unslottedPet(long now)
	{
		for (int i = entries.size() - 1; i >= 0; i--)
		{
			Entry e = entries.get(i);
			if (now - e.at > PET_SLOT_MS)
			{
				return null;
			}
			if (e.getKind() == Entry.Kind.PET && !e.isSlot())
			{
				return e;
			}
		}
		return null;
	}

	/**
	 * Names a pet that has just arrived after the follower that came out with it, when the game
	 * did not name it in the collection log.
	 *
	 * @return the pet named, or null
	 */
	public Entry namePet(String name, long now)
	{
		if (name == null || name.isEmpty())
		{
			return null;
		}
		for (int i = entries.size() - 1; i >= 0; i--)
		{
			Entry e = entries.get(i);
			if (now - e.at > PET_NAME_MS)
			{
				return null;
			}
			if (e.getKind() == Entry.Kind.PET)
			{
				if (!ChatParser.UNNAMED_PET.equals(e.getTitle()))
				{
					return null;
				}
				e.title = name;
				return e;
			}
		}
		return null;
	}

	/**
	 * Strikes one entry from the annals, for the rare line that should not be there. The prologue
	 * stays; the year's play time and kill counts are kept, as they were not written by the entry.
	 *
	 * @return whether an entry was struck
	 */
	public boolean remove(long at, Entry.Kind kind, String title)
	{
		for (int i = entries.size() - 1; i >= 0; i--)
		{
			Entry e = entries.get(i);
			if (e.at == at && e.getKind() == kind && e.getTitle().equals(title) && kind != Entry.Kind.BEGIN)
			{
				entries.remove(i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Gives recent entries named after an item, such as a collection log slot, that item's
	 * picture once the loot it came from is seen.
	 */
	public void attachItem(String name, int itemId, long since)
	{
		for (int i = entries.size() - 1; i >= 0; i--)
		{
			Entry e = entries.get(i);
			if (e.at < since)
			{
				break;
			}
			if (e.getItemId() == 0 && e.getTitle().equalsIgnoreCase(name))
			{
				e.itemId = itemId;
			}
		}
	}

	/** Time logged in. Called every game tick with the time it stands for. */
	public void tick(long now, long ms, ZoneId zone)
	{
		if (!isBegun())
		{
			return;
		}
		Year year = year(now, zone);
		year.playedMs += ms;
		week(now, zone).playedMs += ms;
		long day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay();
		pace.note(day, xp);
		Pace.Snap today = pace.on(day);
		if (today != null)
		{
			today.ms += ms;
		}
		if (year.days.isEmpty() || year.days.get(year.days.size() - 1) != day)
		{
			if (!year.days.contains(day))
			{
				year.days.add(day);
				Collections.sort(year.days);
			}
		}
	}

	/** The year a moment falls in, begun from the last readings if it is new. */
	Year year(long now, ZoneId zone)
	{
		int number = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone).getYear();
		Year last = years.isEmpty() ? null : years.get(years.size() - 1);
		if (last != null && last.year == number)
		{
			return last;
		}
		Year found = getYear(number);
		if (found != null)
		{
			return found;
		}
		Year year = new Year(number);
		year.startXp.putAll(xp);
		year.endXp.putAll(xp);
		year.startLevel.putAll(levels);
		year.endLevel.putAll(levels);
		years.add(year);
		years.sort((a, b) -> Integer.compare(a.year, b.year));
		return year;
	}

	int totalLevel()
	{
		int total = 0;
		for (int level : levels.values())
		{
			total += level;
		}
		return total;
	}

	long totalXp()
	{
		long total = 0;
		for (long x : xp.values())
		{
			total += x;
		}
		return total;
	}

	private Entry add(Entry entry)
	{
		entries.add(entry);
		if (entries.size() > MAX_ENTRIES)
		{
			compact();
		}
		return entry;
	}

	/** Drops the least telling entry, the oldest of them on a tie, until the annals fit. */
	private void compact()
	{
		while (entries.size() > MAX_ENTRIES)
		{
			int worst = -1;
			for (int i = 0; i < entries.size() - 1; i++)
			{
				Entry e = entries.get(i);
				if (e.getKind() == Entry.Kind.BEGIN)
				{
					continue;
				}
				if (worst < 0 || e.rank() < entries.get(worst).rank())
				{
					worst = i;
				}
			}
			if (worst < 0)
			{
				return;
			}
			entries.remove(worst);
		}
	}

	/** The total experience mark crossed between two readings, or 0. */
	static long totalXpMilestone(long before, long now)
	{
		long[] marks = {10_000_000L, 25_000_000L, 50_000_000L, 100_000_000L, 250_000_000L, 500_000_000L};
		long found = 0;
		for (long mark : marks)
		{
			if (before < mark && now >= mark)
			{
				found = mark;
			}
		}
		// Past half a billion, every further half billion.
		long step = 500_000_000L;
		if (now >= step * 2 && before / step < now / step)
		{
			found = now / step * step;
		}
		return found;
	}

	static String shortAmount(long n)
	{
		if (n >= 1_000_000_000L)
		{
			String text = String.format(Locale.ENGLISH, "%.1f", n / 1_000_000_000.0);
			return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text) + "B";
		}
		return n / 1_000_000 + "M";
	}

	static String number(long n)
	{
		return String.format(Locale.ENGLISH, "%,d", n);
	}
}
