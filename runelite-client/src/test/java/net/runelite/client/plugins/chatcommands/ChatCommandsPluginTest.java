/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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
package net.runelite.client.plugins.chatcommands;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import static net.runelite.api.ChatMessageType.GAMEMESSAGE;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ChatColorConfig;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChatCommandsPluginTest
{
	@Mock
	@Bind
	Client client;

	@Mock
	@Bind
	ConfigManager configManager;

	@Mock
	@Bind
	ScheduledExecutorService scheduledExecutorService;

	@Mock
	@Bind
	ChatColorConfig chatColorConfig;

	@Mock
	@Bind
	ChatCommandsConfig chatCommandsConfig;

	@Inject
	ChatCommandsPlugin chatCommandsPlugin;

	@Before
	public void before()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
	}

	private ChatMessage createMessage(ChatMessageType type, String name, String message, String sender, int timestamp)
	{
		ChatMessage chatMessage = mock(ChatMessage.class);
		when(chatMessage.getType()).thenReturn(type);
		when(chatMessage.getName()).thenReturn(name);
		when(chatMessage.getMessage()).thenReturn(message);
		when(chatMessage.getSender()).thenReturn(sender);
		when(chatMessage.getTimestamp()).thenReturn(timestamp);
		return chatMessage;
	}

	@Test
	public void testCorporealBeastKill()
	{
		when(client.getUsername()).thenReturn("Adam");

		ChatMessage chatMessageEvent = createMessage(GAMEMESSAGE, "", "Your Corporeal Beast kill count is: <col=ff0000>4</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessageEvent);

		verify(configManager).setConfiguration("killcount.adam", "corporeal beast", 4);
	}

	@Test
	public void testTheatreOfBlood()
	{
		when(client.getUsername()).thenReturn("Adam");

		ChatMessage chatMessageEvent = createMessage(GAMEMESSAGE, "", "Your completed Theatre of Blood count is: <col=ff0000>73</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessageEvent);

		verify(configManager).setConfiguration("killcount.adam", "theatre of blood", 73);
	}

	@Test
	public void testWintertodt()
	{
		when(client.getUsername()).thenReturn("Adam");

		ChatMessage chatMessageEvent = createMessage(GAMEMESSAGE, "", "Your subdued Wintertodt count is: <col=ff0000>4</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessageEvent);

		verify(configManager).setConfiguration("killcount.adam", "wintertodt", 4);
	}

	@Test
	public void testKreearra()
	{
		when(client.getUsername()).thenReturn("Adam");

		ChatMessage chatMessageEvent = createMessage(GAMEMESSAGE, "", "Your Kree'arra kill count is: <col=ff0000>4</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessageEvent);

		verify(configManager).setConfiguration("killcount.adam", "kree'arra", 4);
	}

	@Test
	public void testBarrows()
	{
		when(client.getUsername()).thenReturn("Adam");

		ChatMessage chatMessageEvent = createMessage(GAMEMESSAGE, "", "Your Barrows chest count is: <col=ff0000>277</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessageEvent);

		verify(configManager).setConfiguration("killcount.adam", "barrows chests", 277);
	}

	@Test
	public void testHerbiboar()
	{
		when(client.getUsername()).thenReturn("Adam");

		ChatMessage chatMessageEvent = createMessage(GAMEMESSAGE, "", "Your herbiboar harvest count is: <col=ff0000>4091</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessageEvent);

		verify(configManager).setConfiguration("killcount.adam", "herbiboar", 4091);
	}

	@Test
	public void testPersonalBest()
	{
		final String FIGHT_DURATION = "Fight duration: <col=ff0000>2:06</col>. Personal best: 1:19.";

		when(client.getUsername()).thenReturn("Adam");

		// This sets lastBoss
		ChatMessage chatMessage = createMessage(GAMEMESSAGE, "", "Your Kree'arra kill count is: <col=ff0000>4</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessage);

		chatMessage = createMessage(GAMEMESSAGE, "", FIGHT_DURATION, null, 0);
		chatCommandsPlugin.onChatMessage(chatMessage);

		verify(configManager).setConfiguration(eq("personalbest.adam"), eq("kree'arra"), eq(79));
	}

	@Test
	public void testPersonalBestNoTrailingPeriod()
	{
		final String FIGHT_DURATION = "Fight duration: <col=ff0000>0:59</col>. Personal best: 0:55";

		when(client.getUsername()).thenReturn("Adam");

		// This sets lastBoss
		ChatMessage chatMessage = createMessage(GAMEMESSAGE, "", "Your Zulrah kill count is: <col=ff0000>4</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessage);

		chatMessage = createMessage(GAMEMESSAGE, "", FIGHT_DURATION, null, 0);
		chatCommandsPlugin.onChatMessage(chatMessage);

		verify(configManager).setConfiguration(eq("personalbest.adam"), eq("zulrah"), eq(55));
	}

	@Test
	public void testNewPersonalBest()
	{
		final String NEW_PB = "Fight duration: <col=ff0000>3:01</col> (new personal best).";

		when(client.getUsername()).thenReturn("Adam");

		// This sets lastBoss
		ChatMessage chatMessage = createMessage(GAMEMESSAGE, "", "Your Kree'arra kill count is: <col=ff0000>4</col>.", null, 0);
		chatCommandsPlugin.onChatMessage(chatMessage);

		chatMessage = createMessage(GAMEMESSAGE, "", NEW_PB, null, 0);
		chatCommandsPlugin.onChatMessage(chatMessage);

		verify(configManager).setConfiguration(eq("personalbest.adam"), eq("kree'arra"), eq(181));
	}
}