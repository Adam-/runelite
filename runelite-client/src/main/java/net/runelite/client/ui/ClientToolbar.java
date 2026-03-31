/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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
package net.runelite.client.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import static net.runelite.client.ui.ClientUI.CONFIG_CLIENT_SIDEBAR_ORDER;
import static net.runelite.client.ui.ClientUI.CONFIG_GROUP;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

/**
 * Plugin toolbar.
 */
@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ClientToolbar
{
	private static final int TAB_IMAGE_SIZE = 16;

	private final Provider<ClientUI> clientUI;
	private final ConfigManager configManager;

	@Getter(AccessLevel.PACKAGE)
	private JTabbedPane sidebar;
	private final List<NavigationButton> navButtons = new ArrayList<>();
	private final Deque<HistoryEntry> selectedTabHistory = new ArrayDeque<>();
	private NavigationButton selectedTab;
	private int dragCurIndex = -1;

	@RequiredArgsConstructor
	private static class HistoryEntry
	{
		private final boolean sidebarOpen;
		private final NavigationButton navBtn;
	}

	void init()
	{
		sidebar = new JTabbedPane(JTabbedPane.RIGHT);
		sidebar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sidebar.setOpaque(true);
		sidebar.putClientProperty(FlatClientProperties.STYLE, "tabInsets: 2,5,2,5; variableSize: true; deselectable: true; tabHeight: 26");
		sidebar.setSelectedIndex(-1);
		sidebar.addChangeListener(ev ->
		{
			if (dragCurIndex > -1)
			{
				return;
			}

			NavigationButton oldSelectedTab = selectedTab;
			NavigationButton newSelectedTab;

			int index = sidebar.getSelectedIndex();
			if (index < 0)
			{
				newSelectedTab = null;
			}
			else
			{
				newSelectedTab = navButtons.get(index);
			}

			if (oldSelectedTab == newSelectedTab)
			{
				return;
			}

			selectedTab = newSelectedTab;

			if (sidebar.isVisible())
			{
				pushHistory();

				if (oldSelectedTab != null)
				{
					SwingUtil.deactivate(oldSelectedTab.getPanel());
				}
				if (newSelectedTab != null)
				{
					SwingUtil.activate(newSelectedTab.getPanel());
				}

				if (newSelectedTab == null)
				{
					clientUI.get().giveClientFocus();
				}
			}
		});
		sidebar.addMouseMotionListener(new MouseMotionListener()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (dragCurIndex > -1)
				{
					int dragEndIndex = sidebar.indexAtLocation(e.getX(), e.getY());
					if (dragEndIndex > -1 && dragCurIndex != dragEndIndex)
					{
						System.out.println("mouse drag rebuild " + dragCurIndex + " to " + dragEndIndex);
						reorderNavButton(dragCurIndex, dragEndIndex);
						saveSidebarOrder();
						rebuildSidebar();
						dragCurIndex = dragEndIndex;
					}
				}
			}

			@Override
			public void mouseMoved(MouseEvent e)
			{
			}
		});
		final var defaultMouseListener = sidebar.getMouseListeners()[0];
		sidebar.removeMouseListener(defaultMouseListener);
		sidebar.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				defaultMouseListener.mouseClicked(e);

				if (e.getButton() == MouseEvent.BUTTON3)
				{
					int index = 0;
					for (var navBtn : navButtons)
					{
						Rectangle bounds = sidebar.getBoundsAt(index++);
						if (bounds != null && bounds.contains(e.getX(), e.getY()))
						{
							if (navBtn.getPopup() != null)
							{
								var menu = new JPopupMenu();
								navBtn.getPopup().forEach((name, cb) ->
								{
									var menuItem = new JMenuItem(name);
									menuItem.addActionListener(ev -> cb.run());
									menu.add(menuItem);
								});
								menu.show(sidebar, e.getX(), e.getY());
							}
							return;
						}
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				// TODO RL HOTKEY THING
				if (SwingUtilities.isLeftMouseButton(e) && (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
				{
					dragCurIndex = sidebar.indexAtLocation(e.getX(), e.getY());
					System.out.println("Drag start at " + dragCurIndex);
					return;
				}
				defaultMouseListener.mousePressed(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (dragCurIndex > -1)
				{
					System.out.println("Drag end");
					dragCurIndex = -1;
					int index = sidebar.getSelectedIndex();
					selectedTab = index == -1 ? null : navButtons.get(index);
					return;
				}

				defaultMouseListener.mouseReleased(e);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				defaultMouseListener.mouseEntered(e);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				defaultMouseListener.mouseExited(e);
			}
		});
		JPopupMenu menu = new JPopupMenu();
		JMenuItem item = new JMenuItem("Reset order");
		item.addActionListener(l -> unsetSidebarOrder());
		menu.add(item);
		sidebar.setComponentPopupMenu(menu);
	}

	private void reorderNavButton(int from, int to)
	{
		var n = navButtons.remove(from);
		navButtons.add(to, n);
	}

	private Comparator<NavigationButton> navButtonComparator()
	{
		var order = loadSidebarOrder();
		return Comparator.<NavigationButton>comparingInt(n ->
		{
			int i = order.indexOf(n.getId());
			if (i == -1)
			{
				i = Integer.MAX_VALUE; // unknown panels go to the end
			}
			return i;
		}).thenComparing(NavigationButton.COMPARATOR);
	}


	public void addNavigation(NavigationButton button)
	{
		SwingUtilities.invokeLater(() -> addNavigation0(button));
	}

	public void removeNavigation(NavigationButton button)
	{
		SwingUtilities.invokeLater(() -> removeNavigation0(button));
	}

	void addNavigation0(NavigationButton navBtn)
	{
		if (navBtn.getPanel() == null)
		{
			clientUI.get().addToolbarPanelNavButton(navBtn);
			return;
		}

		if (navButtons.contains(navBtn))
		{
			return;
		}

		navButtons.add(navBtn);
		navButtons.sort(navButtonComparator());

		Icon icon = new ImageIcon(ImageUtil.resizeImage(navBtn.getIcon(), TAB_IMAGE_SIZE, TAB_IMAGE_SIZE));

		int index = navButtons.indexOf(navBtn);
		sidebar.insertTab(null, icon, navBtn.getPanel().getWrappedPanel(), navBtn.getTooltip(), index);

		// insertTab changes the selected index when the first tab is inserted, avoid this
		if (sidebar.getTabCount() == 1)
		{
			sidebar.setSelectedIndex(-1);
		}
	}

	void removeNavigation0(NavigationButton navBtn)
	{
		if (navBtn.getPanel() == null)
		{
			clientUI.get().removeToolbarPanelNavButton(navBtn);
		}
		else
		{
			boolean closingOpenTab = !selectedTabHistory.isEmpty() && selectedTabHistory.getLast().navBtn == navBtn;
			selectedTabHistory.removeIf(it -> it.navBtn == navBtn);
			sidebar.remove(navBtn.getPanel().getWrappedPanel());
			if (closingOpenTab)
			{
				HistoryEntry entry = selectedTabHistory.isEmpty()
					? new HistoryEntry(true, null)
					: selectedTabHistory.removeLast();

				openPanel(entry.navBtn, entry.sidebarOpen);
			}
		}

		navButtons.remove(navBtn);
	}

	void rebuildSidebar()
	{
		navButtons.sort(navButtonComparator());

		var component = sidebar.getSelectedComponent();
		sidebar.removeAll();

		for (var navButton : navButtons)
		{
			Icon icon = new ImageIcon(ImageUtil.resizeImage(navButton.getIcon(), TAB_IMAGE_SIZE, TAB_IMAGE_SIZE));
			sidebar.insertTab(null, icon, navButton.getPanel().getWrappedPanel(), navButton.getTooltip(),
				sidebar.getTabCount());
		}

		if (component != null)
		{
			sidebar.setSelectedComponent(component);
		}
		else
		{
			sidebar.setSelectedIndex(-1);
		}
	}

	public void openPanel(NavigationButton button)
	{
		assert SwingUtilities.isEventDispatchThread() : "must be on EDT";
		openPanel(button, true);
	}

	void openPanel(NavigationButton navBtn, boolean showSidebar)
	{
		if (navBtn != null && !navButtons.contains(navBtn))
		{
			return;
		}

		int index = navBtn == null ? -1 : navButtons.indexOf(navBtn);
		sidebar.setSelectedIndex(index);

		clientUI.get().toggleSidebar(showSidebar, false);

		pushHistory();
	}

	boolean isVisible()
	{
		return sidebar.isVisible();
	}

	// do not call this directly, call from ClientUI.toggleSidebar
	void toggleSidebar(boolean open, boolean pushHistory)
	{
		sidebar.setVisible(open);

		if (pushHistory)
		{
			pushHistory();
		}

		if (selectedTab != null)
		{
			if (open)
			{
				SwingUtil.activate(selectedTab.getPanel());
			}
			else
			{
				SwingUtil.deactivate(selectedTab.getPanel());
			}
		}
	}

	void open()
	{
		NavigationButton open = null;
		while (!selectedTabHistory.isEmpty())
		{
			HistoryEntry historyEntry = selectedTabHistory.removeLast();
			if (historyEntry.navBtn != null)
			{
				open = historyEntry.navBtn;
				break;
			}
		}

		if (open == null && !navButtons.isEmpty())
		{
			open = navButtons.get(0);
		}

		openPanel(open, true);
	}

	private void pushHistory()
	{
		selectedTabHistory.addLast(new HistoryEntry(sidebar.isVisible(), selectedTab));

		// we keep multiple history entries so you can open a panel, close it, open another, *remove* it, then resume the first open panel
		if (selectedTabHistory.size() > 4)
		{
			HistoryEntry ent = selectedTabHistory.removeFirst();
			// Try to always keep a panel in the history
			if (ent.navBtn != null && selectedTabHistory.stream().noneMatch(it -> it.navBtn != null))
			{
				selectedTabHistory.removeFirst();
				selectedTabHistory.addFirst(ent);
			}
		}
	}

	private void unsetSidebarOrder()
	{
		configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_CLIENT_SIDEBAR_ORDER);
	}

	private void saveSidebarOrder()
	{
		var s = navButtons.stream()
			.map(NavigationButton::getId)
			.collect(Collectors.joining(","));
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_CLIENT_SIDEBAR_ORDER, s);
	}

	private List<String> loadSidebarOrder()
	{
		final String s = configManager.getConfiguration(CONFIG_GROUP, CONFIG_CLIENT_SIDEBAR_ORDER);
		if (s == null)
		{
			return Collections.emptyList();
		}
		final String[] navButtonIds = s.split(",");
		return Arrays.asList(navButtonIds);
	}
}
