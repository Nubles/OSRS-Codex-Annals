package com.codex.annals.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.annals.core.Annals;
import com.codex.annals.core.Entry;
import com.codex.annals.core.Keepsake;
import com.codex.annals.core.Wealth;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.time.ZoneOffset;
import org.junit.Test;

/** The suite's "Worth on cards": off by default, a shared card leaves the account's worth out; the panel shows it either way. */
public class WorthOnCardsTest
{
	private static boolean same(BufferedImage a, BufferedImage b)
	{
		if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
		{
			return false;
		}
		for (int y = 0; y < a.getHeight(); y += 3)
		{
			for (int x = 0; x < a.getWidth(); x += 3)
			{
				if (a.getRGB(x, y) != b.getRGB(x, y))
				{
					return false;
				}
			}
		}
		return true;
	}

	@Test
	public void cardsLeaveTheWorthOutUnlessAskedFor()
	{
		long now = System.currentTimeMillis();
		Annals a = DamagedSavesTest.history(now, 90);
		StringBuilder hist = new StringBuilder("{\"total\":836600000,\"history\":[");
		for (int i = 400; i >= 0; i--)
		{
			hist.append(i == 400 ? "" : ",").append("{\"t\":").append(now - i * 86_400_000L).append(",\"v\":").append(500_000_000L + (400 - i) * 2_000_000L).append("}");
		}
		Wealth.Saved worth = Wealth.read(new Gson(), hist.append("]}").toString());
		ItemIcons items = (id, r) -> null;
		Icons icons = name -> null;
		AnnalsPanel p = new AnnalsPanel(items, icons, new CardActions()
		{
			public void save(BufferedImage c, String n)
			{
			}

			public void copy(BufferedImage c)
			{
			}
		});
		p.setWealth(worth);
		p.update(a, ZoneOffset.UTC, "Iron Quill");
		p.showTab(3);
		BufferedImage withWorth = YearCard.render(com.codex.annals.core.Recap.of(a, java.time.LocalDate.now(ZoneOffset.UTC).getYear(), ZoneOffset.UTC),
			Wealth.of(worth, java.time.LocalDate.now(ZoneOffset.UTC).getYear(), ZoneOffset.UTC, true), true, items, icons, () -> { });
		assertFalse("off by default, the year card has no worth", same(withWorth, p.card()));
		p.setShowWorth(true);
		assertTrue("on, it has", same(withWorth, p.card()));

		// A keepsake's figures do not tell the worth that day unless asked to.
		Entry pet = null;
		for (Entry e : a.getEntries())
		{
			pet = e.getKind() == Entry.Kind.PET ? e : pet;
		}
		for (String[] tile : KeepsakeCard.tiles(Keepsake.of(a, pet, null, ZoneOffset.UTC)))
		{
			assertFalse(tile[1], "Worth".equals(tile[1]));
		}
		p.setShowWorth(false);
		p.keep(pet);
		BufferedImage kept = p.card();
		p.setShowWorth(true);
		assertFalse("the keepsake gains the worth when asked", same(kept, p.card()));
	}
}
