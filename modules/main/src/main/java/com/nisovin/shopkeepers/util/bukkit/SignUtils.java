package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Helpers related to signs.
 */
public class SignUtils {

	public static void setBothSidesGlowingText(Sign sign, boolean glowingText) {
		sign.getSide(Side.FRONT).setGlowingText(glowingText);
		sign.getSide(Side.BACK).setGlowingText(glowingText);
	}

	public static void setBothSidesText(Sign sign, @NonNull String[] lines) {
		setLines(sign.getSide(Side.FRONT), lines);
		setLines(sign.getSide(Side.BACK), lines);
	}

	public static void setLines(SignSide signSide, @NonNull String[] lines) {
		signSide.setLine(0, lines[0]);
		signSide.setLine(1, lines[1]);
		signSide.setLine(2, lines[2]);
		signSide.setLine(3, lines[3]);
	}

	private SignUtils() {
	}
}
