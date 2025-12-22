package com.codix.tools;

public class SufferingTheme {
    private String name;
    private double tension;
    private int ticketCount;
    private double extraResources;

    public SufferingTheme(String name, double tension, int ticketCount, double extraResources) {
        this.name = name;
        this.tension = tension;
        this.ticketCount = ticketCount;
        this.extraResources = extraResources;
    }

    public String getName() { return name; }
    public double getTension() { return tension; }
    public int getTicketCount() { return ticketCount; }
    public double getExtraResources() { return extraResources; }
}