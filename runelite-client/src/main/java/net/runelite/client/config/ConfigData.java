package net.runelite.client.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ConfigData
{
	private final File configPath;

	private final ConcurrentHashMap<String, String> properties;
	private Map<String, String> patchChanges = new HashMap<>();

	ConfigData(File configPath)
	{
		this.configPath = configPath;

		Properties props = new Properties();
		try (FileInputStream fin = new FileInputStream(configPath))
		{
			props.load(fin);
		}
		catch (FileNotFoundException ignored)
		{
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}

		properties = new ConcurrentHashMap<>(props.size());
		props.forEach((k, v) -> properties.put((String) k, (String) v));
	}

	String getProperty(String key)
	{
		return properties.get(key);
	}

	synchronized String setProperty(String key, String value)
	{
		patchChanges.put(key, value);
		return properties.put(key, value);
	}

	synchronized String unset(String key)
	{
		patchChanges.put(key, null);
		return properties.remove(key);
	}

	synchronized void putAll(Map<String, String> values)
	{
		patchChanges.putAll(values);
		properties.putAll(values);
	}

	Set<String> keySet()
	{
		return properties.keySet();
	}

	Map<String, String> get()
	{
		return Collections.unmodifiableMap(properties);
	}

	synchronized Map<String, String> swapChanges()
	{
		if (patchChanges.isEmpty())
		{
			return Collections.emptyMap();
		}

		Map<String, String> p = patchChanges;
		patchChanges = new HashMap<>();
		return p;
	}

	void patch(Map<String, String> patch)
	{
		// load + patch + store instead of just flushing the in-memory properties to disk so that
		// multiple clients editing one config data (such as rs profile config) get their data merged
		// correctly

		File lckFile = new File(configPath.getParentFile(), configPath.getName() + ".lck");
		try (FileOutputStream lockOut = new FileOutputStream(lckFile);
			 FileChannel lckChannel = lockOut.getChannel())
		{
			lckChannel.lock();

			Properties tempProps = new Properties();
			try (FileInputStream in = new FileInputStream(configPath))
			{
				tempProps.load(in);
			}
			catch (FileNotFoundException e)
			{
				log.debug("config file {} does not exist", configPath);
			}

			if (tempProps.isEmpty())
			{
				// this probably doesn't happen outside of the very first save (when no file exists)
				// but to be safe in the event the prop is deleted off disk, flush the entire properties
				// from memory
				tempProps.putAll(properties);
			}
			else
			{
				// apply patches
				for (Map.Entry<String, String> entry : patch.entrySet())
				{
					if (entry.getValue() == null)
					{
						tempProps.remove(entry.getKey());
					}
					else
					{
						tempProps.put(entry.getKey(), entry.getValue());
					}
				}
			}

			File tempFile = File.createTempFile("runelite_config", null, configPath.getParentFile());
			try (FileOutputStream out = new FileOutputStream(tempFile);
				 FileChannel channel = out.getChannel();
				 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
			{
				tempProps.store(writer, "RuneLite configuration");
				channel.force(true);
			}

			try
			{
				Files.move(tempFile.toPath(), configPath.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			catch (AtomicMoveNotSupportedException ex)
			{
				log.debug("atomic move not supported", ex);
				Files.move(tempFile.toPath(), configPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex)
		{
			log.error("unable to save configuration file", ex);
		}
		lckFile.delete();
	}
}
