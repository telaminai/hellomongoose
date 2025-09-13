package com.telamin.mongoose.example.hellomongoose;

import com.fluxtion.agrona.concurrent.BusySpinIdleStrategy;
import com.fluxtion.runtime.node.ObjectEventHandlerNode;
import com.telamin.mongoose.config.EventFeedConfig;
import com.telamin.mongoose.config.EventProcessorConfig;
import com.telamin.mongoose.config.MongooseServerConfig;
import com.telamin.mongoose.config.ThreadConfig;
import com.telamin.mongoose.connector.memory.InMemoryEventSource;

import java.util.concurrent.atomic.AtomicInteger;

import static com.telamin.mongoose.MongooseServer.bootServer;

public final class HelloMongoose {

    public static final AtomicInteger COUNT = new AtomicInteger(0);

    public static void resetCount() {
        COUNT.set(0);
    }

    public static int getCount() {
        return COUNT.get();
    }

    public static void main(String[] args) {
        // 1) Business logic handler
        var handler = new ObjectEventHandlerNode() {
            @Override
            protected boolean handleEvent(Object event) {
                if (event instanceof String s) {
                    COUNT.incrementAndGet();
                    System.out.println("thread:'" + Thread.currentThread().getName() + "' Got event: " + s);
                }
                return true;
            }
        };

        // 2) Build in-memory feed
        var feed = new InMemoryEventSource<String>();

        // 3) Build and boot server with an in-memory feed and handler using builder APIs
        var eventProcessorConfig = EventProcessorConfig.builder()
                .customHandler(handler)
                .build();

        var feedConfig = EventFeedConfig.<String>builder()
                .instance(feed)
                .name("hello-feed")
                .broadcast(true)
                .agent("feed-agent", new BusySpinIdleStrategy())
                .build();

        var threadConfig = ThreadConfig.builder()
                .agentName("processor-agent")
                .idleStrategy(new BusySpinIdleStrategy())
                .build();

        var app = MongooseServerConfig.builder()
                .addProcessor("processor-agent", "hello-handler", eventProcessorConfig)
                .addEventFeed(feedConfig)
                .addThread(threadConfig)
                .build();

        var server = bootServer(app, rec -> { /* optional log listener */ });

        // 4) Publish a few events
        System.out.println("thread:'" + Thread.currentThread().getName() + "' publishing events\n");
        feed.offer("hi");
        feed.offer("mongoose");

        // 5) Cleanup (in a real app, keep running)
        server.stop();
    }
}
