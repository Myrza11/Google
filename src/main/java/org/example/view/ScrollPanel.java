package org.example.view;

import javax.swing.*;
import java.awt.*;

public class ScrollPanel extends JPanel {
    private JPanel buttonPanel;

    public ScrollPanel() {
        setLayout(null);
        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(20, 400));

        setScrollButton();
    }
    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    public void setScrollButton() {
        buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        buttonPanel.setSize(new Dimension(20, 40));

        add(buttonPanel);
    }

    public void updateScroll(int scrollOffsetY, int maxScrollOffsetY) {
        if (maxScrollOffsetY <= 0) {
            maxScrollOffsetY = 1; // Защита от деления на 0
        }

        // Высота области прокрутки
        int availableHeight = getHeight() - buttonPanel.getHeight();

        // Новая позиция buttonPanel (высчитываем процент от maxScrollOffsetY)
        int newY = (int) ((double) scrollOffsetY / maxScrollOffsetY * availableHeight);

        // Устанавливаем новую позицию кнопки
        buttonPanel.setBounds(0, newY, buttonPanel.getWidth(), buttonPanel.getHeight());

        revalidate();
        repaint();
    }
}
