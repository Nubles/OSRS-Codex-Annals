package com.codex.annals;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexAnnalsPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexAnnalsPlugin.class);
		RuneLite.main(args);
	}
}
