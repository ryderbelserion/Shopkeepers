package com.nisovin.shopkeepers.spigot.text;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.spigot.SpigotFeatures;
import com.nisovin.shopkeepers.text.ClickEventText;
import com.nisovin.shopkeepers.text.FormattingText;
import com.nisovin.shopkeepers.text.HoverEventText;
import com.nisovin.shopkeepers.text.InsertionText;
import com.nisovin.shopkeepers.text.PlaceholderText;
import com.nisovin.shopkeepers.text.PlainText;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.text.TranslatableText;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

public final class SpigotText {

	// Note: This is not intended to be called directly, but only via TextUtils.
	public static void sendMessage(CommandSender recipient, Text message) {
		Validate.notNull(recipient, "recipient is null");
		Validate.notNull(message, "message is null");
		// Skip sending if the (plain) message is empty: Allows disabling of messages.
		if (message.isPlainTextEmpty()) return;

		if (SpigotFeatures.isSpigotAvailable()) {
			// Send message with additional text features:
			Internal.sendMessage(recipient, message);
		} else {
			// Fallback: Send message as plain text.
			String plainMessage = message.toPlainText();
			TextUtils.sendMessage(recipient, plainMessage);
		}
	}

	public static boolean debugging = false;

	// Separate class that gets only accessed if Spigot is present. Avoids class loading issues.
	private static final class Internal {

		// SENDING

		public static void sendMessage(CommandSender recipient, Text message) {
			assert recipient != null && message != null;
			BaseComponent component = toSpigot(message);
			if (debugging) {
				Log.info("Text: " + message);
				Log.info("Plain text: " + message.toPlainText());
				Log.info("Plain format text: " + message.toPlainFormatText());
				Log.info("Component: " + component);
				Bukkit.getConsoleSender().spigot().sendMessage(component);
			}
			recipient.spigot().sendMessage(component);
		}

		// CONVERSION

		private static final class TextStyle {

			private @Nullable ChatColor color = null;
			private @Nullable Boolean bold = null;
			private @Nullable Boolean italic = null;
			private @Nullable Boolean underlined = null;
			private @Nullable Boolean strikethrough = null;
			private @Nullable Boolean obfuscated = null;

			public void setColor(ChatColor color) {
				assert color != null && color.isColor();
				this.color = color;
			}

			public void setFormatting(ChatColor formatting) {
				assert formatting != null && formatting.isFormat();
				switch (formatting) {
				case BOLD:
					bold = true;
					break;
				case ITALIC:
					italic = true;
					break;
				case UNDERLINE:
					underlined = true;
					break;
				case STRIKETHROUGH:
					strikethrough = true;
					break;
				case MAGIC:
					obfuscated = true;
					break;
				default:
					Log.warning("Unexpected Text formatting: " + formatting);
					break;
				}
			}

			public void reset() {
				color = null;
				bold = null;
				italic = null;
				underlined = null;
				strikethrough = null;
				obfuscated = null;
			}

			public void apply(BaseComponent component) {
				assert component != null;
				component.setColor(Unsafe.nullableAsNonNull(toSpigot(color)));
				component.setBold(Unsafe.nullableAsNonNull(bold));
				component.setItalic(Unsafe.nullableAsNonNull(italic));
				component.setUnderlined(Unsafe.nullableAsNonNull(underlined));
				component.setStrikethrough(Unsafe.nullableAsNonNull(strikethrough));
				component.setObfuscated(Unsafe.nullableAsNonNull(obfuscated));
			}
		}

		private static BaseComponent toSpigot(Text text) {
			assert text != null;
			BaseComponent root = new TextComponent();
			toSpigot(text, null, root, new TextStyle());
			return root;
		}

		private static BaseComponent toSpigot(
				Text text,
				@Nullable TextComponent previous,
				BaseComponent parent,
				TextStyle textStyle
		) {
			assert text != null && parent != null && textStyle != null;

			// Conversion depending on type of Text and whether it can be combined with the current
			// component or a new one is required:
			@Nullable TextComponent current = previous;
			BaseComponent component;
			boolean ignoreChild = false;
			if (text instanceof FormattingText) {
				ChatColor formatting = ((FormattingText) text).getFormatting();
				if (formatting == ChatColor.RESET) {
					textStyle.reset();
					current = newTextComponent(parent, textStyle);
					current.setColor(Unsafe.nullableAsNonNull(toSpigot(ChatColor.RESET)));
				} else if (formatting.isColor()) {
					textStyle.setColor(formatting);
					if (current == null || hasText(current) || hasExtra(current)) {
						current = newTextComponent(parent, textStyle);
					} else {
						current.setColor(Unsafe.nullableAsNonNull(toSpigot(formatting)));
					}
				} else {
					assert formatting.isFormat();
					textStyle.setFormatting(formatting);
					if (current == null || hasText(current) || hasExtra(current)) {
						current = newTextComponent(parent, textStyle);
					} else {
						setFormatting(current, formatting);
					}
				}
				component = current;
			} else if (text instanceof PlainText) {
				if (current == null || hasText(current) || hasExtra(current)) {
					current = newTextComponent(parent, textStyle);
				}
				current.setText(((PlainText) text).getText());
				component = current;
			} else if (text instanceof PlaceholderText) {
				PlaceholderText placeholderText = (PlaceholderText) text;
				if (placeholderText.hasPlaceholderArgument()) {
					// Gets handled below when handling the child
					if (current == null) {
						current = newTextComponent(parent, textStyle);
					}
				} else {
					if (current == null || hasText(current) || hasExtra(current)) {
						current = newTextComponent(parent, textStyle);
					}
					current.setText(placeholderText.getFormattedPlaceholderKey());
				}
				component = current;
			} else if (text instanceof HoverEventText) {
				if (current == null || hasText(current) || hasExtra(current)) {
					current = newTextComponent(parent, textStyle);
				}
				current.setHoverEvent(toSpigot((HoverEventText) text));
				component = current;
			} else if (text instanceof ClickEventText) {
				if (current == null || hasText(current) || hasExtra(current)) {
					current = newTextComponent(parent, textStyle);
				}
				current.setClickEvent(toSpigot((ClickEventText) text));
				component = current;
			} else if (text instanceof InsertionText) {
				if (current == null || hasText(current) || hasExtra(current)) {
					current = newTextComponent(parent, textStyle);
				}
				current.setInsertion(((InsertionText) text).getInsertion());
				component = current;
			} else if (text instanceof TranslatableText) {
				// Create new translatable component:
				TranslatableText translatableText = (TranslatableText) text;
				String translationKey = translatableText.getTranslationKey();
				assert translationKey != null;

				// Convert translation arguments:
				List<? extends @NonNull Text> translationArgs = translatableText.getTranslationArguments();
				assert translationArgs != null;
				Object[] spigotTranslationArgs = new Object[translationArgs.size()];
				for (int i = 0; i < translationArgs.size(); ++i) {
					spigotTranslationArgs[i] = toSpigot(translationArgs.get(i));
				}

				component = new TranslatableComponent(translationKey, spigotTranslationArgs);
				parent.addExtra(component);
				textStyle.apply(component);
				current = null;
				ignoreChild = true;
			} else {
				throw new IllegalArgumentException("Unknown type of Text: "
						+ text.getClass().getName());
			}
			assert component != null;

			// Child: Add as child to current component, to inherit its features.
			Text child = text.getChild();
			if (!ignoreChild && child != null) {
				// This modifies the passed TextStyle to contain the last encountered style:
				toSpigot(child, current, component, textStyle);
			}

			// Next: Add as child to parent component to not inherit the features of the current
			// component.
			Text next = text.getNext();
			if (next != null) {
				toSpigot(next, current, parent, textStyle);
			}
			return component;
		}

		private static TextComponent newTextComponent(BaseComponent parent, TextStyle textStyle) {
			assert parent != null && textStyle != null;
			TextComponent component = new TextComponent();
			parent.addExtra(component);
			textStyle.apply(component);
			return component;
		}

		private static boolean hasText(TextComponent component) {
			assert component != null;
			return !StringUtils.isEmpty(component.getText());
		}

		private static boolean hasExtra(BaseComponent component) {
			assert component != null;
			List<BaseComponent> extra = component.getExtra();
			return (extra != null && !extra.isEmpty());
		}

		// CHAT COLOR

		private static net.md_5.bungee.api.@Nullable ChatColor toSpigot(
				@Nullable ChatColor chatColor
		) {
			if (chatColor == null) return null;
			return net.md_5.bungee.api.ChatColor.getByChar(chatColor.getChar());
		}

		private static void setFormatting(BaseComponent component, ChatColor formatting) {
			assert component != null && formatting != null;
			switch (formatting) {
			case BOLD:
				component.setBold(true);
				break;
			case ITALIC:
				component.setItalic(true);
				break;
			case UNDERLINE:
				component.setUnderlined(true);
				break;
			case STRIKETHROUGH:
				component.setStrikethrough(true);
				break;
			case MAGIC:
				component.setObfuscated(true);
				break;
			default:
				Log.warning("Unexpected Text formatting: " + formatting);
				break;
			}
		}

		// HOVER EVENT

		private static net.md_5.bungee.api.chat.HoverEvent toSpigot(HoverEventText hoverEvent) {
			assert hoverEvent != null;
			net.md_5.bungee.api.chat.HoverEvent.Action action = toSpigot(hoverEvent.getAction());
			BaseComponent[] value = new BaseComponent[] { toSpigot(hoverEvent.getValue()) };
			return new net.md_5.bungee.api.chat.HoverEvent(action, value);
		}

		private static net.md_5.bungee.api.chat.HoverEvent.Action toSpigot(
				HoverEventText.Action hoverEventAction
		) {
			assert hoverEventAction != null;
			switch (hoverEventAction) {
			case SHOW_TEXT:
				return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;
			case SHOW_ITEM:
				return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM;
			case SHOW_ENTITY:
				return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ENTITY;
			default:
				throw new IllegalStateException("Unexpected hover event action: "
						+ hoverEventAction);
			}
		}

		// CLICK EVENT

		private static net.md_5.bungee.api.chat.ClickEvent toSpigot(ClickEventText clickEvent) {
			assert clickEvent != null;
			net.md_5.bungee.api.chat.ClickEvent.Action action = toSpigot(clickEvent.getAction());
			return new net.md_5.bungee.api.chat.ClickEvent(action, clickEvent.getValue());
		}

		private static net.md_5.bungee.api.chat.ClickEvent.Action toSpigot(
				ClickEventText.Action clickEventAction
		) {
			assert clickEventAction != null;
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
				throw new IllegalStateException("Unexpected click event action: "
						+ clickEventAction);
			}
		}

		private Internal() {
		}
	}

	private SpigotText() {
	}
}
