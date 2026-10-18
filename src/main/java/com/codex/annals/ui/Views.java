package com.codex.annals.ui;

import com.codex.annals.core.Entry;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

/** What every tab shares: the width the side panel lays out to, and the quiet note card. */
final class Views
{
	static final int WIDTH = 200;

	private Views()
	{
	}

	static Card note(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.label(title, Theme.body(), Theme.GOLD), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		return card;
	}

	/** The picture an entry shows: its item, else its skill, else none. */
	static BufferedImage icon(Entry e, ItemIcons items, Icons icons, Runnable onLoaded)
	{
		if (e.getItemId() > 0 && items != null)
		{
			return items.icon(e.getItemId(), onLoaded);
		}
		if (e.getSkill() != null && icons != null)
		{
			return icons.skill(e.getSkill());
		}
		return null;
	}
}
