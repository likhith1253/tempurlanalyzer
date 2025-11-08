package com.sentinelai.guard.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class FirebaseUserDetailsService implements UserDetailsService {

    private final FirebaseAuth firebaseAuth;

    public FirebaseUserDetailsService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public UserDetails loadUserByUsername(String uid) throws UsernameNotFoundException {
        try {
            UserRecord userRecord = firebaseAuth.getUser(uid);
            return new User(
                userRecord.getUid(),
                "", // Firebase doesn't expose password
                Collections.emptyList()
            );
        } catch (FirebaseAuthException e) {
            throw new UsernameNotFoundException("User not found with uid: " + uid, e);
        }
    }
}
