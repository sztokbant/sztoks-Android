package br.net.du.customwebapp.service;

import static br.net.du.customwebapp.config.Customizable.BUTTON_CONFIGS;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import br.net.du.customwebapp.R;
import br.net.du.customwebapp.model.ButtonConfig;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import java.net.MalformedURLException;
import java.net.URL;

public class FloatingActionMenuManager {

    private final Context applicationContext;
    private final FloatingActionMenu floatingActionMenu;
    private final WebView webView;
    private final AppUrls appUrls;

    private String baseUrl;

    public FloatingActionMenuManager(
            final Context applicationContext,
            final FloatingActionMenu floatingActionMenu,
            final WebView webView,
            final AppUrls appUrls) {
        this.applicationContext = applicationContext;
        this.floatingActionMenu = floatingActionMenu;
        this.webView = webView;
        this.appUrls = appUrls;

        populateWithCurrentDomain(webView.getUrl());
    }

    public void refresh() {
        final String webViewUrl = webView.getUrl();

        if (!appUrls.isCurrentDomain(webViewUrl)) {
            populateWithCurrentDomain(webViewUrl);
        }

        if (appUrls.isSignedOutUrl(webViewUrl)) {
            floatingActionMenu.hideMenu(false);
        } else {
            floatingActionMenu.showMenu(false);
        }
    }

    public void closeMenu() {
        floatingActionMenu.close(false);
    }

    private void populateWithCurrentDomain(final String webViewUrl) {
        try {
            final String webViewHost = new URL(webViewUrl).getHost();
            if (!appUrls.getCurrentDomain().equals(webViewHost)) {
                appUrls.setCurrentDomain(webViewHost);
            }
        } catch (final MalformedURLException e) {
            // ignore
        }

        baseUrl = appUrls.getCurrentUrl();

        floatingActionMenu.removeAllMenuButtons();
        for (final ButtonConfig buttonConfig : BUTTON_CONFIGS) {
            floatingActionMenu.addMenuButton(initButton(buttonConfig));
        }
    }

    private FloatingActionButton initButton(final ButtonConfig buttonConfig) {
        final FloatingActionButton button = new FloatingActionButton(applicationContext);

        button.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        button.setColorNormal(
                applicationContext.getResources().getColor(R.color.menu_labels_colorNormal));
        button.setColorPressed(
                applicationContext.getResources().getColor(R.color.menu_labels_colorPressed));
        button.setImageResource(buttonConfig.getImageResource());
        button.setLabelText(buttonConfig.getLabelText());
        button.setButtonSize(FloatingActionButton.SIZE_MINI);

        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        webView.loadUrl(baseUrl + "/" + buttonConfig.getPath());
                        closeMenu();
                    }
                });

        return button;
    }
}
