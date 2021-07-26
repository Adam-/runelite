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

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("interacthighlight")
public interface InteractHighlightConfig extends Config
{
	@ConfigItem(
		keyName = "showHover",
		name = "Show on hover",
		description = "Outline npcs and objects when hovered",
		position = 1
	)
	default boolean showHover()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInteract",
		name = "Show on interact",
		description = "Outline npcs and objects when interacted",
		position = 2
	)
	default boolean showInteract()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "npcHoverHighlightColor",
		name = "NPC hover",
		description = "The color of the hover outline for npcs",
		position = 3
	)
	default Color npcHoverHighlightColor()
	{
		return new Color(0x90FFFF00, true);
	}

	@Alpha
	@ConfigItem(
		keyName = "objectHoverHighlightColor",
		name = "Object hover",
		description = "The color of the hover outline for objects",
		position = 4
	)
	default Color objectHoverHighlightColor()
	{
		return new Color(0x9000FFFF, true);
	}

	@Alpha
	@ConfigItem(
		keyName = "npcInteractHighlightColor",
		name = "NPC interact",
		description = "The color of the target outline for npcs",
		position = 5
	)
	default Color npcInteractHighlightColor()
	{
		return new Color(0x90FFFF00, true);
	}

	@Alpha
	@ConfigItem(
		keyName = "objectInteractHighlightColor",
		name = "Object interact",
		description = "The color of the target outline for objects",
		position = 6
	)
	default Color objectInteractHighlightColor()
	{
		return new Color(0x9000FFFF, true);
	}

	@ConfigItem(
		keyName = "borderWidth",
		name = "Border Width",
		description = "Width of the outlined border",
		position = 7
	)
	default int borderWidth()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "outlineFeather",
		name = "Outline feather",
		description = "Specify between 0-4 how much of the model outline should be faded",
		position = 8
	)
	@Range(
		max = 4
	)
	default int outlineFeather()
	{
		return 4;
	}
}
