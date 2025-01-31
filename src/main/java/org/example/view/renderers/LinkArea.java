package org.example.view.renderers;

import java.awt.Rectangle;
import java.awt.Desktop;
import java.net.URI;

public class LinkArea {
    private final Rectangle area;
    private String url;

    public LinkArea(Rectangle area, String url, String baseUrl) {
        this.area = area;
        this.url = url;
        setUrl(baseUrl);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String baseUrl) {
        if (!(url.startsWith("http") && url.startsWith("https")))
            url = baseUrl + url;
    }

    public boolean contains(int x, int y) {
        return area.contains(x, y);
    }

    public void openUrlInBrowser() {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
