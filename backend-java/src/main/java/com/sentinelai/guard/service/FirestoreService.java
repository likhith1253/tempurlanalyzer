package com.sentinelai.guard.service;

import com.sentinelai.guard.model.Alert;
import com.sentinelai.guard.model.DomainAnalysis;
import com.sentinelai.guard.model.Report;
import com.sentinelai.guard.model.User;

import java.util.List;
import java.util.Optional;

public interface FirestoreService {
    // Domain Analysis Methods
    Optional<DomainAnalysis> findDomainById(String id);
    Optional<DomainAnalysis> findDomainByHost(String host);
    DomainAnalysis saveDomain(DomainAnalysis domain);
    
    // Alert Methods
    Alert saveAlert(Alert alert);
    List<Alert> findAlertsByDomainId(String domainId, int limit);
    
    // User Methods
    Optional<User> findUserById(String userId);
    Optional<User> findUserByEmail(String email);
    User saveUser(User user);
    
    // Report Methods
    Report saveReport(Report report);
    
    // Utility Methods
    void clearCaches();
}
