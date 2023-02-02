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

	private final JPanel profileView = new JPanel(new GridBagLayout());
	private final JLabel instructionsLabel;

	ProfilePanel(ProfilePlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel();
		title.setText("Profiles");
		title.setForeground(Color.WHITE);
		title.setVisible(true);

		JLabel addProfile = new JLabel(ADD_ICON);
		addProfile.setToolTipText("Add new profile");
		addProfile.addMouseListener(new MouseAdapter()
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
				addProfile.setIcon(ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				addProfile.setIcon(ADD_ICON);
			}
		});

		JPopupMenu menu = setupCreateProfile();
		addProfile.setComponentPopupMenu(menu);

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(1, 0, 10, 0));
		titlePanel.add(title, BorderLayout.WEST);
		titlePanel.add(addProfile, BorderLayout.EAST);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		centerPanel.add(profileView, BorderLayout.CENTER);

		instructionsLabel = new JLabel("foo");
//		Font boldFont = new Font(instructionsLabel.getFont().getFontName(), Font.BOLD, instructionsLabel.getFont().getSize());
//		instructionsLabel.setFont(boldFont);
		instructionsLabel.setBorder(new EmptyBorder(10,0,10,0));
		centerPanel.add(instructionsLabel, BorderLayout.NORTH);

		add(titlePanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}

	private JPopupMenu setupCreateProfile()
	{
		final JPopupMenu menu = new JPopupMenu();

		final JMenuItem createProfile = new JMenuItem("Create new profile");
		createProfile.addActionListener(e -> plugin.create());
		menu.add(createProfile);

		final JMenuItem importProfile = new JMenuItem("Import profile");
		importProfile.addActionListener(e ->
		{
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
		return menu;
	}

	public void rebuild(List<ConfigProfile> profiles)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		SwingUtil.fastRemoveAll(profileView);

		int num = 0;
		for (ConfigProfile profile : profiles)
		{
			if (profile.getName().startsWith("$") && !plugin.isDeveloperMode())
			{
				// internal profile
				continue;
			}

			profileView.add(new ProfileCard(plugin, profile, plugin.active().getId() == profile.getId()), constraints);
			constraints.gridy++;

			profileView.add(Box.createRigidArea(new Dimension(0, 10)), constraints);
			constraints.gridy++;

			++num;
		}

		if (num <= 3)
		{
			StringBuilder builder = new StringBuilder()
				.append("<html>")
				.append("Profiles are separate sets of plugins and settings that you can switch between at any time.<br/>")
				.append("<br/>");
			if (num <= 1)
			{
				builder.append("You have just one profile, which is the currently active profile. Add more by clicking the green" +
					" plus icon, or right-click and import a profile");
			}
			else
			{
				builder.append("Switch between profiles by double clicking them, and right click them to view more options.");
			}
			builder.append("</html>");

			instructionsLabel.setText(builder.toString());
			instructionsLabel.setVisible(true);
		}
		else
		{
			instructionsLabel.setVisible(false);
		}

		revalidate();
	}
}
