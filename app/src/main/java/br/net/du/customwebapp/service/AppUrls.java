package br.net.du.customwebapp.service;

import android.content.Context;
import android.content.SharedPreferences;
import java.net.MalformedURLException;
import java.net.URL;

public class AppUrls {
    private static final String CURRENT_DOMAIN_KEY = "currentDomain";

    private final SharedPreferences appPreferences;

    private final String[] allowedDomains;
    private final String genericDomainPrefix;
    private final String genericDomainSuffix;
    private final String[] signedOutUrlPatterns;

    private String currentDomain;

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

        this.genericDomainPrefix = genericDomainPrefix;
        this.genericDomainSuffix = genericDomainSuffix;
        this.signedOutUrlPatterns = signedOutUrlPatterns;
    }

    public String getCurrentDomain() {
        return currentDomain;
    }

    public void setCurrentDomain(final String currentDomain) {
        this.currentDomain = currentDomain;
        appPreferences.edit().putString(CURRENT_DOMAIN_KEY, currentDomain).apply();
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

    public boolean isDownloadable(final String url) {
        return isAllowed(url) && url.endsWith(".zip");
    }
}
