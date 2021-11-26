/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
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
package net.runelite.client.ui.overlay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import net.runelite.api.Varbits;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.OverlayMenuClicked;

@Slf4j
public class WidgetOverlay extends Overlay
{
	public static Collection<WidgetOverlay> createOverlays(Injector injector)
	{
		Provider<WidgetOverlay> p = injector.getProvider(WidgetOverlay.class);
		return Arrays.asList(
			p.get().init(WidgetInfo.RESIZABLE_MINIMAP_WIDGET, OverlayPosition.CANVAS_TOP_RIGHT),
			p.get().init(WidgetInfo.RESIZABLE_MINIMAP_STONES_WIDGET, OverlayPosition.CANVAS_TOP_RIGHT),
			// The client forces the oxygen bar below the xp tracker, so set its priority lower
			p.get().init(WidgetInfo.FOSSIL_ISLAND_OXYGENBAR, OverlayPosition.TOP_CENTER, OverlayPriority.HIGH),
			injector.getInstance(XpTrackerWidgetOverlay.class).init(WidgetInfo.EXPERIENCE_TRACKER_WIDGET, OverlayPosition.TOP_RIGHT),
			p.get().init(WidgetInfo.RAIDS_POINTS_INFOBOX, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.TOB_PARTY_INTERFACE, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.TOB_PARTY_STATS, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.GWD_KC, OverlayPosition.TOP_LEFT).makeHideable("God Wars Essence"),
			p.get().init(WidgetInfo.TITHE_FARM, OverlayPosition.TOP_RIGHT),
			p.get().init(WidgetInfo.PEST_CONTROL_BOAT_INFO, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.PEST_CONTROL_KNIGHT_INFO_CONTAINER, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.PEST_CONTROL_ACTIVITY_SHIELD_INFO_CONTAINER, OverlayPosition.TOP_RIGHT),
			p.get().init(WidgetInfo.ZEAH_MESS_HALL_COOKING_DISPLAY, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.PVP_KILLDEATH_COUNTER, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.SKOTIZO_CONTAINER, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.KOUREND_FAVOUR_OVERLAY, OverlayPosition.TOP_CENTER),
			p.get().init(WidgetInfo.PYRAMID_PLUNDER_DATA, OverlayPosition.TOP_CENTER),
			p.get().init(WidgetInfo.LMS_INFO, OverlayPosition.TOP_RIGHT),
			p.get().init(WidgetInfo.LMS_KDA, OverlayPosition.TOP_RIGHT),
			p.get().init(WidgetInfo.GAUNTLET_TIMER_CONTAINER, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.HALLOWED_SEPULCHRE_TIMER_CONTAINER, OverlayPosition.TOP_LEFT),
			// The client forces the health overlay bar below the xp tracker, so set its priority lower
			p.get().init(WidgetInfo.HEALTH_OVERLAY_BAR, OverlayPosition.TOP_CENTER, OverlayPriority.HIGH),
			p.get().init(WidgetInfo.TOB_HEALTH_BAR, OverlayPosition.TOP_CENTER),
			p.get().init(WidgetInfo.NIGHTMARE_PILLAR_HEALTH, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.VOLCANIC_MINE_VENTS_INFOBOX_GROUP, OverlayPosition.BOTTOM_RIGHT),
			p.get().init(WidgetInfo.VOLCANIC_MINE_STABILITY_INFOBOX_GROUP, OverlayPosition.BOTTOM_LEFT),
			p.get().init(WidgetInfo.MULTICOMBAT_FIXED, OverlayPosition.BOTTOM_RIGHT),
			p.get().init(WidgetInfo.MULTICOMBAT_RESIZEABLE_MODERN, OverlayPosition.CANVAS_TOP_RIGHT),
			p.get().init(WidgetInfo.MULTICOMBAT_RESIZEABLE_CLASSIC, OverlayPosition.CANVAS_TOP_RIGHT),
			p.get().init(WidgetInfo.TEMPOROSS_STATUS_INDICATOR, OverlayPosition.TOP_LEFT),
			p.get().init(WidgetInfo.BA_HEAL_TEAMMATES, OverlayPosition.BOTTOM_LEFT),
			p.get().init(WidgetInfo.BA_TEAM, OverlayPosition.TOP_RIGHT),
			p.get().init(WidgetInfo.PVP_WILDERNESS_SKULL_CONTAINER, OverlayPosition.DETACHED)
				.makeHideable("Wilderness Indicator")
		);
	}

	protected final Client client;
	protected final ChatMessageManager chatMessageManager;

	@Setter
	@Accessors(fluent = true)
	private WidgetInfo widgetInfo;
	private final Rectangle parentBounds = new Rectangle();
	private boolean revalidate;

	private String name;
	@Getter
	private boolean hideable;
	private boolean hidden;

	@Inject
	private WidgetOverlay(Client client, EventBus eventBus, ChatMessageManager chatMessageManager)
	{
		this.client = client;
		this.chatMessageManager = chatMessageManager;
		eventBus.register(this);
	}

	protected WidgetOverlay init(final WidgetInfo widgetInfo, final OverlayPosition overlayPosition)
	{
		return init(widgetInfo, overlayPosition, OverlayPriority.HIGHEST);
	}

	protected WidgetOverlay init(final WidgetInfo widgetInfo, final OverlayPosition overlayPosition, final OverlayPriority overlayPriority)
	{
		this.widgetInfo = widgetInfo;
		setPriority(overlayPriority);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPosition(overlayPosition);
		// It's almost possible to drawAfterInterface(widgetInfo.getGroupId()) here, but that fires
		// *after* the native components are drawn, which is too late.
		return this;
	}

	@Override
	public String getName()
	{
		return Objects.toString(widgetInfo);
	}

	private WidgetOverlay makeHideable(String name)
	{
		this.name = name;
		hideable = true;
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, "Hide", name));
		return this;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Widget widget = client.getWidget(widgetInfo);
		if (widget == null)
		{
			return null;
		}

		if (widget.isHidden())
		{
			if (!hidden || !client.isKeyPressed(KeyCode.KC_SHIFT))
			{
				return null;
			}
			widget.setHidden(false);
		}
		else
		{
			if (hidden && !client.isKeyPressed(KeyCode.KC_SHIFT))
			{
				widget.setHidden(true);
				return null;
			}
		}

		final Rectangle parent = getParentBounds(widget);
		if (parent.isEmpty())
		{
			return null;
		}

		final Rectangle bounds = getBounds();
		// OverlayRenderer sets the overlay bounds to where it would like the overlay to render at prior to calling
		// render(). If the overlay has a preferred location or position set we update the widget position to that.
		if (getPreferredLocation() != null || getPreferredPosition() != null)
		{
			// The widget relative pos is relative to the parent
			widget.setRelativeX(bounds.x - parent.x);
			widget.setRelativeY(bounds.y - parent.y);
		}
		else
		{
			if (revalidate)
			{
				revalidate = false;
				log.debug("Revalidating {}", widgetInfo);
				// Revalidate the widget to reposition it back to its normal location after an overlay reset
				widget.revalidate();
			}

			// Update the overlay bounds to the widget bounds so the drag overlay renders correctly.
			// Note OverlayManager uses original bounds reference to render managing mode and for
			// onMouseOver, so update the existing bounds vs. replacing the reference.
			Rectangle widgetBounds = widget.getBounds();
			bounds.setBounds(widgetBounds.x, widgetBounds.y, widgetBounds.width, widgetBounds.height);
		}

		return new Dimension(widget.getWidth(), widget.getHeight());
	}

	private Rectangle getParentBounds(final Widget widget)
	{
		if (widget == null || widget.isHidden())
		{
			parentBounds.setBounds(new Rectangle());
			return parentBounds;
		}

		final Widget parent = widget.getParent();
		final Rectangle bounds = parent == null
			? new Rectangle(client.getRealDimensions())
			: parent.getBounds();

		parentBounds.setBounds(bounds);
		return bounds;
	}

	@Override
	public Rectangle getParentBounds()
	{
		if (!client.isClientThread())
		{
			// During overlay drag this is called on the EDT, so we just
			// cache and reuse the last known parent bounds.
			return parentBounds;
		}

		final Widget widget = client.getWidget(widgetInfo);
		return getParentBounds(widget);
	}

	@Override
	public void revalidate()
	{
		// Revalidate must be called on the client thread, so defer til next frame
		revalidate = true;
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked overlayMenuClicked)
	{
		OverlayMenuEntry overlayMenuEntry = overlayMenuClicked.getEntry();
		if (overlayMenuEntry.getMenuAction() == MenuAction.RUNELITE_OVERLAY
			&& overlayMenuClicked.getOverlay() == this)
		{
			String option = overlayMenuClicked.getEntry().getOption();
			if (option.equals("Hide"))
			{
				log.debug("Hiding component {}", widgetInfo);
				hidden = true;
				// Switch menu entry to show
				getMenuEntries().clear();
				getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, "Show", name));

				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage("You've hidden the " + name + " overlay. It will remain hidden until " +
						"you unhide it by holding shift and selecting \"Show\".")
					.build());
			}
			else if (option.equals("Show"))
			{
				log.debug("Showing component {}", widgetInfo);
				hidden = false;
				// Switch menu entry to hide
				getMenuEntries().clear();
				getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, "Hide", name));
			}
		}
	}

	private static class XpTrackerWidgetOverlay extends WidgetOverlay
	{
		private final OverlayManager overlayManager;

		@Inject
		private XpTrackerWidgetOverlay(OverlayManager overlayManager, Client client, EventBus eventBus,
			ChatMessageManager chatMessageManager)
		{
			super(client, eventBus, chatMessageManager);
			this.overlayManager = overlayManager;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			// The xptracker component layer isn't hidden if the counter and process bar are both configured "Off",
			// it just has its children hidden.
			if (client.getVar(Varbits.EXPERIENCE_TRACKER_COUNTER) == 30 // Off
				&& client.getVar(Varbits.EXPERIENCE_TRACKER_PROGRESS_BAR) == 0) // Off
			{
				return null;
			}

			return super.render(graphics);
		}

		/**
		 * Get the overlay position of the xptracker based on the configured location in-game.
		 *
		 * @return
		 */
		@Override
		public OverlayPosition getPosition()
		{
			if (!client.isClientThread())
			{
				// During overlay drag, getPosition() is called on the EDT, so we just
				// cache and reuse the last known configured position.
				return super.getPosition();
			}

			OverlayPosition position;
			switch (client.getVar(Varbits.EXPERIENCE_TRACKER_POSITION))
			{
				case 0:
				default:
					position = OverlayPosition.TOP_RIGHT;
					break;
				case 1:
					position = OverlayPosition.TOP_CENTER;
					break;
				case 2:
					position = OverlayPosition.TOP_LEFT;
					break;
			}

			if (position != super.getPosition())
			{
				log.debug("Xp tracker moved position");
				setPosition(position);
				overlayManager.rebuildOverlayLayers();
			}
			return position;
		}
	}
}
