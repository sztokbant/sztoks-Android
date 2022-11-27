package br.net.du.customwebapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AppUrlsTest {
    private static final String CURRENT_DOMAIN_KEY = "currentDomain";

    private static final String PACKAGE_NAME = "package.name";
    private static final String PROD_DOMAIN = "prod.domain";
    private static final String OTHER_DOMAIN = "other.domain";
    private static final String GENERIC_DOMAIN_PREFIX = "prefix";
    private static final String GENERIC_DOMAIN_SUFFIX = ".suffix";
    private static final String GENERIC_DOMAIN =
            GENERIC_DOMAIN_PREFIX + "-generic" + GENERIC_DOMAIN_SUFFIX;
    private static final String SIGNED_OUT_PATH = "/signedOut";
    private static final String[] SIGNED_OUT_URL_PATTERNS =
            new String[] {"/withParamOnly?s=0", SIGNED_OUT_PATH};

    private static final String PROD_URL = "https://" + PROD_DOMAIN;
    private static final String OTHER_URL = "http://" + OTHER_DOMAIN;
    private static final String GENERIC_URL = "https://" + GENERIC_DOMAIN;

    private static final String EXTERNAL_URL = "https://www.amazon.com";

    @Mock private Context context;

    @Mock private SharedPreferences.Editor editor;

    @Mock private SharedPreferences sharedPreferences;

    private AppUrls appUrls;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(sharedPreferences.getString(eq(CURRENT_DOMAIN_KEY), anyString())).thenReturn(null);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        when(sharedPreferences.edit()).thenReturn(editor);

        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(context.getSharedPreferences(eq(PACKAGE_NAME), eq(Context.MODE_PRIVATE)))
                .thenReturn(sharedPreferences);

        appUrls =
                new AppUrls(
                        context,
                        PROD_DOMAIN,
                        new String[] {OTHER_DOMAIN},
                        GENERIC_DOMAIN_PREFIX,
                        GENERIC_DOMAIN_SUFFIX,
                        SIGNED_OUT_URL_PATTERNS);
    }

    @Test
    public void isAllowed_prefixSomethingSuffix_true() {
        assertTrue(
                appUrls.isAllowed(
                        "https://" + GENERIC_DOMAIN_PREFIX + "something" + GENERIC_DOMAIN_SUFFIX));
        assertTrue(
                appUrls.isAllowed(
                        "https://" + GENERIC_DOMAIN_PREFIX + "-something" + GENERIC_DOMAIN_SUFFIX));
    }

    @Test
    public void isAllowed_prodUrl_true() {
        assertTrue(appUrls.isAllowed(PROD_URL));
        assertTrue(appUrls.isAllowed(PROD_URL + "/stats"));
        assertTrue(appUrls.isAllowed(PROD_URL + "/welcome"));
    }

    @Test
    public void isAllowed_otherUrl_true() {
        assertTrue(appUrls.isAllowed(OTHER_URL));
        assertTrue(appUrls.isAllowed(OTHER_URL + "/stats"));
        assertTrue(appUrls.isAllowed(OTHER_URL + "/welcome"));
    }

    @Test
    public void isAllowed_genericUrl_true() {
        assertTrue(appUrls.isAllowed(GENERIC_URL));
        assertTrue(appUrls.isAllowed(GENERIC_URL + "/stats"));
        assertTrue(appUrls.isAllowed(GENERIC_URL + "/welcome"));
    }

    @Test
    public void isAllowed_externalUrls_false() {
        assertFalse(appUrls.isAllowed(EXTERNAL_URL));
    }

    @Test
    public void isAllowed_somethingPrefix_false() {
        assertFalse(
                appUrls.isAllowed(
                        "https://something" + GENERIC_DOMAIN_PREFIX + GENERIC_DOMAIN_SUFFIX));
        assertFalse(
                appUrls.isAllowed(
                        "https://something-" + GENERIC_DOMAIN_PREFIX + GENERIC_DOMAIN_SUFFIX));
        assertFalse(
                appUrls.isAllowed(
                        "https://something-"
                                + GENERIC_DOMAIN_PREFIX
                                + "-something"
                                + GENERIC_DOMAIN_SUFFIX));
    }

    @Test
    public void isAllowed_prefixSuffix_false() {
        assertFalse(appUrls.isAllowed("https://" + GENERIC_DOMAIN_PREFIX + GENERIC_DOMAIN_SUFFIX));
    }

    @Test
    public void isAllowed_aboutBlank_false() {
        assertFalse(appUrls.isAllowed("about:blank"));
    }

    @Test
    public void isSignedOutUrl_baseUrlAndOtherUrl_false() {
        assertFalse(appUrls.isSignedOutUrl(PROD_URL));
        assertFalse(appUrls.isSignedOutUrl(PROD_URL + "/stats"));
    }

    @Test
    public void isSignedOutUrl_signedOut_true() {
        assertTrue(appUrls.isSignedOutUrl(PROD_URL + SIGNED_OUT_PATH));
    }

    @Test
    public void isSignedOutUrl_signedOutWithParam_true() {
        assertTrue(
                appUrls.isSignedOutUrl(
                        PROD_URL + SIGNED_OUT_PATH + "?login_email=user%40example.com"));
    }

    @Test
    public void isSignedOutUrl_signedOutWithPath_true() {
        assertTrue(
                appUrls.isSignedOutUrl(
                        PROD_URL
                                + SIGNED_OUT_PATH
                                + "/1a2b7842207de53103d01fd8b54d7fda4d5bbc52e048532ef8c0fbeadc50edd0"));
    }

    @Test
    public void isSignedOutUrl_withParamOnlyHasParam_true() {
        assertTrue(appUrls.isSignedOutUrl(PROD_URL + "/withParamOnly?s=0"));
    }

    @Test
    public void isSignedOutUrl_withParamOnlyNoParam_false() {
        assertFalse(appUrls.isSignedOutUrl(PROD_URL + "/withParamOnly"));
    }

    @Test
    public void getCurrentDomain_appStart_defaultToProdDomain() {
        // WHEN
        final String currentDomain = appUrls.getCurrentDomain();

        // THEN
        assertEquals(PROD_DOMAIN, currentDomain);
        verify(editor, times(1)).putString(eq(CURRENT_DOMAIN_KEY), eq(PROD_DOMAIN));
        verify(editor, times(1)).apply();
    }

    @Test
    public void getCurrentUrl_appStart_defaultToProdUrl() {
        // WHEN
        final String currentUrl = appUrls.getCurrentUrl();

        // THEN
        assertEquals(PROD_URL, currentUrl);
        verify(editor, times(1)).putString(eq(CURRENT_DOMAIN_KEY), eq(PROD_DOMAIN));
        verify(editor, times(1)).apply();
    }

    @Test
    public void setCurrentDomain_betaDomain_updateCurrentDomainAndProperty() {
        // GIVEN
        assertTrue(appUrls.isCurrentDomain(PROD_DOMAIN));

        // WHEN
        appUrls.setCurrentDomain(GENERIC_DOMAIN);

        // THEN
        assertTrue(appUrls.isCurrentDomain(GENERIC_DOMAIN));
        assertFalse(appUrls.isCurrentDomain(PROD_DOMAIN));

        verify(editor, times(1)).putString(eq(CURRENT_DOMAIN_KEY), eq(PROD_DOMAIN));
        verify(editor, times(1)).putString(eq(CURRENT_DOMAIN_KEY), eq(GENERIC_DOMAIN));
        verify(editor, times(2)).apply();
    }

    @Test
    public void isDownloadable_validDomainZipFile_returnTrue() {
        assertTrue(appUrls.isDownloadable(PROD_URL + "/export_data_download.zip"));
    }

    @Test
    public void isDownloadable_validDomainHtmlFile_returnFalse() {
        assertFalse(appUrls.isDownloadable(PROD_URL + "/export_data_download.html"));
    }

    @Test
    public void isDownloadable_invalidDomainZipFile_returnFalse() {
        assertFalse(appUrls.isDownloadable(EXTERNAL_URL + "/export_data_download.zip"));
    }

    @Test
    public void isDownloadable_invalidDomainHtmlFile_returnFalse() {
        assertFalse(appUrls.isDownloadable(EXTERNAL_URL + "/export_data_download.html"));
    }
}
