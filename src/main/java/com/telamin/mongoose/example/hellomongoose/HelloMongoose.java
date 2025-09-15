package com.telamin.mongoose.example.hellomongoose;

import com.fluxtion.agrona.concurrent.BusySpinIdleStrategy;
import com.telamin.mongoose.MongooseServer;
import com.telamin.mongoose.config.EventFeedConfig;
import com.telamin.mongoose.config.EventProcessorConfig;
import com.telamin.mongoose.config.MongooseServerConfig;
import com.telamin.mongoose.config.ThreadConfig;
import com.telamin.mongoose.connector.memory.InMemoryEventSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
        Consumer<Object> handler = event -> System.out.println("Got event: " + event);

        // 2) Build in-memory feed
        var feed = new InMemoryEventSource<String>();

        // 3) Build and boot mongoose server with an in-memory feed and handler using builder APIs
        var eventProcessorConfig = EventProcessorConfig.builder()
                .handlerFunction(handler)
                .name("hello-handler")
                .build();

        var feedConfig = EventFeedConfig.<String>builder()
                .instance(feed)
                .name("hello-feed")
                .broadcast(true)
                .agent("feed-agent", new BusySpinIdleStrategy())
                .build();

        var app = MongooseServerConfig.builder()
                .addProcessor("processor-agent", eventProcessorConfig)
                .addEventFeed(feedConfig)
                .build();

        var server = MongooseServer.bootServer(app);

        // 4) Publish a few events
        System.out.println("thread:'" + Thread.currentThread().getName() + "' publishing events\n");
        feed.offer("hi");
        feed.offer("mongoose");

        // 5) Cleanup (in a real app, keep running)
        server.stop();
    }
}
