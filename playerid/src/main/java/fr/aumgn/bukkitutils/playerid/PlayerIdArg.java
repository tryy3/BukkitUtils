package fr.aumgn.bukkitutils.playerid;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.aumgn.bukkitutils.command.arg.CommandArg;
import fr.aumgn.bukkitutils.command.arg.CommandArgFactory;
import fr.aumgn.bukkitutils.command.exception.CommandUsageError;
import fr.aumgn.bukkitutils.command.messages.Messages;

public class PlayerIdArg extends CommandArg<PlayerId> {

    public static class Factory extends CommandArgFactory<PlayerId> {

        static {
            CommandArgFactory.register(PlayerId.class, new Factory());
        }

        @Override
        public PlayerIdArg createCommandArg(Messages messages, String string) {
            return new PlayerIdArg(messages, string);
        }
    }

    public PlayerIdArg(Messages messages, String string) {
        super(messages, string);
    }

    @Override
    public PlayerId value() {
        return PlayerId.get(string);
    }

    public PlayerId value(CommandSender sender) {
        if (string != null) {
            return value();
        }

        if (!(sender instanceof Player)) {
            throw new CommandUsageError(messages.playerNeeded());
        }

        return PlayerId.get(((Player)sender).getName());
    }
}