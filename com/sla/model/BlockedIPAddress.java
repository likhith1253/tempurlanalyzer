package com.sla.model;

public class BlockedIPAddress implements Comparable<BlockedIPAddress> {
    public final String ipAddress;
    
    public BlockedIPAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    @Override
    public int compareTo(BlockedIPAddress other) {
        return this.ipAddress.compareTo(other.ipAddress);
    }
}
