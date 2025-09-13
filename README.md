# Hello Mongoose Example

This is a Maven project with a single runtime dependency on `com.telamin:mongoose`. It provides a minimal "Hello,
Mongoose" application showing how to:

- Define a simple event handler using Fluxtion’s ObjectEventHandlerNode
- Set up an in-memory event source
- Boot a Mongoose Server with one processor and one input feed
- Publish a couple of events and stop the server

The example’s main class:

- [HelloMongoose](src/main/java/com/telamin/mongoose/example/hellomongoose/HelloMongoose.java)

Mongoose maven dependency:

```xml

<dependencies>
    <dependency>
        <groupId>com.telamin</groupId>
        <artifactId>mongoose</artifactId>
        <version>${mongoose.version}</version>
    </dependency>
</dependencies>
```

## What it demonstrates

- Wiring business logic (an ObjectEventHandlerNode) into a Mongoose processor
- Using InMemoryEventSource to publish events programmatically
- Configuring agent execution and idle strategy via Mongoose builder APIs (EventProcessorGroupConfig, EventProcessorConfig, EventFeedConfig, MongooseServerConfig)

## Prerequisites

- Java 17+
- Maven 3.8+
- Access to the com.telamin:mongoose dependency (installed locally or available in your Maven repositories)
    - If you are developing alongside the Mongoose repo, run `mvn -q install` in the Mongoose project first to install
      it to your local repository, and ensure the version in this example’s pom.xml (<mongoose.version>) matches.

## Sample code

The sample below shows three pieces of configuration and how the server uses them:

- Handler (business logic): an ObjectEventHandlerNode that receives events on the processor thread and prints them.
- Feed (input source): an InMemoryEventSource<String> that we programmatically offer events to; with broadcast=true it
  delivers published events to all processors without explicit subscriptions.
- App (server config): a MongooseServerConfig that wires the handler into a named processor agent and registers the feed
  as a named event-source worker with its own agent and idle strategy. The bootServer(app, ...) call reads this config,
  starts the agents, connects the feed to the processor, and returns a running server instance.

```java
public final class HelloMongoose {

    public static void main(String[] args) {
        // 1) Business logic handler
        var handler = new ObjectEventHandlerNode() {
            @Override
            protected boolean handleEvent(Object event) {
                if (event instanceof String s) {
                    System.out.println("thread:'" + Thread.currentThread().getName() + "' Got event: " + s);
                }
                return true;
            }
        };

        // 2) Build in-memory feed
        var feed = new InMemoryEventSource<String>();

        // 3) Build and boot server with an in-memory feed and handler using builder APIs
        var processorGroup = EventProcessorGroupConfig.builder()
                .agentName("processor-agent")
                .put("hello-handler", EventProcessorConfig.builder()
                        .customHandler(handler)
                        .build())
                .build();

        var feedConfig = EventFeedConfig.<String>builder()
                .instance(feed)
                .name("hello-feed")
                .broadcast(true)
                .agent("feed-agent", new BusySpinIdleStrategy())
                .build();

        var app = MongooseServerConfig.builder()
                .addProcessorGroup(processorGroup)
                .addEventFeed(feedConfig)
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
```

How it boots and runs:

- EventProcessorGroupConfig.builder().agentName("processor-agent").put("hello-handler", ...) declares the processor group and adds your handler as a processor.
- EventFeedConfig.builder().instance(feed).name("hello-feed").broadcast(true).agent("feed-agent", new BusySpinIdleStrategy()) declares the in-memory feed and its agent/idle strategy.
- MongooseServerConfig.builder().addProcessorGroup(processorGroup).addEventFeed(feedConfig).build() assembles the app configuration.
- bootServer(app, rec -> { ... }) reads the app config, spins up the feed-agent and processor-agent threads, wires the feed to the processor (broadcast in this example), and returns a server handle. Offering to feed will then drive the handler on the processor-agent thread.

## Build

From this project directory:

- Build: `./mvnw -q package`

## Run

There are two common ways to run the example, both of which set the JUL log level to WARNING:

1) Via your IDE:

- Set the main class to `com.telamin.mongoose.example.hellomongoose.HelloMongoose`
- Run with the JVM vm option `-Djava.util.logging.config.file=src/main/resources/logging.properties`

2) Via the shaded JAR:

- Build: `./mvnw -q package`
- Run: `java -Djava.util.logging.config.file=./logging.properties -jar target/hellomongoose-1.0.0-SNAPSHOT-shaded.jar`

Expected output:

```
thread:'main' publishing events

thread:'processor-agent' Got event: hi
thread:'processor-agent' Got event: mongoose
```

## Notes

- Threading: This example intentionally prints the current thread name to the console so you can see where work happens.
  The main method publishes on thread 'main', while the processor runs on a dedicated agent thread (named '
  processor-agent' in this example), so the handler logs appear from that thread. This demonstrates the separation
  between event publication and processing.
- The main class configures a processor agent and a feed agent, both with a BusySpinIdleStrategy for very low latency.
  For general usage, consider a less CPU-intensive idle strategy.
- If the dependency `com.telamin:mongoose` is not found, ensure you have installed it locally (or use a published
  version) and that <mongoose.version> in pom.xml matches what’s available.

## Links

- Mongoose GitHub repository: https://github.com/telaminai/mongoose
- Mongoose project homepage:https://telaminai.github.io/mongoose/
- Example source in this project: `src/main/java/com/telamin/mongoose/example/hellomongoose/HelloMongoose.java`

## Configuring java.util.logging (JUL)

This example configures JUL using a file rather than programmatically. The config file is at:

- `src/main/resources/logging.properties`

Key settings:

- `.level=WARNING` sets the global default log level to WARNING (and above).
- `handlers=java.util.logging.ConsoleHandler` ensures logs go to the console.
- `java.util.logging.ConsoleHandler.level=WARNING` aligns the handler level.

To activate the file, pass `-Djava.util.logging.config.file=/path/to/logging.properties` when you run the app.
