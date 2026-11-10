package core;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;

/**
 * Design system tokens and UI styling utilities for Smart Parking.
 * Follows a refined Dark Forest Green / Deep Teal and Light Slate palette.
 */
public class ThemeConstants {

    // Brand Palette
    public static final Color BRAND_FOREST_DARK  = new Color(16, 42, 35);    // #102A23
    public static final Color BRAND_FOREST_MED   = new Color(22, 60, 50);    // #163C32
    public static final Color BRAND_TEAL_ACCENT  = new Color(28, 115, 87);   // #1C7357
    public static final Color BRAND_TEAL_HOVER   = new Color(36, 140, 106);  // #248C6A
    public static final Color BRAND_TEAL_LIGHT   = new Color(230, 245, 239); // #E6F5EF

    // Neutral Surfaces & Panels
    public static final Color BG_MAIN            = new Color(245, 248, 246); // #F5F8F6
    public static final Color BG_CARD            = Color.WHITE;
    public static final Color BORDER_LIGHT       = new Color(222, 230, 226); // #DEE6E2
    public static final Color BORDER_FOCUSED     = new Color(28, 115, 87);

    // Text Colors
    public static final Color TEXT_PRIMARY       = new Color(26, 36, 33);    // #1A2421
    public static final Color TEXT_SECONDARY     = new Color(85, 105, 98);   // #556962
    public static final Color TEXT_MUTED         = new Color(135, 155, 148); // #879B94
    public static final Color TEXT_ON_DARK       = new Color(240, 245, 243);

    // Semantic Status Colors
    public static final Color STAT_TOTAL_BG      = new Color(38, 77, 102);   // Deep Blue
    public static final Color STAT_AVAIL_BG      = new Color(26, 110, 72);   // Forest Green
    public static final Color STAT_OCCUPIED_BG   = new Color(168, 64, 46);   // Terracotta/Red

    public static final Color BADGE_AVAIL_BG     = new Color(226, 247, 236);
    public static final Color BADGE_AVAIL_TXT    = new Color(18, 102, 63);
    public static final Color BADGE_OCC_BG       = new Color(254, 234, 230);
    public static final Color BADGE_OCC_TXT      = new Color(178, 48, 30);
    public static final Color BADGE_CANC_BG      = new Color(237, 241, 244);
    public static final Color BADGE_CANC_TXT     = new Color(90, 105, 115);

    // Typography
    public static final Font FONT_HEADER_LARGE   = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER_MEDIUM  = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_HEADER_SMALL   = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY_REGULAR   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD      = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL          = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_STAT_NUMBER    = new Font("Segoe UI", Font.BOLD, 26);

    public static JButton createStyledButton(String text, Color bg, Color fg, boolean isPrimary) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(isPrimary ? BRAND_TEAL_HOVER : bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setForeground(fg);
        btn.setFont(FONT_BODY_BOLD);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(8, 16, 8, 16));
        return btn;
    }

    public static JButton createOutlineButton(String text, Color borderAndTextColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(borderAndTextColor.getRed(), borderAndTextColor.getGreen(), borderAndTextColor.getBlue(), 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(borderAndTextColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setForeground(borderAndTextColor);
        btn.setFont(FONT_BODY_BOLD);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(8, 16, 8, 16));
        return btn;
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
    }
}
