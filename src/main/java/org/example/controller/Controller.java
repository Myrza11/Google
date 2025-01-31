package org.example.controller;

import org.example.controller.commands.Command;
import org.example.controller.commands.SearchCommand;
import org.example.model.Model;
import org.example.view.Canvas;
import org.example.view.Viewer;
import org.example.view.renderers.LinkArea;
import org.example.view.renderers.Renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;

public class Controller implements ActionListener {
    private final Model model;
    private final Viewer viewer;
    private Renderer renderer;
    private HashMap<String, Command> commandMap;

    public Controller(Viewer viewer) {
        this.viewer = viewer;
        model = new Model(viewer);
        setupCommands();

    }

    public void attachCanvasMouseEvents(Canvas canvas) {
        renderer = canvas.getRenderer();
        canvas.addMouseListener(new CanvasMouseListener(this));
        canvas.addMouseMotionListener(new LinkHoverHandler(this));
        viewer.getCanvas().addMouseWheelListener(new Scroll(viewer.getCanvas()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        commandMap.get(actionCommand).execute();
    }

    private void setupCommands() {
        SearchCommand searchCommand = new SearchCommand(viewer, model);
        commandMap = new HashMap<>();
        commandMap.put("Search", searchCommand);
    }

    void handleMouseClick(MouseEvent e) {
        if (renderer == null) {
            return;
        }

        int x = e.getX();
        int y = e.getY();

        for (LinkArea linkArea : renderer.getLinkAreas()) {
            if (linkArea.contains(x, y)) {
                viewer.setNewUrl(linkArea.getUrl());
                commandMap.get("Search").execute();
//                linkArea.openUrlInBrowser();
                break;
            }
        }
    }

    public void clearLinkAreas() {
        if (renderer != null) {
            renderer.getLinkAreas().clear();
        }
    }

    public boolean isCursorOverLink(int x, int y) {
        for (LinkArea linkArea : renderer.getLinkAreas()) {
            if (linkArea.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    public Model getModel() {
        return model;
    }

    public Viewer getViewer() {
        return viewer;
    }
}
