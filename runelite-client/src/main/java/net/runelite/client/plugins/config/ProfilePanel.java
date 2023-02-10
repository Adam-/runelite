/*
 * Copyright (c) 2023 Adam <Adam@sigterm.info>
 * Copyright (c) 2023 Abex
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
package net.runelite.client.plugins.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.config.ProfileManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.screenmarkers.ScreenMarkerPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

@Slf4j
class ProfilePanel extends PluginPanel
{
	private static final int MAX_PROFILES = 20;

	private static final ImageIcon ADD_ICON = new ImageIcon(ImageUtil.loadImageResource(ScreenMarkerPlugin.class, "add_icon.png"));
	private static final ImageIcon DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(ProfilePanel.class, "mdi_delete.png"));
	private static final ImageIcon EXPORT_ICON = new ImageIcon(ImageUtil.loadImageResource(ProfilePanel.class, "mdi_export.png"));
	private static final ImageIcon RENAME_ICON;
	private static final ImageIcon RENAME_ACTIVE_ICON;
	private static final ImageIcon CLONE_ICON = new ImageIcon(ImageUtil.loadImageResource(ProfilePanel.class, "mdi_content-duplicate.png"));
	private static final ImageIcon ARROW_RIGHT_ICON = new ImageIcon(ImageUtil.loadImageResource(ProfilePanel.class, "/util/arrow_right.png"));
	private static final ImageIcon SYNC_ICON;
	private static final ImageIcon SYNC_ACTIVE_ICON;

	private final ConfigManager configManager;
	private final ProfileManager profileManager;
	private final ScheduledExecutorService executor;
	private final EventBus eventBus;

	private final JPanel profilesList;
	private final JButton addButton;
	private final JButton importButton;

	private final Map<Long, ProfileCard> cards = new HashMap<>();

	private File lastFileChooserDirectory;

	static
	{
		BufferedImage rename = ImageUtil.loadImageResource(ProfilePanel.class, "mdi_rename.png");
		RENAME_ICON = new ImageIcon(rename);
		RENAME_ACTIVE_ICON = new ImageIcon(ImageUtil.recolorImage(rename, ColorScheme.BRAND_ORANGE));

		BufferedImage sync = ImageUtil.loadImageResource(ProfilePanel.class, "cloud_sync.png");
		SYNC_ICON = new ImageIcon(sync);
		SYNC_ACTIVE_ICON = new ImageIcon(ImageUtil.recolorImage(sync, ColorScheme.BRAND_ORANGE));
	}

	@Inject
	ProfilePanel(
		ConfigManager configManager,
		ProfileManager profileManager,
		ScheduledExecutorService executor,
		EventBus eventBus
	)
	{
		this.profileManager = profileManager;
		this.configManager = configManager;
		this.executor = executor;
		this.eventBus = eventBus;

		setBorder(new EmptyBorder(10, 10, 10, 10));

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		profilesList = new JPanel();
		profilesList.setLayout(new DynamicGridLayout(0, 1, 0, 6));

		addButton = new JButton("New Profile", ADD_ICON);
		addButton.addActionListener(ev -> createProfile());

		importButton = new JButton("Import Profile");
		importButton.addActionListener(ev ->
		{
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Profile import");
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("RuneLite properties", "properties"));
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setCurrentDirectory(lastFileChooserDirectory);
			int selection = fileChooser.showOpenDialog(this);
			if (selection == JFileChooser.APPROVE_OPTION)
			{
				File file = fileChooser.getSelectedFile();
				lastFileChooserDirectory = file.getParentFile();
				importProfile(file);
			}
		});

		JLabel info = new JLabel("<html>"
			+ "Profiles are separate sets of plugins and settings that you can switch between at any time.");

		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(profilesList)
			.addGap(8)
			.addGroup(layout.createParallelGroup()
				.addComponent(addButton)
				.addComponent(importButton))
			.addGap(8)
			.addComponent(info));

		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(profilesList)
			.addGroup(layout.createSequentialGroup()
				.addComponent(addButton)
				.addGap(8)
				.addComponent(importButton))
			.addComponent(info));

		{
			Object refresh = "this could just be a lambda, but no, it has to be abstracted";
			getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refresh);
			getActionMap().put(refresh, new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					reload();
				}
			});
		}
	}

	@Override
	public void onActivate()
	{
		eventBus.register(this);
		reload();
	}

	@Override
	public void onDeactivate()
	{
		SwingUtil.fastRemoveAll(profilesList);
		eventBus.unregister(this);
	}

	private void reload()
	{
		executor.submit(() ->
		{
			try (ProfileManager.Lock lock = profileManager.lock())
			{
				reload(lock.getProfiles());
			}
		});
	}

	private void reload(List<ConfigProfile> profiles)
	{
		SwingUtilities.invokeLater(() ->
		{
			SwingUtil.fastRemoveAll(profilesList);
			cards.clear();

			long activePanel = configManager.getProfile().getId();
			boolean limited = profiles.stream().filter(v -> !v.isInternal()).count() >= MAX_PROFILES;

			for (ConfigProfile profile : profiles)
			{
				if (profile.isInternal())
				{
					continue;
				}

				ProfileCard pc = new ProfileCard(profile, activePanel == profile.getId(), limited);
				cards.put(profile.getId(), pc);
				profilesList.add(pc);
			}

			addButton.setEnabled(!limited);
			importButton.setEnabled(!limited);

			profilesList.revalidate();
		});
	}

	private class ProfileCard extends JPanel
	{
		private final ConfigProfile profile;
		private final JButton delete;
		private final JTextField name;
		private final JButton activate;
		private final JPanel expand;
		private final JToggleButton rename;

		private boolean expanded;
		private boolean active;

		private ProfileCard(ConfigProfile profile, boolean isActive, boolean limited)
		{
			this.profile = profile;

			setBackground(ColorScheme.DARKER_GRAY_COLOR);

			name = new JTextField();
			name.setText(profile.getName());
			name.setEditable(false);
			name.setEnabled(false);
			name.setOpaque(false);
			name.setBorder(null);
			name.addActionListener(ev -> stopRenaming(true));
			name.addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					{
						stopRenaming(false);
					}
				}
			});

			activate = new JButton(ARROW_RIGHT_ICON);
			activate.setDisabledIcon(ARROW_RIGHT_ICON);
			activate.addActionListener(ev -> switchToProfile(profile.getId()));
			SwingUtil.removeButtonDecorations(activate);

			{
				expand = new JPanel();
				expand.setOpaque(false);
				expand.setLayout(new BorderLayout());

				JPanel btns = new JPanel();
				btns.setOpaque(false);
				btns.setLayout(new DynamicGridLayout(1, 0, 0, 0));
				expand.add(btns, BorderLayout.WEST);

				rename = new JToggleButton(RENAME_ICON);
				rename.setSelectedIcon(RENAME_ACTIVE_ICON);
				rename.setToolTipText("Rename profile");
				SwingUtil.removeButtonDecorations(rename);
				rename.addActionListener(ev ->
				{
					if (rename.isSelected())
					{
						startRenaming();
					}
					else
					{
						stopRenaming(true);
					}
				});
				btns.add(rename);

				JButton clone = new JButton(CLONE_ICON);
				clone.setToolTipText("Duplicate profile");
				SwingUtil.removeButtonDecorations(clone);
				clone.addActionListener(ev -> cloneProfile(profile));
				clone.setEnabled(!limited);
				btns.add(clone);

				JButton export = new JButton(EXPORT_ICON);
				export.setToolTipText("Export profile");
				SwingUtil.removeButtonDecorations(export);
				export.addActionListener(ev ->
				{
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setDialogTitle("Profile export");
					fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("RuneLite properties", "properties"));
					fileChooser.setAcceptAllFileFilterUsed(false);
					fileChooser.setCurrentDirectory(lastFileChooserDirectory);
					fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), profile.getName() + ".properties"));
					int selection = fileChooser.showSaveDialog(this);
					if (selection == JFileChooser.APPROVE_OPTION)
					{
						File file = fileChooser.getSelectedFile();
						lastFileChooserDirectory = file.getParentFile();
						// add properties file extension
						if (!file.getName().endsWith(".properties"))
						{
							file = new File(file.getParentFile(), file.getName() + ".properties");
						}
						exportProfile(profile, file);
					}
				});
				btns.add(export);

				JToggleButton sync = new JToggleButton(SYNC_ICON);
				SwingUtil.removeButtonDecorations(sync);
				sync.setSelectedIcon(SYNC_ACTIVE_ICON);
				sync.setToolTipText("Sync");
				sync.setSelected(profile.isSync());
				sync.addActionListener(ev -> toggleSync(profile, sync.isSelected()));
				btns.add(sync);

				delete = new JButton(DELETE_ICON);
				delete.setToolTipText("Delete profile");
				SwingUtil.removeButtonDecorations(delete);
				delete.addActionListener(ev ->
				{
					int confirm = JOptionPane.showConfirmDialog(ProfileCard.this,
						"Are you sure you want to delete this profile?",
						"Warning", JOptionPane.OK_CANCEL_OPTION);
					if (confirm == 0)
					{
						deleteProfile(profile);
					}
				});
				btns.add(delete);
			}

			{
				GroupLayout layout = new GroupLayout(this);
				this.setLayout(layout);

				layout.setVerticalGroup(layout.createParallelGroup()
					.addGroup(layout.createSequentialGroup()
						.addComponent(name, 24, 24, 24)
						.addComponent(expand))
					.addComponent(activate, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

				layout.setHorizontalGroup(layout.createSequentialGroup()
					.addGap(4)
					.addGroup(layout.createParallelGroup()
						.addComponent(name, GroupLayout.DEFAULT_SIZE, 0x7000, 0x7000)
						.addComponent(expand))
					.addComponent(activate));
			}

			MouseAdapter expandListener = new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent ev)
				{
					if (disabled(ev))
					{
						if (ev.getClickCount() == 2)
						{
							if (!active)
							{
								switchToProfile(profile.getId());
							}
						}
						else
						{
							setExpanded(!expanded);
						}
					}
				}

				@Override
				public void mouseEntered(MouseEvent ev)
				{
					if (disabled(ev))
					{
						setBackground(ColorScheme.DARK_GRAY_COLOR);
					}
				}

				@Override
				public void mouseExited(MouseEvent ev)
				{
					if (disabled(ev))
					{
						setBackground(ColorScheme.DARKER_GRAY_COLOR);
					}
				}

				private boolean disabled(MouseEvent ev)
				{
					Component target = ev.getComponent();
					if (target instanceof JButton)
					{
						return !target.isEnabled();
					}
					if (target instanceof JTextField)
					{
						return !((JTextField) target).isEditable();
					}
					return true;
				}
			};
			addMouseListener(expandListener);
			name.addMouseListener(expandListener);
			activate.addMouseListener(expandListener);

			setActive(isActive);
			setExpanded(false);
		}

		void setActive(boolean active)
		{
			this.active = active;
			setBorder(new MatteBorder(0, 4, 0, 0, active
				? ColorScheme.BRAND_ORANGE
				: ColorScheme.DARKER_GRAY_COLOR));
			delete.setEnabled(!active);
			activate.setEnabled(expanded && !active);
		}

		void setExpanded(boolean expanded)
		{
			this.expanded = expanded;
			expand.setVisible(expanded);
			activate.setEnabled(expanded && !active);
			if (rename.isSelected())
			{
				stopRenaming(true);
			}
			revalidate();
		}

		private void startRenaming()
		{
			name.setEnabled(true);
			name.setEditable(true);
			name.setOpaque(true);
			name.requestFocusInWindow();
			name.selectAll();
		}

		private void stopRenaming(boolean save)
		{
			name.setEditable(false);
			name.setEnabled(false);
			name.setOpaque(false);

			rename.setSelected(false);

			if (save)
			{
				renameProfile(profile.getId(), name.getText().trim());
			}
		}
	}

	private void createProfile()
	{
		try (ProfileManager.Lock lock = profileManager.lock())
		{
			String name = "New Profile";
			int number = 1;
			while (lock.findProfile(name) != null)
			{
				name = "New Profile (" + (number++) + ")";
			}

			log.debug("selected new profile name: {}", name);
			lock.createProfile(name);

			reload(lock.getProfiles());
		}
	}

	private void deleteProfile(ConfigProfile profile)
	{
		// disabling sync causes the profile to be deleted
		configManager.toggleSync(profile, false);

		try (ProfileManager.Lock lock = profileManager.lock())
		{
			lock.removeProfile(profile.getId());

			reload(lock.getProfiles());
		}
	}

	private void renameProfile(long id, String name)
	{
		try (ProfileManager.Lock lock = profileManager.lock())
		{
			ConfigProfile profile = lock.findProfile(id);
			if (profile == null)
			{
				log.warn("rename for nonexistent profile {}", id);
				// maybe profile was removed by another client, reload the panel
				reload(lock.getProfiles());
				return;
			}

			log.debug("renaming profile {} to {}", profile, name);

			lock.renameProfile(profile, name);
			// the panel updates the name label so it isn't necessary to rebuild
			configManager.renameProfile(profile, name);
		}
	}

	private void switchToProfile(long id)
	{
		ConfigProfile profile;
		try (ProfileManager.Lock lock = profileManager.lock())
		{
			profile = lock.findProfile(id);
			if (profile == null)
			{
				log.warn("change to nonexistent profile {}", id);
				// maybe profile was removed by another client, reload the panel
				reload(lock.getProfiles());
				return;
			}

			log.debug("Switching profile to {}", profile.getName());

			// change active profile
			lock.getProfiles().forEach(p -> p.setActive(false));
			profile.setActive(true);
			lock.dirty();
		}

		configManager.switchProfile(profile);
	}

	@Subscribe
	private void onProfileChanged(ProfileChanged ev)
	{
		SwingUtilities.invokeLater(() ->
		{
			for (ProfileCard card : cards.values())
			{
				card.setActive(false);
			}

			ProfileCard card = cards.get(configManager.getProfile().getId());
			if (card == null)
			{
				reload();
				return;
			}

			card.setActive(true);
		});
	}

	private void exportProfile(ConfigProfile profile, File file)
	{
		log.debug("Exporting profile {} to {}", profile.getName(), file);
		executor.execute(() ->
		{
			// save config to disk so the export copies the full config
			configManager.sendConfig();

			File source = ProfileManager.profileConfigFile(profile);
			if (!source.exists())
			{
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Profile '" + profile.getName() + "' can not be exported because it has no settings."));
				return;
			}

			try
			{
				Files.copy(
					source.toPath(),
					file.toPath(),
					StandardCopyOption.REPLACE_EXISTING
				);
			}
			catch (IOException e)
			{
				log.error("error performing profile export", e);
			}
		});
	}

	private void importProfile(File file)
	{
		log.debug("Importing profile from {}", file);

		try (ProfileManager.Lock lock = profileManager.lock())
		{
			String name = "Imported Profile";
			int number = 1;
			while (lock.findProfile(name) != null)
			{
				name = "Imported Profile (" + number++ + ")";
			}

			log.debug("selected new profile name: {}", name);
			ConfigProfile profile = lock.createProfile(name);

			reload(lock.getProfiles());

			// copy the provided properties file
			Files.copy(
				file.toPath(),
				ProfileManager.profileConfigFile(profile).toPath()
			);
		}
		catch (IOException e)
		{
			log.error("error importing profile", e);
		}
	}

	private void cloneProfile(ConfigProfile profile)
	{
		executor.execute(() ->
		{
			// save config to disk so the clone copies the full config
			configManager.sendConfig();

			try (ProfileManager.Lock lock = profileManager.lock())
			{
				int num = 1;
				String name;
				do
				{
					name = profile.getName() + " (" + (num++) + ")";
				} while (lock.findProfile(name) != null);

				log.debug("Cloning profile {} to {}", profile.getName(), name);

				ConfigProfile clonedProfile = lock.createProfile(name);
				reload(lock.getProfiles());

				// copy config if present
				File from = ProfileManager.profileConfigFile(profile);
				File to = ProfileManager.profileConfigFile(clonedProfile);

				if (from.exists())
				{
					try
					{
						Files.copy(
							from.toPath(),
							to.toPath()
						);
					}
					catch (IOException e)
					{
						log.error("error cloning profile", e);
					}
				}
			}
		});
	}

	private void toggleSync(ConfigProfile profile, boolean sync) {
		log.debug("Setting sync for {}: {}", profile.getName(), sync);
		configManager.toggleSync(profile, sync);
	}
}
