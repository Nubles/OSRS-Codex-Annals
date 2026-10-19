package com.codex.annals.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class KeepsakeCardTest
{
	@Test
	public void saysNothingTwiceUnderTheName()
	{
		// A quest's stored detail only repeats the kind written above its name.
		assertNull(KeepsakeCard.subline("a quest complete", "Quest complete"));
		assertEquals("Pet from Vorkath", KeepsakeCard.subline("a pet", "Pet from Vorkath"));
		assertNull(KeepsakeCard.subline("a pet", ""));
		assertNull(KeepsakeCard.subline("a pet", null));
	}
}
