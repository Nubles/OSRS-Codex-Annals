package com.codex.annals.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Fail-safe reading of what other Codex plugins save: every helper here gives back a plain
 * default instead of throwing, whatever shape the text turns out to be.
 */
public final class Json
{
	private Json()
	{
	}

	/** The saved text as a JSON object, or null when it is absent, empty, malformed or not an object. */
	public static JsonObject object(Gson gson, String raw)
	{
		if (raw == null || raw.trim().isEmpty())
		{
			return null;
		}
		try
		{
			JsonElement e = gson.fromJson(raw, JsonElement.class);
			return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		}
		catch (RuntimeException | StackOverflowError e)
		{
			return null;
		}
	}

	/**
	 * Whether a record's {@code version} is one this reader knows: 1, or absent in a record from
	 * before the field was written. A later version is a different shape, read as no record at all.
	 */
	public static boolean versionOne(JsonObject o)
	{
		if (o == null)
		{
			return false;
		}
		JsonElement v = o.get("version");
		if (v == null || v.isJsonNull())
		{
			return true;
		}
		double n = number(v, Double.NaN);
		return n == 1;
	}

	public static JsonObject object(JsonObject o, String key)
	{
		JsonElement e = o == null ? null : o.get(key);
		return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
	}

	public static JsonArray array(JsonObject o, String key)
	{
		JsonElement e = o == null ? null : o.get(key);
		return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
	}

	/** A number, or the fallback when it is missing, not a number, or not finite. */
	public static double number(JsonElement e, double fallback)
	{
		if (e == null || !e.isJsonPrimitive())
		{
			return fallback;
		}
		JsonPrimitive p = e.getAsJsonPrimitive();
		try
		{
			double d = p.isNumber() ? p.getAsDouble() : p.isString() ? Double.parseDouble(p.getAsString().trim()) : Double.NaN;
			return Double.isNaN(d) || Double.isInfinite(d) ? fallback : d;
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	public static double number(JsonObject o, String key, double fallback)
	{
		return o == null ? fallback : number(o.get(key), fallback);
	}

	/** A whole number clamped to a long, or the fallback. */
	public static long whole(JsonObject o, String key, long fallback)
	{
		double d = number(o, key, Double.NaN);
		if (Double.isNaN(d))
		{
			return fallback;
		}
		if (d >= Long.MAX_VALUE)
		{
			return Long.MAX_VALUE;
		}
		if (d <= Long.MIN_VALUE)
		{
			return Long.MIN_VALUE;
		}
		return (long) d;
	}

	/** Text, or a number written as text, or null. */
	public static String text(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive())
		{
			return null;
		}
		JsonPrimitive p = e.getAsJsonPrimitive();
		if (p.isString())
		{
			return p.getAsString();
		}
		if (p.isNumber())
		{
			double d = number(p, Double.NaN);
			if (Double.isNaN(d))
			{
				return null;
			}
			return d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf((long) d) : String.valueOf(d);
		}
		return null;
	}

	public static String text(JsonObject o, String key)
	{
		return o == null ? null : text(o.get(key));
	}

	public static boolean bool(JsonObject o, String key)
	{
		JsonElement e = o == null ? null : o.get(key);
		try
		{
			return e != null && e.isJsonPrimitive() && (e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : "true".equalsIgnoreCase(e.getAsString().trim()));
		}
		catch (RuntimeException ex)
		{
			return false;
		}
	}

	/** Cuts text to at most that many characters, on a word where it can, with an ellipsis. */
	public static String cut(String text, int max)
	{
		if (text == null)
		{
			return null;
		}
		String t = text.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
		if (t.length() <= max)
		{
			return t;
		}
		int space = t.lastIndexOf(' ', max - 1);
		return (space > max / 2 ? t.substring(0, space) : t.substring(0, max - 1)) + "…";
	}
}
