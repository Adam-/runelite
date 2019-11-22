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
