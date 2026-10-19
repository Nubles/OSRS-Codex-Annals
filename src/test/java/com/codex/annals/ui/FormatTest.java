package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatTest
{
	@Test
	public void tellsEveryAmountInThreeFigures()
	{
		assertEquals("8,240", Format.amount(8_240));
		assertEquals("41.2k", Format.amount(41_200));
		assertEquals("412k", Format.amount(412_000));
		assertEquals("4.90M", Format.amount(4_900_000));
		assertEquals("26.0M", Format.amount(26_000_000));
		assertEquals("98.7M", Format.amount(98_700_000));
		assertEquals("529M", Format.amount(529_400_000));
		assertEquals("1.50B", Format.amount(1_500_000_000L));
	}

	@Test
	public void roundsUpIntoTheNextUnit()
	{
		assertEquals("100k", Format.amount(99_960));
		assertEquals("1.00M", Format.amount(999_600));
		assertEquals("10.0M", Format.amount(9_999_999));
		assertEquals("1.00B", Format.amount(999_950_000L));
	}

	@Test
	public void signsChangesTheSameWay()
	{
		assertEquals("+248M", Format.signedCoins(248_400_000));
		assertEquals("-4.10M", Format.signedCompact(-4_100_000));
		assertEquals("0", Format.signedCoins(0));
	}
}
