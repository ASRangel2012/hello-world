package com.example.helloworld.client;

/**
 * Abstraction over the (external) quote provider so the transport can be
 * swapped between the simulated and HTTP implementations via configuration.
 */
public interface QuoteClient {

    String quoteOfTheDay();
}
