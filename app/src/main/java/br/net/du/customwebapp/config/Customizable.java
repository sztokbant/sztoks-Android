package br.net.du.customwebapp.config;

import br.net.du.customwebapp.R;
import br.net.du.customwebapp.model.ButtonConfig;

public class Customizable {
    // Define buttons in floating action menu and their paths
    public static final ButtonConfig[] BUTTON_CONFIGS = {
        new ButtonConfig(
                "+ Donation Transaction",
                R.drawable.ic_launcher,
                "snapshot/%s/newDonationTransaction"),
        new ButtonConfig(
                "+ Investing Activity Transaction",
                R.drawable.ic_launcher,
                "snapshot/%s/newInvestmentTransaction"),
        new ButtonConfig(
                "+ Income Transaction", R.drawable.ic_launcher, "snapshot/%s/newIncomeTransaction"),
        new ButtonConfig(
                "+ Liability Account", R.drawable.ic_launcher, "snapshot/%s/newLiabilityAccount"),
        new ButtonConfig("+ Asset Account", R.drawable.ic_launcher, "snapshot/%s/newAssetAccount")
    };

    // Define other domains allowed in the app's webview
    public static final String[] OTHER_ALLOWED_DOMAINS = new String[] {"sztoks.com"};

    // Define signed-out URL paths to prevent floating action menu from being displayed
    public static final String[] SIGNED_OUT_URL_PATTERNS = new String[] {"/login", "/signup"};

    // Define optional prefix, suffix for extra allowed domains
    public static final String GENERIC_DOMAIN_PREFIX = "sztoks-";
    public static final String GENERIC_DOMAIN_SUFFIX = ".herokuapp.com";
}
