package fr.aumgn.bukkitutils.command.executor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.aumgn.bukkitutils.command.Command;
import fr.aumgn.bukkitutils.command.Commands;
import fr.aumgn.bukkitutils.command.CommandsMessages;
import fr.aumgn.bukkitutils.command.args.CommandArgs;
import fr.aumgn.bukkitutils.command.args.CommandArgsParser;
import fr.aumgn.bukkitutils.command.exception.CommandException;
import fr.aumgn.bukkitutils.command.exception.CommandUsageError;
import fr.aumgn.bukkitutils.glob.exceptions.GlobException;
import fr.aumgn.bukkitutils.glob.exceptions.UnbalancedSquareBracketException;

public class MethodCommandExecutor implements CommandExecutor {

    private final CommandsMessages messages;
    private final Commands instance;
    private final Method preExecute;
    private final Method method;
    private final int min;
    private final int max;
    private final boolean strictFlags;
    private final Set<Character> flags;
    private final Set<Character> argsFlags;
    private final boolean isPlayerCommand;

    public MethodCommandExecutor(CommandsMessages messages, Commands instance,
            Method preExecute, Method method, int min, int max,
            boolean strictFlags, String flags, String argsFlags) {
        this.messages = messages;
        this.instance = instance;
        this.preExecute = preExecute;
        this.method = method;

        if (method.getParameterTypes().length > 1) {
            this.min = min;
            this.max = max;
        } else {
            this.min = -1;
            this.max = 0;
        }
        this.strictFlags = strictFlags;
        this.flags = new HashSet<Character>();
        for (char flag : flags.toCharArray()) {
            this.flags.add(flag);
        }
        this.argsFlags = new HashSet<Character>();
        for (char flag : argsFlags.toCharArray()) {
            this.argsFlags.add(flag);
        }
        this.isPlayerCommand = Player.class.isAssignableFrom(
                method.getParameterTypes()[0]);
    }

    public MethodCommandExecutor(CommandsMessages messages, Commands instance,
            Method preExecute, Method method, Command command) {
        this(messages, instance, preExecute, method, command.min(),
                command.max(), command.strictFlags(), command.flags(),
                command.argsFlags());
    }

    @Override
    public boolean onCommand(CommandSender sender,
            org.bukkit.command.Command cmd, String lbl, String[] rawArgs) {
        if (isPlayerCommand && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + messages.playerOnly());
            return true;
        }
        try {
            CommandArgs args;
            if (min >= 0) {
                args = getArgs(rawArgs);
            } else {
                if (rawArgs.length > 0) {
                    throw new CommandUsageError(
                            messages.tooManyArguments(rawArgs.length, 0));
                }
                args = null;
            }
            callCommand(lbl, sender, args);
        } catch (GlobException exc) {
            handleGlobException(exc);
        } catch (CommandUsageError error) {
            sender.sendMessage(ChatColor.RED + error.getMessage());
            return false;
        } catch (Throwable thr) {
            sender.sendMessage(ChatColor.RED + thr.getMessage());
        }
        return true;
    }

    private CommandArgs getArgs(String[] rawArgs){
        CommandArgsParser parser = new CommandArgsParser(messages, rawArgs);
        parser.validate(strictFlags, flags, argsFlags, min, max);
        return new CommandArgs(messages, parser);
    }

    private void callCommand(String name, CommandSender sender,
            CommandArgs args) throws Throwable {
        try {
            if (preExecute != null) {
                preExecute.invoke(instance, sender, args);
            }
            if (args != null) {
                method.invoke(instance, sender, args);
            } else {
                method.invoke(instance, sender);
            }
        } catch (InvocationTargetException exc) {
            Throwable cause = exc.getCause();
            if (cause instanceof CommandException) {
                throw cause;
            }
            unhandledError(name, args, cause);
        } catch (IllegalArgumentException exc) {
            unhandledError(name, args, exc);
        } catch (IllegalAccessException exc) {
            unhandledError(name, args, exc);
        }
    }

    private void handleGlobException(GlobException exc) {
        if (exc instanceof UnbalancedSquareBracketException) {
            UnbalancedSquareBracketException usbExc;
            usbExc = ((UnbalancedSquareBracketException) exc);
            throw new CommandUsageError(
                    messages.globUnbalancedSquareBracket(usbExc.getGlob()));
        }

        throw new RuntimeException(exc);
    }

    private void unhandledError(String name, CommandArgs args, Throwable exc) {
        if (!(exc instanceof org.bukkit.command.CommandException)) {
            Bukkit.getLogger().severe(
                    "Exception occured while executing \""+ name + "\"");
            if (args != null) {
                if (args.hasFlags()) {
                    StringBuilder flagsString = new StringBuilder();
                    for (char flag : args.flags()) {
                        flagsString.append(flag);
                    }
                    Bukkit.getLogger().severe(
                            "Flags : " + flagsString.toString());
                }
                if (args.length() > 0) {
                    StringBuilder arguments = new StringBuilder();
                    for (String arg : args.asList()) {
                        arguments.append(arg);
                        arguments.append(" ");
                    }
                    Bukkit.getLogger().severe(
                            "Arguments : " + arguments.toString());
                }
            }
        }
        Bukkit.getLogger().log(Level.SEVERE, "Exception : ", exc);
    }
}
