package br.net.du.customwebapp.activity;

import static br.net.du.customwebapp.config.Customizable.GENERIC_DOMAIN_PREFIX;
import static br.net.du.customwebapp.config.Customizable.GENERIC_DOMAIN_SUFFIX;
import static br.net.du.customwebapp.config.Customizable.OTHER_ALLOWED_DOMAINS;
import static br.net.du.customwebapp.config.Customizable.SIGNED_OUT_URL_PATTERNS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import br.net.du.customwebapp.R;
import br.net.du.customwebapp.service.AppUrls;
import br.net.du.customwebapp.service.FloatingActionMenuManager;
import com.github.clans.fab.FloatingActionMenu;

public class MainActivity extends Activity {
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private AppUrls appUrls;
    private SwipeRefreshLayout swipeRefresh;
    private WebView webView;

    private FloatingActionMenuManager floatingActionMenuManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // https://medium.com/swlh/splash-screen-in-android-8ab250e40190
        setTheme(R.style.CustomTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appUrls =
                new AppUrls(
                        getBaseContext(),
                        getString(R.string.prod_domain),
                        OTHER_ALLOWED_DOMAINS,
                        GENERIC_DOMAIN_PREFIX,
                        GENERIC_DOMAIN_SUFFIX,
                        SIGNED_OUT_URL_PATTERNS);

        buildSwipeRefreshLayout();

        CookieSyncManager.createInstance(getBaseContext());

        buildWebView();
        populateWebView(savedInstanceState);

        floatingActionMenuManager =
                new FloatingActionMenuManager(
                        getApplicationContext(), getFloatingActionMenu(), webView, appUrls);
    }

    private FloatingActionMenu getFloatingActionMenu() {
        final FloatingActionMenu menu = findViewById(R.id.floating_action_menu);

        menu.setVisibility(View.INVISIBLE);
        menu.setAnimationDelayPerItem(14);
        menu.setMenuButtonColorNormal(
                getApplicationContext().getResources().getColor(R.color.menu_labels_colorNormal));
        menu.setMenuButtonColorPressed(
                getApplicationContext().getResources().getColor(R.color.menu_labels_colorPressed));

        return menu;
    }

    /**
     * Builds SwipeRefreshLayout object to reload the WebView on refresh.
     * https://developer.android.com/training/swipe/respond-refresh-request.html
     */
    private void buildSwipeRefreshLayout() {
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // not calling reload() as it reposts a page if the request was POST
                        webView.loadUrl(webView.getUrl());
                    }
                });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void buildWebView() {
        webView = (WebView) findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new RestrictedWebViewClient());
        webView.setWebChromeClient(buildWebChromeClient());

        webView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(final View view, final MotionEvent motionEvent) {
                        floatingActionMenuManager.closeMenu();
                        return false;
                    }
                });

        // Disable auto-complete suggestions to prevent NullPointerException with AutofillPopup
        webView.getSettings().setSaveFormData(false);

        webView.setDownloadListener(buildDownloadListener());

        appendAppInfoToUserAgent();
    }

    private void appendAppInfoToUserAgent() {
        final String currentUserAgentString = webView.getSettings().getUserAgentString();

        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (final NameNotFoundException e) {
            // ignored
        }

        final String appInfo = getString(R.string.app_user_agent) + "-" + versionCode;

        if (!currentUserAgentString.endsWith(appInfo)) {
            webView.getSettings().setUserAgentString(currentUserAgentString + " " + appInfo);
        }
    }

    private DownloadListener buildDownloadListener() {
        return new DownloadListener() {
            @Override
            public void onDownloadStart(
                    final String url,
                    final String userAgent,
                    final String contentDisposition,
                    final String mimeType,
                    final long contentLength) {
                final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                request.setMimeType(mimeType);
                request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
                request.addRequestHeader("User-Agent", userAgent);

                final String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                final String description =
                        getResources().getString(R.string.downloading_file) + " " + fileName;
                request.setDescription(description);

                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, fileName);

                longToast(description);

                final DownloadManager downloadManager =
                        (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                downloadManager.enqueue(request);
            }
        };
    }

    /**
     * Populates webView's contents, either from Intent (external link), savedInstanceState or
     * current URL.
     */
    private void populateWebView(final Bundle savedInstanceState) {
        final Intent intent = getIntent();
        if (!Intent.ACTION_MAIN.equals(intent.getAction())) {
            onNewIntent(intent);
        } else if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(appUrls.getCurrentUrl());
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            webView.loadUrl(intent.getData().toString());
        }
    }

    /**
     * Builds a WebChromeClient enabled to handle a WebView confirm dialog
     * http://stackoverflow.com/questions/2726377/how-to-handle-a-webview-confirm-dialog/2726503
     *
     * @return Customized WebChromeClient
     */
    private WebChromeClient buildWebChromeClient() {
        return new WebChromeClient() {

            @Override
            public boolean onJsConfirm(
                    final WebView view,
                    final String url,
                    final String message,
                    final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(message)
                        .setPositiveButton(
                                android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            final DialogInterface dialog, final int which) {
                                        result.confirm();
                                    }
                                })
                        .setNegativeButton(
                                android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            final DialogInterface dialog, final int which) {
                                        result.cancel();
                                    }
                                })
                        .create()
                        .show();

                return true;
            }
        };
    }

    /**
     * Enables web page history navigation for WebView
     * https://developer.android.com/guide/webapps/webview.html#NavigatingHistory
     */
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            floatingActionMenuManager.closeMenu();
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default system
        // behavior (probably
        // exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    /** Saves WebView state in order to prevent it from losing context on screen rotation */
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    longToast(getResources().getString(R.string.storage_permission_granted));
                } else {
                    longToast(getResources().getString(R.string.storage_permission_denied));
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void longToast(final String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    /**
     * WebViewClient with external handling of "mailto:" URLs, ignoring "tel:" and URLs not allowed
     * by AppUrls. It will show SwipeRefreshLayout progress spinner when loading URL.
     *
     * <p>http://stackoverflow.com/questions/3623137/howto-handle-mailto-in-android-webview
     * http://stackoverflow.com/questions/17994750/open-external-links-in-the-browser-with-android-webview
     */
    private class RestrictedWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {

            if (url.startsWith(WebView.SCHEME_MAILTO)) {
                final MailTo mailto = MailTo.parse(url);

                final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {mailto.getTo()});
                emailIntent.putExtra(Intent.EXTRA_CC, mailto.getCc());
                final String subject =
                        mailto.getSubject() != null
                                ? mailto.getSubject()
                                : view.getContext()
                                        .getResources()
                                        .getString(R.string.default_email_subject);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, mailto.getBody());

                view.getContext().startActivity(emailIntent);
            } else if (url.startsWith(WebView.SCHEME_TEL)) {
                // prevents accidental clicks on numbers from being interpreted as "tel:"
            } else if (appUrls.isDownloadable(url)) {
                if (checkWriteExternalStoragePermission()) {
                    view.loadUrl(url);
                }
            } else if (appUrls.isAllowed(url)) {
                view.loadUrl(url);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
            }

            return true;
        }

        private boolean checkWriteExternalStoragePermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
                return false;
            }
            return true;
        }

        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            swipeRefresh.setRefreshing(true);
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            super.onPageFinished(view, url);

            // Ensures session cookie will be quickly saved from RAM to storage
            // https://developer.android.com/reference/android/webkit/CookieSyncManager.html
            // UPDATE 2020-11-25: Although deprecated, this is still needed in order to properly
            // handle logout.
            CookieSyncManager.getInstance().sync();

            floatingActionMenuManager.refresh();
            swipeRefresh.setRefreshing(false);
        }

        @Override
        public void onReceivedError(
                final WebView view,
                final WebResourceRequest request,
                final WebResourceError error) {
            final Context context = view.getContext();
            final Resources resources = context.getResources();
            new AlertDialog.Builder(context)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(resources.getString(R.string.connection_error_title))
                    .setMessage(resources.getString(R.string.connection_error_message))
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {}
                            })
                    .show();
        }
    }
}
