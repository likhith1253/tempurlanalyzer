package com.sentinelai.guard.service.impl;

import com.sentinelai.guard.model.Alert;
import com.sentinelai.guard.model.DomainAnalysis;
import com.sentinelai.guard.model.Report;
import com.sentinelai.guard.model.User;
import com.sentinelai.guard.repository.SentinelFirestoreClient;
import com.sentinelai.guard.service.FirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirestoreServiceImpl implements FirestoreService {

    private final SentinelFirestoreClient firestoreClient;

    @Override
    public Optional<DomainAnalysis> findDomainById(String id) {
        return firestoreClient.findDomainById(id);
    }

    @Override
    public Optional<DomainAnalysis> findDomainByHost(String host) {
        return firestoreClient.findDomainByHost(host);
    }

    @Override
    public DomainAnalysis saveDomain(DomainAnalysis domain) {
        return firestoreClient.saveDomain(domain);
    }

    @Override
    public Alert saveAlert(Alert alert) {
        return firestoreClient.saveAlert(alert);
    }

    @Override
    public List<Alert> findAlertsByDomainId(String domainId, int limit) {
        return firestoreClient.findAlertsByDomainId(domainId, limit);
    }

    @Override
    public Optional<User> findUserById(String userId) {
        return firestoreClient.findUserById(userId);
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return firestoreClient.findUserByEmail(email);
    }

    @Override
    public User saveUser(User user) {
        return firestoreClient.saveUser(user);
    }

    @Override
    public Report saveReport(Report report) {
        log.info("Saving report with ID: {}", report.getReportId());
        return firestoreClient.saveReport(report);
    }

    @Override
    public void clearCaches() {
        firestoreClient.clearCaches();
    }
}
