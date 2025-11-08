package com.sla.service;

import com.sla.model.Role;
import com.sla.model.User;

public class UserAccessControl {
    private final User user;
    
    public UserAccessControl(User user) {
        this.user = user;
    }
    
    public boolean canViewFullData() {
        return user.role == Role.ADMIN || user.role == Role.INVESTIGATOR;
    }
    
    public String redactIPAddress(String ipAddress) {
        if (canViewFullData() || ipAddress == null) {
            return ipAddress;
        }
        
        int lastDot = ipAddress.lastIndexOf('.');
        return (lastDot != -1) ? ipAddress.substring(0, lastDot) + ".XXX" : "REDACTED";
    }
}
