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
import fr.aumgn.bukkitutils.command.CommandArgs;
import fr.aumgn.bukkitutils.command.Commands;
import fr.aumgn.bukkitutils.command.exception.CommandException;
import fr.aumgn.bukkitutils.command.exception.CommandUsageError;
import fr.aumgn.bukkitutils.command.messages.Messages;

public class MethodCommandExecutor implements CommandExecutor {

    private final Messages local;
    private final Commands instance;
    private final Method method;
    private final int min;
    private final int max;
    private final Set<Character> flags;
    private final boolean isPlayerCommand;

    public MethodCommandExecutor(Messages local, Commands instance, Method method, Command command) {
        this.local = local;
        this.instance = instance;
        this.method = method;

        this.min = command.min();
        this.max = command.max();
        this.flags = new HashSet<Character>();
        for (char flag : command.flags().toCharArray()) {
            this.flags.add(flag);
        }
        this.isPlayerCommand = Player.class.isAssignableFrom(
                method.getParameterTypes()[0]);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String lbl, String[] rawArgs) {
        if (isPlayerCommand && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + local.playerOnly());
            return true;
        }
        try {
            CommandArgs args = new CommandArgs(local, rawArgs, flags, min, max);
            callCommand(lbl, sender, args);
        } catch (CommandUsageError error) {
            sender.sendMessage(ChatColor.RED + error.getMessage());
            return false;
        } catch (Throwable thr) {
            sender.sendMessage(ChatColor.RED + thr.getMessage());
        }
        return true;
    }

    private void callCommand(String name, CommandSender sender, CommandArgs args) throws Throwable {
        try {
            method.invoke(instance, sender, args);
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

    private void unhandledError(String name, CommandArgs args, Throwable exc) {
        if (!(exc instanceof org.bukkit.command.CommandException)) {
            Bukkit.getLogger().severe("Exception occured while executing \""+ name + "\"");
            if (args.hasFlags()) {
                StringBuilder flagsString = new StringBuilder();
                for (char flag : args.flags()) {
                    flagsString.append(flag);
                }
                Bukkit.getLogger().severe("Flags : " + flagsString.toString());
            }
            if (args.length() > 0) {
                StringBuilder arguments = new StringBuilder();
                for (String arg : args.asList()) {
                    arguments.append(arg);
                    arguments.append(" ");
                }
                Bukkit.getLogger().severe("Arguments : " + arguments.toString());
            }
        }
        Bukkit.getLogger().log(Level.SEVERE, "Exception : ", exc);
    }
}