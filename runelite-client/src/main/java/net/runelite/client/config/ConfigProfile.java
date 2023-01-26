package net.runelite.client.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ConfigProfile
{
	@Getter
	private final long id;
	@Getter
	@Setter
	private String name;
	@Getter
	@Setter
	private boolean sync;
}
