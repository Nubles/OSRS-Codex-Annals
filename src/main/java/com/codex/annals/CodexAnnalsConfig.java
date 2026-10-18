package com.codex.annals;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(CodexAnnalsConfig.GROUP)
public interface CodexAnnalsConfig extends Config
{
	String GROUP = "codexannals";

	@ConfigSection(
		name = "Annals",
		description = "What the annals write down",
		position = 0
	)
	String annals = "annals";

	@ConfigSection(
		name = "Display",
		description = "What Codex Annals shows in the game and on the card",
		position = 1
	)
	String display = "display";

	@ConfigItem(
		keyName = "everyLevel",
		name = "Every level",
		description = "Write down every level gained; turn off to keep only every tenth level and the 99s",
		position = 0,
		section = annals
	)
	default boolean everyLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notableDrop",
		name = "Notable drop value",
		description = "A single drop worth at least this many coins, at the Grand Exchange price, is written into the annals",
		position = 1,
		section = annals
	)
	@Range(min = 10_000)
	default int notableDrop()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "chatLine",
		name = "Announce milestones",
		description = "Put a line in the chat box when a 99, a pet, a first kill or another milestone is written down",
		position = 0,
		section = display
	)
	default boolean chatLine()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nameOnCard",
		name = "Name on card",
		description = "Write your character's name on the Year in Gielinor card",
		position = 1,
		section = display
	)
	default boolean nameOnCard()
	{
		return true;
	}
}
