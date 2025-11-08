package com.sla.model;

public class Geolocation {
    public final String prefix;
    public final String country;
    public final String city;
    
    public Geolocation(String prefix, String country, String city) {
        this.prefix = prefix;
        this.country = country;
        this.city = city;
    }
    
    @Override
    public String toString() {
        return city + ", " + country;
    }
}
