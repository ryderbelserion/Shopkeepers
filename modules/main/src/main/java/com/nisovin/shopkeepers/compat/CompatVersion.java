package com.nisovin.shopkeepers.compat;

import com.nisovin.shopkeepers.util.java.Validate;

public class CompatVersion {

	private final String compatVersion;
	private final String minecraftVersion;
	private final String mappingsVersion;

	public CompatVersion(String compatVersion, String minecraftVersion, String mappingsVersion) {
		Validate.notEmpty(compatVersion);
		Validate.notEmpty(minecraftVersion);
		Validate.notEmpty(mappingsVersion);
		this.compatVersion = compatVersion;
		this.minecraftVersion = minecraftVersion;
		this.mappingsVersion = mappingsVersion;
	}

	/**
	 * Gets the Shopkeepers compatibility version.
	 * 
	 * @return the Shopkeepers compatibility version
	 */
	public String getCompatVersion() {
		return compatVersion;
	}

	/**
	 * Gets the Minecraft server version.
	 * 
	 * @return the Minecraft server version
	 */
	public String getMinecraftVersion() {
		return minecraftVersion;
	}

	/**
	 * Gets the server mappings version.
	 * 
	 * @return the server mappings version
	 */
	public String getMappingsVersion() {
		return mappingsVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + compatVersion.hashCode();
		result = prime * result + minecraftVersion.hashCode();
		result = prime * result + mappingsVersion.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof CompatVersion)) return false;
		CompatVersion other = (CompatVersion) obj;
		if (!compatVersion.equals(other.compatVersion)) return false;
		if (!mappingsVersion.equals(other.mappingsVersion)) return false;
		if (!minecraftVersion.equals(other.minecraftVersion)) return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompatVersion [compatVersion=");
		builder.append(compatVersion);
		builder.append(", minecraftVersion=");
		builder.append(minecraftVersion);
		builder.append(", mappingsVersion=");
		builder.append(mappingsVersion);
		builder.append("]");
		return builder.toString();
	}
}
