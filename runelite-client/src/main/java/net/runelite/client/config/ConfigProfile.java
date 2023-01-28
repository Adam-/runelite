package net.runelite.client.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@ToString
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
	@Getter
	@Setter
	private boolean defaultProfile;
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private long rev;
}
