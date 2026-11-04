package com.codex.annals.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The drop rates Gielinor Wrapped may print beside a drop, every one as the Old School RuneScape
 * Wiki writes it. A drop is only given a rate when its item and, for an item, where it came from
 * match a line here; any other drop is told without one, never with a guess. Pets whose rate
 * depends on the mode, the team or a slayer task are left out for the same reason.
 */
public final class DropRates
{
	/** One rate: what drops it, as the wiki writes the chance, and the chance of it on one kill. */
	public static final class Rate
	{
		private final String item;
		/** The monster it must come from, or null for a pet, which is known by its own name. */
		private final String source;
		private final String text;
		private final double chance;

		Rate(String item, String source, String text, double chance)
		{
			this.item = item;
			this.source = source;
			this.text = text;
			this.chance = chance;
		}

		public String getItem()
		{
			return item;
		}

		/** The monster it comes from; for a pet, the boss named in the wiki's pet list. */
		public String getSource()
		{
			return source;
		}

		/** The chance as the wiki writes it: "1/4,000", "2 × 1/1,024". */
		public String getText()
		{
			return text;
		}

		/** The chance of it on one kill, as a fraction: 1/4,000 is 0.00025. */
		public double getChance()
		{
			return chance;
		}

		/** "1 in 4,000", rounded to the nearest whole kill: how rare it reads on a card. */
		public String oneIn()
		{
			long n = Math.round(1 / chance);
			return "1 in " + String.format(Locale.ENGLISH, "%,d", n);
		}

		boolean pet()
		{
			return pet;
		}

		private boolean pet;
	}

	private static final List<Rate> RATES = new ArrayList<>();

	static
	{
		// https://oldschool.runescape.wiki/w/Pet: the boss pets with one rate, whatever the mode.
		pet("Abyssal orphan", "Abyssal Sire", 2_560);
		pet("Baby mole", "Giant Mole", 3_000);
		pet("Baron", "Duke Sucellus", 2_500);
		pet("Beef", "Brutus", 1_000);
		pet("Butch", "Vardorvis", 3_000);
		pet("Gull", "Shellbane Gryphon", 3_000);
		pet("Hellpuppy", "Cerberus", 3_000);
		pet("Huberte", "The Hueycoatl", 400);
		pet("Ikkle hydra", "Alchemical Hydra", 3_000);
		pet("Kalphite princess", "Kalphite Queen", 3_000);
		pet("Lil'viathan", "The Leviathan", 2_500);
		pet("Moxi", "Amoxliatl", 3_000);
		pet("Muphin", "Phantom Muspah", 2_500);
		pet("Nexling", "Nex", 500);
		pet("Noon", "Grotesque Guardians", 3_000);
		pet("Olmlet", "Chambers of Xeric", 53);
		pet("Pet dagannoth prime", "Dagannoth Prime", 5_000);
		pet("Pet dagannoth rex", "Dagannoth Rex", 5_000);
		pet("Pet dagannoth supreme", "Dagannoth Supreme", 5_000);
		pet("Pet dark core", "Corporeal Beast", 5_000);
		pet("Pet general graardor", "General Graardor", 5_000);
		pet("Pet k'ril tsutsaroth", "K'ril Tsutsaroth", 5_000);
		pet("Pet kraken", "Kraken", 3_000);
		pet("Pet kree'arra", "Kree'arra", 5_000);
		pet("Pet smoke devil", "Thermonuclear smoke devil", 3_000);
		pet("Pet snakeling", "Zulrah", 4_000);
		pet("Pet zilyana", "Commander Zilyana", 5_000);
		pet("Phoenix", "Wintertodt", 5_000);
		pet("Prince black dragon", "King Black Dragon", 3_000);
		add(new Rate("Scorpia's offspring", "Scorpia", "1/2,015.75", 1 / 2_015.75), true);
		pet("Scurry", "Scurrius", 3_000);
		pet("Skotos", "Skotizo", 65);
		pet("Smolcano", "Zalcano", 2_250);
		pet("Sraracha", "Sarachnis", 3_000);
		pet("Tiny tempor", "Tempoross", 8_000);
		pet("Vorki", "Vorkath", 3_000);
		pet("Wisp", "The Whisperer", 2_000);

		// https://oldschool.runescape.wiki/w/Zulrah: the uniques are rolled twice a kill.
		add(new Rate("Tanzanite fang", "Zulrah", "2 × 1/1,024", 2 / 1_024.0), false);
		add(new Rate("Magic fang", "Zulrah", "2 × 1/1,024", 2 / 1_024.0), false);
		add(new Rate("Serpentine visage", "Zulrah", "2 × 1/1,024", 2 / 1_024.0), false);
		add(new Rate("Uncut onyx", "Zulrah", "2 × 1/1,024", 2 / 1_024.0), false);
		add(new Rate("Tanzanite mutagen", "Zulrah", "2 × 1/13,107", 2 / 13_107.0), false);
		add(new Rate("Magma mutagen", "Zulrah", "2 × 1/13,107", 2 / 13_107.0), false);
		item("Jar of swamp", "Zulrah", 3_000);
		// https://oldschool.runescape.wiki/w/Vorkath
		item("Draconic visage", "Vorkath", 5_000);
		item("Skeletal visage", "Vorkath", 5_000);
		item("Dragonbone necklace", "Vorkath", 1_000);
		item("Jar of decay", "Vorkath", 3_000);
		// https://oldschool.runescape.wiki/w/Dragon_warhammer
		item("Dragon warhammer", "Lizardman shaman", 3_000);
		// https://oldschool.runescape.wiki/w/Corporeal_Beast
		add(new Rate("Holy elixir", "Corporeal Beast", "3/512", 3 / 512.0), false);
		item("Spectral sigil", "Corporeal Beast", 1_365);
		item("Arcane sigil", "Corporeal Beast", 1_365);
		item("Elysian sigil", "Corporeal Beast", 4_095);
		item("Jar of spirits", "Corporeal Beast", 1_000);
		// https://oldschool.runescape.wiki/w/Abyssal_demon
		item("Abyssal whip", "Abyssal demon", 512);
		item("Abyssal dagger", "Abyssal demon", 32_000);
		// https://oldschool.runescape.wiki/w/Kraken
		item("Kraken tentacle", "Kraken", 400);
		item("Trident of the seas (full)", "Kraken", 512);
		item("Jar of dirt", "Kraken", 1_000);
	}

	private DropRates()
	{
	}

	private static void pet(String name, String boss, int oneIn)
	{
		add(new Rate(name, boss, "1/" + String.format(Locale.ENGLISH, "%,d", oneIn), 1.0 / oneIn), true);
	}

	private static void item(String name, String source, int oneIn)
	{
		add(new Rate(name, source, "1/" + String.format(Locale.ENGLISH, "%,d", oneIn), 1.0 / oneIn), false);
	}

	private static void add(Rate rate, boolean pet)
	{
		rate.pet = pet;
		RATES.add(rate);
	}

	/** Every rate known, for tests. */
	static List<Rate> all()
	{
		return Collections.unmodifiableList(RATES);
	}

	/**
	 * The wiki's rate for a pet, drop or log slot in the annals, or null when it has none here: an
	 * entry of another kind, an item not listed, or one that came from somewhere else.
	 */
	public static Rate of(Entry e)
	{
		if (e == null)
		{
			return null;
		}
		Entry.Kind kind = e.getKind();
		if (kind == Entry.Kind.PET)
		{
			Rate named = find(e.getTitle(), null, true);
			if (named != null)
			{
				return named;
			}
			// Named after its follower ("Snakeling", "General Graardor Jr."): the same pet by its picture.
			int item = Pets.item(e.getTitle());
			for (Rate r : RATES)
			{
				if (item != 0 && r.pet && Pets.item(r.item) == item)
				{
					return r;
				}
			}
			return null;
		}
		if (kind != Entry.Kind.DROP && kind != Entry.Kind.COLLECTION)
		{
			return null;
		}
		String source = source(e.getDetail());
		return source == null ? null : find(item(e.getTitle()), source, false);
	}

	private static Rate find(String item, String source, boolean pet)
	{
		for (Rate r : RATES)
		{
			if (r.pet == pet && r.item.equalsIgnoreCase(item) && (pet || r.source.equalsIgnoreCase(source)))
			{
				return r;
			}
		}
		return null;
	}

	/** "Tanzanite fang" from "2 x Tanzanite fang", as the annals title a stack. */
	static String item(String title)
	{
		int x = title.indexOf(" x ");
		if (x > 0 && title.substring(0, x).replace(",", "").matches("\\d+"))
		{
			return title.substring(x + 3);
		}
		return title;
	}

	/** Where a drop came from, by the detail the annals wrote: "From Zulrah", "New log slot from Zulrah"; or null. */
	static String source(String detail)
	{
		if (detail == null)
		{
			return null;
		}
		if (detail.startsWith("From "))
		{
			return detail.substring(5).trim();
		}
		int at = detail.indexOf(" from ");
		return detail.startsWith("New log slot") && at > 0 ? detail.substring(at + 6).trim() : null;
	}
}
