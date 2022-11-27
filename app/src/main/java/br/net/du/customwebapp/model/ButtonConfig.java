package br.net.du.customwebapp.model;

public class ButtonConfig {
    private final String labelText;
    private final int imageResource;
    private final String path;

    public ButtonConfig(final String labelText, final int imageResource, final String path) {
        this.labelText = labelText;
        this.imageResource = imageResource;
        this.path = path;
    }

    public String getLabelText() {
        return labelText;
    }

    public int getImageResource() {
        return imageResource;
    }

    public String getPath() {
        return path;
    }
}
