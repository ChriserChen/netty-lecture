package com.example;

import com.example.server.MarketSocketClient;

public class Main {

    public static void main(String[] args) throws Exception {
        new MarketSocketClient().run();
    }
}
