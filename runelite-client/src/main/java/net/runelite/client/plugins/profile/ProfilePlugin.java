package net.runelite.client.plugins.profile;

import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
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
@Slf4j
public class ProfilePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	@Inject
	private ProfileManager profileManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	@Named("developerMode")
	@Getter(AccessLevel.PACKAGE)
	private boolean developerMode;

	private ProfilePanel pluginPanel;
	private NavigationButton navigationButton;
	static File lastFileChooserDirectory;

	@Override
	protected void startUp()
	{
		pluginPanel = new ProfilePanel(this);

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

//	@Subscribe
//	public void onProfileChanged(ProfileChanged profileChanged) {
//		scheduledExecutorService.execute(this::load);
//	}

	private void load() {
		List<ConfigProfile> profiles;
		try (ProfileManager.Lock lock = profileManager.lock()) {
			profiles = lock.getProfiles();
		}

		SwingUtilities.invokeLater(() -> pluginPanel.rebuild(profiles));
	}

	ConfigProfile active() {
		return configManager.getProfile();
	}

	void create() {
		try (ProfileManager.Lock lock = profileManager.lock()) {
			List<ConfigProfile> profiles = lock.getProfiles();

			if (profiles.size() > 20) {

			}

			String name = "New Profile";
			int number = 1;
			while (lock.findProfile(name) != null) {
				name = "New Profile (" + number++ + ")";
			}

			log.debug("selected new profile name: {}", name);
			lock.createProfile(name);
		}

		scheduledExecutorService.execute(this::load);
	}

	void delete(long id) {
		try (ProfileManager.Lock lock = profileManager.lock()) {
			ConfigProfile profile = lock.findProfile(id);
			if (profile == null) {
				log.warn("delete for nonexistent profile {}", id);
				return;
			}

			lock.removeProfile(id);
		}

		scheduledExecutorService.execute(this::load);
	}

	void rename(long id, String name) {
		try (ProfileManager.Lock lock = profileManager.lock()) {
			ConfigProfile profile = lock.findProfile(id);
			if (profile == null) {
				log.warn("rename for nonexistent profile {}", id);
				return;
			}

			log.debug("renaming profile {} to {}", profile, name);

			profile.setName(name);
			lock.dirty();

			// the panel updates the name label so it isn't necessary to rebuild
		}
	}

	void change(long id) {
		ConfigProfile profile;
		try (ProfileManager.Lock lock = profileManager.lock())
		{
			profile = lock.findProfile(id);
			if (profile == null)
			{
				log.warn("change to nonexistent profile {}", id);
				// maybe profile was removed by another client, reload the panel
				scheduledExecutorService.execute(this::load);
				return;
			}

			log.debug("Switching profile to {}", profile.getName());

			// change active profile
			lock.getProfiles().forEach(p -> p.setActive(false));
			profile.setActive(true);
			lock.dirty();
		}

		configManager.switchProfile(profile);
		scheduledExecutorService.execute(this::load);
	}

	void export(ConfigProfile profile, File file) {
		log.debug("Exporting profile {} to {}", profile.getName(), file);
		scheduledExecutorService.execute(() ->
		{
			// save config to disk so the export copies the full config
			configManager.sendConfig();

			try
			{
				Files.copy(
					ProfileManager.profileConfigFile(profile).toPath(),
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

	void profileImport(File file) {
		log.debug("Importing profile from {}", file);

		try (ProfileManager.Lock lock = profileManager.lock()) {
			List<ConfigProfile> profiles = lock.getProfiles();

			if (profiles.size() > 20) {

			}

			String name = "Imported Profile";
			int number = 1;
			while (lock.findProfile(name) != null) {
				name = "Imported Profile (" + number++ + ")";
			}

			log.debug("selected new profile name: {}", name);
			ConfigProfile profile = lock.createProfile(name);

			// overwrite new profile properties with the provided file
			Files.copy(
				file.toPath(),
				ProfileManager.profileConfigFile(profile).toPath(),
				StandardCopyOption.REPLACE_EXISTING
			);
		}
		catch (IOException e)
		{
			log.error("error importing profile", e);
		}

		scheduledExecutorService.execute(this::load);
	}
}
