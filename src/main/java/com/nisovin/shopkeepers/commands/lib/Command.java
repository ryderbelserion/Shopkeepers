package com.nisovin.shopkeepers.commands.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.common.collect.Lists;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.arguments.FallbackArgument;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class Command {

	public static final String COMMAND_PREFIX = "/";
	public static final String ARGUMENTS_SEPARATOR = " ";
	public static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final String name;
	private final List<String> aliases; // unmodifiable
	private String description = "";
	// null if no permission is required:
	private String permission = null;
	private List<CommandArgument<?>> arguments;
	private Command parent = null;
	private final CommandRegistry childCommands = new CommandRegistry(this);

	// hides this command from the help page:
	private boolean hiddenInParentHelp = false;
	private boolean hiddenInOwnHelp = false;
	// makes the parent help content display this command's child commands:
	private boolean includeChildsInParentHelp = false;

	// formatting (null results in the parent's formatting to be used):
	private String helpTitleFormat = null;
	private String helpUsageFormat = null;
	private String helpDescFormat = null;
	private String helpChildUsageFormat = null;
	private String helpChildDescFormat = null;

	public Command(String name) {
		this(name, null);
	}

	public Command(String name, List<String> aliases) {
		Validate.notEmpty(name, "Command name is empty!");
		this.name = name;

		// validate and copy aliases:
		if (aliases == null || aliases.isEmpty()) {
			this.aliases = Collections.emptyList();
		} else {
			List<String> aliasesCopy = new ArrayList<>(aliases);
			// validate aliases:
			for (String alias : aliasesCopy) {
				Validate.notEmpty(alias, "Command contains empty alias!");
				Validate.isTrue(!StringUtils.containsWhitespace(alias), "Command contains alias with whitespace!");
			}
			this.aliases = Collections.unmodifiableList(aliasesCopy);
		}
	}

	/**
	 * Gets the name of this command.
	 * <p>
	 * For {@link BaseCommand base commands} this name is supposed to be unique among the commands of the same plugin.
	 * There might conflicts with the commands of other plugins though.
	 * <p>
	 * For child commands this name is supposed to be unique among the child commands of the same parent command.
	 * 
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Gets all the aliases of this command.
	 * <p>
	 * Depending on the names and aliases of other commands, not all aliases might actually be active for this command.
	 * 
	 * @return an unmodifiable view on the aliases, might be empty (but not <code>null</code>)
	 */
	public final List<String> getAliases() {
		return aliases;
	}

	/**
	 * A short one-line description of what this command does.
	 * <p>
	 * Might be used in command listings and the command help.
	 * 
	 * @return a short description, might be empty to indicate that no description is available
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Sets the short description for this command.
	 * 
	 * @param description
	 *            the description
	 * @see #getDescription()
	 */
	protected final void setDescription(String description) {
		this.description = description == null ? "" : description;
	}

	/**
	 * Gets the permission which is required for executing this command.
	 * <p>
	 * This only affects execution of this specific command, and not those of the child commands. Each child command has
	 * to specify its requirements and behavior separately.
	 * <p>
	 * This permission is for example used by {@link #testPermission(CommandSender)}.
	 * 
	 * @return the required permission, <code>null</code> if no required permission is set
	 */
	public final String getPermission() {
		return permission;
	}

	/**
	 * Sets the required permission for being able to execute this command.
	 * 
	 * @param permission
	 *            the permission
	 * @see #getPermission()
	 */
	protected final void setPermission(String permission) {
		this.permission = permission;
	}

	/**
	 * Checks whether the given {@link CommandSender} is potentially allowed to execute this command.
	 * <p>
	 * This only affects execution of this specific command, and not those of the child commands. Each child command has
	 * to specify its requirements and behavior separately.
	 * <p>
	 * By default the implementation checks the permission returned by {@link #getPermission()}, but it may be
	 * overridden to check for additional {@link CommandSender} specific conditions. If implementations are unsure if
	 * the {@link CommandSender} can execute this command, <code>true</code> should be returned.
	 * <p>
	 * The result of this method may be used to determine whether this command is listed in command listings for the
	 * given {@link CommandSender}.
	 * 
	 * @param sender
	 *            the sender
	 * @return <code>true</code> if the given {@link CommandSender} might be allowed to execute this command
	 */
	public boolean testPermission(CommandSender sender) {
		Validate.notNull(sender);
		return permission == null || PermissionUtils.hasPermission(sender, permission);
	}

	/**
	 * Similar to {@link Command#testPermission(CommandSender)}, but throws an exception with feedback message in case
	 * the given {@link CommandSender} is not allowed to execute this command.
	 * 
	 * @param sender
	 *            the sender
	 * @throws NoPermissionException
	 *             if the sender is not allowed to execute this command
	 */
	public void checkPermission(CommandSender sender) throws NoPermissionException {
		Validate.notNull(sender);
		if (!this.testPermission(sender)) {
			throw this.noPermissionException();
		}
	}

	/**
	 * Checks if the given {@link CommandSender} has the specified permission, and throws a
	 * {@link NoPermissionException} with corresponding error message if not.
	 * 
	 * @param sender
	 *            the sender
	 * @param permission
	 *            the permission, can be <code>null</code>
	 * @throws NoPermissionException
	 *             if the sender does not have the permission
	 */
	public void checkPermission(CommandSender sender, String permission) throws NoPermissionException {
		Validate.notNull(sender);
		if (permission != null && !PermissionUtils.hasPermission(sender, permission)) {
			throw this.noPermissionException();
		}
	}

	protected NoPermissionException noPermissionException() {
		return new NoPermissionException(Settings.msgNoPermission);
	}

	/**
	 * Checks whether the given type of {@link CommandSender} is accepted to execute this command.
	 * <p>
	 * This only affects execution of this specific command, and not those of the child commands. Each child command has
	 * to specify its requirements and behavior separately.
	 * <p>
	 * If overridden, it is recommended to also override {@link #checkCommandSource(CommandSender)} in order to use a
	 * more specific error message there in case the command sender cannot use the command.
	 * 
	 * @param sender
	 *            the command sender
	 * @return <code>true</code> of the command sender is accepted
	 */
	public boolean isAccepted(CommandSender sender) {
		Validate.notNull(sender);
		// by default all command senders are accepted:
		return true;
	}

	/**
	 * Checks whether the given type of {@link CommandSender} is accepted to execute this command.
	 * <p>
	 * If the {@link CommandSender} is not accepted the thrown {@link CommandSourceRejectedException} contains a
	 * user-friendly rejection message.
	 * 
	 * @param sender
	 *            the sender, receives feedback
	 * @throws CommandSourceRejectedException
	 *             if the given type of command sender is not accepted to execute this command
	 */
	public void checkCommandSource(CommandSender sender) throws CommandSourceRejectedException {
		Validate.notNull(sender);
		if (!this.isAccepted(sender)) {
			throw new CommandSourceRejectedException("You cannot execute this command here!");
		}
	}

	/**
	 * Gets the command format of this command.
	 * <p>
	 * The command format depends on the chain of parent commands and does not include this command's arguments.<br>
	 * Example: For a command {@code '/mail send <player> <message>'} this would be {@code '/mail send'}.
	 * 
	 * @return the command format
	 */
	public final String getCommandFormat() {
		if (parent == null) {
			// this is a base command:
			return COMMAND_PREFIX + this.getName();
		} else {
			// append primary alias to the format of the parent:
			return parent.getCommandFormat() + ARGUMENTS_SEPARATOR + this.getName();
		}
	}

	/**
	 * Gets all arguments of this command.
	 * 
	 * @return an unmodifiable view on all arguments of this command
	 */
	public final List<CommandArgument<?>> getArguments() {
		return arguments == null ? Collections.<CommandArgument<?>>emptyList() : Collections.unmodifiableList(arguments);
	}

	/**
	 * Gets the argument with the specified name.
	 * 
	 * @param name
	 *            the argument name
	 * @return the argument, or <code>null</code> if there is no such argument
	 */
	public final CommandArgument<?> getArgument(String name) {
		if (arguments == null) return null;
		for (CommandArgument<?> argument : arguments) {
			if (argument.getName().equals(name)) {
				return argument;
			}
		}
		return null;
	}

	/**
	 * Adds an {@link CommandArgument} to this command.
	 * 
	 * @param argument
	 *            the argument
	 */
	protected final void addArgument(CommandArgument<?> argument) {
		Validate.notNull(argument, "Argument is null!");
		Validate.isTrue(this.getArgument(argument.getName()) == null, "There is already another argument with that name: " + argument.getName());
		Validate.isTrue(argument.getParent() == null, "Cannot add argument with parent!");
		// make sure that no parent can be set once the argument has been added:
		argument.setParent(null); // parent can only be set once

		// lazy initialization:
		if (arguments == null) {
			arguments = new ArrayList<>();
		}
		arguments.add(argument);
	}

	/**
	 * Gets the arguments format of this command.
	 * <p>
	 * Example: For a command {@code '/message <player> <message>'}, this would be {@code '<player> <message>'}.<br>
	 * If this command is a child-command, the argument format is meant to only contain the arguments for this
	 * child-command.
	 * <p>
	 * The returned format is empty if this command does not use any arguments, if all arguments are hidden, or if this
	 * command only acts as parent for other commands.
	 * 
	 * @return the arguments format, possibly empty
	 */
	public final String getArgumentsFormat() {
		if (arguments == null) return "";
		StringBuilder argumentsFormat = new StringBuilder();
		for (CommandArgument<?> argument : arguments) {
			String argumentFormat = argument.getFormat();
			if (!argumentFormat.isEmpty()) {
				argumentsFormat.append(argumentFormat).append(ARGUMENTS_SEPARATOR);
			}
		}
		if (argumentsFormat.length() == 0) {
			return "";
		} else {
			return argumentsFormat.substring(0, argumentsFormat.length() - ARGUMENTS_SEPARATOR.length());
		}
	}

	/**
	 * Gets the usage format of this command.
	 * <p>
	 * This is basically the command format appended with the arguments format.
	 * 
	 * @return the usage format
	 */
	public final String getUsageFormat() {
		String usageFormat = this.getCommandFormat();
		// append arguments:
		String argsFormat = this.getArgumentsFormat();
		if (!argsFormat.isEmpty()) {
			usageFormat += ARGUMENTS_SEPARATOR + argsFormat;
		}
		return usageFormat;
	}

	public final Command getParent() {
		return parent;
	}

	// gets set by the parent command during registration of this command as child-command:
	final void setParent(Command parent) {
		this.parent = parent;
	}

	public final CommandRegistry getChildCommands() {
		return childCommands;
	}

	/**
	 * Searches and prepares the {@link CommandContext} and arguments array for a matching child-command.
	 * <p>
	 * A command which registers custom {@link CommandRegistry}s might want to override and extend this method, to
	 * include checking for those custom child-commands and parsing of context arguments for those.
	 * 
	 * @param args
	 *            the command arguments
	 * @return the child-command together with the prepared arguments, or <code>null</code> if none was found
	 */
	protected Command getChildCommand(CommandArgs args) {
		if (args.hasNext()) {
			String childCommandAlias = args.peek();
			Command childcommand = this.getChildCommands().getCommand(childCommandAlias);
			if (childcommand != null) {
				// move cursor forward to cut away the used-up argument:
				args.next();
				return childcommand;
			}
		}

		// no matching child-command command was found:
		return null;
	}

	/**
	 * Handles a few common things before the command gets executed, like:
	 * <ul>
	 * <li>passing the command handling to a matching child-command, otherwise:
	 * <li>checking if the {@link CommandSource} is accepted
	 * <li>checking the command permission for the {@link CommandSender}
	 * <li>parsing the arguments
	 * <li>calling {@link #execute(CommandInput, CommandContext, CommandArgs)}
	 * </ul>
	 * 
	 * @param input
	 *            the command input
	 * @param context
	 *            the context
	 * @param args
	 *            the command arguments
	 * @throws CommandException
	 *             if command execution failed
	 */
	public void handleCommand(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		// search for matching child-command:
		Command childCommand = this.getChildCommand(args);
		if (childCommand != null) {
			// delegate to child-command:
			childCommand.handleCommand(input, context, args);
		} else {
			CommandSender sender = input.getSender();
			// check if this type of command sender supported:
			this.checkCommandSource(sender);

			// check if the command sender has the required permission to proceed:
			this.checkPermission(sender);

			// no applicable child-command was found:
			// parse arguments:
			this.parseArguments(input, context, args);

			// execute this command:
			this.execute(input, context, args);
		}
	}

	/**
	 * Parses all arguments for this command.
	 * 
	 * @param input
	 *            the input
	 * @param context
	 *            the command context to store the parsed values in
	 * @param args
	 *            the command arguments to extract the argument values from
	 * @throws ArgumentParseException
	 *             if a required argument cannot be parsed or there are unparsed remaining arguments
	 */
	protected void parseArguments(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		if (arguments == null) {
			// this command has no arguments:
			return;
		}

		// keep track of fallbacks:
		FallbackArgumentException[] fallbacks = null;
		int fallbackIndex = -1;
		int fallbackArgsIndex = -1; // args index at time of first fallback
		Object fallbackArgsState = null;
		BufferedCommandContext fallbackContext = null;

		// parse all arguments:
		CommandContext activeContext = context;
		int argumentsCount = arguments.size();
		for (int currentArgumentIndex = 0; currentArgumentIndex < argumentsCount; ++currentArgumentIndex) {
			CommandArgument<?> argument = arguments.get(currentArgumentIndex);
			boolean success = false;
			ArgumentParseException parseException = null;
			try {
				argument.parse(input, activeContext, args);
				success = true; // parsing successful
			} catch (FallbackArgumentException e) {
				// keep track of fallback(s):
				if (fallbacks == null) { // lazy initialization
					fallbacks = new FallbackArgumentException[argumentsCount];
				}
				fallbacks[currentArgumentIndex] = e;
				assert currentArgumentIndex > fallbackIndex;
				if (fallbackIndex == -1) {
					// first fallback, capture current state:
					fallbackIndex = currentArgumentIndex;
					fallbackArgsIndex = args.getCurrentIndex();
					fallbackArgsState = args.getState();
					// keep track of context changes while continuing with pending fallback:
					fallbackContext = new BufferedCommandContext(context);
					activeContext = fallbackContext;
				}
			} catch (ArgumentParseException e) { // argument couldn't be parsed
				parseException = e;
			}
			assert !(parseException instanceof FallbackArgumentException);

			if (fallbackIndex == -1) { // no pending fallback(s)
				if (parseException != null) {
					throw parseException; // parsing failed at the current argument
				}
			} else {
				// handle fallback(s) if parsing failed (without providing a fallback itself), if the current command
				// argument was able to parse arguments, or if there are no more command arguments to parse:
				boolean parsingFailed = (parseException != null);
				boolean argumentsConsumed = (success && fallbackArgsIndex != args.getCurrentIndex());
				boolean finalArgument = (currentArgumentIndex == (argumentsCount - 1));

				if (parsingFailed || argumentsConsumed || finalArgument) {
					// handle pending fallback(s):

					boolean parsingContextChanged = false;
					// reset context to state before fallback:
					activeContext = context;

					CommandArgs activeArgs = args;
					if (argumentsConsumed) {
						// use empty args for fallback(s), because some other command argument was able to parse the
						// argument which triggered the first fallback:
						activeArgs = CommandArgs.EMPTY;
						parsingContextChanged = true;
					} else {
						// reset args, just in case:
						args.setState(fallbackArgsState);
						fallbackArgsState = null;
					}

					// evaluate fallbacks one after the other:
					// if parsing fails but is postponed, remember and continue with next fallback
					// if parsing fails with another fallback, evaluate new fallback immediately (loop)
					// if parsing fails, abort
					// if parsing succeeds and arguments got consumed, abort fallbacks and restart parsing from there
					// if parsing succeeds but no arguments got consumed, continue with next fallback
					// when all fallbacks were evaluated:
					// if args were consumed: parsing gets restarted at the fallback that consumed those arg(s)
					// if no args were consumed, and current argument fail is postponed, continue with next argument
					// if no args were consumed, and current argument succeeded, continue with next argument
					// if no args were consumed, and current argument failed, abort with failure

					boolean fallbackConsumedArgs = false;
					int currentFallbackIndex = fallbackIndex;
					for (; currentFallbackIndex <= currentArgumentIndex; ++currentFallbackIndex) {
						FallbackArgumentException fallback = fallbacks[currentFallbackIndex];
						if (fallback == null) continue;

						FallbackArgumentException currentFallback = fallback;
						boolean fallbackSuccess = false;
						ArgumentParseException fallbackError = null;
						boolean hasRemainingArgs = activeArgs.getRemainingSize() > 0;
						int prevArgsIndex = activeArgs.getCurrentIndex();
						while (true) { // evaluate recursive fallbacks
							FallbackArgument<?> fallbackArgument = currentFallback.getFallbackArgument();
							try {
								fallbackArgument.parseFallback(input, activeContext, activeArgs, parsingContextChanged, currentFallback);
								// fallback success:
								fallbackSuccess = true;
								break;
							} catch (FallbackArgumentException e) {
								// got another fallback: evaluate it immediately in the next loop iteration
								currentFallback = e;
								continue;
							} catch (ArgumentParseException e) {
								// fallback error:
								fallbackError = e;
								break;
							}
						}
						assert fallbackSuccess ^ (fallbackError != null); // xor

						// fallbacks get aborted if some fallback is able to successfully parse something:
						assert !fallbackConsumedArgs;
						fallbackConsumedArgs = (prevArgsIndex != activeArgs.getCurrentIndex());
						// assumption: args state got restored (no args consumed) if parsing failed
						Validate.State.isTrue(fallbackSuccess || !fallbackConsumedArgs,
								"Parsing argument fallback failed, but arguments did not get properly reset: "
										+ fallback.getFallbackArgument().getName());

						if (hasRemainingArgs && !fallbackConsumedArgs) {
							// there are arguments remaining but the fallback did not consume any of them
							// regardless of whether the fallback has been successful or not, abort parsing with the
							// original root exception: TODO move into FallbackArgument?

							// TODO There might be situations in which the following fallbacks may be able to parse
							// these remaining args. But in the most common cases continuing would currently result in
							// less good error messages for the user.
							// Eg. "/list [player (default:self)] [page (default:1)]"
							// "/list a": 'a' could either be an invalid player, or an invalid page, or an unexpected
							// argument.
							// "/list a 2": 'a' is an invalid player. The fallback would produce 'self' as the default
							// value for the player argument but not consume the 'a'. Continuing with the next fallback
							// would result in either an 'invalid page' or even (when continuing further) an 'unexpected
							// argument' error. By aborting early with the original root excpetion, the player gets the
							// expected 'invalid player' error in this case.

							// TODO Solve these ambiguities, maybe by parsing all possible paths / argument bindings.
							// Parsing succeeds if there is a single successful binding. If there are multiple
							// successful bindings, print a warning regarding the ambiguity (requires some way for the
							// user to resolve it). If there is no successful binding, somehow pick a likely relevant
							// error message (ambiguous as well, if there are multiple different bindings producing
							// different errors).
							throw fallback.getRootException();
						}
						assert !hasRemainingArgs || fallbackConsumedArgs;

						if (fallbackConsumedArgs) {
							assert fallbackSuccess;
							// The fallback did consume arguments: Abort fallbacks and restart parsing from here
							// Note: This case can never occur if the active args are empty (if some later command
							// argument was able to consume arguments)
							break;
						} else if (fallbackError != null) {
							// abort parsing with the fallback error:
							throw fallbackError;
						} else {
							assert fallbackSuccess;
							// success, but no args consumed: continue with the next fallback
							continue;
						}
					}

					if (fallbackConsumedArgs) {
						// restart parsing from the argument after the current fallback:
						currentArgumentIndex = currentFallbackIndex;
						// disregard any context changes that happened since the fallback:
						fallbackContext = null;
						fallbackIndex = -1; // no longer in fallback mode
						continue;
					} else {
						// fallbacks were successfully evaluated, but no arguments were consumed by them, resulting in
						// the same situation as before
						// apply context changes that happened between the fallback and current argument:
						fallbackContext.applyBuffer();
						fallbackContext = null;
						fallbackIndex = -1; // no longer in fallback mode

						if (parsingFailed) {
							assert parseException != null;
							// the original situation lead to the current parsing exception:
							throw parseException; // parsing failed at the current argument
						} else {
							// continue parsing with the next argument (if there is one, else parsing is over)
							continue;
						}
					}
				}
			}
		}

		// handle unparsed arguments (if any):
		this.handleUnparsedArguments(input, context, args);

		// parsing was successful
	}

	private void handleUnparsedArguments(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		if (args.getRemainingSize() == 0) return; // no unparsed arguments

		// remaining unexpected/unparsed arguments:
		String firstUnparsedArg = args.peek();
		if (!this.getChildCommands().getCommands().isEmpty()) {
			// has child commands: throw an 'unknown command' exception
			// TODO only use this exception if no arguments got parsed by this command?
			throw new ArgumentParseException(this.getUnknownCommandMessage(firstUnparsedArg));
		} else {
			// throw an 'invalid argument' exception for the first unparsed argument after the last parsed argument:
			CommandArgument<?> firstUnparsedArgument = null;
			for (CommandArgument<?> argument : Lists.reverse(this.getArguments())) {
				if (!context.has(argument.getName())) {
					firstUnparsedArgument = argument;
				} else {
					break; // stop at the first (from behind) found parsed argument
				}
			}
			if (firstUnparsedArgument != null) {
				throw firstUnparsedArgument.invalidArgumentError(firstUnparsedArg);
			} else {
				// throw an 'unexpected argument' exception:
				throw new ArgumentParseException(TextUtils.replaceArgs(Settings.msgCommandArgumentUnexpected,
						"{argument}", firstUnparsedArg));
			}
		}
	}

	protected String getUnknownCommandMessage(String command) {
		return TextUtils.replaceArgs(Settings.msgCommandUnknown, "{command}", command);
	}

	/**
	 * Executes this specific command.
	 * <p>
	 * By default, simply {@link #sendHelp(CommandSender)} is called when executed. Override this method for custom
	 * command behavior.
	 * 
	 * @param input
	 *            the command input
	 * @param context
	 *            the context
	 * @param args
	 *            the command arguments
	 * @throws CommandException
	 *             if command execution failed
	 */
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		// default command behavior: print command help information
		this.sendHelp(input.getSender());
	}

	/**
	 * Gets tab completion suggestions for the final argument of the given input.
	 * <p>
	 * Notes regarding the default implementation:
	 * <ul>
	 * <li>Only the names of child-commands are completed, but no suggestions are provided for for argument values.
	 * <li>If no suggestions are found, <code>null</code> is returned (instead of an empty list) in order to let the
	 * server perform its default tab completion.
	 * </ul>
	 * 
	 * @param input
	 *            the command input
	 * @param context
	 *            the context
	 * @param args
	 *            the command arguments, including the partial final argument to be completed (which can be an empty
	 *            string)
	 * @return the suggestions for the final argument
	 */
	public List<String> handleTabCompletion(CommandInput input, CommandContext context, CommandArgs args) {
		// search for matching child-command:
		Command childCommand = this.getChildCommand(args);
		if (childCommand != null) {
			// delegate to child-command:
			return childCommand.handleTabCompletion(input, context, args);
		}

		// no applicable child-command was found:
		CommandSender sender = input.getSender();
		// check if this type of command sender supported:
		if (!this.isAccepted(sender)) {
			// not supported type of command sender:
			return Collections.emptyList();
		}

		// check if this command sender has the required permission to proceed:
		if (!this.testPermission(sender)) {
			// command sender not allowed:
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		if (args.getRemainingSize() == 1) {
			String finalArgument = CommandUtils.normalize(args.peek());
			// include matching child-command aliases (max one per command):
			// asserts that all aliases for one command come in one row
			Command lastMatchingCommand = null;
			for (Entry<String, Command> aliasEntry : this.getChildCommands().getAliasesMap().entrySet()) {
				String alias = aliasEntry.getKey(); // normalized
				Command aliasCommand = aliasEntry.getValue();
				if (lastMatchingCommand != null && lastMatchingCommand == aliasCommand) {
					// we have already included a suggestion for this child command, skip:
					continue;
				}
				// we have reached an alias for a new child command:
				lastMatchingCommand = null;

				// does the alias match the input?
				if (alias.startsWith(finalArgument)) {
					// exclude further aliases for this command:
					lastMatchingCommand = aliasCommand;

					// check if recipient even has required permission for this command:
					if (!aliasCommand.testPermission(sender)) {
						// missing permission for this command, skip:
						continue;
					}

					// add this alias to the suggestions:
					// TODO maybe use the original alias here, and not the normalized one?
					suggestions.add(alias);
				}
			}
		}

		// parse and complete arguments:
		if (arguments != null) {
			for (CommandArgument<?> argument : arguments) {
				int remainingArgs = args.getRemainingSize();
				if (remainingArgs == 0) {
					// no argument left which could be completed:
					break;
				}
				Object state = args.getState();
				try {
					argument.parse(input, context, args);
					// successfully parsed:
					if (!args.hasNext()) {
						// this consumed the last argument:
						// reset args and provide alternative completions for the last argument instead:
						args.setState(state);
						suggestions.addAll(argument.complete(input, context, args));
						break;
					} else if (args.getRemainingSize() == remainingArgs) {
						// no error during parsing, but none of the remaining args used up:
						// -> this was an optional argument which got skipped
						// include suggestions (if it has any), but continue:
						suggestions.addAll(argument.complete(input, context, args));

						// reset state and then let the following arguments also try to complete the same arg(s):
						args.setState(state);
						continue;
					}
				} catch (FallbackArgumentException e) {
					// parsing failed, but registered a fallback:
					// check for completions, but continue parsing
					args.setState(state);
					suggestions.addAll(argument.complete(input, context, args));
					args.setState(state);
					continue;
				} catch (ArgumentParseException e) {
					if (argument.getReducedFormat().isEmpty()) {
						// argument is hidden, check for completions but continue parsing:
						args.setState(state);
						suggestions.addAll(argument.complete(input, context, args));
						args.setState(state);
						continue;
					} else {
						// parsing might have failed because of an invalid partial last argument
						// -> check for and include suggestions
						args.setState(state);
						suggestions.addAll(argument.complete(input, context, args));
						// parsing might also have failed because of an invalid argument inside the sequence of
						// arguments
						// -> skip later arguments (current argument will not provide suggestions in that case, because
						// it isn't using up the last argument)
						break;
					}
				}
			}
		}

		return Collections.unmodifiableList(suggestions);
	}

	// help page related:

	/**
	 * Checks whether the child-commands of this {@link Command} are included in the parent's help content.
	 * <p>
	 * By default only direct child commands are included in the help content.
	 * 
	 * @return <code>true</code> if child-commands are included in the parent's help content
	 */
	public final boolean isIncludeChildsInParentHelp() {
		return includeChildsInParentHelp;
	}

	/**
	 * Sets whether the child-commands of this {@link Command} are included in the parent's help content.
	 * <p>
	 * By default only direct child commands are included in the help content.
	 * 
	 * @param includeChilds
	 *            <code>true</code> to include the child commands of this command in the parent's help content
	 */
	protected final void setIncludeChildsInParentHelp(boolean includeChilds) {
		this.includeChildsInParentHelp = includeChilds;
	}

	/**
	 * Checks whether this {@link Command} is hidden in the parent's help contents.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @return <code>true</code> if this command is hidden in the parent's help contents
	 */
	public final boolean isHiddenInParentHelp() {
		return hiddenInParentHelp;
	}

	/**
	 * Sets whether this {@link Command} is hidden in the parent's help contents.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @param hiddenInParentHelp
	 *            <code>true</code> to exclude this command in the parent's help contents
	 */
	protected final void setHiddenInParentHelp(boolean hiddenInParentHelp) {
		this.hiddenInParentHelp = hiddenInParentHelp;
	}

	/**
	 * Checks whether this {@link Command} is hidden in its own help page.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @return <code>true</code> if this command is hidden in its own help page
	 */
	public final boolean isHiddenInOwnHelp() {
		return hiddenInOwnHelp;
	}

	/**
	 * Sets whether this {@link Command} is hidden in its own help page.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @param hiddenInOwnHelp
	 *            <code>true</code> to exclude this command in its own help contents
	 */
	protected final void setHiddenInOwnHelp(boolean hiddenInOwnHelp) {
		this.hiddenInOwnHelp = hiddenInOwnHelp;
	}

	// help page formatting:

	/**
	 * Sets the format to use for the title when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * Available placeholders are: <code>{command}, {usage}, {arguments}</code><br>
	 * If the format is an empty String, no title will be used in the help pages.<br>
	 * If the format is set to <code>null</code>, the format of the parent command gets used. If no parent is available
	 * or the parent format is an empty String, a default format gets used.
	 * 
	 * @param helpTitleFormat
	 *            the format
	 */
	protected void setHelpTitleFormat(String helpTitleFormat) {
		this.helpTitleFormat = helpTitleFormat;
	}

	/**
	 * Sets the format to use for this command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * Available placeholders are: <code>{command}, {usage}, {arguments}</code><br>
	 * If the format is an empty String, no command usage will be printed in the help pages.<br>
	 * If the format is set to <code>null</code>, the format of the parent command gets used. If no parent is available
	 * or the parent format is an empty String, a default format gets used.
	 * 
	 * @param helpUsageFormat
	 *            the format
	 */
	protected void setHelpUsageFormat(String helpUsageFormat) {
		this.helpUsageFormat = helpUsageFormat;
	}

	/**
	 * Sets the format to use for this command's description when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * Available placeholders are: <code>{description}</code><br>
	 * If the format is an empty String, no command description will be printed in the help pages.<br>
	 * If the format is set to <code>null</code>, the format of the parent command gets used. If no parent is available
	 * or the parent format is an empty String, a default format gets used.
	 * 
	 * @param helpDescFormat
	 *            the format
	 */
	protected void setHelpDescFormat(String helpDescFormat) {
		this.helpDescFormat = helpDescFormat;
	}

	/**
	 * Sets the format to use for a child-command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * Available placeholders are: <code>{command}, {usage}, {arguments}</code><br>
	 * If the format is an empty String, no child commands will be included in the help pages.<br>
	 * If the format is set to <code>null</code>, the format of the parent command gets used. If no parent is available
	 * or the parent format is an empty String, a default format gets used.
	 * 
	 * @param helpChildUsageFormat
	 *            the format
	 */
	protected void setHelpChildUsageFormat(String helpChildUsageFormat) {
		this.helpChildUsageFormat = helpChildUsageFormat;
	}

	/**
	 * Sets the format to use for a child-command's description when sending the help via
	 * {@link #sendHelp(CommandSender)}.
	 * <p>
	 * Available placeholders are: <code>{description}</code><br>
	 * If the format is an empty String, no child command descriptions will be included in the help pages.<br>
	 * If the format is set to <code>null</code>, the format of the parent command gets used. If no parent is available
	 * or the parent format is an empty String, a default format gets used.
	 * 
	 * @param helpChildDescFormat
	 *            the format
	 */
	protected void setHelpChildDescFormat(String helpChildDescFormat) {
		this.helpChildDescFormat = helpChildDescFormat;
	}

	/**
	 * Gets the format to use for the title when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpTitleFormat(String)
	 */
	protected final String getHelpTitleFormat() {
		String format = this.helpTitleFormat;
		if (format == null) {
			String parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpTitleFormat()).equals("")) {
				format = parentFormat;
			} else {
				// default:
				String paddingStyle = ChatColor.AQUA.toString();
				format = paddingStyle + "-------[ "
						+ ChatColor.DARK_GREEN + "Command Help: "
						+ ChatColor.GOLD + ChatColor.ITALIC + "{command}"
						+ paddingStyle + " ]-------";
			}

		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for this command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpUsageFormat(String)
	 */
	protected final String getHelpUsageFormat() {
		String format = this.helpUsageFormat;
		if (format == null) {
			String parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpUsageFormat()).equals("")) {
				format = parentFormat;
			} else {
				// default:
				format = ChatColor.YELLOW + "{usage}";
			}
		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for this command's description when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpDescFormat(String)
	 */
	protected final String getHelpDescFormat() {
		String format = this.helpDescFormat;
		if (format == null) {
			String parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpDescFormat()).equals("")) {
				format = parentFormat;
			} else {
				// default:
				format = ChatColor.DARK_GRAY + " - " + ChatColor.DARK_AQUA + "{description}";
			}
		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for a child-command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpChildUsageFormat(String)
	 */
	protected final String getHelpChildUsageFormat() {
		String format = this.helpChildUsageFormat;
		if (format == null) {
			String parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpChildUsageFormat()).equals("")) {
				format = parentFormat;
			} else {
				// default:
				format = this.getHelpUsageFormat();
			}

		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for a child-command's description when sending the help via
	 * {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpChildDescFormat(String)
	 */
	protected final String getHelpChildDescFormat() {
		String format = this.helpChildDescFormat;
		if (format == null) {
			String parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpChildDescFormat()).equals("")) {
				format = parentFormat;
			} else {
				// default:
				format = this.getHelpDescFormat();
			}
		}
		assert format != null;
		return format;
	}

	/**
	 * Checks if the recipient has the permission to view the help of this command.
	 * <p>
	 * This checks if the recipient has the permission to execute this command or any of the child commands.
	 * 
	 * @param recipient
	 *            the recipient
	 * @return <code>true</code> if the recipient is allowed to view the help
	 */
	protected final boolean hasHelpPermission(CommandSender recipient) {
		if (this.testPermission(recipient)) return true;
		for (Command childCommand : this.getChildCommands().getCommands()) {
			if (childCommand.hasHelpPermission(recipient)) return true;
		}
		return false;
	}

	/**
	 * Sends usage information about this command and its child-commands to the given {@link CommandSender}.
	 * 
	 * @param recipient
	 *            the recipient
	 * @throws NoPermissionException
	 *             if the sender is not allowed to view the help of this command
	 */
	public void sendHelp(CommandSender recipient) throws NoPermissionException {
		Validate.notNull(recipient);

		// make sure the recipient has the required permission:
		if (!this.hasHelpPermission(recipient)) {
			throw this.noPermissionException();
		}

		// prepare common placeholders:
		String[] placeholders = new String[] {
				"{command}", this.getCommandFormat(),
				"{usage}", this.getUsageFormat(),
				"{arguments}", this.getArgumentsFormat()
		};

		// title:
		String titleFormat = this.getHelpTitleFormat();
		assert titleFormat != null;
		if (!titleFormat.isEmpty()) {
			TextUtils.sendMessage(recipient, titleFormat, placeholders);
		}

		// skip info about the command if it is hidden or the recipient does not have the required permission:
		if (!this.isHiddenInOwnHelp() && this.testPermission(recipient)) {
			String commandInfo = "";

			// command usage:
			String usageFormat = this.getHelpUsageFormat();
			assert usageFormat != null;
			if (!usageFormat.isEmpty()) {
				String commandUsage = TextUtils.replaceArgs(usageFormat, placeholders);
				commandInfo += commandUsage;
			}

			// command description:
			String descriptionFormat = this.getHelpDescFormat();
			assert descriptionFormat != null;
			if (!descriptionFormat.isEmpty()) {
				String description = this.getDescription();
				if (!description.isEmpty()) {
					String commandDescription = TextUtils.replaceArgs(descriptionFormat, "{description}", description);
					commandInfo += commandDescription;
				}
			}

			if (!commandInfo.isEmpty()) {
				TextUtils.sendMessage(recipient, commandInfo);
			}
		}

		// include child-commands help:
		String childUsageFormat = this.getHelpChildUsageFormat();
		String childDescFormat = this.getHelpChildDescFormat();
		this.sendChildCommandsHelp(recipient, childUsageFormat, childDescFormat, this);
	}

	protected void sendChildCommandsHelp(CommandSender recipient, String childUsageFormat, String childDescFormat, Command command) {
		Validate.notNull(recipient);
		if (childUsageFormat == null || childUsageFormat.isEmpty()) {
			// not including child commands at all:
			return;
		}

		// print usage and description of child-commands:
		for (Command childCommand : command.getChildCommands().getCommands()) {
			// skip info about the command if it is hidden or the recipient does not have the required permission:
			if (!childCommand.isHiddenInParentHelp() && childCommand.testPermission(recipient)) {
				// prepare common placeholders:
				String[] childPlaceholders = new String[] {
						"{command}", childCommand.getCommandFormat(),
						"{usage}", childCommand.getUsageFormat(),
						"{arguments}", childCommand.getArgumentsFormat()
				};

				String commandInfo = "";

				// command usage:
				String childUsage = TextUtils.replaceArgs(childUsageFormat, childPlaceholders);
				commandInfo += childUsage;

				// command description:
				if (childDescFormat != null && !childDescFormat.isEmpty()) {
					String childDescription = childCommand.getDescription();
					if (!childDescription.isEmpty()) {
						String childDescriptionFormat = TextUtils.replaceArgs(childDescFormat,
								"{description}", childDescription);
						commandInfo += childDescriptionFormat;
					}
				}

				TextUtils.sendMessage(recipient, commandInfo);
			}

			// optionally include the child-command's child-commands in help content:
			if (childCommand.isIncludeChildsInParentHelp()) {
				this.sendChildCommandsHelp(recipient, childUsageFormat, childDescFormat, childCommand);
			}
		}
	}
}
