package com.codex.annals.core;

import java.util.Locale;

/**
 * Coins and experience as every Codex plugin writes them: in full below 10,000, whole k below a
 * million, M to one decimal, B to two. Rounding that reaches the next unit is written in it, so
 * 999,600 reads "1.0M", never "1000k".
 */
public final class Amount
{
	private Amount()
	{
	}

	public static String of(long n)
	{
		if (n < 0)
		{
			return "-" + of(n == Long.MIN_VALUE ? Long.MAX_VALUE : -n);
		}
		if (n < 10_000)
		{
			return String.format(Locale.ENGLISH, "%,d", n);
		}
		long k = Math.round(n / 1_000.0);
		if (k < 1_000)
		{
			return k + "k";
		}
		long tenths = Math.round(n / 100_000.0);
		if (tenths < 10_000)
		{
			return tenths / 10 + "." + tenths % 10 + "M";
		}
		long hundredths = Math.round(n / 10_000_000.0);
		return String.format(Locale.ENGLISH, "%,d", hundredths / 100) + "." + String.format(Locale.ENGLISH, "%02d", hundredths % 100) + "B";
	}
}
