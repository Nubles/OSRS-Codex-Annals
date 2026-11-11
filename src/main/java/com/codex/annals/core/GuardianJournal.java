package com.codex.annals.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Codex Guardian's run journal, read for its deaths, close calls and milestones when Guardian
 * does not yet write a story of its own: an older Guardian still has its deaths told once, in
 * Guardian's words. Only read, never written; anything that does not read is left out.
 */
public final class GuardianJournal
{
	public static final String GROUP = "codexguardian";
	public static final String KEY = "journal";

	private GuardianJournal()
	{
	}

	/** The journal's telling moments as story moments, oldest first; empty when there is no journal. */
	public static List<Story.Moment> moments(Gson gson, String raw)
	{
		JsonObject o = Json.object(gson, raw);
		JsonArray entries = Json.array(o, "entries");
		if (entries == null)
		{
			return Collections.emptyList();
		}
		List<Story.Moment> out = new ArrayList<>();
		for (JsonElement e : entries)
		{
			if (e == null || !e.isJsonObject())
			{
				continue;
			}
			JsonObject j = e.getAsJsonObject();
			long at = Json.whole(j, "at", 0);
			String type = Json.text(j, "type");
			String title = Json.text(j, "title");
			String detail = Json.text(j, "detail");
			if (at <= 0 || type == null || title == null || title.trim().isEmpty())
			{
				continue;
			}
			String kind;
			String figure = null;
			switch (type)
			{
				case "DEATH":
				case "UNCONFIRMED_DEATH":
					kind = "death";
					break;
				case "SAFE_DEATH":
					kind = "death";
					figure = "a safe death";
					break;
				case "CLOSE_CALL":
					kind = "closeCall";
					break;
				case "STATUS_LOST":
					kind = "run";
					break;
				case "MILESTONE":
					kind = "milestone";
					break;
				default:
					continue;
			}
			String text = detail == null || detail.trim().isEmpty() ? title : title + ": " + detail;
			out.add(new Story.Moment(Story.App.GUARDIAN, at, kind, Json.cut(text, Story.MAX_TEXT), figure, null));
		}
		out.sort((a, b) -> Long.compare(a.getT(), b.getT()));
		return out.size() > Story.MAX_MOMENTS ? new ArrayList<>(out.subList(out.size() - Story.MAX_MOMENTS, out.size())) : out;
	}
}
