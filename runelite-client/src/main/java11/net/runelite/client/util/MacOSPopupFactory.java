package net.runelite.client.util;

import java.awt.Component;
import javax.swing.Popup;
import javax.swing.PopupFactory;

public class MacOSPopupFactory extends PopupFactory
{
	@Override
	public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException
	{
		return super.getPopup(owner, contents, x, y, true);
	}
}