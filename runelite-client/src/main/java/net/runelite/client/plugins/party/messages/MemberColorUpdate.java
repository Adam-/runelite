package net.runelite.client.plugins.party.messages;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberColorUpdate extends PartyMemberMessage
{
	private Color color;
}
