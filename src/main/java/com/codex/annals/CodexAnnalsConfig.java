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
		name = "Milestones in chat",
		description = "Add a line to the chat box when a 99, a pet, a first kill or another milestone is written down",
		position = 0,
		section = display
	)
	default boolean chatLine()
	{
		return true;
	}

	@ConfigItem(
		keyName = "memories",
		name = "On this day at login",
		description = "When you log in on the anniversary of something in your annals, such as a 99 a year ago, add a line to the chat box",
		position = 1,
		section = display
	)
	default boolean memories()
	{
		return true;
	}

	@ConfigItem(
		keyName = "weekly",
		name = "Last week at login",
		description = "At your first login of a new week, add a line to the chat box with what the week before held: time played, experience, kills and log slots",
		position = 2,
		section = display
	)
	default boolean weekly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nameOnCard",
		name = "Name on cards",
		description = "Write your character's name on the Year in Gielinor card and on keepsake cards; turn off to share them without it",
		position = 3,
		section = display
	)
	default boolean nameOnCard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "worthOnCard",
		name = "Worth on cards",
		description = "Print your account's worth from Codex Ledger on the Year in Gielinor card and on keepsake cards; the panel shows it either way",
		position = 4,
		section = display
	)
	default boolean worthOnCard()
	{
		return false;
	}
}
