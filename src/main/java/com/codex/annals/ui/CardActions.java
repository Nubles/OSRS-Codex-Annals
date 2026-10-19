package com.codex.annals.ui;

import java.awt.image.BufferedImage;

/** What the card's buttons do; the plugin answers. */
public interface CardActions
{
	/** @param name the file name offered, without its extension */
	void save(BufferedImage card, String name);

	void copy(BufferedImage card);
}
