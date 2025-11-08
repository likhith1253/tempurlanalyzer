package com.sentinelai.guard.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sentinelai.guard.model.Alert;
import com.sentinelai.guard.model.DomainAnalysis;
import com.sentinelai.guard.model.Report;
import com.sentinelai.guard.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class SentinelFirestoreClient {

    private static final String COLLECTION_DOMAINS = "domains";
    private static final String COLLECTION_ALERTS = "alerts";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_REPORTS = "reports";

    private final com.google.cloud.firestore.Firestore firestore;
    private final Cache<String, DomainAnalysis> domainCache;
    private final Cache<String, User> userCache;
    private final boolean cacheEnabled;

    public SentinelFirestoreClient(
            @Value("${firestore.cache.enabled:true}") boolean cacheEnabled,
            @Value("${firestore.cache.maxSize:1000}") int cacheMaxSize,
            @Value("${firestore.cache.expireAfterWrite:300}") int cacheExpireAfterWrite) {
        
        try {
            this.firestore = FirestoreClient.getFirestore();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Firestore client", e);
        }
        this.cacheEnabled = cacheEnabled;
        
        // Initialize Caffeine cache
        this.domainCache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheExpireAfterWrite, TimeUnit.SECONDS)
                .build();
                
        this.userCache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheExpireAfterWrite, TimeUnit.SECONDS)
                .build();
    }

    // ===== Domain Analysis Methods =====

    public Optional<DomainAnalysis> findDomainById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }

        // Try cache first
        if (cacheEnabled) {
            DomainAnalysis cached = domainCache.getIfPresent(id);
            if (cached != null) {
                return Optional.of(cached);
            }
        }

        try {
            DocumentSnapshot document = firestore.collection(COLLECTION_DOMAINS).document(id).get().get();
            if (document.exists()) {
                DomainAnalysis domain = document.toObject(DomainAnalysis.class);
                if (domain != null) {
                    domain.setId(document.getId());
                    if (cacheEnabled) {
                        domainCache.put(id, domain);
                    }
                    return Optional.of(domain);
                }
            }
        } catch (Exception e) {
            log.error("Error finding domain by ID: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<DomainAnalysis> findDomainByHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            return Optional.empty();
        }

        // Normalize host
        host = host.toLowerCase().trim();
        
        // Try cache first
        if (cacheEnabled) {
            final String normalizedHost = host.toLowerCase().trim();
            Optional<DomainAnalysis> cached = domainCache.asMap().values().stream()
                    .filter(d -> normalizedHost.equals(d.getHost().toLowerCase()))
                    .findFirst();
            if (cached.isPresent()) {
                return cached;
            }
        }

        try {
            Query query = firestore.collection(COLLECTION_DOMAINS)
                    .whereEqualTo("host", host)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(1);

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            
            if (!documents.isEmpty()) {
                DomainAnalysis domain = documents.get(0).toObject(DomainAnalysis.class);
                if (domain != null) {
                    domain.setId(documents.get(0).getId());
                    if (cacheEnabled) {
                        domainCache.put(domain.getId(), domain);
                    }
                    return Optional.of(domain);
                }
            }
        } catch (Exception e) {
            log.error("Error finding domain by host: " + host, e);
        }
        return Optional.empty();
    }

    public DomainAnalysis saveDomain(DomainAnalysis domain) {
        if (domain == null) {
            throw new IllegalArgumentException("Domain cannot be null");
        }

        try {
            domain.setUpdatedNow();
            DocumentReference docRef;
            
            if (domain.getId() == null || domain.getId().isEmpty()) {
                // New document
                docRef = firestore.collection(COLLECTION_DOMAINS).document();
                domain.setId(docRef.getId());
            } else {
                // Existing document
                docRef = firestore.collection(COLLECTION_DOMAINS).document(domain.getId());
            }
            
            // Convert to map and save
            Map<String, Object> data = domain.toMap();
            docRef.set(data).get();
            
            // Update cache
            if (cacheEnabled) {
                domainCache.put(domain.getId(), domain);
            }
            
            return domain;
        } catch (Exception e) {
            log.error("Error saving domain: " + domain.getHost(), e);
            throw new RuntimeException("Failed to save domain", e);
        }
    }

    // ===== Alert Methods =====

    public Alert saveAlert(Alert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("Alert cannot be null");
        }

        try {
            alert.setUpdatedNow();
            DocumentReference docRef;
            
            if (alert.getId() == null || alert.getId().isEmpty()) {
                // New alert
                docRef = firestore.collection(COLLECTION_ALERTS).document();
                alert.setId(docRef.getId());
            } else {
                // Existing alert
                docRef = firestore.collection(COLLECTION_ALERTS).document(alert.getId());
            }
            
            // Convert to map and save
            Map<String, Object> data = alert.toMap();
            docRef.set(data).get();
            
            return alert;
        } catch (Exception e) {
            log.error("Error saving alert", e);
            throw new RuntimeException("Failed to save alert", e);
        }
    }

    public List<Alert> findAlertsByDomainId(String domainId, int limit) {
        if (domainId == null || domainId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Query query = firestore.collection(COLLECTION_ALERTS)
                    .whereEqualTo("domainId", domainId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit);

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            return querySnapshot.get().getDocuments().stream()
                    .map(doc -> {
                        Alert alert = doc.toObject(Alert.class);
                        alert.setId(doc.getId());
                        return alert;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding alerts for domain: " + domainId, e);
            return Collections.emptyList();
        }
    }

    // ===== User Methods =====

    public Optional<User> findUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Optional.empty();
        }

        // Try cache first
        if (cacheEnabled) {
            User cached = userCache.getIfPresent(userId);
            if (cached != null) {
                return Optional.of(cached);
            }
        }

        try {
            DocumentSnapshot document = firestore.collection(COLLECTION_USERS).document(userId).get().get();
            if (document.exists()) {
                User user = document.toObject(User.class);
                if (user != null) {
                    user.setId(document.getId());
                    if (cacheEnabled) {
                        userCache.put(userId, user);
                    }
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            log.error("Error finding user by ID: " + userId, e);
        }
        return Optional.empty();
    }

    public Optional<User> findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        // Try cache first
        if (cacheEnabled) {
            Optional<User> cached = userCache.asMap().values().stream()
                    .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                    .findFirst();
            if (cached.isPresent()) {
                return cached;
            }
        }

        try {
            Query query = firestore.collection(COLLECTION_USERS)
                    .whereEqualTo("email", email.toLowerCase().trim())
                    .limit(1);

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            
            if (!documents.isEmpty()) {
                User user = documents.get(0).toObject(User.class);
                if (user != null) {
                    user.setId(documents.get(0).getId());
                    if (cacheEnabled) {
                        userCache.put(user.getId(), user);
                    }
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            log.error("Error finding user by email: " + email, e);
        }
        return Optional.empty();
    }

    public User saveUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        try {
            user.setUpdatedNow();
            DocumentReference docRef;
            
            if (user.getId() == null || user.getId().isEmpty()) {
                // New user
                docRef = firestore.collection(COLLECTION_USERS).document();
                user.setId(docRef.getId());
            } else {
                // Existing user
                docRef = firestore.collection(COLLECTION_USERS).document(user.getId());
            }
            
            // Convert to map and save
            Map<String, Object> data = user.toMap();
            docRef.set(data).get();
            
            // Update cache
            if (cacheEnabled) {
                userCache.put(user.getId(), user);
            }
            
            return user;
        } catch (Exception e) {
            log.error("Error saving user: " + (user != null ? user.getEmail() : "null"), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }
    
    /**
     * Clears all caches maintained by this client
     */
    public Report saveReport(Report report) {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }

        try {
            report.setUpdatedNow();
            DocumentReference docRef;
            
            if (report.getId() == null || report.getId().isEmpty()) {
                // New report
                docRef = firestore.collection(COLLECTION_REPORTS).document();
                report.setId(docRef.getId());
            } else {
                // Existing report
                docRef = firestore.collection(COLLECTION_REPORTS).document(report.getId());
            }
            
            // Convert to map and save
            Map<String, Object> data = report.toMap();
            docRef.set(data).get();
            
            return report;
        } catch (Exception e) {
            log.error("Error saving report", e);
            throw new RuntimeException("Failed to save report", e);
        }
    }

    /**
     * Clears all caches maintained by this client
     */
    public void clearCaches() {
        if (cacheEnabled) {
            domainCache.invalidateAll();
            userCache.invalidateAll();
            log.debug("Cleared all caches");
        }
    }}
