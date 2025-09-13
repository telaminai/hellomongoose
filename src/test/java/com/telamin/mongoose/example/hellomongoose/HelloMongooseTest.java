package com.telamin.mongoose.example.hellomongoose;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloMongooseTest {

    @Test
    void main_increments_static_count_for_two_events() {
        // reset any prior state
        HelloMongoose.resetCount();

        // Run main (publishes two events and stops the server)
        HelloMongoose.main(new String[0]);

        // Wait briefly for the processor thread to handle events (robust across environments)
        long deadline = System.currentTimeMillis() + 1_000; // up to 1s
        while (HelloMongoose.getCount() < 2 && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                // ignore and continue waiting until deadline or condition met
            }
        }

        // In this example, two String events are offered: "hi" and "mongoose"
        assertEquals(2, HelloMongoose.getCount(), "Expected two processed events within timeout");
    }
}
