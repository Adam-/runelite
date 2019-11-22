package net.runelite.client.plugins.chatleagueicons;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatPlayer;
import net.runelite.api.ClanMember;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.GameState;
import net.runelite.api.IndexedSprite;
import net.runelite.api.MessageNode;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.vars.AccountType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldClient;
import net.runelite.http.api.worlds.WorldResult;

@PluginDescriptor(
	name = "Chat League Icons",
	description = "Change icon for players playing on league worlds.",
	tags = {"icons", "chat", "league", "recent"}
)
@Slf4j
public class ChatLeagueIconsPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private WorldClient worldClient;

	@Inject
	private ChatLeagueIconsConfig config;

	private static final String LEAGUE_ACTIVITY = "Twisted League"; // Needs to be changed each league
	private static final String SCRIPT_EVENT_SET_CHATBOX_INPUT = "setChatboxInput";
	private int leagueIconOffset = -1; // Starting index of league icons
	private int leagueIconIndex = -1; // Currently chosen icon index

	@Provides
	ChatLeagueIconsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatLeagueIconsConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals("chatLeagueIcons"))
		{
			setLeagueIconIndex();
			updatePlayerChatIcon();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			loadLeagueIcon();
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent scriptCallbackEvent)
	{
		if (scriptCallbackEvent.getEventName().equals(SCRIPT_EVENT_SET_CHATBOX_INPUT))
		{
			updatePlayerChatIcon();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (client.getGameState() != GameState.LOADING && client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		String cleanName = getCleanName(chatMessage.getName());

		switch (chatMessage.getType())
		{
			case FRIENDSCHAT:
				if (config.clanChatIcons() && friendlyOnLeagueWorld(cleanName))
				{
					insertLeagueIcon(chatMessage);
				}
				break;
			case PRIVATECHAT:
			case MODPRIVATECHAT:
				// Unable to change icon on PMs if they are not a friend or in clan chat
				if (config.privateMessageIcons() && friendlyOnLeagueWorld(cleanName))
				{
					insertLeagueIcon(chatMessage);
				}
				break;
			case PUBLICCHAT:
			case MODCHAT:
				if (config.publicChatIcons() && playerOnLeagueWorld())
				{
					insertLeagueIcon(chatMessage);
				}
				break;
		}
	}

	/**
	 * Set the league icon index of chosen icon.
	 */
	private void setLeagueIconIndex()
	{
		leagueIconIndex = leagueIconOffset + config.leagueIcon().ordinal();
	}

	/**
	 * Adds the League Icon in front of player names chatting from a league world.
	 *
	 * @param chatMessage chat message to edit sender name on
	 */
	private void insertLeagueIcon(final ChatMessage chatMessage)
	{
		log.debug(chatMessage.toString());
		String cleanName = getCleanName(chatMessage.getName());
		String nameWithIcon = getNameWithIcon(leagueIconIndex, cleanName);

		final MessageNode messageNode = chatMessage.getMessageNode();
		messageNode.setName(nameWithIcon);

		chatMessageManager.update(messageNode); // no idea what this does, was in emoji plugin
		client.refreshChat();
	}

	/**
	 * Update the player icon in chat widget if playing on League world
	 */
	private void updatePlayerChatIcon()
	{
		if (playerOnLeagueWorld())
		{
			AccountType accountType = client.getAccountType();
			String name = client.getLocalPlayer().getName();

			if (config.playerChatIcon())
			{
				name = getNameWithIcon(leagueIconIndex, name);
			}
			else if (accountType.isIronman())
			{
				int index = -1;
				switch (accountType)
				{
					case IRONMAN:
						index = 2;
						break;
					case HARDCORE_IRONMAN:
						index = 10;
						break;
					case ULTIMATE_IRONMAN:
						index = 3;
						break;
				}
				name = getNameWithIcon(index, name);
			}

			Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
			if (chatboxInput != null)
			{
				final boolean isChatboxTransparent = client.isResized() && client.getVar(Varbits.TRANSPARENT_CHATBOX) == 1;
				final Color textColor = isChatboxTransparent ? JagexColors.CHAT_TYPED_TEXT_TRANSPARENT_BACKGROUND : JagexColors.CHAT_TYPED_TEXT_OPAQUE_BACKGROUND;
				chatboxInput.setText(name + ": " + ColorUtil.wrapWithColorTag(client.getVar(VarClientStr.CHATBOX_TYPED_TEXT) + "*", textColor));
			}
		}
	}

	/**
	 * Create a player name with icon in front of the name
	 *
	 * @param iconIndex index of icon to add
	 * @param name      name of player
	 * @return String with icon in front of the name
	 */
	private String getNameWithIcon(int iconIndex, String name)
	{
		String icon = "<img=" + iconIndex + ">";
		return icon + name;
	}

	/**
	 * Checks if a player name is a friend or clan member on a league world.
	 *
	 * @param name name of player to check.
	 * @return boolean true/false.
	 */
	private boolean friendlyOnLeagueWorld(String name)
	{
		ChatPlayer player = getChatPlayerFromName(name);

		if (player == null)
		{
			return false;
		}

		int friendlyWorld = player.getWorld();
		return isLeagueWorld(friendlyWorld);
	}

	/**
	 * Checks if the player is currently on a league world.
	 *
	 * @return boolean true/false.
	 */
	private boolean playerOnLeagueWorld()
	{
		int currentWorld = client.getWorld();
		return isLeagueWorld(currentWorld);
	}

	/**
	 * Checks if the world is a League world.
	 *
	 * @param worldNumber number of the world to check.
	 * @return boolean true/false if it is a league world or not.
	 */
	private boolean isLeagueWorld(int worldNumber)
	{
		try
		{
			final WorldResult worldResult = worldClient.lookupWorlds();

			if (worldResult == null)
			{
				log.warn("Failed to lookup worlds.");
				return false;
			}

			final World world = worldResult.findWorld(worldNumber);

			if (world != null)
			{
				String activity = world.getActivity();
				return activity.equals(LEAGUE_ACTIVITY);
			}
		}
		catch (IOException e)
		{
			log.warn("Error looking up world {}. Error: {}", worldNumber, e);
		}
		return false;
	}

	/**
	 * Loads all league icons into the client.
	 */
	private void loadLeagueIcon()
	{
		final IndexedSprite[] modIcons = client.getModIcons();

		if (leagueIconOffset != -1 || modIcons == null)
		{
			return;
		}

		leagueIconOffset = modIcons.length;
		final LeagueIcon[] leagueIcons = LeagueIcon.values();
		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + leagueIcons.length);

		for (int i = 0; i < leagueIcons.length; i++)
		{
			final LeagueIcon leagueIcon = leagueIcons[i];

			try
			{
				final BufferedImage image = leagueIcon.loadImage();
				final IndexedSprite sprite = ImageUtil.getImageIndexedSprite(image, client);
				newModIcons[leagueIconOffset + i] = sprite;
			}
			catch (Exception ex)
			{
				log.warn("Failed to load the sprite for league icon " + leagueIcon, ex);
			}
		}

		setLeagueIconIndex();
		client.setModIcons(newModIcons);
	}

	/**
	 * Cleans up a username by removing possible tags. Effectively removing any previous icon.
	 *
	 * @param name name to clean.
	 * @return the name without tags.
	 */
	private String getCleanName(String name)
	{
		return Text.removeTags(name);
	}

	/**
	 * Gets a ChatPlayer object from a clean name by searching clan and friends list.
	 *
	 * @param name name of player to find.
	 * @return ChatPlayer if found, else null.
	 */
	private ChatPlayer getChatPlayerFromName(String name)
	{
		// Search clan members first, because if a friend is in the clan chat but their private
		// chat is 'off', then we won't know the world
		ClanMember[] clanMembers = client.getClanMembers();

		if (clanMembers != null)
		{
			for (ClanMember clanMember : clanMembers)
			{
				if (clanMember != null && clanMember.getUsername().equals(name))
				{
					return clanMember;
				}
			}
		}

		Friend[] friends = client.getFriends();

		if (friends != null)
		{
			for (Friend friend : friends)
			{
				if (friend != null && friend.getName().equals(name))
				{
					return friend;
				}
			}
		}

		return null;
	}
}
