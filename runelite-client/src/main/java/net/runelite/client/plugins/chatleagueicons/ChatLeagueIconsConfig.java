/*
 * Copyright (c) 2019, hsamoht <https://github.com/hsamoht>
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
package net.runelite.client.plugins.chatleagueicons;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("chatLeagueIcons")
public interface ChatLeagueIconsConfig extends Config
{
	@ConfigItem(
		keyName = "leagueIcon",
		name = "Icon",
		description = "The league icon to display in chat.",
		position = 1
	)
	default LeagueIcon leagueIcon()
	{
		return LeagueIcon.LEAGUE_TWISTED;
	}

	@ConfigItem(
		keyName = "publicChatIcons",
		name = "Public Chat Icons",
		description = "Add league icons to players talking in public chat on league worlds.",
		position = 2
	)
	default boolean publicChatIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "privateMessageIcons",
		name = "Private Message Icons",
		description = "Add league icons to league players talking in private message.",
		position = 3
	)
	default boolean privateMessageIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanChatIcons",
		name = "Clan Chat Icons",
		description = "Add league icons to league players talking in clan chat.",
		position = 4
	)
	default boolean clanChatIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "playerChatIcon",
		name = "Player Chat Icon",
		description = "Add league icons to player in chat.",
		position = 5
	)
	default boolean playerChatIcon()
	{
		return true;
	}
}
