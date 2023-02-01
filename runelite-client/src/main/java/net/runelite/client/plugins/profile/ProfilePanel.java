package net.runelite.client.plugins.profile;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.plugins.screenmarkers.ScreenMarkerPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

class ProfilePanel extends PluginPanel
{
	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_HOVER_ICON;

	private static final Color DEFAULT_BORDER_COLOR = Color.GREEN;
	private static final Color DEFAULT_FILL_COLOR = new Color(0, 255, 0, 0);

	private static final int DEFAULT_BORDER_THICKNESS = 3;

	static
	{
		final BufferedImage addIcon = ImageUtil.loadImageResource(ScreenMarkerPlugin.class, "add_icon.png");
		ADD_ICON = new ImageIcon(addIcon);
		ADD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));
	}

	private final ProfilePlugin plugin;

	private final JLabel title = new JLabel();
	private final JLabel addMarker = new JLabel(ADD_ICON);

	private final JPanel profileView = new JPanel(new GridBagLayout());
	private final PluginErrorPanel noMarkersPanel = new PluginErrorPanel();

	ProfilePanel(ProfilePlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));

		title.setText("Profiles");
		title.setForeground(Color.WHITE);

		northPanel.add(title, BorderLayout.WEST);
		northPanel.add(addMarker, BorderLayout.EAST);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		profileView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		noMarkersPanel.setContent("Screen Markers", "Highlight a region on your screen.");
		noMarkersPanel.setVisible(false);

		profileView.add(noMarkersPanel, constraints);
		constraints.gridy++;

//		creationPanel = new ScreenMarkerCreationPanel(plugin);
//		creationPanel.setVisible(false);
//
//		markerView.add(creationPanel, constraints);
		constraints.gridy++;

		setupCreateProfile();

		centerPanel.add(profileView, BorderLayout.CENTER);

		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}

	private void setupCreateProfile() {
		addMarker.setToolTipText("Add new profile");
		addMarker.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				create();
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
		createProfile.addActionListener(e -> {
create();
		});
		menu.add(createProfile);

		final JMenuItem importProfile = new JMenuItem("Import profile");
		importProfile.addActionListener(e -> {

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
			profileView.add(new PPanel(plugin, profile), constraints);
			constraints.gridy++;

			profileView.add(Box.createRigidArea(new Dimension(0, 10)), constraints);
			constraints.gridy++;
		}

		boolean empty = constraints.gridy == 0;
		noMarkersPanel.setVisible(empty);
		title.setVisible(!empty);

		profileView.add(noMarkersPanel, constraints);
		constraints.gridy++;

		//markerView.add(creationPanel, constraints);
		//constraints.gridy++;

//		repaint();
		revalidate();
//		invalidate();
	}

	private void create() {
		plugin.create();
//		if (profileView.isShowing())
	}
}
