package com.codex.annals.core;

/** The game's experience table: what each level takes, and the level a total of experience reaches. */
final class Experience
{
	static final int MAX_LEVEL = 99;
	static final long MAX_XP = 200_000_000L;
	private static final long[] TABLE = new long[MAX_LEVEL + 1];

	static
	{
		double points = 0;
		for (int level = 1; level < MAX_LEVEL; level++)
		{
			points += Math.floor(level + 300 * Math.pow(2, level / 7.0));
			TABLE[level + 1] = (long) Math.floor(points / 4);
		}
	}

	private Experience()
	{
	}

	/** The experience a level starts at: 0 for level 1, 13,034,431 for 99. */
	static long forLevel(int level)
	{
		return TABLE[Math.max(1, Math.min(MAX_LEVEL, level))];
	}

	/** The level a total of experience reaches, at most 99. */
	static int level(long xp)
	{
		for (int level = MAX_LEVEL; level > 1; level--)
		{
			if (xp >= TABLE[level])
			{
				return level;
			}
		}
		return 1;
	}
}
