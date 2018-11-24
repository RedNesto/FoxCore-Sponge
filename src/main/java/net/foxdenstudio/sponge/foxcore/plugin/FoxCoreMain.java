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

package net.foxdenstudio.sponge.foxcore.plugin;

import com.google.inject.Inject;
import net.foxdenstudio.sponge.foxcore.common.network.server.packet.ServerPositionPacket;
import net.foxdenstudio.sponge.foxcore.common.network.server.packet.ServerPrintStringPacket;
import net.foxdenstudio.sponge.foxcore.plugin.command.*;
import net.foxdenstudio.sponge.foxcore.plugin.command.misc.CommandPWD;
import net.foxdenstudio.sponge.foxcore.plugin.command.misc.CommandWhat;
import net.foxdenstudio.sponge.foxcore.plugin.command.misc.CommandWho;
import net.foxdenstudio.sponge.foxcore.plugin.listener.WandBlockListener;
import net.foxdenstudio.sponge.foxcore.plugin.listener.WandEntityListener;
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxcore.plugin.state.PositionStateField;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxcore.plugin.wand.FCWandRegistry;
import net.foxdenstudio.sponge.foxcore.plugin.wand.data.ImmutableWandData;
import net.foxdenstudio.sponge.foxcore.plugin.wand.data.WandData;
import net.foxdenstudio.sponge.foxcore.plugin.wand.data.WandDataBuilder;
import net.foxdenstudio.sponge.foxcore.plugin.wand.data.WandKeys;
import net.foxdenstudio.sponge.foxcore.plugin.wand.types.CounterWand;
import net.foxdenstudio.sponge.foxcore.plugin.wand.types.PositionWand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Plugin(id = "foxcore",
        name = "FoxCore",
        description = "Core plugin for Fox plugins. This plugin also contains some core functionality that other plugins may wish to use.",
        authors = {"gravityfox"},
        url = "https://github.com/FoxDenStudio/FoxCore"
)
public final class FoxCoreMain {

    private static final UUID FOX_UUID = UUID.fromString("f275f223-1643-4fac-9fb8-44aaf5b4b371");
    private static final String FOX_APPENDER_NAME = "FoxFile";
    private static final String FOX_LOGGER_CONFIG_NAME = "fox";
    private static final String FOXCORE_LOGGER_NAME = "fox.core";
    private static FoxCoreMain instance;

    private Logger logger = LoggerFactory.getLogger(FOXCORE_LOGGER_NAME);

    @Inject
    private Game game;

    @Inject
    private EventManager eventManager;

    @Inject
    @ConfigDir(sharedRoot = true)
    private Path configDirectory;

    private Path foxLogDirectory = Paths.get("logs", "fox");

    @Inject
    private PluginContainer container;

    private FCCommandDispatcher fcDispatcher;
    private FCServerNetworkManager.ServerChannel foxcoreNetworkChannel;

    public static FoxCoreMain instance() {
        return instance;
    }

    @Listener
    public void construct(GameConstructionEvent event) {
        instance = this;
    }

    @Listener
    public void preInit(GamePreInitializationEvent event) {
        //logger.info("Injecting fox logger");
        //setupLogging();

        // Loads WandKeys and builds its Keys when the PluginContainer is on the CauseStack,
        // otherwise people are getting kicked when joining the server if they have a wand in their inventories
        // since Keys are built when no PluginContainer is on the CauseStack in this case.
        // For some reason it started happening around the same time they changed the versioning scheme.
        WandKeys.ID.getId();

        logger.info("Beginning FoxCore initialization");
        logger.info("Version: " + container.getVersion().orElse("Unknown"));

        logger.info("Initializing state manager");
        FCStateManager.init();

        logger.info("Initializing network packet manager");
        FCServerNetworkManager.instance();

        logger.info("Configuring commands");
        configureCommands();
    }

    // This code doesn't work for Log4J 2.0-beta9
    // Will be used in MC 1.12.2 where Log4J is updated.
    /*private void setupLogging() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        String foxLogFile = foxLogDirectory.resolve("fox-latest.log").toString();
        String foxLogPattern = foxLogDirectory.toString() + "/fox-%i.log";
        PatternLayout layout = PatternLayout.createLayout("[%d{HH:mm:ss}] [%t/%level] [%logger]: %msg%n", config, null, null, null);
        Appender appender = RollingRandomAccessFileAppender.createAppender(foxLogFile, foxLogPattern, null, FOX_APPENDER_NAME, null,
                OnStartupTriggeringPolicy.createPolicy(),
                DefaultRolloverStrategy.createStrategy("3", null, "max", "0", config),
                layout, null, "true", "false", null, config);
        appender.start();
        config.getAppenders().putIfAbsent(appender.getName(), appender);
        AppenderRef ref = AppenderRef.createAppenderRef(appender.getName(), null, null);
        AppenderRef[] refs = {ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger("true", "all", FOX_LOGGER_NAME, null, refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.getLoggers().putIfAbsent(loggerConfig.getName(), loggerConfig);
        ctx.updateLoggers(config);
    }*/

    @Listener
    public void init(GameInitializationEvent event) {
        logger.info("Registering positions state field");
        FCStateManager.instance().registerStateFactory(new PositionStateField.Factory(), PositionStateField.ID, PositionStateField.ID, Aliases.POSITIONS_ALIASES);

        logger.info("Registering wand factories");
        registerWands();

        logger.info("Registering commands");
        game.getCommandManager().register(this, fcDispatcher, "foxcore", "foxc", "fcommon", "fc");
    }

    @Listener
    public void setupNetworking(GameInitializationEvent event){
        logger.info("Starting network packet manager");
        FCServerNetworkManager.instance().registerNetworkingChannels();
        logger.info("Creating server network channel");
        foxcoreNetworkChannel = FCServerNetworkManager.instance().getOrCreateServerChannel("foxcore");
        logger.info("Registering packet listeners");
        registerPackets();
    }

    @Listener
    public void registerListeners(GameInitializationEvent event) {
        logger.info("Registering event listeners");
        EventManager manager = game.getEventManager();
        try {
            manager.registerListeners(this, FCServerNetworkManager.instance());
        } catch (Exception e) {
            logger.error("Error registering Network Manager Listeners", e);
        }
        try {
            manager.registerListener(this, InteractBlockEvent.class, Order.FIRST, new WandBlockListener());
        } catch (Exception e) {
            logger.error("Error registering Wand Block Listener", e);
        }
        try {
            manager.registerListener(this, InteractEntityEvent.class, Order.FIRST, new WandEntityListener());
        } catch (Exception e) {
            logger.error("Error registering Wand Entity Listener", e);
        }
    }

    @Listener
    public void registerData(GameInitializationEvent event) {
        logger.info("Registering custom data manipulators");
        DataRegistration.builder()
                .dataClass(WandData.class)
                .immutableClass(ImmutableWandData.class)
                .builder(new WandDataBuilder())
                .manipulatorId("wanddata")
                .dataName("FoxCoreWandData")
                .buildAndRegister(this.container);
    }

    @Listener
    public void configurePermissions(GamePostInitializationEvent event) {
        logger.info("Configuring permissions");
        game.getServiceManager().provide(PermissionService.class).get().getDefaults()
                .getTransientSubjectData().setPermission(SubjectData.GLOBAL_CONTEXT, "foxcore.command.info", Tristate.TRUE);
    }

    private void configureCommands() {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "FoxCore\n"));
        builder.append(Text.of("Version: " + container.getVersion().orElse("Unknown") + "\n"));
        builder.append(Text.of("Author: gravityfox\n"));

        this.fcDispatcher = new FCCommandDispatcher("/foxcore", "Core commands for state and selections.");
        fcDispatcher.register(new CommandCurrent(), "current", "cur", "c");
        fcDispatcher.register(new CommandState(), "state", "buffer", "set", "s");
        fcDispatcher.register(new CommandPosition(), "position", "pos", "p");
        fcDispatcher.register(new CommandFlush(), "flush", "clear", "wipe", "f");
        fcDispatcher.register(new CommandWand(), "wand", "tool", "stick", "w");
        //fcDispatcher.register(new CommandTest(), "test");
        fcDispatcher.register(new CommandDebug(), "debug");
        fcDispatcher.register(new CommandHUD(), "hud", "scoreboard");

        fcDispatcher.register(new CommandAbout(builder.build()), "about", "info");

        FCCommandDispatcher miscDispatcher = new FCCommandDispatcher("/foxcore misc", "Misc commands that may be helpful.");
        miscDispatcher.register(new CommandPWD(), "pwd", "directory", "dir");
        miscDispatcher.register(new CommandWho(), "who", "plugin");
        miscDispatcher.register(new CommandWhat(), "what", "command");

        fcDispatcher.register(miscDispatcher, "misc", "miscellaneous", "util");
    }

    private void registerPackets() {
        FCServerNetworkManager manager = FCServerNetworkManager.instance();
        manager.registerPacket(ServerPositionPacket.ID);
        manager.registerPacket(ServerPrintStringPacket.ID);
    }

    private void registerWands() {
        FCWandRegistry registry = FCWandRegistry.getInstance();
        registry.registerBuilder(PositionWand.TYPE, new PositionWand.Factory());
        registry.registerBuilder(CounterWand.type, new CounterWand.Factory());
    }

    public Logger logger() {
        return logger;
    }

    public Game game() {
        return game;
    }

    public FCCommandDispatcher getFCDispatcher() {
        return fcDispatcher;
    }

    public FCServerNetworkManager.ServerChannel getFoxcoreNetworkChannel() {
        return foxcoreNetworkChannel;
    }

    @Listener
    public void playerJoin(ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();
        if (player.getUniqueId().equals(FOX_UUID)) {
            logger.info("A code fox has slipped into the server.");
        }
        FCServerNetworkManager manager = FCServerNetworkManager.instance();
        manager.negotiateHandshake(player);
    }

    public Path getConfigDirectory() {
        return configDirectory;
    }

    public Path getLogDirectory() {
        return foxLogDirectory;
    }

    public PluginContainer getContainer() {
        return container;
    }
}
