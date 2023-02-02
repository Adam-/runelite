/*
 * Copyright (c) 2018, Kamiel, <https://github.com/Kamielvf>
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
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
package net.runelite.client.plugins.profile;

import com.google.common.base.Strings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.plugins.screenmarkers.ScreenMarkerPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

class ProfileCard extends JPanel
{
	private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
		BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

	private static final ImageIcon DELETE_ICON;
	private static final ImageIcon DELETE_HOVER_ICON;

	private final JLabel deleteLabel = new JLabel();

	private final FlatTextField nameInput = new FlatTextField();
	private final JButton rename = new JButton("Rename");

	private final ProfilePlugin plugin;
	private final ConfigProfile profile;
	private final boolean active;

	static
	{
		final BufferedImage deleteImg = ImageUtil.loadImageResource(ScreenMarkerPlugin.class, "delete_icon.png");
		DELETE_ICON = new ImageIcon(deleteImg);
		DELETE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(deleteImg, -100));
	}

	ProfileCard(ProfilePlugin plugin, ConfigProfile profile, boolean active)
	{
				this.plugin = plugin;
		this.profile = profile;
		this.active = active;

		setLayout(new BorderLayout());

		JPanel nameActions = setupNameActions();

		nameInput.setText(profile.getName());
		nameInput.setBorder(null);
		nameInput.setEditable(false);
		nameInput.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameInput.setPreferredSize(new Dimension(0, 24));
		nameInput.getTextField().setForeground(Color.WHITE);
		nameInput.getTextField().setBorder(new EmptyBorder(0, 8, 0, 0));
		nameInput.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					// this is enough to trip the focus listener to save.
					requestFocusInWindow();
				}
			}
		});
		nameInput.getTextField().addFocusListener(new FocusListener()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				save();
			}
		});

		JPanel nameWrapper = new JPanel(new BorderLayout());
		nameWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameWrapper.setBorder(NAME_BOTTOM_BORDER);

		nameWrapper.add(nameInput, BorderLayout.CENTER);
		nameWrapper.add(nameActions, BorderLayout.EAST);

		JPanel bottomContainer = new JPanel(new BorderLayout());
		bottomContainer.setBorder(new EmptyBorder(8, 0, 8, 0));
		bottomContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		leftActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		rightActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		deleteLabel.setIcon(DELETE_ICON);
		deleteLabel.setToolTipText("Delete profile");
		deleteLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				int confirm = JOptionPane.showConfirmDialog(ProfileCard.this,
					"Are you sure you want to delete this profile?",
					"Warning", JOptionPane.OK_CANCEL_OPTION);

				if (confirm == 0)
				{
					plugin.delete(profile.getId());
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				deleteLabel.setIcon(DELETE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				deleteLabel.setIcon(DELETE_ICON);
			}
		});

		if (!active)
		rightActions.add(deleteLabel);

		bottomContainer.add(leftActions, BorderLayout.WEST);
		bottomContainer.add(rightActions, BorderLayout.EAST);

		add(nameWrapper, BorderLayout.NORTH);
		add(bottomContainer, BorderLayout.CENTER);

		if(active)
		{
			setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, ColorScheme.BRAND_ORANGE));
		}
		else
		{
			setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, getBackground()));
			// mouse listener for switching profiles
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
					{
						plugin.change(profile.getId());
					}
				}
			});
		}

			updateVisibility();
		updateFill();
		updateBorder();
		updateBorder();
		updateLabelling();

		setupMenu();
	}

	private void setupMenu() {
		JPopupMenu menu = new JPopupMenu();

//		final JMenuItem rename = new JMenuItem("Rename");
//		rename.addActionListener(e -> {
//			String profileName = JOptionPane.showInputDialog("Profile name");
//		});
//		menu.add(rename);

		if (!active)
		{
			final JMenuItem deleteProfile = new JMenuItem("Delete");
			deleteProfile.addActionListener(e ->
			{
				int confirm = JOptionPane.showConfirmDialog(ProfileCard.this,
					"Are you sure you want to delete this profile?",
					"Warning", JOptionPane.OK_CANCEL_OPTION);
				if (confirm == 0)
				{
					plugin.delete(profile.getId());
				}
			});
			menu.add(deleteProfile);
		}

		final JMenuItem exportProfile = new JMenuItem("Export");
		exportProfile.addActionListener(e ->
		{
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Profile export");
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("RuneLite properties", "properties"));
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setCurrentDirectory(ProfilePlugin.lastFileChooserDirectory);
			int selection = fileChooser.showSaveDialog(this);
			if (selection == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				ProfilePlugin.lastFileChooserDirectory = file.getParentFile();
				// add properties file extension
				file = new File(file.getParentFile(), file.getName() + ".properties");
				plugin.export(profile, file);
			}
		});
		menu.add(exportProfile);

		final JMenuItem cloneProfile = new JMenuItem("Clone");
		cloneProfile.addActionListener(e -> {

		});
		menu.add(cloneProfile);

		setComponentPopupMenu(menu);
	}

	private JPanel setupNameActions() {
		SwingUtil.removeButtonDecorations(rename);
		rename.setHorizontalAlignment(SwingConstants.RIGHT);
//		rename.setBorder(null);
		rename.setFont(FontManager.getRunescapeSmallFont());
		rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		rename.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				nameInput.setEditable(true);
				updateNameActions(true);
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker().darker());
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
			}
		});

		JPanel nameActions = new JPanel(new BorderLayout(3, 0));
		nameActions.setBorder(new EmptyBorder(0, 0, 0, 8));
		nameActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

//		nameActions.add(save, BorderLayout.EAST);
//		nameActions.add(cancel, BorderLayout.WEST);
		nameActions.add(rename, BorderLayout.CENTER);

		return nameActions;
	}

	private void preview(boolean on)
	{
//		if (visible)
//		{
//			return;
//		}

//		marker.getMarker().setVisible(on);
	}

	private void toggle(boolean on)
	{
//		visible = on;
//		marker.getMarker().setVisible(visible);
//		plugin.updateConfig();
//		updateVisibility();
	}

	private void toggleLabelling(boolean on)
	{
//		showLabel = on;
//		marker.getMarker().setLabelled(showLabel);
//		plugin.updateConfig();
//		updateLabelling();
	}

	private void save()
	{
		if (Strings.isNullOrEmpty(nameInput.getText())) {
			nameInput.setText(profile.getName());
			return;
		}

		plugin.rename(profile.getId(), nameInput.getText());
//		marker.getMarker().setName(nameInput.getText());
//		plugin.updateConfig();

		nameInput.setEditable(false);
		updateNameActions(false);
		requestFocusInWindow();
	}

//	private void cancel()
//	{
//		nameInput.setEditable(false);
////		nameInput.setText(marker.getMarker().getName());
//		updateNameActions(false);
//		requestFocusInWindow();
//	}

	private void updateNameActions(boolean saveAndCancel)
	{
//		save.setVisible(saveAndCancel);
//		cancel.setVisible(saveAndCancel);
		rename.setVisible(!saveAndCancel);

		if (saveAndCancel)
		{
			nameInput.getTextField().requestFocusInWindow();
			nameInput.getTextField().selectAll();
		}
	}

	/* Updates the thickness without saving on config */
	private void updateThickness(boolean save)
	{
//		marker.getMarker().setBorderThickness((Integer) thicknessSpinner.getValue());
//		updateBorder();
//		if (save)
//		{
//			plugin.updateConfig();
//		}
	}

	private void updateVisibility()
	{
//		visibilityLabel.setIcon(visible ? VISIBLE_ICON : INVISIBLE_ICON);
//		visibilityLabel.setToolTipText(visible ? "Hide screen marker" : "Show screen marker");
	}

	private void updateLabelling()
	{
//		labelIndicator.setIcon(showLabel ? LABEL_ICON : NO_LABEL_ICON);
//		labelIndicator.setToolTipText(showLabel ? "Hide label" : "Show label");
	}

	private void updateFill()
	{
//		final boolean isFullyTransparent = marker.getMarker().getFill().getAlpha() == 0;
//
//		if (isFullyTransparent)
//		{
//			fillColorIndicator.setBorder(null);
//		}
//		else
//		{
//			Color color = marker.getMarker().getFill();
//			Color fullColor = new Color(color.getRed(), color.getGreen(), color.getBlue());
//			fillColorIndicator.setBorder(new MatteBorder(0, 0, 3, 0, fullColor));
//		}
//
//		fillColorIndicator.setIcon(isFullyTransparent ? NO_FILL_COLOR_ICON : FILL_COLOR_ICON);
	}

	private void updateBorder()
	{
//		if (marker.getMarker().getBorderThickness() == 0)
//		{
//			borderColorIndicator.setBorder(null);
//		}
//		else
//		{
//			Color color = marker.getMarker().getColor();
//			borderColorIndicator.setBorder(new MatteBorder(0, 0, 3, 0, color));
//		}
//
//		borderColorIndicator.setIcon(marker.getMarker().getBorderThickness() == 0 ? NO_BORDER_COLOR_ICON : BORDER_COLOR_ICON);
	}

	private void openFillColorPicker()
	{
//		final Color fillColor = marker.getMarker().getFill();
//		RuneliteColorPicker colorPicker = plugin.getColorPickerManager().create(
//			SwingUtilities.windowForComponent(this),
//			fillColor.getAlpha() == 0 ? ColorUtil.colorWithAlpha(fillColor, DEFAULT_FILL_OPACITY) : fillColor,
//			marker.getMarker().getName() + " Fill",
//			false);
//		colorPicker.setLocation(getLocationOnScreen());
//		colorPicker.setOnColorChange(c ->
//		{
//			marker.getMarker().setFill(c);
//			updateFill();
//		});
//		colorPicker.setOnClose(c -> plugin.updateConfig());
//		colorPicker.setVisible(true);
	}

	private void openBorderColorPicker()
	{
//		RuneliteColorPicker colorPicker = plugin.getColorPickerManager().create(
//			SwingUtilities.windowForComponent(this),
//			marker.getMarker().getColor(),
//			marker.getMarker().getName() + " Border",
//			false);
//		colorPicker.setLocation(getLocationOnScreen());
//		colorPicker.setOnColorChange(c ->
//		{
//			marker.getMarker().setColor(c);
//			updateBorder();
//		});
//		colorPicker.setOnClose(c -> plugin.updateConfig());
//		colorPicker.setVisible(true);
	}
}
