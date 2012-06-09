package fr.aumgn.bukkitutils.command.arg.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.aumgn.bukkitutils.command.arg.CommandArg;
import fr.aumgn.bukkitutils.command.arg.CommandArgFactory;
import fr.aumgn.bukkitutils.command.exception.CommandError;
import fr.aumgn.bukkitutils.command.exception.CommandUsageError;
import fr.aumgn.bukkitutils.command.messages.Messages;

public class WorldArg extends CommandArg<World> {

    public static class Factory extends CommandArgFactory<World> {

        @Override
        public WorldArg createCommandArg(Messages messages, String string) {
            return new WorldArg(messages, string);
        }
    }

    public WorldArg(Messages messages, String string) {
        super(messages, string);
    }

    public static class NoSuchWorld extends CommandError {
        private static final long serialVersionUID = -4832133406864970323L;

        public NoSuchWorld(Messages messages, String name) {
            super(String.format(messages.noSuchWorld(), name));
        }
    }

    @Override
    public World value() {
        World world = Bukkit.getWorld(string);
        if (world == null) {
            throw new NoSuchWorld(messages, string);
        }

        return world;
    }

    @Override
    public World value(CommandSender sender) {
        if (string != null) {
            return value();
        }

        if (!(sender instanceof Player)) {
            throw new CommandUsageError("You have to specify a world.");
        }

        return ((Player) sender).getWorld();
    }

    @Override
    public List<World> match() {
        if (string.equals("*")) {
            return Bukkit.getWorlds();
        }

        String pattern = string.toLowerCase(Locale.ENGLISH);
        List<World> worlds = new ArrayList<World>();

        for (World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase().contains(pattern)) {
                worlds.add(world);
            }
        }

        if (worlds.isEmpty()) {
            throw new NoSuchWorld(messages, string);
        }

        return worlds;
    }

    @Override
    public List<World> match(CommandSender sender) {
        if (string != null) {
            return match();
        }

        if (!(sender instanceof Player)) {
            throw new CommandUsageError(messages.worldNeeded());
        }

        return Collections.<World>singletonList(((Player) sender).getWorld());
    }
}