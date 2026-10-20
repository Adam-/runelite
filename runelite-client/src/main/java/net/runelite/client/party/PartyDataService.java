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
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.messages.PartyDataMessage;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;

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

	private boolean invDirty, wornDirty;

	private PartyDataMessage lastStatus;

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

		boolean hasChange = false;
		boolean canDelay = !forceSend;
		final PartyDataMessage update = new PartyDataMessage();
		if (lastStatus == null)
		{
			forceSend = true;
		}

		if (isSubscribed(PartyDataType.CHARACTER_NAME) && (forceSend || !characterName.equals(lastStatus.getCharacterName())))
		{
			hasChange = true;
			update.setCharacterName(characterName);
		}
		if (isSubscribed(PartyDataType.HEALTH) && (forceSend || healthCurrent != lastStatus.getHealthCurrent()))
		{
			hasChange = true;
			update.setHealthCurrent(healthCurrent);
		}
		if (isSubscribed(PartyDataType.HEALTH) && (forceSend || healthMax != lastStatus.getHealthMax()))
		{
			hasChange = true;
			update.setHealthMax(healthMax);
		}
		if (isSubscribed(PartyDataType.PRAYER) && (forceSend || prayerCurrent != lastStatus.getPrayerCurrent()))
		{
			hasChange = true;
			update.setPrayerCurrent(prayerCurrent);
		}
		if (isSubscribed(PartyDataType.PRAYER) && (forceSend || prayerMax != lastStatus.getPrayerMax()))
		{
			hasChange = true;
			update.setPrayerMax(prayerMax);
		}
		if (isSubscribed(PartyDataType.ENERGY) && (forceSend || runEnergy != lastStatus.getRunEnergy()))
		{
			hasChange = true;
			update.setRunEnergy(runEnergy);
		}
		if (isSubscribed(PartyDataType.SPEC) && (forceSend || specEnergy != lastStatus.getSpecEnergy()))
		{
			hasChange = true;
			canDelay = !forceSend && specEnergy - lastStatus.getSpecEnergy() == 10; // delay regen
			update.setSpecEnergy(specEnergy);
		}
		if (isSubscribed(PartyDataType.VENGEANCE) && (forceSend || vengActive != lastStatus.getVengeanceActive()))
		{
			hasChange = true;
			update.setVengeanceActive(vengActive);
		}
		if (isSubscribed(PartyDataType.INV) && (forceSend || invDirty))
		{
			hasChange = true;
			update.setInv(serializeInv(client.getItemContainer(InventoryID.INV)));
			invDirty = false;
		}
		if (isSubscribed(PartyDataType.INV) && (forceSend || wornDirty))
		{
			hasChange = true;
			update.setWorn(serializeInv(client.getItemContainer(InventoryID.WORN));
			wornDirty = false;
		}

		if (canDelay && client.getTickCount() % messageFreq(party.getMembers().size()) != 0)
		{
			return;
		}

		if (hasChange)
		{
			party.send(update);
			// non-null values for next-tick comparison
			lastStatus = new PartyDataMessage(
				characterName,
				healthCurrent,
				healthMax,
				prayerCurrent,
				prayerMax,
				runEnergy,
				specEnergy,
				vengActive,
				null,
				null
			);
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

	private static int messageFreq(int partySize)
	{
		// introduce a tick delay for each member >6
		return Math.max(1, partySize - 6);
	}
}
