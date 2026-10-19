package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatTest
{
	@Test
	public void amountsFollowTheSuiteRule()
	{
		// In full below 10,000; whole k below a million; M to one decimal; B to two.
		assertEquals("8,240", Format.amount(8_240));
		assertEquals("10k", Format.amount(10_000));
		assertEquals("41k", Format.amount(41_200));
		assertEquals("96k", Format.amount(95_700));
		assertEquals("412k", Format.amount(412_000));
		assertEquals("1.2M", Format.amount(1_240_000));
		assertEquals("2.4M", Format.amount(2_370_000));
		assertEquals("26.0M", Format.amount(26_000_000));
		assertEquals("98.7M", Format.amount(98_700_000));
		assertEquals("812.4M", Format.amount(812_400_000));
		assertEquals("2.10B", Format.amount(2_100_000_000L));
		// Rounding that reaches the next unit is written in it.
		assertEquals("100k", Format.amount(99_960));
		assertEquals("1.0M", Format.amount(999_600));
		assertEquals("999k", Format.amount(999_499));
		assertEquals("1.00B", Format.amount(999_960_000));
		assertEquals("+880k", Format.xp(880_000));
		assertEquals("9,223,372,036.85B", Format.amount(Long.MAX_VALUE));
	}

	@Test
	public void signsChangesTheSameWay()
	{
				assertEquals("-4.1M", Format.signedCompact(-4_100_000));
		assertEquals("+248.4M", Format.signedCoins(248_400_000));
		assertEquals("0", Format.signedCoins(0));
	}

	@Test
	public void aFirstDaysPlayAgreesWithTheYearsHero()
	{
		// The crest once read "Hours this year 0" beside "5 minutes played".
		assertEquals("5m", Format.played(5 * 60_000L + 30_000));
		assertEquals("5 minutes", Format.hours(5 * 60_000L + 30_000));
		assertEquals("59m", Format.played(59 * 60_000L));
		assertEquals("1h", Format.played(60 * 60_000L));
		assertEquals("1,203h", Format.played(1_203 * 3_600_000L));
		assertEquals("0m", Format.played(-5));
	}
}
