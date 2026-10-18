package com.codex.annals;

import com.codex.annals.core.Annals;
import com.codex.annals.core.Baseline;
import com.codex.annals.core.ChatParser;
import com.codex.annals.core.Entry;
import com.codex.annals.ui.AnnalsPanel;
import com.codex.annals.ui.CardActions;
import com.codex.annals.ui.Icons;
import com.codex.annals.ui.ItemIcons;
import com.codex.annals.ui.Sigil;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemPrice;
import net.runelite.http.api.loottracker.LootRecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Codex Annals",
	description = "A lifetime timeline of your account, every level, quest, diary, log slot, pet and first kill, and a yearly Year in Gielinor card to share",
	tags = {"timeline", "history", "annals", "year", "recap", "milestones", "levels", "quests", "collection", "card", "share", "codex"}
)
public class CodexAnnalsPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexAnnalsPlugin.class);
	private static final String ANNALS_KEY = "annals";
	/** A game tick, the time each logged-in tick stands for. */
	private static final long TICK_MS = 600;
	/** Ticks between panel refreshes when only the clock moved, about half a minute. */
	private static final int REFRESH_TICKS = 50;
	/** Ticks between saves once something was written down, about a minute. */
	private static final int SAVE_TICKS = 100;
	/** Ticks between saves when only the clock moved, about five minutes; the annals are a large value to write. */
	private static final int CLOCK_SAVE_TICKS = 500;
	/** Ticks a new pet has to come out as a follower and give its name. */
	private static final int PET_TICKS = 5;
	/** How long a loot or kill count stays fresh enough to name what a message was about. */
	private static final long RECENT_MS = 10_000;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Gson gson;

	@Inject
	private CodexAnnalsConfig config;

	private AnnalsPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Annals annals = new Annals();
	private boolean live;
	private boolean pendingBegin;
	private boolean dirty;
	private boolean unsaved;
	/** Something more than the clock changed since the last save. */
	private boolean entriesChanged;
	private int ticks;
	private int petTicks;
	private String recentKill;
	private long recentKillAt;
	private final Deque<RecentItem> recentLoot = new ArrayDeque<>();

	/** An item seen in loot a moment ago, to give a collection log entry its picture. */
	private static final class RecentItem
	{
		final String name;
		final int id;
		final long at;

		RecentItem(String name, int id, long at)
		{
			this.name = name;
			this.id = id;
			this.at = at;
		}
	}

	@Provides
	CodexAnnalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexAnnalsConfig.class);
	}

	@Override
	protected void startUp()
	{
		ItemIcons items = (itemId, onLoaded) ->
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			image.onLoaded(onLoaded);
			return image;
		};
		Icons icons = name ->
		{
			try
			{
				return skillIconManager.getSkillImage(Skill.valueOf(name.toUpperCase()), true);
			}
			catch (IllegalArgumentException e)
			{
				return null;
			}
		};
		panel = new AnnalsPanel(items, icons, new CardActions()
		{
			@Override
			public void save(BufferedImage card, int year)
			{
				saveCard(card, year);
			}

			@Override
			public void copy(BufferedImage card)
			{
				copyCard(card);
			}
		});
		panel.setShowName(config.nameOnCard());
		panel.onStrike(this::strike);
		navButton = NavigationButton.builder()
			.tooltip("Codex Annals")
			.icon(Sigil.icon(16))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			loadProfile();
			pendingBegin = client.getGameState() == GameState.LOGGED_IN;
			publish();
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			save();
			annals = new Annals();
			profile = null;
			live = false;
			pendingBegin = false;
		});
		panel = null;
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		// Up to five minutes of play time would otherwise be lost when the client is closed while logged in.
		CompletableFuture<Void> done = new CompletableFuture<>();
		clientThread.invokeLater(() ->
		{
			try
			{
				save();
			}
			finally
			{
				done.complete(null);
			}
		});
		event.waitFor(done);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			if (!live)
			{
				pendingBegin = true;
			}
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			pendingBegin = false;
			if (live)
			{
				live = false;
				save();
				publish();
			}
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		save();
		live = false;
		loadProfile();
		pendingBegin = client.getGameState() == GameState.LOGGED_IN;
		publish();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// The annals themselves are saved in this group too; only the card's name setting concerns the panel.
		if (!CodexAnnalsConfig.GROUP.equals(event.getGroup()) || !"nameOnCard".equals(event.getKey()))
		{
			return;
		}
		AnnalsPanel target = panel;
		if (target != null)
		{
			boolean showName = config.nameOnCard();
			SwingUtilities.invokeLater(() -> target.setShowName(showName));
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		long now = System.currentTimeMillis();
		if (pendingBegin)
		{
			tryBegin(now);
		}
		if (!live)
		{
			return;
		}
		annals.tick(now, TICK_MS, ZoneId.systemDefault());
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			written(annals.combat(player.getCombatLevel(), now));
		}
		if (petTicks > 0)
		{
			petTicks--;
			NPC follower = client.getFollower();
			if (follower != null && follower.getName() != null)
			{
				written(annals.namePet(Text.removeTags(follower.getName()), now));
				petTicks = 0;
			}
		}
		unsaved = true;
		ticks++;
		if (ticks % SAVE_TICKS == 0 && (entriesChanged || ticks % CLOCK_SAVE_TICKS == 0))
		{
			save();
		}
		if (dirty || ticks % REFRESH_TICKS == 0)
		{
			publish();
		}
	}

	/** Opens the annals once the account and its stats can be read. */
	private void tryBegin(long now)
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null || client.getOverallExperience() <= 0)
		{
			return;
		}
		if (profile == null || !profile.equals(configManager.getRSProfileKey()))
		{
			loadProfile();
			if (profile == null)
			{
				return;
			}
		}
		pendingBegin = false;
		Map<String, Long> xp = new LinkedHashMap<>();
		Map<String, Integer> levels = new LinkedHashMap<>();
		List<String> maxed = new ArrayList<>();
		int total = 0;
		for (Skill skill : Skill.values())
		{
			int level = client.getRealSkillLevel(skill);
			xp.put(skill.getName(), (long) client.getSkillExperience(skill));
			levels.put(skill.getName(), level);
			total += level;
			if (level >= 99)
			{
				maxed.add(skill.getName());
			}
		}
		Baseline baseline = null;
		if (!annals.isBegun())
		{
			// Only read once, the day the annals begin: asking every quest its state runs a script each.
			int done = 0;
			for (Quest quest : Quest.values())
			{
				done += quest.getState(client) == QuestState.FINISHED ? 1 : 0;
			}
			baseline = new Baseline(now, total, client.getOverallExperience(), player.getCombatLevel(),
				client.getVarpValue(VarPlayerID.QP), done, Quest.values().length, maxed);
		}
		int yearsBefore = annals.getYears().size();
		int lastYear = yearsBefore == 0 ? 0 : annals.getYears().get(yearsBefore - 1).getYear();
		written(annals.begin(now, ZoneId.systemDefault(), player.getName(), baseline, xp, levels, player.getCombatLevel()));
		if (yearsBefore > 0 && annals.getYears().size() > yearsBefore && config.chatLine())
		{
			announce("your " + lastYear + " in Gielinor is complete. Its card is waiting in the Annals panel.");
		}
		live = true;
		save();
		dirty = true;
		publish();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!live)
		{
			return;
		}
		written(annals.stat(event.getSkill().getName(), event.getXp(), event.getLevel(), System.currentTimeMillis(),
			ZoneId.systemDefault(), config.everyLevel()));
		unsaved = true;
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		NPC npc = event.getNpc();
		loot(npc == null ? null : npc.getName(), event.getItems());
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		// Kills arrive through NpcLootReceived on their own, and player kills are left out.
		LootRecordType type = event.getType();
		if (type == LootRecordType.NPC || type == LootRecordType.PLAYER)
		{
			return;
		}
		loot(event.getName(), event.getItems());
	}

	private void loot(String source, Collection<ItemStack> items)
	{
		if (!live)
		{
			return;
		}
		long now = System.currentTimeMillis();
		String from = source == null ? null : Text.removeTags(source);
		if (from != null)
		{
			recentKill = from;
			recentKillAt = now;
		}
		while (!recentLoot.isEmpty() && now - recentLoot.peekFirst().at > RECENT_MS)
		{
			recentLoot.removeFirst();
		}
		for (ItemStack stack : items)
		{
			int id = itemManager.canonicalize(stack.getId());
			ItemComposition item = itemManager.getItemComposition(id);
			String name = item.getName();
			recentLoot.addLast(new RecentItem(name, id, now));
			// A collection log message can come before the loot it was about.
			annals.attachItem(name, id, now - RECENT_MS);
			long value = (long) itemManager.getItemPrice(id) * stack.getQuantity();
			if (value >= config.notableDrop())
			{
				written(annals.drop(name, stack.getQuantity(), id, value, from, now));
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!live || (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM))
		{
			return;
		}
		String message = Text.removeTags(event.getMessage()).replace(' ', ' ').trim();
		long now = System.currentTimeMillis();
		ZoneId zone = ZoneId.systemDefault();
		ChatParser.KillCount kc = ChatParser.killCount(message);
		if (kc != null)
		{
			recentKill = kc.getName();
			recentKillAt = now;
			written(annals.killCount(kc.getName(), kc.getCount(), now, zone));
			return;
		}
		Entry entry = ChatParser.entry(message, now, now - recentKillAt <= RECENT_MS ? recentKill : null);
		if (entry == null)
		{
			return;
		}
		if (entry.getKind() == Entry.Kind.PET && message.startsWith(ChatParser.PET_FOLLOWS))
		{
			petTicks = PET_TICKS;
		}
		Entry kept = annals.record(entry, now, zone);
		if (kept != null && kept.getKind() == Entry.Kind.COLLECTION)
		{
			for (RecentItem item : recentLoot)
			{
				if (now - item.at <= RECENT_MS && item.name.equalsIgnoreCase(kept.getTitle()))
				{
					annals.attachItem(item.name, item.id, now - RECENT_MS);
				}
			}
			if (kept.getItemId() == 0)
			{
				// No loot to take the picture from, as with a raid's chest opened later: find it by name.
				for (ItemPrice price : itemManager.search(kept.getTitle()))
				{
					if (price.getName().equalsIgnoreCase(kept.getTitle()))
					{
						annals.attachItem(kept.getTitle(), price.getId(), now - RECENT_MS);
						break;
					}
				}
			}
		}
		written(kept);
	}

	private void written(List<Entry> entries)
	{
		for (Entry e : entries)
		{
			written(e);
		}
	}

	/** Marks the annals changed and, for the milestones the game does not announce itself, says so in chat. */
	private void written(Entry e)
	{
		if (e == null)
		{
			return;
		}
		dirty = true;
		unsaved = true;
		entriesChanged = true;
		if (!config.chatLine())
		{
			return;
		}
		switch (e.getKind())
		{
			case MAX:
			case TOTAL:
			case XP:
			case COMBAT:
			case BOSS:
			case DROP:
				announce(e.getTitle() + ", written into your annals.");
				break;
			default:
				break;
		}
	}

	private void announce(String text)
	{
		String line = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT).append("Codex Annals: ")
			.append(ChatColorType.NORMAL).append(text)
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(line)
			.build());
	}

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		annals = new Annals();
		recentLoot.clear();
		petTicks = 0;
		if (profile == null)
		{
			return;
		}
		try
		{
			String saved = configManager.getRSProfileConfiguration(CodexAnnalsConfig.GROUP, ANNALS_KEY);
			if (saved != null && !saved.isEmpty())
			{
				Annals read = gson.fromJson(saved, Annals.class);
				if (read != null)
				{
					annals = read.repair();
				}
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved annals, starting them afresh", e);
		}
	}

	private void save()
	{
		if (!unsaved || profile == null || !annals.isBegun())
		{
			return;
		}
		unsaved = false;
		entriesChanged = false;
		configManager.setConfiguration(CodexAnnalsConfig.GROUP, profile, ANNALS_KEY, gson.toJson(annals));
	}

	/** Sends the panel a copy of the annals, so Swing never reads what the client thread is writing. */
	private void publish()
	{
		dirty = false;
		Annals copy = gson.fromJson(gson.toJson(annals), Annals.class).repair();
		Player player = client.getLocalPlayer();
		String name = client.getGameState() == GameState.LOGGED_IN && player != null ? player.getName() : null;
		AnnalsPanel target = panel;
		if (target != null)
		{
			ZoneId zone = ZoneId.systemDefault();
			SwingUtilities.invokeLater(() -> target.update(copy.isBegun() ? copy : null, zone, name));
		}
	}

	/** Strikes an entry the player chose from the panel; runs on the Swing thread. */
	private void strike(Entry entry)
	{
		long at = entry.getAt();
		Entry.Kind kind = entry.getKind();
		String title = entry.getTitle();
		clientThread.invokeLater(() ->
		{
			if (annals.remove(at, kind, title))
			{
				dirty = true;
				unsaved = true;
				entriesChanged = true;
				save();
				publish();
			}
		});
	}

	/** Asks where to save the card with RuneLite's own file chooser, then writes it there; runs on the Swing thread. */
	private void saveCard(BufferedImage card, int year)
	{
		List<Filepath> chosen = new Filepath.Chooser()
			.setIsSave()
			.setAcceptsFiles()
			.setDialogTitle("Save Year in Gielinor card")
			.addExtensionFilter("PNG image", "png")
			.setDefaultExtension("png")
			.setFileName("Year in Gielinor " + year + ".png")
			.showDialog(panel);
		if (chosen == null || chosen.isEmpty())
		{
			return;
		}
		Filepath target = chosen.get(0);
		executor.execute(() ->
		{
			String note;
			try (OutputStream out = target.openOutputStream())
			{
				ImageIO.write(card, "png", out);
				note = "Saved as " + target.getFileName() + ".";
			}
			catch (IOException e)
			{
				log.warn("Could not save the year card", e);
				note = "The card could not be saved: " + e.getMessage();
			}
			String shown = note;
			AnnalsPanel view = panel;
			if (view != null)
			{
				SwingUtilities.invokeLater(() -> view.cardNote(shown));
			}
		});
	}

	private void copyCard(BufferedImage card)
	{
		String note;
		try
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransfer(card), null);
			note = "Copied. Paste it anywhere you share pictures.";
		}
		catch (IllegalStateException e)
		{
			note = "The clipboard is busy, try again in a moment.";
		}
		AnnalsPanel target = panel;
		if (target != null)
		{
			target.cardNote(note);
		}
	}
}
