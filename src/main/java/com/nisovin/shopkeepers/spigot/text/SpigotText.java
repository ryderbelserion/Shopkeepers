package com.nisovin.shopkeepers.spigot.text;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.spigot.SpigotFeatures;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

public class SpigotText {

	public static void sendMessage(CommandSender recipient, String message, Map<String, Text> textArgs) {
		sendMessage(recipient, message, (textArgs == null) ? null : textArgs.entrySet());
	}

	public static void sendMessage(CommandSender recipient, String message, Iterable<Map.Entry<String, Text>> textArgs) {
		if (recipient == null || message == null) return;
		if (!SpigotFeatures.isSpigotAvailable()) {
			// send message with only basic text replacement:
			Iterable<Map.Entry<String, String>> stringArgs = null;
			if (textArgs != null) {
				// TODO improve this
				Stream<Map.Entry<String, String>> stringArgsStream = Utils.stream(textArgs)
						.filter(e -> e != null && e.getKey() != null && e.getValue() != null)
						.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getText()));
				stringArgs = stringArgsStream::iterator;
			}
			TextUtils.sendMessage(recipient, message, stringArgs);
		} else {
			// send message with additional text features:
			Internal.sendMessage(recipient, message, textArgs);
		}
	}

	// Separate class that gets only accessed if Spigot is present. Avoids class loading issues.
	private static class Internal {

		private static void sendMessage(CommandSender recipient, String message, Iterable<Map.Entry<String, Text>> textArgs) {
			assert recipient != null && message != null;
			// Note: We currently avoid TextComponent.fromLegacyText since it is relatively costly (compared to using a
			// plain TextComponent) and legacy color codes inside regular TextComponents work fine as well for our
			// purposes.
			BaseComponent text = new TextComponent(message); // uses legacy color codes
			if (textArgs != null) {
				for (Entry<String, Text> argEntry : textArgs) {
					String key = argEntry.getKey();
					Text textArg = argEntry.getValue();
					if (key == null || textArg == null) continue; // skip invalid entries
					Internal.replaceTextArgument(text, key, textArg);
				}
			}
			recipient.spigot().sendMessage(text);
		}

		// text replacement and insertion of hover/click events and translation keys are done by modifying the
		// component's text and inserting new extra components if required
		// returns true if a match for the given key has been found
		private static boolean replaceTextArgument(BaseComponent component, String key, Text textArg) {
			assert component != null && key != null && textArg != null;
			if (component instanceof TextComponent) {
				TextComponent textComponent = (TextComponent) component;
				String text = textComponent.getText();

				int keyIndex = text.indexOf(key);
				if (keyIndex >= 0) {
					// split text component in order to insert hover/click event and/or translation key:
					String prefix = text.substring(0, keyIndex);
					String suffix = text.substring(keyIndex + key.length(), text.length());

					List<BaseComponent> newExtra = new ArrayList<>();

					String translationKey = textArg.getTranslationKey(); // can be null
					Object[] translationArgs = textArg.getTranslationArgs(); // can be null
					String insertion = textArg.getInsertion(); // can be null
					HoverEvent hoverEvent = textArg.getHoverEvent(); // can be null
					ClickEvent clickEvent = textArg.getClickEvent(); // can be null

					BaseComponent textArgComponent;
					if (!prefix.isEmpty()) {
						textComponent.setText(prefix);

						// insert new component via extra:
						if (translationKey != null) {
							textArgComponent = new TranslatableComponent(translationKey, translationArgs);
						} else {
							textArgComponent = new TextComponent(textArg.getText()); // uses legacy color codes
						}
						newExtra.add(textArgComponent);
					} else {
						if (translationKey != null) {
							textComponent.setText(""); // clear original text
							// insert new translatable component:
							textArgComponent = new TranslatableComponent(translationKey, translationArgs);
							newExtra.add(textArgComponent);
						} else {
							// use original text component:
							textComponent.setText(textArg.getText()); // uses legacy color codes
							textArgComponent = textComponent;
						}
					}
					assert textArgComponent != null;

					// setup text actions:
					textArgComponent.setInsertion(insertion);
					setHoverEvent(textArgComponent, hoverEvent);
					setClickEvent(textArgComponent, clickEvent);
					newExtra.add(textArgComponent);

					if (!suffix.isEmpty()) {
						// insert suffix via extra component:
						newExtra.add(new TextComponent(suffix));
					}

					// add old extra:
					List<BaseComponent> oldExtra = component.getExtra();
					if (oldExtra != null) {
						newExtra.addAll(oldExtra);
					}

					// apply new extra:
					if (!newExtra.isEmpty()) {
						component.setExtra(newExtra);
					}
					return true;
				} // else: continue checking the extra for key matches
			}

			// check each extra component for key matches:
			List<BaseComponent> extra = component.getExtra();
			if (extra != null) {
				for (BaseComponent extraComponent : extra) {
					boolean match = replaceTextArgument(extraComponent, key, textArg);
					if (match) {
						// we can abort after the first match for the key:
						return true;
					}
				}
			}
			return false; // no match has been found for the key
		}

		// HOVER EVENT

		private static net.md_5.bungee.api.chat.HoverEvent toSpigot(HoverEvent hoverEvent) {
			if (hoverEvent == null) return null;
			net.md_5.bungee.api.chat.HoverEvent.Action action = toSpigot(hoverEvent.getAction());
			// uses legacy color codes:
			BaseComponent[] value = new BaseComponent[] { new TextComponent(hoverEvent.getValue()) };
			return new net.md_5.bungee.api.chat.HoverEvent(action, value);
		}

		private static net.md_5.bungee.api.chat.HoverEvent.Action toSpigot(HoverEvent.Action hoverEventAction) {
			if (hoverEventAction == null) return null;
			switch (hoverEventAction) {
			case SHOW_TEXT:
				return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;
			case SHOW_ITEM:
				return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM;
			case SHOW_ENTITY:
				return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ENTITY;
			case SHOW_ACHIEVEMENT:
				return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ACHIEVEMENT;
			default:
				throw new IllegalStateException("Unexpected hover event action: " + hoverEventAction);
			}
		}

		private static void setHoverEvent(BaseComponent component, HoverEvent hoverEvent) {
			assert component != null;
			if (hoverEvent == null) return; // no hover event
			net.md_5.bungee.api.chat.HoverEvent spigotHoverEvent = toSpigot(hoverEvent);
			component.setHoverEvent(spigotHoverEvent);
		}

		// CLICK EVENT

		private static net.md_5.bungee.api.chat.ClickEvent toSpigot(ClickEvent clickEvent) {
			if (clickEvent == null) return null;
			net.md_5.bungee.api.chat.ClickEvent.Action action = toSpigot(clickEvent.getAction());
			return new net.md_5.bungee.api.chat.ClickEvent(action, clickEvent.getValue());
		}

		private static net.md_5.bungee.api.chat.ClickEvent.Action toSpigot(ClickEvent.Action clickEventAction) {
			if (clickEventAction == null) return null;
			switch (clickEventAction) {
			case OPEN_URL:
				return net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL;
			case OPEN_FILE:
				return net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_FILE;
			case RUN_COMMAND:
				return net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND;
			case SUGGEST_COMMAND:
				return net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND;
			case CHANGE_PAGE:
				return net.md_5.bungee.api.chat.ClickEvent.Action.CHANGE_PAGE;
			default:
				throw new IllegalStateException("Unexpected click event action: " + clickEventAction);
			}
		}

		private static void setClickEvent(BaseComponent component, ClickEvent clickEvent) {
			assert component != null;
			if (clickEvent == null) return; // no click event
			net.md_5.bungee.api.chat.ClickEvent spigotClickEvent = toSpigot(clickEvent);
			component.setClickEvent(spigotClickEvent);
		}

		private Internal() {
		}
	}

	private SpigotText() {
	}
}
