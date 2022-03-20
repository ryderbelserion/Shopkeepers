package com.nisovin.shopkeepers.util.bukkit;

public final class ColorUtils {

	/**
	 * Converts the given HSB color to RGB.
	 * <p>
	 * The HSB color components are expected to be values in the range between {@code 0.0} and
	 * {@code 1.0}.
	 * <p>
	 * The returned integer value encodes the RGB color components in the lowest 24 bits, with the
	 * remaining bits being {@code 0} (as used by {@link org.bukkit.Color#fromRGB(int)}.
	 * 
	 * @param hue
	 *            the hue
	 * @param saturation
	 *            the saturation
	 * @param brightness
	 *            the brightness
	 * @return the RGB value
	 */
	public static int HSBtoRGB(float hue, float saturation, float brightness) {
		// We simply delegate to the built-in AWT implementation:
		int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
		// AWT returns a RGB value with ones in the highest order bits, so we need to remove those:
		return rgb & 0x00FFFFFF;
	}

	private ColorUtils() {
	}
}
