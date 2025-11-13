package com.frog.common.security.config;

/**
 *
 *
 * @author Deng
 * createData 2025/11/10 10:39
 * @version 1.0
 */
public interface PkceChallengeStore {

    void save(String authorizationUri, String codeChallenge);

    String load(String authorizationUri);

    void remove(String authorizationUri);
}

