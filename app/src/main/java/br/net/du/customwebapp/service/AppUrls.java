package br.net.du.customwebapp.service;

import android.content.Context;
import android.content.SharedPreferences;
import java.net.MalformedURLException;
import java.net.URL;

public class AppUrls {
    private static final String CURRENT_DOMAIN_KEY = "currentDomain";
    private static final String CURRENT_SNAPSHOT_ID_KEY = "currentSnapshotId";
    private static final String LATEST_SNAPSHOT_ID_VALUE = "0";

    private final SharedPreferences appPreferences;

    private final String[] allowedDomains;
    private final String genericDomainPrefix;
    private final String genericDomainSuffix;
    private final String[] signedOutUrlPatterns;

    private String currentDomain;
    private String currentSnapshotId;

    public AppUrls(
            final Context context,
            final String prodDomain,
            final String[] otherAllowedDomains,
            final String genericDomainPrefix,
            final String genericDomainSuffix,
            final String[] signedOutUrlPatterns) {
        allowedDomains = new String[1 + otherAllowedDomains.length];
        allowedDomains[0] = prodDomain;
        for (int i = 0; i < otherAllowedDomains.length; i++) {
            allowedDomains[i + 1] = otherAllowedDomains[i];
        }

        appPreferences =
                context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        currentDomain = appPreferences.getString(CURRENT_DOMAIN_KEY, null);
        if (currentDomain == null) {
            setCurrentDomain(prodDomain);
        }

        currentSnapshotId = appPreferences.getString(CURRENT_SNAPSHOT_ID_KEY, null);
        if (currentSnapshotId == null) {
            setCurrentSnapshotId(LATEST_SNAPSHOT_ID_VALUE);
        }

        this.genericDomainPrefix = genericDomainPrefix;
        this.genericDomainSuffix = genericDomainSuffix;
        this.signedOutUrlPatterns = signedOutUrlPatterns;
    }

    public String getCurrentDomain() {
        return currentDomain;
    }

    public String getCurrentSnapshotId() {
        return currentSnapshotId;
    }

    public void setCurrentDomain(final String currentDomain) {
        this.currentDomain = currentDomain;
        appPreferences.edit().putString(CURRENT_DOMAIN_KEY, currentDomain).apply();
    }

    public void setCurrentSnapshotId(final String currentSnapshotId) {
        this.currentSnapshotId = currentSnapshotId;
        appPreferences.edit().putString(CURRENT_SNAPSHOT_ID_KEY, currentSnapshotId).apply();
    }

    public void setCurrentSnapshotIdFromUrl(final String url) {
        String urlSnapshotId = LATEST_SNAPSHOT_ID_VALUE;

        if (url != null && url.contains("/snapshot/")) {
            try {
                urlSnapshotId = url.split("/snapshot/")[1].split("/")[0];
            } catch (final Exception e) {
                urlSnapshotId = LATEST_SNAPSHOT_ID_VALUE;
            }
        }

        setCurrentSnapshotId(urlSnapshotId);
    }

    public String getCurrentUrl() {
        return "https://" + currentDomain;
    }

    public boolean isAllowed(final String url) {
        return urlContainsAnyPattern(url, allowedDomains) || isAllowedGeneric(url);
    }

    private boolean isAllowedGeneric(final String stringUrl) {
        if (genericDomainPrefix == null || genericDomainSuffix == null) {
            return false;
        }

        if ("".equals(genericDomainPrefix) && "".equals(genericDomainSuffix)) {
            return false;
        }

        final String domain;
        try {
            final URL url = new URL(stringUrl);
            domain = url.getHost();
        } catch (final MalformedURLException e) {
            return false;
        }

        return domain.startsWith(genericDomainPrefix)
                && domain.endsWith(genericDomainSuffix)
                && domain.length() > genericDomainPrefix.length() + genericDomainSuffix.length();
    }

    public boolean isSignedOutUrl(final String url) {
        // TODO This behaves unexpectedly when signedOutUrlPatterns contains
        // "/someSubstringWithParamOnly?s=0" as well as "/someSubstring", it will return true for
        // "/someSubstringWithParamOnly" (without the param) as it matches "/someSubstring"
        return urlContainsAnyPattern(url, signedOutUrlPatterns);
    }

    private boolean urlContainsAnyPattern(final String url, final String[] patterns) {
        for (final String pattern : patterns) {
            if (url.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrentDomain(final String webViewUrl) {
        return !currentDomain.isEmpty() && webViewUrl.contains(currentDomain);
    }

    public boolean isCurrentSnapshotId(final String webViewUrl) {
        return !currentSnapshotId.isEmpty()
                && webViewUrl.contains("/snapshot/" + currentSnapshotId + "/");
    }

    public boolean isDownloadable(final String url) {
        return isAllowed(url) && url.endsWith(".zip");
    }
}
