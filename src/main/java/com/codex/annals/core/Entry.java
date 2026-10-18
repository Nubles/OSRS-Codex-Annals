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
		BEGIN(10, Chapter.ALL);

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
	int itemId;
	long value;
	int level;
	/** The level a level entry rose from. */
	int from;

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
		this.level = level;
		return this;
	}

	Entry item(int itemId, long value)
	{
		this.itemId = itemId;
		this.value = value;
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
		return level;
	}

	public int getItemId()
	{
		return itemId;
	}

	public long getValue()
	{
		return value;
	}

	/** How much the entry matters when only a few fit: its kind, then a drop's size. */
	public double rank()
	{
		double bonus = value > 0 ? Math.min(25, Math.log10(Math.max(1, value)) * 2.5) : 0;
		if (getKind() == Kind.LEVEL)
		{
			bonus = level / 10.0;
		}
		return getKind().getWeight() + bonus;
	}

	public boolean in(Chapter chapter)
	{
		return chapter == Chapter.ALL || getKind().getChapter() == chapter;
	}
}
