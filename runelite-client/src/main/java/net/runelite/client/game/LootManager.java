/*
 * Copyright (c) 2018, Adam Adam <Adam@sigterm.info>
 * Copyright (c) 2018, TheStonedTurtle <https://github.com/TheStonedTurtle>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.events.EventLootReceived;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.util.Text;

@Singleton
@Slf4j
public class LootManager
{
	private final EventBus eventBus;
	private final Provider<Client> client;
	private final Multimap<Integer, ItemStack> itemSpawns = HashMultimap.create();
	private WorldPoint playerLocationLastTick;

	private static final Pattern CLUE_SCROLL_PATTERN = Pattern.compile("You have completed [0-9]+ ([a-z]+) Treasure Trails.");
	// Not local since it's also used in onChatMessage to determine clue scroll type
	private LootEventType eventType;

	// Used to trigger the event on first open.
	private boolean hasOpenedRaidsRewardChest = false;
	private boolean hasOpenedTheatreOfBloodRewardChest = false;
	private WorldPoint krakenPlayerLocation;

	private static final Map<Integer, Integer> NPC_DEATH_ANIMATIONS = ImmutableMap.of(
			NpcID.CAVE_KRAKEN, AnimationID.CAVE_KRAKEN_DEATH,
			NpcID.AIR_WIZARD, AnimationID.WIZARD_DEATH,
			NpcID.WATER_WIZARD, AnimationID.WIZARD_DEATH,
			NpcID.EARTH_WIZARD, AnimationID.WIZARD_DEATH,
			NpcID.FIRE_WIZARD, AnimationID.WIZARD_DEATH
	);

	@Inject
	private LootManager(EventBus eventBus, Provider<Client> client)
	{
		this.eventBus = eventBus;
		this.client = client;
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned playerDespawned)
	{
		final Player player = playerDespawned.getPlayer();
		final Client client = this.client.get();
		final LocalPoint location = LocalPoint.fromWorld(client, player.getWorldLocation());
		if (location == null)
		{
			return;
		}

		final int x = location.getSceneX();
		final int y = location.getSceneY();
		final int packed = x << 8 | y;
		final Collection<ItemStack> items = itemSpawns.get(packed);

		if (items.isEmpty())
		{
			return;
		}

		for (ItemStack item : items)
		{
			log.debug("Drop from {}: {}", player.getName(), item.getId());
		}

		eventBus.post(new PlayerLootReceived(player, items));
	}

	@Subscribe
	public void onNpcDespawn(NpcDespawned npcDespawned)
	{
		final NPC npc = npcDespawned.getNpc();
		if (!npc.isDead())
		{
			return;
		}

		newNpcLootEvent(npc);
	}


	/**
	 * Create new NpcLootReceived event
	 * @param npc NPC to check
	 */
	private void newNpcLootEvent(NPC npc)
	{
		Client client = this.client.get();
		WorldPoint worldLocation = npc.getWorldLocation();

		switch (npc.getId())
		{
			case NpcID.KRAKEN:
			case NpcID.KRAKEN_6640:
			case NpcID.KRAKEN_6656:
				worldLocation = playerLocationLastTick;
				break;
			case NpcID.CAVE_KRAKEN:
				worldLocation = krakenPlayerLocation;
				break;
			case NpcID.ZULRAH:		// Green
			case NpcID.ZULRAH_2043: // Red
			case NpcID.ZULRAH_2044: // Blue
				for (Map.Entry<Integer, ItemStack> entry : itemSpawns.entries())
				{
					if (entry.getValue().getId() == ItemID.ZULRAHS_SCALES)
					{
						int packed = entry.getKey();
						int unpackedX = packed << 8;
						int unpackedY = packed & 0xFF;
						worldLocation = new WorldPoint(unpackedX, unpackedY, worldLocation.getPlane());
						break;
					}
				}
				break;
			case NpcID.VORKATH:
			case NpcID.VORKATH_8058:
			case NpcID.VORKATH_8059:
			case NpcID.VORKATH_8060:
			case NpcID.VORKATH_8061:
				int x = worldLocation.getX() + 3;
				int y = worldLocation.getY() + 3;
				if (playerLocationLastTick.getX() < x)
				{
					x -= 4;
				}
				else if (playerLocationLastTick.getX() > x)
				{
					x += 4;
				}
				if (playerLocationLastTick.getY() < y)
				{
					y -= 4;
				}
				else if (playerLocationLastTick.getY() > y)
				{
					y += 4;
				}
				worldLocation = new WorldPoint(x, y, worldLocation.getPlane());
				break;
		}

		final LocalPoint location = LocalPoint.fromWorld(client, worldLocation);
		if (location == null)
		{
			return;
		}

		final int x = location.getSceneX();
		final int y = location.getSceneY();
		final int size = npc.getComposition().getSize();

		final Collection<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < size; ++i)
		{
			for (int j = 0; j < size; ++j)
			{
				final int packed = (x + i) << 8 | (y + j);
				Collection<ItemStack> groundItems = itemSpawns.get(packed);
				if (!groundItems.isEmpty())
				{
					items.addAll(groundItems);
				}
			}
		}

		if (!items.isEmpty())
		{
			for (ItemStack item : items)
			{
				log.debug("Drop from {}: {}x {} ", npc.getName(), item.getQuantity(), item.getId());
			}
			final NpcLootReceived npcLootReceived = new NpcLootReceived(npc, items);
			eventBus.post(npcLootReceived);
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		if (!(e.getActor() instanceof NPC))
		{
			return;
		}

		NPC n = (NPC) e.getActor();
		int id = n.getId();

		// We only care about certain NPCs
		if (!NPC_DEATH_ANIMATIONS.containsKey(id))
		{
			return;
		}

		// Current animation is death animation?
		if (NPC_DEATH_ANIMATIONS.get(id) == n.getAnimation())
		{
			if (id == NpcID.CAVE_KRAKEN)
			{
				// Big Kraken drops loot wherever player is standing when animation starts.
				krakenPlayerLocation = this.client.get().getLocalPlayer().getWorldLocation();
			}
			else
			{
				// These NPCs drop loot on death animation, which is right now.
				newNpcLootEvent(n);
			}
		}
	}

	@Subscribe
	public void onItemSpawn(ItemSpawned itemSpawned)
	{
		final Item item = itemSpawned.getItem();
		final Tile tile = itemSpawned.getTile();
		final LocalPoint location = tile.getLocalLocation();
		final int packed = location.getSceneX() << 8 | location.getSceneY();
		itemSpawns.put(packed, new ItemStack(item.getId(), item.getQuantity()));
		log.debug("Item spawn {}x {} loc {},{}", item.getQuantity(), item.getId(), location.getSceneX(), location.getSceneY());
	}

	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged itemQuantityChanged)
	{
		final Item item = itemQuantityChanged.getItem();
		final Tile tile = itemQuantityChanged.getTile();
		final LocalPoint location = tile.getLocalLocation();
		final int packed = location.getSceneX() << 8 | location.getSceneY();
		final int diff = itemQuantityChanged.getNewQuantity() - itemQuantityChanged.getOldQuantity();

		if (diff <= 0)
		{
			return;
		}

		itemSpawns.put(packed, new ItemStack(item.getId(), diff));
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		playerLocationLastTick = client.get().getLocalPlayer().getWorldLocation();
		itemSpawns.clear();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		Client client = this.client.get();

		// Not inside Raids?
		if (client.getVar(Varbits.IN_RAID) == 0)
		{
			this.hasOpenedRaidsRewardChest = false;
		}

		// Not inside Theatre of Blood?
		int theatreState = client.getVar(Varbits.THEATRE_OF_BLOOD);
		if (theatreState == 0 || theatreState == 1)
		{
				this.hasOpenedTheatreOfBloodRewardChest = false;
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		Client client = this.client.get();

		ItemContainer container = null;
		switch (event.getGroupId())
		{
			case (WidgetID.BARROWS_REWARD_GROUP_ID):
				eventType = LootEventType.BARROWS;
				container = client.getItemContainer(InventoryID.BARROWS_REWARD);
				break;
			case (WidgetID.CHAMBERS_OF_XERIC_REWARD_GROUP_ID):
				if (hasOpenedRaidsRewardChest)
				{
					return;
				}

				eventType = LootEventType.CHAMBERS_OF_XERIC;
				container = client.getItemContainer(InventoryID.CHAMBERS_OF_XERIC_CHEST);
				break;
			case (WidgetID.THEATRE_OF_BLOOD_GROUP_ID):
				if (hasOpenedTheatreOfBloodRewardChest)
				{
					return;
				}

				eventType = LootEventType.THEATRE_OF_BLOOD;
				container = client.getItemContainer(InventoryID.THEATRE_OF_BLOOD_CHEST);
				break;
			case (WidgetID.CLUE_SCROLL_REWARD_GROUP_ID):
				// event type should be set via ChatMessage for clue scrolls.
				// Clue Scrolls use same InventoryID as Barrows
				container = client.getItemContainer(InventoryID.BARROWS_REWARD);
				break;
			default:
				return;
		}

		if (container != null)
		{
			// Convert container items to collection of ItemStack
			Collection<ItemStack> items = new ArrayList<>();
			for (Item item : container.getItems())
			{
				items.add(new ItemStack(item.getId(), item.getQuantity()));
			}

			if (!items.isEmpty())
			{
				log.debug("Loot Received from Event: {}", eventType);
				for (ItemStack item : items)
				{
					log.debug("Item Received: {}x {}", item.getQuantity(), item.getId());
				}

				final EventLootReceived lootReceived = new EventLootReceived(eventType, items);
				eventBus.post(lootReceived);
			}
			else
			{
				log.debug("No items to find for Event: {} | Container: {}", eventType, container);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.SERVER && event.getType() != ChatMessageType.FILTERED)
		{
			return;
		}

		String chatMessage = event.getMessage();

		// Check if message is for a clue scroll reward
		Matcher m = CLUE_SCROLL_PATTERN.matcher(Text.removeTags(chatMessage));
		if (m.find())
		{
			String type = m.group(1).toLowerCase();
			switch (type)
			{
				case "easy":
					eventType = LootEventType.CLUE_SCROLL_EASY;
					break;
				case "medium":
					eventType = LootEventType.CLUE_SCROLL_MEDIUM;
					break;
				case "hard":
					eventType = LootEventType.CLUE_SCROLL_HARD;
					break;
				case "elite":
					eventType = LootEventType.CLUE_SCROLL_ELITE;
					break;
				case "master":
					eventType = LootEventType.CLUE_SCROLL_MASTER;
					break;
				default:
					eventType = LootEventType.UNKNOWN_EVENT;
			}
		}
	}
}
