package com.nisovin.shopkeepers.ui.confirmations;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface ConfirmationUIConfig {

	public String getTitle();

	public @Nullable List<? extends String> getConfirmationLore();
}
