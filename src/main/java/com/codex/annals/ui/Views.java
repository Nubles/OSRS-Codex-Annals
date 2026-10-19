package com.codex.annals.ui;

import com.codex.annals.core.Entry;
import com.codex.annals.core.Pets;
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

	/** The quest point cape: the game's own picture of quests done. */
	static final int QUEST_ITEM = 9813;
	/** The achievement diary cape. */
	static final int DIARY_ITEM = 19476;

	/** The picture an entry shows: its item, else its skill, else the picture of its kind (a pet's own, a quest's or diary's cape), else none. */
	static BufferedImage icon(Entry e, ItemIcons items, Icons icons, Runnable onLoaded)
	{
		int item = item(e);
		if (item > 0 && items != null)
		{
			return items.icon(item, onLoaded);
		}
		if (e.getSkill() != null && icons != null)
		{
			return icons.skill(e.getSkill());
		}
		return null;
	}

	/** The item an entry is pictured by, or 0 for none. */
	static int item(Entry e)
	{
		if (e.getItemId() > 0)
		{
			return e.getItemId();
		}
		switch (e.getKind())
		{
			case PET:
				return Pets.item(e.getTitle());
			case QUEST:
				return QUEST_ITEM;
			case DIARY:
				return DIARY_ITEM;
			default:
				return 0;
		}
	}
}
