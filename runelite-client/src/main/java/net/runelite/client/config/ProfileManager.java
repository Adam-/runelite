package net.runelite.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Slf4j
public class ProfileManager
{
	private static final File PROFILES_DIR = new File(RuneLite.RUNELITE_DIR, "profiles2");
	private static final File PROFILES = new File(PROFILES_DIR, "profiles.json");

	private final Gson gson;

	static
	{
		PROFILES_DIR.mkdirs();
	}

	public class Lock implements AutoCloseable
	{
		private final File lockFile;
		private final FileOutputStream lockOut;
		private final FileChannel lockChannel;
		private final List<ConfigProfile> profiles;
		private boolean modified = false;

		@SneakyThrows
		public Lock()
		{
			lockFile = new File(PROFILES_DIR, "profiles.lck");
			lockOut = new FileOutputStream(lockFile);
			lockChannel = lockOut.getChannel();
			lockChannel.lock();
			profiles = new ArrayList<>(load());
		}

		private List<ConfigProfile> load()
		{
			try (FileInputStream in = new FileInputStream(PROFILES))
			{
				return gson.fromJson(new InputStreamReader(in), Profiles.class)
					.getProfiles();
			}
			catch (FileNotFoundException ex)
			{
				return Collections.emptyList();
			}
			catch (IOException | JsonSyntaxException e)
			{
				log.warn("unable to read profiles", e);
				return Collections.emptyList();
			}
		}

		@Override
		@SneakyThrows
		public void close()
		{
			if (modified)
			{
				log.debug("saving {} profiles", profiles.size());

				File tempFile = File.createTempFile("runelite_profiles", null, PROFILES.getParentFile());
				try (FileOutputStream out = new FileOutputStream(tempFile);
					 FileChannel channel = lockOut.getChannel();
					 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
				{
					Profiles profilesData = new Profiles();
					profilesData.setProfiles(profiles);
					gson.toJson(profilesData, writer);
					channel.force(true);
				}

				try
				{
					Files.move(tempFile.toPath(), PROFILES.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				}
				catch (AtomicMoveNotSupportedException ex)
				{
					log.debug("atomic move not supported", ex);
					Files.move(tempFile.toPath(), PROFILES.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}

			lockOut.close();
			lockFile.delete();
		}

		public List<ConfigProfile> getProfiles()
		{
			return profiles;
		}

		public ConfigProfile createProfile(String name, long id)
		{
			ConfigProfile profile = new ConfigProfile(id);
			profile.setName(name);
			profile.setSync(false);
			profile.setRev(-1);
			profiles.add(profile);
			modified = true;
			log.debug("Created profile {}", profile);
			return profile;
		}

		public ConfigProfile createProfile(String name)
		{
			return createProfile(name, System.nanoTime());
		}

		public ConfigProfile findProfile(String name)
		{
			for (ConfigProfile configProfile : profiles)
			{
				if (configProfile.getName().equals(name))
				{
					return configProfile;
				}
			}
			return null;
		}

		public ConfigProfile findProfile(long id)
		{
			for (ConfigProfile configProfile : profiles)
			{
				if (configProfile.getId() == id)
				{
					return configProfile;
				}
			}
			return null;
		}

		public void removeProfile(long id)
		{
			// keep the properties around on disk as a backup. If this profile is active on another client
			// the profile will be recreated there later with the same id.
			modified |= profiles.removeIf(p -> p.getId() == id);
		}

		public void renameProfile(ConfigProfile profile, String name)
		{
			File oldFile = profileConfigFile(profile);
			profile.setName(name);
			modified = true;
			File newFile = profileConfigFile(profile);

			if (!oldFile.exists())
			{
				// no config file is valid if the profile hasn't been used yet.
				return;
			}

			try
			{
				Files.move(
					oldFile.toPath(),
					newFile.toPath()
				);
			}
			catch (IOException e)
			{
				log.error("error renaming profile", e);
			}
		}

		public void dirty()
		{
			modified = true;
		}
	}

	public Lock lock()
	{
		return new Lock();
	}

	public static File profileConfigFile(ConfigProfile profile)
	{
		return new File(PROFILES_DIR, profile.getName() + "-" + profile.getId() + ".properties");
	}
}
