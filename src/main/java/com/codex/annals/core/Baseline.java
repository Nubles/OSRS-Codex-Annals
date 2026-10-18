package com.codex.annals.core;

import java.util.ArrayList;
import java.util.List;

/** Where the account stood on the day the annals began: the prologue to everything written after. */
public class Baseline
{
	long at;
	int totalLevel;
	long totalXp;
	int combat;
	int questPoints;
	int questsDone;
	int questsTotal;
	List<String> maxed = new ArrayList<>();

	Baseline()
	{
	}

	public Baseline(long at, int totalLevel, long totalXp, int combat, int questPoints, int questsDone, int questsTotal, List<String> maxed)
	{
		this.at = at;
		this.totalLevel = totalLevel;
		this.totalXp = totalXp;
		this.combat = combat;
		this.questPoints = questPoints;
		this.questsDone = questsDone;
		this.questsTotal = questsTotal;
		this.maxed = new ArrayList<>(maxed);
	}

	public long getAt()
	{
		return at;
	}

	public int getTotalLevel()
	{
		return totalLevel;
	}

	public long getTotalXp()
	{
		return totalXp;
	}

	public int getCombat()
	{
		return combat;
	}

	public int getQuestPoints()
	{
		return questPoints;
	}

	public int getQuestsDone()
	{
		return questsDone;
	}

	public int getQuestsTotal()
	{
		return questsTotal;
	}

	public List<String> getMaxed()
	{
		return maxed == null ? new ArrayList<>() : maxed;
	}
}
