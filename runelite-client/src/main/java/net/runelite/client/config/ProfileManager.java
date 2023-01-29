package net.runelite.client.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
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

//	public List<ConfigProfile> listProfiles()
//	{
//		try (FileInputStream in = new FileInputStream(PROFILES);
//			 FileChannel channel = in.getChannel()
//		)
//		{
//			channel.lock(0L, Long.MAX_VALUE, true);///hmm what about this lock?
//			return gson.fromJson(new InputStreamReader(in),
//				new TypeToken<List<ConfigProfile>>()
//				{
//				}.getType());
//		}
//		catch (FileNotFoundException ex)
//		{
//			return Collections.emptyList();
//		}
//		catch (IOException e)
//		{
//			log.warn("unable to read profiles", e);
//			return Collections.emptyList();
//		}
//	}

	public class Lock implements AutoCloseable {
		private final File lockFile;
		private final FileOutputStream lockOut;
		private final FileChannel lockChannel;
		private final List<ConfigProfile> profiles;
		private boolean modified = false;

		@SneakyThrows
		public Lock() //throws IOException
		{
			lockFile = new File(PROFILES_DIR, "profiles.lck");
				lockOut = new FileOutputStream(lockFile);
			lockChannel = lockOut.getChannel();
			lockChannel.lock();
			profiles = load();
		}

		private List<ConfigProfile> load() {
			try (FileInputStream in = new FileInputStream(PROFILES)
//				 FileChannel channel = in.getChannel()
			)
			{
//				channel.lock(0L, Long.MAX_VALUE, true);
				return gson.fromJson(new InputStreamReader(in),
					new TypeToken<List<ConfigProfile>>()
					{
					}.getType());
			}
			catch (FileNotFoundException ex)
			{
				return Collections.emptyList();
			}
			catch (IOException e)
			{
				log.warn("unable to read profiles", e);
				return Collections.emptyList();
			}
		}

		@Override
		@SneakyThrows
		public void close() //throws IOException
		{
			if (modified) {
				File tempFile = File.createTempFile("runelite_profiles", null, PROFILES.getParentFile());
				try (FileOutputStream out = new FileOutputStream(tempFile);
					 FileChannel channel = lockOut.getChannel();
					 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
				{
					gson.toJson(profiles, writer);
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
//			return Collections.unmodifiableList(profiles);
		}

		public void addProfile(ConfigProfile profile) {
			profiles.add(profile);
			modified=true;
		}

		public ConfigProfile createProfile(String name) {
			if (findProfile(name) != null) {
				// profile names are used for the properties on disk, so they have to be unique
				throw new IllegalArgumentException("profile " + name + " already exists");
			}

			ConfigProfile profile = new ConfigProfile(System.nanoTime());
			profile.setName(name);
			profile.setSync(false);
			profiles.add(profile);
			modified=true;
			log.debug("Created profile {}", profile);
			return profile;
		}

		public ConfigProfile findProfile(String name) {
			for (ConfigProfile configProfile : profiles) {
				if (configProfile.getName().equals(name)) {
					return configProfile;
				}
			}
			return null;
		}

		public ConfigProfile findProfile(long id) {
			for (ConfigProfile configProfile : profiles) {
				if (configProfile.getId() == id) {
					return configProfile;
				}
			}
			return null;
		}
	}

	public Lock lock() //throws IOException
	{
		return new Lock();
	}

//	public ConfigProfile createProfile(String name)
//	{
//		ConfigProfile profile = new ConfigProfile(System.nanoTime());
//		profile.setName(name);
//		profile.setSync(false);
//		loadEditSave(c -> c.add(profile));
//		log.debug("Created profile {}", profile);
//		return profile;
//	}

//	public ConfigProfile findOrCreateProfile(String name) {
//		AtomicReference<ConfigProfile> ref = new AtomicReference<>();
//		loadEditSave(profiles -> {
//			for (ConfigProfile profile : profiles) {
//				if (profile.getName().equals(name)) {
//					ref.set(profile);
//					return;
//				}
//			}
//
//			ConfigProfile profile = new ConfigProfile(System.nanoTime());
//			profile.setName(name);
//			profile.setSync(false);
//			ref.set(profile);
//			profiles.add(profile);
//			log.debug("Created profile {}", profile);
//		});
//		return ref.get();
//	}

//	public void updateDefault(String name) {
//		loadEditSave(profiles -> {
//			for (ConfigProfile p : profiles) {
//				p.setDefaultProfile(p.getName().equals(name));
//			}
//			log.debug("Default profile changed to: {}", name);
//		});
//	}

//	public ConfigProfile updateProfile(ConfigProfile configProfile, Consumer<ConfigProfile> consumer) {
//		AtomicReference<ConfigProfile> ref = new AtomicReference<>();
//		loadEditSave(profiles -> {
//			for (ConfigProfile p : profiles) {
//				if (p.getId() == configProfile.getId()) {
//					consumer.accept(p);
//					ref.set(configProfile);
//					return;
//				}
//			}
//
//			log.debug("updating profile which doesn't exist! {}", configProfile);
//
//			// updating a profile which doesn't exist?
//			consumer.accept(configProfile);
//			profiles.add(configProfile);
//			ref.set(configProfile);
//		});
//		return ref.get();
//	}
//
//	public void removeProfile(ConfigProfile profile)
//	{
//		loadEditSave(c ->
//		{
//			if (c.removeIf(p -> p.getId() == profile.getId()))
//			{
//				log.debug("Removed profile {}", profile);
//			}
//		});
//	}

//	public void clone(ConfigProfile from, ConfigProfile to) throws IOException
//	{
//		// neither are internal profiles
//		assert !from.getName().startsWith("$");
//		assert !to.getName().startsWith("$");
//
//		// this doesn't work if either from or to are active profiles -
//		// either from might have pending changes or to will have an
//		// associated ConfigData that isn't reloaded
//
//		File fromFile = profileConfigFile(from);
//		File toFile = profileConfigFile(to);
//
//		log.debug("Cloning profile {} -> {}", from, to);
//
////		// backup target properties
////		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
////		File backupFile = new File(toFile.getParentFile(), toFile.getName() + "." + dateFormat.format(new Date()));
////		try
////		{
////			Files.copy(toFile.toPath(), backupFile.toPath());
////		}
////		catch (IOException ex)
////		{
////			throw new IOException("backup failed", ex);
////		}
//
//		// copy source properties to target
//		Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//	}

//	public void loadEditSave(Consumer<List<ConfigProfile>> c)
//	{
//		File lckFile = new File(PROFILES_DIR, "profiles.lck");
//		try (FileOutputStream lockOut = new FileOutputStream(lckFile);
//			 FileChannel lckChannel = lockOut.getChannel())
//		{
//			lckChannel.lock();
//
//			List<ConfigProfile> profiles;
//			try (FileInputStream in = new FileInputStream(PROFILES))
//			{
//				profiles = gson.fromJson(new InputStreamReader(in),
//					new TypeToken<List<ConfigProfile>>()
//					{
//					}.getType());
//			}
//			catch (FileNotFoundException ex)
//			{
//				profiles = new ArrayList<>();
//			}
//			catch (IOException e)
//			{
//				log.warn("unable to read profiles", e);
//				profiles = new ArrayList<>();
//			}
//
//			c.accept(profiles);
//
//			File tempFile = File.createTempFile("runelite_profiles", null, PROFILES.getParentFile());
//			try (FileOutputStream out = new FileOutputStream(tempFile);
//				 FileChannel channel = lockOut.getChannel();
//				 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
//			{
//				gson.toJson(profiles, writer);
//				channel.force(true);
//			}
//
//			try
//			{
//				Files.move(tempFile.toPath(), PROFILES.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
//			}
//			catch (AtomicMoveNotSupportedException ex)
//			{
//				log.debug("atomic move not supported", ex);
//				Files.move(tempFile.toPath(), PROFILES.toPath(), StandardCopyOption.REPLACE_EXISTING);
//			}
//		}
//		catch (IOException ex)
//		{
//			log.error("unable to write profiles", ex);
//		}
//		lckFile.delete();
//	}

	public static File profileConfigFile(ConfigProfile profile)
	{
		return new File(PROFILES_DIR, profile.getName() + ".properties");
	}
}
