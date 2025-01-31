package org.example.view.renderers;

import org.example.model.Model;
import org.example.model.renderTree.RenderNode;
import org.example.model.renderTree.RenderTree;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Color;
import java.awt.Font;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Renderer {
    private Model model;
    private Graphics2D g2d;
    private List<LinkArea> linkAreas;
    private RenderTree renderTree;
    private CssRenderer cssRenderer;
    private Map<String, Consumer<RenderNode>> renderers;
    public Renderer(Model model) {
        this.model = model;
        this.linkAreas = new ArrayList<>();
        cssRenderer = new CssRenderer();
        createRenderers();
    }

    public int renderElement(Graphics2D g2d, RenderTree renderTree) {
        this.g2d = g2d;
        this.renderTree = renderTree;
        renderElement(renderTree.getRoot(), renderTree.getRoot().getTagName());
        return renderTree.getWindowHeight();
    }

    private void renderElement(RenderNode node, String parentTag) {
        drawRect(node);
        String tagToUse = node.getTagName().equals("text") ? parentTag : node.getTagName();
        renderers.getOrDefault(tagToUse, this::renderDefault).accept(node);

        System.out.println(tagToUse);

        if (node.getTextContent() != null && node.getTextContent().isEmpty()
                && !node.getChildren().isEmpty() && !node.getTagName().equals("ul")) {
            for (RenderNode child : node.getChildren()) {
                renderElement(child, node.getTagName());
            }
        }
        renderTree.setWindowHeight((int) (node.getY() + node.getHeight()));
    }

    private void createRenderers() {
        renderers = new HashMap<>();

        renderers.put("h1", this::renderText);
        renderers.put("h2", this::renderText);
        renderers.put("h3", this::renderText);
        renderers.put("h4", this::renderText);
        renderers.put("h5", this::renderText);
        renderers.put("h6", this::renderText);

        renderers.put("p", this::renderText);
        renderers.put("span", this::renderText);
        renderers.put("em", this::renderText);
        renderers.put("strong", this::renderText);
        renderers.put("i", this::renderText);
        renderers.put("b", this::renderText);
        renderers.put("li", this::renderText);

        renderers.put("a", this::renderLink);
        renderers.put("ul", this::renderList);
        renderers.put("img", this::renderImage);
    }

    private void drawRect(RenderNode node) {
        int width = (int) node.getWidth();
        int height = (int) node.getHeight();

        Color originalColor = g2d.getColor();

        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.drawRect(node.getX(), node.getY() - g2d.getFontMetrics().getHeight(), width, height);

        g2d.setColor(originalColor);
    }

    private void renderText(RenderNode node) {
        g2d.setColor(Color.black);
        String content = node.getTextContent();

        cssRenderer.setFont(g2d, node);

        if (!content.isEmpty()) {
            g2d.drawString(content, node.getX(), node.getY());
            cssRenderer.resetFont(g2d);
        }
    }

    private void renderLink(RenderNode node) {
        String link = node.getTextContent();

        while (!node.getTagName().equals("text") && !node.getTagName().equals("img")
                && node.getChildren().size() == 1) {
            node = node.getChildren().get(0);
        }

        if (node.getTagName().equals("text")) {
            renderTextLink(node, link);
        } else if (node.getTagName().equals("img")) {
            renderImageLink(node, link);
        }
    }

    private void renderTextLink(RenderNode node, String linkContent) {
        g2d.setColor(Color.BLUE);
        Font underlineFont = g2d.getFont().deriveFont(Font.PLAIN);
        g2d.setFont(underlineFont);

        cssRenderer.setFont(g2d, node);

        FontMetrics fm = g2d.getFontMetrics();

        g2d.drawString(node.getTextContent(), node.getX(), node.getY());
        g2d.drawLine(node.getX(), node.getY() + 2, node.getX() + fm.stringWidth(node.getTextContent()), node.getY() + 2);

        Rectangle clickableArea = new Rectangle(node.getX(), node.getY() - fm.getAscent(), fm.stringWidth(node.getTextContent()),
                fm.getHeight());
        linkAreas.add(new LinkArea(clickableArea, linkContent, model.getBaseUrl()));
    }

    private void renderImageLink(RenderNode node, String linkContent) {
        try {
            renderImage(node);

            Rectangle clickableArea = new Rectangle(node.getX(), node.getY(), (int) node.getWidth(),
                    (int) node.getHeight());
            linkAreas.add(new LinkArea(clickableArea, linkContent, model.getBaseUrl()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderList(RenderNode node) {
        for (RenderNode child : node.getChildren()) {
            g2d.drawString("•", node.getX(), node.getY());
            renderElement(child, child.getTagName());
        }
    }

    @SuppressWarnings("deprecation")
    private void renderImage(RenderNode node) {
        try {
            String content = node.getTextContent();
            URL imgUrl = new URL(content);

            Image img = ImageIO.read(imgUrl);

            if (img != null) {
                int imgWidth = img.getWidth(null);
                int imgHeight = img.getHeight(null);

                if (imgWidth > node.getWidth() || imgHeight > node.getHeight()) {
                    double widthRatio = node.getWidth() / imgWidth;
                    double scale = widthRatio;

                    int scaledWidth = (int) (imgWidth * scale);
                    int scaledHeight = (int) (imgHeight * scale);

                    node.setHeight(scaledHeight);

                    g2d.drawImage(img, node.getX(), node.getY(), scaledWidth, scaledHeight, null);
                } else {
                    g2d.drawImage(img, node.getX(), node.getY(), imgWidth, imgHeight, null);
                }
            }
        } catch (IOException e) {
            e.getMessage();
        }
    }

    private void renderDefault(RenderNode node) {}

    public List<LinkArea> getLinkAreas() {
        return linkAreas;
    }

    public int getCanvasHeight() {
        if (renderTree != null)
            return renderTree.getWindowHeight();
        return 0;
    }
}
