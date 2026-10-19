package com.codex.annals.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Every pet's picture, by the name the game gives it. Pets cannot be traded, so the price search
 * the annals use for a log slot's picture never finds one; this list, taken from the game's item
 * names, does. A pet the list does not know keeps the kind's own mark.
 */
public final class Pets
{
	private static final Map<String, Integer> ITEMS = new HashMap<>();

	static
	{
		put("Pet chaos elemental", 11995);
		put("Pet Dagannoth Supreme", 12643);
		put("Pet Dagannoth Prime", 12644);
		put("Pet Dagannoth Rex", 12645);
		put("Baby Mole", 12646);
		put("Kalphite Princess", 12647);
		put("Pet Smoke Devil", 12648);
		put("Pet Kree'arra", 12649);
		put("Pet General Graardor", 12650);
		put("Pet Zilyana", 12651);
		put("Pet K'ril Tsutsaroth", 12652);
		put("Prince Black Dragon", 12653);
		put("Pet Kraken", 12655);
		put("Pet Penance Queen", 12703);
		put("Pet dark core", 12816);
		put("Pet Snakeling", 12921);
		put("Chompy chick", 13071);
		put("Venenatis spiderling", 13177);
		put("Callisto cub", 13178);
		put("Vet'ion jr.", 13179);
		put("Scorpia's offspring", 13181);
		put("TzRek-Jad", 13225);
		put("Hellpuppy", 13247);
		put("Abyssal orphan", 13262);
		put("Heron", 13320);
		put("Rock golem", 13321);
		put("Beaver", 13322);
		put("Baby chinchompa", 13323);
		put("Bloodhound", 19730);
		put("Giant Squirrel", 20659);
		put("Tangleroot", 20661);
		put("Rocky", 20663);
		put("Rift guardian", 20665);
		put("Phoenix", 20693);
		put("Olmlet", 20851);
		put("Skotos", 21273);
		put("Jal-Nib-Rek", 21291);
		put("Herbi", 21509);
		put("Noon", 21748);
		put("Midnight", 21750);
		put("Vorki", 21992);
		put("Pet Corporeal Critter", 22318);
		put("TzRek-Zuk", 22319);
		put("Puppadile", 22376);
		put("Tektiny", 22378);
		put("Vanguard", 22380);
		put("Vasa Minirio", 22382);
		put("Vespina", 22384);
		put("Lil' Zik", 22473);
		put("Ikkle Hydra", 22746);
		put("Sraracha", 23495);
		put("Youngllef", 23757);
		put("Corrupted Youngllef", 23759);
		put("Smolcano", 23760);
		put("Little Nightmare", 24491);
		put("Enraged Tektiny", 24656);
		put("Flying Vespina", 24658);
		put("Dark Squirrel", 24701);
		put("Red", 24847);
		put("Ziggy", 24849);
		put("Lil' Creator", 25348);
		put("Lil' Destructor", 25350);
		put("JalRek-Jad", 25519);
		put("Great blue heron", 25600);
		put("Tiny tempor", 25602);
		put("Baby Mole-rat", 25613);
		put("Lil' Maiden", 25748);
		put("Lil' Bloat", 25749);
		put("Lil' Nylo", 25750);
		put("Lil' Sot", 25751);
		put("Lil' Xarp", 25752);
		put("Little parasite", 25836);
		put("Nexling", 26348);
		put("Greatish guardian", 26899);
		put("Abyssal protector", 26901);
		put("Tumeken's guardian", 27352);
		put("Elidinis' guardian", 27354);
		put("Akkhito", 27382);
		put("Babi", 27383);
		put("Kephriti", 27384);
		put("Zebo", 27385);
		put("Tumeken's damaged guardian", 27386);
		put("Elidinis' damaged guardian", 27387);
		put("Muphin", 27590);
		put("Wisp", 28246);
		put("Butch", 28248);
		put("Baron", 28250);
		put("Lil'viathan", 28252);
		put("Pheasant", 28669);
		put("Fox", 28670);
		put("Scurry", 28801);
		put("Smol Heredit", 28960);
		put("Quetzin", 28962);
		put("Nid", 29836);
		put("Rax", 29838);
		put("Bone Squirrel", 30151);
		put("Huberte", 30152);
		put("Moxi", 30154);
		put("Bran", 30622);
		put("Ric", 30624);
		put("Yami", 30888);
		put("Dom", 31130);
		put("Soup", 31283);
		put("Gull", 31285);
		put("Gulliver", 31287);
		put("Beef", 33124);
		put("Maggot marquess", 33642);
		put("Mr McGroot", 34040);
		put("Aggy", 34042);
	}

	private Pets()
	{
	}

	private static void put(String name, int itemId)
	{
		ITEMS.put(name.toLowerCase(Locale.ENGLISH), itemId);
	}

	/** The item that pictures a pet, by its name in any case ("Vorki", "Pet snakeling"), or 0 for one not known. */
	public static int item(String name)
	{
		if (name == null)
		{
			return 0;
		}
		Integer id = ITEMS.get(name.trim().toLowerCase(Locale.ENGLISH));
		return id == null ? 0 : id;
	}
}
