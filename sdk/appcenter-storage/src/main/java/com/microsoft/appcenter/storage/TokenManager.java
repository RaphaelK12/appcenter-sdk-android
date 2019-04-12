/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;

import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.HashSet;
import java.util.Set;

import static com.microsoft.appcenter.storage.Constants.LOG_TAG;
import static com.microsoft.appcenter.storage.Constants.PREFERENCE_PARTITION_NAMES;
import static com.microsoft.appcenter.storage.Constants.PREFERENCE_PARTITION_PREFIX;

/**
 * Token cache service.
 */
public class TokenManager {

    /**
     * Shared token manager instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static TokenManager sInstance;

    private final Context mContext;

    private TokenManager(Context context) {
        mContext = context;
    }

    /**
     * Get token manager instance.
     *
     * @return Shared token manager instance.
     */
    public static TokenManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TokenManager(context);
        }
        return sInstance;
    }

    /**
     * List all cached tokens' partition names.
     *
     * @return Set of cached tokens' partition name.
     */
    private Set<String> getPartitionNames() {
        Set<String> partitionNames = SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES);
        return partitionNames == null ? new HashSet<String>() : partitionNames;
    }

    /**
     * Get the cached token access to given partition.
     *
     * @param partitionName The partition name for get the token.
     * @return Cached token.
     */
    TokenResult getCachedToken(String partitionName) {
        return getCachedToken(partitionName, false);
    }

    TokenResult getCachedToken(String partitionName, boolean includeExpiredToken) {
        String encryptedTokenResult = SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + partitionName);
        String decryptedTokenResult = CryptoUtils.getInstance(mContext).decrypt(encryptedTokenResult, false).getDecryptedData();
        TokenResult token = Utils.getGson().fromJson(decryptedTokenResult, TokenResult.class);
        if (token != null) {
            if (!includeExpiredToken) {

                /* If the token is expired. */
                if (System.currentTimeMillis() >= token.expiresOn().getTime()) {
                    AppCenterLog.warn(LOG_TAG, String.format("Cached token result is expired for partition '%s'", partitionName));
                    return null;
                }
            }
            AppCenterLog.debug(LOG_TAG, String.format("Retrieved token from cache for partition '%s'", partitionName));
            return token;
        }
        AppCenterLog.warn(LOG_TAG, String.format("Failed to retrieve token or none found in cache for partition '%s'", partitionName));
        return null;
    }

    /**
     * Set the token to cache.
     *
     * @param tokenResult The token to be cached.
     */
    public synchronized void setCachedToken(TokenResult tokenResult) {
        Set<String> partitionNamesSet = getPartitionNames();
        String removedAccountIdPartition = Utils.removeAccountIdFromPartitionName(tokenResult.partition());
        if (!partitionNamesSet.contains(removedAccountIdPartition)) {
            partitionNamesSet.add(removedAccountIdPartition);
            SharedPreferencesManager.putStringSet(PREFERENCE_PARTITION_NAMES, partitionNamesSet);
        }
        String strTokenResult = Utils.getGson().toJson(tokenResult);
        String encryptedTokenResult = CryptoUtils.getInstance(mContext).encrypt(strTokenResult);
        SharedPreferencesManager.putString(PREFERENCE_PARTITION_PREFIX + removedAccountIdPartition, encryptedTokenResult);
    }

    /**
     * Remove all the cached access tokens for all partition names.
     */
    @SuppressWarnings("WeakerAccess")
    public synchronized void removeAllCachedTokens() {
        Set<String> partitionNamesSet = getPartitionNames();
        for (String partitionName : partitionNamesSet) {
            if (partitionName.equals(Constants.READONLY)) {
                continue;
            }
            SharedPreferencesManager.remove(PREFERENCE_PARTITION_PREFIX + partitionName);
        }
        partitionNamesSet.clear();
        SharedPreferencesManager.putStringSet(PREFERENCE_PARTITION_NAMES, partitionNamesSet);
        AppCenterLog.info(LOG_TAG, "Removed all tokens in all partitions");
    }
}
