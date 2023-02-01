package net.runelite.client.plugins.profile;

import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.config.ProfileManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Profiles",
	description = "Configuration profile management",
	loadWhenOutdated = true
)
public class ProfilePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	@Inject
	private ProfileManager profileManager;

	private ProfilePanel pluginPanel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		pluginPanel = new ProfilePanel();

		final BufferedImage icon = new BufferedImage(12,12,TYPE_INT_RGB);// ImageUtil.loadImageResource(getClass(), ICON_FILE);

		navigationButton = NavigationButton.builder()
			.tooltip("Profiles")
			.icon(icon)
			.priority(5)
			.panel(pluginPanel)
			.build();

		clientToolbar.addNavigation(navigationButton);

		scheduledExecutorService.execute(this::load);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		pluginPanel = null;
	}

	private void load() {
		List<ConfigProfile> profiles;
		try (ProfileManager.Lock lock = profileManager.lock()) {
			profiles = lock.getProfiles();
		}

		SwingUtilities.invokeLater(() -> pluginPanel.rebuild(profiles));
	}
}
