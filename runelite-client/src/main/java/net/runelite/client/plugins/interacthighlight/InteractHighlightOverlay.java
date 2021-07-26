/*
 * Copyright (c) 2021, Adam <Adam@sigterm.info>
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
package net.runelite.client.plugins.interacthighlight;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

class InteractHighlightOverlay extends Overlay
{
	private final Client client;
	private final InteractHighlightPlugin plugin;
	private final InteractHighlightConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private InteractHighlightOverlay(Client client, InteractHighlightPlugin plugin, InteractHighlightConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.showHover())
		{
			renderMouseover();
		}
		if (config.showInteract())
		{
			renderTarget();
		}

		return null;
	}

	private void renderMouseover()
	{
		MenuEntry[] menuEntries = client.getMenuEntries();
		if (menuEntries.length == 0)
		{
			return;
		}

		MenuEntry top = menuEntries[menuEntries.length - 1];
		MenuAction menuAction = MenuAction.of(top.getType());

		switch (menuAction)
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			{
				int x = top.getParam0();
				int y = top.getParam1();
				int id = top.getIdentifier();
				TileObject tileObject = plugin.findTileObject(x, y, id);
				if (tileObject != null && (tileObject != plugin.getInteractedObject() || !config.showInteract()))
				{
					modelOutlineRenderer.drawOutline(tileObject, config.borderWidth(), config.objectHoverHighlightColor(), config.outlineFeather());
				}
				break;
			}
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
			{
				int id = top.getIdentifier();
				NPC npc = plugin.findNpc(id);
				if (npc != null && (npc != plugin.getInteractedNpc() || !config.showInteract()))
				{
					modelOutlineRenderer.drawOutline(npc, config.borderWidth(), config.npcHoverHighlightColor(), config.outlineFeather());
				}
				break;
			}
		}
	}

	private void renderTarget()
	{
		TileObject interactedObject = plugin.getInteractedObject();
		if (interactedObject != null)
		{
			modelOutlineRenderer.drawOutline(interactedObject, config.borderWidth(), config.objectInteractHighlightColor(), config.outlineFeather());
		}

		NPC npc = plugin.getInteractedNpc();
		if (npc != null)
		{
			modelOutlineRenderer.drawOutline(npc, config.borderWidth(), config.npcInteractHighlightColor(), config.outlineFeather());
		}
	}
}
