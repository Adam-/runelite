package net.runelite.client.plugins.profile;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.plugins.screenmarkers.ScreenMarkerPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

class ProfilePanel extends PluginPanel
{
	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_HOVER_ICON;

	static
	{
		final BufferedImage addIcon = ImageUtil.loadImageResource(ScreenMarkerPlugin.class, "add_icon.png");
		ADD_ICON = new ImageIcon(addIcon);
		ADD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));
	}

	private final ProfilePlugin plugin;

	private final JLabel addMarker = new JLabel(ADD_ICON);

	private final JPanel profileView = new JPanel(new GridBagLayout());

	ProfilePanel(ProfilePlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));

		JLabel title = new JLabel();
		title.setText("Profiles");
		title.setForeground(Color.WHITE);
		title.setVisible(true);

		northPanel.add(title, BorderLayout.WEST);
		northPanel.add(addMarker, BorderLayout.EAST);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		profileView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		setupCreateProfile();

		centerPanel.add(profileView, BorderLayout.CENTER);

		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}

	private void setupCreateProfile()
	{
		addMarker.setToolTipText("Add new profile");
		addMarker.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (mouseEvent.getButton() == MouseEvent.BUTTON1)
				{
					plugin.create();
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				addMarker.setIcon(ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				addMarker.setIcon(ADD_ICON);
			}
		});

		final JPopupMenu menu = new JPopupMenu();


		final JMenuItem createProfile = new JMenuItem("Create new profile");
		createProfile.addActionListener(e -> plugin.create());
		menu.add(createProfile);

		final JMenuItem importProfile = new JMenuItem("Import profile");
		importProfile.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Profile import");
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("RuneLite properties", "properties"));
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setCurrentDirectory(ProfilePlugin.lastFileChooserDirectory);
			int selection = fileChooser.showOpenDialog(this);
			if (selection == JFileChooser.APPROVE_OPTION)
			{
				File file = fileChooser.getSelectedFile();
				ProfilePlugin.lastFileChooserDirectory = file.getParentFile();
				plugin.profileImport(file);
			}
		});
		menu.add(importProfile);

		addMarker.setComponentPopupMenu(menu);
	}

	public void rebuild(List<ConfigProfile> profiles)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		SwingUtil.fastRemoveAll(profileView);

		for (ConfigProfile profile : profiles)
		{
			if (profile.getName().startsWith("$") && !plugin.isDeveloperMode())
			{
				// internal profile
				continue;
			}

			profileView.add(new PPanel(plugin, profile, plugin.active().getId() == profile.getId()), constraints);
			constraints.gridy++;

			profileView.add(Box.createRigidArea(new Dimension(0, 10)), constraints);
			constraints.gridy++;
		}

		revalidate();
	}
}
