package com.codex.annals.ui;

import java.awt.image.BufferedImage;

/** What the card's buttons do; the plugin answers. */
public interface CardActions
{
	/** @param name the file name offered, without its extension */
	void save(BufferedImage card, String name);

	void copy(BufferedImage card);

	/**
	 * Saves a picture straight into RuneLite's screenshot folder, as Gielinor Wrapped does with its
	 * story; by default it asks where, as {@link #save} does.
	 *
	 * @param name the file name, without its extension or the date RuneLite adds
	 */
	default void screenshot(BufferedImage image, String name)
	{
		save(image, name);
	}
}
