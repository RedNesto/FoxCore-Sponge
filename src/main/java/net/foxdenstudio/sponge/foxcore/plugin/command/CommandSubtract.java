/*
 * This file is part of FoxCore, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.sponge.foxcore.plugin.command;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxcore.plugin.state.IStateField;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.WORLD_ALIASES;
import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public class CommandSubtract implements CommandCallable {

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).limit(1).parseLastFlags(false).parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        }
        Optional<IStateField> optField = FCStateManager.instance().getStateMap().get(source).getOrCreateFromAlias(parse.args[0]);
        if (!optField.isPresent())
            throw new CommandException(Text.of("\"" + parse.args[0] + "\" is not a valid category!"));
        IStateField field = optField.get();
        if (field == null) throw new CommandException(Text.of("\"" + parse.args[0] + "\" is not a valid category!"));
        String extraArgs = "";
        if (parse.args.length > 1) extraArgs = parse.args[1];
        ProcessResult result = field.subtract(source, extraArgs);
        if (result.isSuccess()) {
            if (result.getMessage().isPresent()) {
                if (!FCHelper.hasColor(result.getMessage().get())) {
                    source.sendMessage(result.getMessage().get().toBuilder().color(TextColors.GREEN).build());
                } else {
                    source.sendMessage(result.getMessage().get());
                }
            } else {
                source.sendMessage(Text.of(TextColors.GREEN, "Successfully removed data from the " + field.getName() + " field!"));
            }
        } else {
            if (result.getMessage().isPresent()) {
                if (!FCHelper.hasColor(result.getMessage().get())) {
                    source.sendMessage(result.getMessage().get().toBuilder().color(TextColors.RED).build());
                } else {
                    source.sendMessage(result.getMessage().get());
                }
            } else {
                source.sendMessage(Text.of(TextColors.RED, "Failed to remove data from the " + field.getName() + " field!"));
            }
        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .limit(1)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parseLastFlags(false)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return FCStateManager.instance().getPrimaryAliases().stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.FINAL)) {
            Optional<IStateField> optField = FCStateManager.instance().getStateMap().get(source).getOrCreateFromAlias(parse.args[0]);
            if (!optField.isPresent()) return ImmutableList.of();
            IStateField field = optField.get();
            String extraArgs = "";
            if (parse.args.length > 1) extraArgs = parse.args[1];
            return field.subtractSuggestions(source, extraArgs);
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxcore.command.state.subtract");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("subtract <field> [args...]");
    }
}
