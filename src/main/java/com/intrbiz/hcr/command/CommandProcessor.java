package com.intrbiz.hcr.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.codahale.metrics.Meter;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.intrbiz.gerald.witchcraft.Witchcraft;
import com.intrbiz.hcr.commands.CommandCommand;
import com.intrbiz.hcr.commands.DelCommand;
import com.intrbiz.hcr.commands.GetCommand;
import com.intrbiz.hcr.commands.InfoCommand;
import com.intrbiz.hcr.commands.KeysCommand;
import com.intrbiz.hcr.commands.PingCommand;
import com.intrbiz.hcr.commands.QuitCommand;
import com.intrbiz.hcr.commands.SetCommand;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;

public class CommandProcessor
{
    // metrics
    
    private static Meter commandUnknownRate = Witchcraft.get().source("hcr").getRegistry().meter("command.unknown.rate");
    
    //
    
    private Map<String, Command> commands = new HashMap<String, Command>();
    
    private final HazelcastInstance hazelcastInstance;
    
    private final IMap<String, String> dataMap;
    
    private final IExecutorService executor;
    
    private final IAtomicLong clientCount;
    
    public CommandProcessor(HazelcastInstance hazelcastInstance)
    {
        super();
        this.hazelcastInstance = hazelcastInstance;
        // setup our data map
        this.dataMap = this.hazelcastInstance.getMap("hcr.data");
        this.executor = this.hazelcastInstance.getExecutorService("hcr.executor");
        this.clientCount = this.hazelcastInstance.getAtomicLong("hcr.client_count");
        // register default commands
        this.registerCommand(new CommandCommand());
        this.registerCommand(new InfoCommand());
        this.registerCommand(new PingCommand());
        this.registerCommand(new QuitCommand());
        this.registerCommand(new GetCommand());
        this.registerCommand(new SetCommand());
        this.registerCommand(new KeysCommand());
        this.registerCommand(new DelCommand());
    }
    
    public void registerCommand(Command command)
    {
        this.commands.put(command.getName().toLowerCase(), command);
    }
    
    public void process(ChannelHandlerContext ctx, String commandName, List<RedisMessage> arguments)
    {
        commandName = commandName.toLowerCase();
        CommandContext cmdCtx = new CommandContext(this, ctx, commandName, arguments);
        Command commandHandler = this.commands.get(commandName);
        if (commandHandler != null)
        {
            commandHandler.execute(cmdCtx);
        }
        else
        {
            commandUnknownRate.mark();
            cmdCtx.writeErrorAndClose("No such command: " + commandName);
        }
    }
    
    public ArrayRedisMessage describe()
    {
        return new ArrayRedisMessage(this.commands.values().stream()
                .map((c) -> c.describe())
                .collect(Collectors.toList())
        );
    }
    
    public HazelcastInstance getHazelcast()
    {
        return this.hazelcastInstance;
    }
    
    public IExecutorService getExecutor()
    {
        return executor;
    }

    public IMap<String, String> getData()
    {
        return this.dataMap;
    }
    
    public long getClientCount()
    {
        return this.clientCount.get();
    }
    
    public void clientConnected()
    {
        this.clientCount.incrementAndGetAsync();
    }
    
    public void clientDisconnected()
    {
        this.clientCount.decrementAndGetAsync();
    }
}
