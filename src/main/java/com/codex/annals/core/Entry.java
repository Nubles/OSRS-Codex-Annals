package com.codex.annals.core;

/** One line in the annals: a level, a quest, a pet, a first kill, a drop worth telling of. */
public class Entry
{
	/** What kind of entry it is; the weight is how much it matters when only a few fit. */
	public enum Kind
	{
		PET(100, Chapter.LOOT),
		MAX(95, Chapter.SKILLS),
		COLLECTION(76, Chapter.LOOT),
		DROP(72, Chapter.LOOT),
		TOTAL(70, Chapter.SKILLS),
		DIARY(64, Chapter.FEATS),
		QUEST(60, Chapter.FEATS),
		BOSS(58, Chapter.BOSSES),
		XP(52, Chapter.SKILLS),
		COMBAT(46, Chapter.SKILLS),
		TASK(40, Chapter.FEATS),
		CLUE(28, Chapter.LOOT),
		LEVEL(20, Chapter.SKILLS),
		BEGIN(10, Chapter.ALL),
		// The kinds below are told by other Codex plugins and woven in for the timeline; the annals never save them.
		/** A death, as Codex Guardian (or another plugin) told it. */
		DEATH(66, Chapter.ALL),
		/** A close call survived. */
		CLOSE_CALL(34, Chapter.ALL),
		/** A personal best from a pilot or splits plugin. */
		PB(56, Chapter.BOSSES),
		/** A find another plugin told of that the annals did not keep. */
		FIND(40, Chapter.LOOT),
		/** Any other moment another plugin told. */
		MOMENT(30, Chapter.ALL);

		private final int weight;
		private final Chapter chapter;

		Kind(int weight, Chapter chapter)
		{
			this.weight = weight;
			this.chapter = chapter;
		}

		public int getWeight()
		{
			return weight;
		}

		public Chapter getChapter()
		{
			return chapter;
		}
	}

	/** The timeline's filters. */
	public enum Chapter
	{
		ALL,
		SKILLS,
		FEATS,
		LOOT,
		BOSSES
	}

	Kind kind;
	long at;
	String title;
	String detail;
	String skill;
	// The numbers are boxed and left empty when they mean nothing, so a save of two thousand
	// entries does not spell out a zero for every one of them. Older saves with zeros read the same.
	Integer itemId;
	Long value;
	Integer level;
	/** The level a level entry rose from. */
	Integer from;
	/** A pet that also filled a collection log slot, so the year counts it as one. */
	Boolean slot;

	// Never saved: what another Codex plugin's story adds to the line, for the timeline only.
	/** The plugin that told this line, for a moment woven in from another plugin's story; null for the annals' own. */
	transient String app;
	/** The telling plugin's own reference for the moment, or null. */
	transient String ref;
	/** The chapter a woven moment is filed under, when it is not its kind's. */
	transient Chapter chapter;
	/** A second clause another plugin adds to one of the annals' own lines, or null. */
	transient String clause;
	/** The plugin that added the clause. */
	transient String clauseApp;

	Entry()
	{
	}

	public Entry(Kind kind, long at, String title, String detail)
	{
		this.kind = kind;
		this.at = at;
		this.title = title;
		this.detail = detail;
	}

	Entry skill(String skill, int level)
	{
		this.skill = skill;
		this.level = level == 0 ? null : level;
		return this;
	}

	Entry item(int itemId, long value)
	{
		this.itemId = itemId == 0 ? null : itemId;
		this.value = value == 0 ? null : value;
		return this;
	}

	public Kind getKind()
	{
		return kind == null ? Kind.LEVEL : kind;
	}

	public long getAt()
	{
		return at;
	}

	public String getTitle()
	{
		return title == null ? "" : title;
	}

	public String getDetail()
	{
		return detail;
	}

	/** The skill a level, 99 or experience entry belongs to, or null. */
	public String getSkill()
	{
		return skill;
	}

	/** The level reached, for a level or 99 entry. */
	public int getLevel()
	{
		return level == null ? 0 : level;
	}

	public int getItemId()
	{
		return itemId == null ? 0 : itemId;
	}

	public long getValue()
	{
		return value == null ? 0 : value;
	}

	/** The level a level entry rose from, or 0 when an older save did not say. */
	public int getFrom()
	{
		return from == null ? 0 : from;
	}

	/** Whether this pet also filled a collection log slot. */
	public boolean isSlot()
	{
		return slot != null && slot;
	}

	/** Empties the numbers that mean nothing, after an older save spelled them out. */
	void compact()
	{
		itemId = itemId == null || itemId == 0 ? null : itemId;
		value = value == null || value == 0 ? null : value;
		level = level == null || level == 0 ? null : level;
		from = from == null || from == 0 ? null : from;
		slot = slot == null || !slot ? null : slot;
	}

	/** How much the entry matters when only a few fit: its kind, then a drop's size. */
	public double rank()
	{
		long worth = getValue();
		double bonus = worth > 0 ? Math.min(25, Math.log10(Math.max(1, worth)) * 2.5) : 0;
		if (getKind() == Kind.LEVEL)
		{
			bonus = getLevel() / 10.0;
		}
		return getKind().getWeight() + bonus;
	}

	public boolean in(Chapter chapter)
	{
		return chapter == Chapter.ALL || (this.chapter != null ? this.chapter : getKind().getChapter()) == chapter;
	}

	/** Whether this line was told by another Codex plugin rather than written by the annals. */
	public boolean isMoment()
	{
		return app != null;
	}

	/** The Codex plugin that told this line, or null for the annals' own. */
	public String getApp()
	{
		return app;
	}

	public String getRef()
	{
		return ref;
	}

	/** What another plugin adds to this line, such as "212 kc · luckier than 66%", or null. */
	public String getClause()
	{
		return clause;
	}

	/** The plugin that added the clause, or null. */
	public String getClauseApp()
	{
		return clauseApp;
	}

	/** A copy of the line, with what was woven into it. */
	Entry copy()
	{
		Entry e = new Entry(kind, at, title, detail);
		e.skill = skill;
		e.itemId = itemId;
		e.value = value;
		e.level = level;
		e.from = from;
		e.slot = slot;
		e.app = app;
		e.ref = ref;
		e.chapter = chapter;
		e.clause = clause;
		e.clauseApp = clauseApp;
		return e;
	}
}
