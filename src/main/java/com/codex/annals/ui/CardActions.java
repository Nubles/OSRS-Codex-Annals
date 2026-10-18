package com.codex.annals.ui;

import java.awt.image.BufferedImage;

/** What the card's buttons do; the plugin answers. */
public interface CardActions
{
	void save(BufferedImage card, int year);

	void copy(BufferedImage card);
}
