/*
 * Copyright (c) 2026, Adam <Adam@sigterm.info>
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
package net.runelite.client.party;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.messages.PartyDataMessage;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.util.Text;

@Singleton
public class PartyDataService
{
	private final Client client;
	private final ClientThread clientThread;
	private final PartyService party;

	private static class Subscription
	{
		Plugin p;
		int mask;
	}

	private final List<Subscription> subscriptions = new ArrayList<>(2);
	private int subscriptionMask;

	private String lastCharacterName;
	private int lastHealthCurrent;
	private int lastHealthMax;
	private int lastPrayerCurrent;
	private int lastPrayerMax;
	private int lastRunEnergy;
	private int lastSpecEnergy;
	private boolean lastVengeActive;

	private boolean invDirty, wornDirty;
	private boolean prayerDirty;

	@Inject
	public PartyDataService(Client client, ClientThread clientThread, PartyService partyService, EventBus eventBus)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.party = partyService;
		eventBus.register(this);
	}

	public synchronized void subscribe(Plugin plugin, PartyDataType... types)
	{
		var opt = subscriptions.stream().filter(s -> s.p == plugin).findFirst();
		if (opt.isPresent())
		{
			opt.get().mask |= typeMask(types);
		}
		else
		{
			Subscription sub = new Subscription();
			sub.p = plugin;
			sub.mask = typeMask(types);
			subscriptions.add(sub);
		}
		recomputeMask();
	}

	public synchronized void unsubscribe(Plugin plugin, PartyDataType... types)
	{
		var opt = subscriptions.stream().filter(s -> s.p == plugin).findFirst();
		if (opt.isPresent())
		{
			Subscription sub = opt.get();
			sub.mask &= ~typeMask(types);
			if (sub.mask == 0)
			{
				subscriptions.remove(sub);
			}
			recomputeMask();
		}
	}

	private boolean isSubscribed(PartyDataType type)
	{
		return (subscriptionMask & (1 << type.ordinal())) != 0;
	}

	private int typeMask(PartyDataType... types)
	{
		int mask = 0;
		for (PartyDataType type : types)
		{
			mask |= 1 << type.ordinal();
		}
		return mask;
	}

	private void recomputeMask()
	{
		subscriptionMask = 0;
		for (Subscription sub : subscriptions)
		{
			subscriptionMask |= sub.mask;
		}
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		checkStateChanged(false);
	}

	@Subscribe
	private void onUserSync(final UserSync event)
	{
		clientThread.invokeLater(() -> checkStateChanged(true));
	}

	@Subscribe
	private void onItemContainerChanged(ItemContainerChanged event)
	{
		switch (event.getContainerId())
		{
			case InventoryID.INV:
				invDirty = true;
				break;
			case InventoryID.WORN:
				wornDirty = true;
				break;
		}
	}

	@Subscribe
	private void onVarbitChanged(VarbitChanged event)
	{
		switch (event.getVarpId())
		{
			case VarPlayerID.PRAYER0:
			case VarPlayerID.PRAYER_RUINOUS_0:
				prayerDirty = true;
		}
	}

	private void checkStateChanged(boolean forceSend)
	{
		if (!party.isInParty())
		{
			return;
		}

		final int healthCurrent = client.getBoostedSkillLevel(Skill.HITPOINTS);
		final int prayerCurrent = client.getBoostedSkillLevel(Skill.PRAYER);
		final int healthMax = client.getRealSkillLevel(Skill.HITPOINTS);
		final int prayerMax = client.getRealSkillLevel(Skill.PRAYER);
		final int runEnergy = (int) Math.ceil(client.getEnergy() / 1000.0) * 10; // flatten to reduce network load
		final int specEnergy = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
		final boolean vengActive = client.getVarbitValue(VarbitID.VENGEANCE_REBOUND) == 1;

		final Player localPlayer = client.getLocalPlayer();
		final String characterName = Strings.nullToEmpty(localPlayer != null && client.getGameState().getState() >= GameState.LOADING.getState() ? localPlayer.getName() : null);

		final PartyDataMessage update = new PartyDataMessage();
		boolean hasChange = false;
		boolean canDelay = !forceSend;

		if (isSubscribed(PartyDataType.CHARACTER_NAME) && (forceSend || !characterName.equals(lastCharacterName)))
		{
			hasChange = true;
			update.setCharacterName(characterName);
		}
		if (isSubscribed(PartyDataType.HEALTH) && (forceSend || healthCurrent != lastHealthCurrent))
		{
			hasChange = true;
			update.setHealthCurrent(healthCurrent);
		}
		if (isSubscribed(PartyDataType.HEALTH) && (forceSend || healthMax != lastHealthMax))
		{
			hasChange = true;
			update.setHealthMax(healthMax);
		}
		if (isSubscribed(PartyDataType.PRAYER) && (forceSend || prayerCurrent != lastPrayerCurrent))
		{
			hasChange = true;
			update.setPrayerCurrent(prayerCurrent);
		}
		if (isSubscribed(PartyDataType.PRAYER) && (forceSend || prayerMax != lastPrayerMax))
		{
			hasChange = true;
			update.setPrayerMax(prayerMax);
		}
		if (isSubscribed(PartyDataType.ENERGY) && (forceSend || runEnergy != lastRunEnergy))
		{
			hasChange = true;
			update.setRunEnergy(runEnergy);
		}
		if (isSubscribed(PartyDataType.SPEC) && (forceSend || specEnergy != lastSpecEnergy))
		{
			hasChange = true;
			canDelay = !forceSend && specEnergy - lastSpecEnergy == 10; // delay regen
			update.setSpecEnergy(specEnergy);
		}
		if (isSubscribed(PartyDataType.VENGEANCE) && (forceSend || vengActive != lastVengeActive))
		{
			hasChange = true;
			update.setVengeanceActive(vengActive);
		}
		if (isSubscribed(PartyDataType.INV) && (forceSend || invDirty))
		{
			hasChange = true;
			update.setInv(serializeInv(client.getItemContainer(InventoryID.INV)));
		}
		if (isSubscribed(PartyDataType.WORN) && (forceSend || wornDirty))
		{
			hasChange = true;
			update.setWorn(serializeInv(client.getItemContainer(InventoryID.WORN)));
		}
		if (isSubscribed(PartyDataType.PRAYER) && (forceSend || prayerDirty))
		{
			hasChange = true;
			update.setPrayers(serializePrayers());
		}

		if (canDelay && client.getTickCount() % messageFreq(party.getMembers().size()) != 0)
		{
			return;
		}

		if (hasChange)
		{
			lastCharacterName = characterName;
			lastHealthCurrent =  healthCurrent;
			lastHealthMax = healthMax;
			lastPrayerCurrent = prayerCurrent;
			lastPrayerMax = prayerMax;
			lastRunEnergy = runEnergy;
			lastSpecEnergy = specEnergy;
			lastVengeActive = vengActive;
			invDirty = wornDirty = prayerDirty = false;

			party.send(update);
		}
	}

	@Subscribe(priority = 1)
	private void onPartyDataMessage(PartyDataMessage event)
	{
		PartyMember member = party.getMemberById(event.getMemberId());
		if (member == null)
		{
			return;
		}

		if (event.getCharacterName() != null)
		{
			final String name = Text.removeTags(Text.toJagexName(event.getCharacterName()));
			if (!name.isEmpty())
			{
				member.setDisplayName(name);
				member.setLoggedIn(true);
			}
			else
			{
				member.setLoggedIn(false);
			}
		}
		if (event.getHealthCurrent() != null)
		{
			member.setHitpoints(event.getHealthCurrent());
		}
		if (event.getHealthMax() != null)
		{
			member.setMaxHitpoints(event.getHealthMax());
		}
		if (event.getPrayerCurrent() != null)
		{
			member.setPrayer(event.getPrayerCurrent());
		}
		if (event.getPrayerMax() != null)
		{
			member.setMaxPrayer(event.getPrayerMax());
		}
		if (event.getRunEnergy() != null)
		{
			member.setRunEnergy(event.getRunEnergy());
		}
		if (event.getSpecEnergy() != null)
		{
			member.setSpecEnergy(event.getSpecEnergy());
		}
		if (event.getVengeanceActive() != null)
		{
			member.setVengeanceActive(event.getVengeanceActive());
		}
		if (event.getInv() != null)
		{
			member.setInv(deserializeInv(event.getInv()));
		}
		if (event.getWorn() != null)
		{
			member.setWorn(deserializeInv(event.getWorn()));
		}
		if (event.getPrayers() != null)
		{
			member.setPrayers(deserializePrayers(event.getPrayers()));
		}
	}

	private static int[] serializeInv(ItemContainer inv)
	{
		if (inv == null)
		{
			return null;
		}

		int size = inv.size();
		int[] s = new int[size << 1];
		for (int i = 0; i < size; ++i)
		{
			Item item = inv.getItem(i);
			if (item == null)
			{
				s[i << 1] = -1;
			}
			else
			{
				s[i << 1] = item.getId();
				s[(1 << 1) | 1] = item.getQuantity();
			}
		}
		return s;
	}

	private static Item[] deserializeInv(int[] s)
	{
		if (s == null)
		{
			return null;
		}

		Item[] items = new Item[s.length >> 1];
		for (int i = 0; i < items.length; ++i)
		{
			int itemId = s[i << 1];
			int quantity = s[(i << 1) | 1];
			if (itemId > -1)
			{
				items[i] = new Item(itemId, quantity);
			}
		}
		return items;
	}

	private long serializePrayers()
	{
		long s = 0;
		for (Prayer p : Prayer.values())
		{
			if (client.getVarbitValue(p.getVarbit()) == 1)
			{
				s |= 1L << p.ordinal();
			}
		}
		return s;
	}

	private static Set<Prayer> deserializePrayers(long s)
	{
		EnumSet<Prayer> prayers = EnumSet.noneOf(Prayer.class);
		for (Prayer p : Prayer.values())
		{
			if ((s & (1L << p.ordinal())) != 0)
			{
				prayers.add(p);
			}
		}
		return prayers;
	}

	private static int messageFreq(int partySize)
	{
		// introduce a tick delay for each member >6
		return Math.max(1, partySize - 6);
	}
}
