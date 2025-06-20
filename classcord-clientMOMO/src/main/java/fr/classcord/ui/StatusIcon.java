package fr.classcord.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Icone représentant un statut utilisateur par un cercle coloré.
 * Utilisé dans la liste des utilisateurs pour afficher leur état (en ligne, absent, etc.).
 */
public class StatusIcon implements Icon {
    // Couleur du cercle représentant le statut
    private final Color color;
    // Taille du cercle (largeur et hauteur en pixels)
    private final int size = 10;

    /**
     * Constructeur avec la couleur à afficher.
     * @param color Couleur du cercle
     */
    public StatusIcon(Color color) {
        this.color = color;
    }

    /**
     * Dessine l'icône sur le composant donné, à la position (x,y).
     * @param c Le composant sur lequel dessiner
     * @param g L'objet Graphics pour dessiner
     * @param x La coordonnée X de départ
     * @param y La coordonnée Y de départ
     */
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // Remplir un cercle avec la couleur du statut
        g.setColor(color);
        g.fillOval(x, y, size, size);
        // Dessiner le contour noir du cercle pour bien le délimiter
        g.setColor(Color.BLACK);
        g.drawOval(x, y, size, size);
    }

    /**
     * Largeur de l'icône en pixels
     * @return taille en largeur
     */
    @Override
    public int getIconWidth() {
        return size;
    }

    /**
     * Hauteur de l'icône en pixels
     * @return taille en hauteur
     */
    @Override
    public int getIconHeight() {
        return size;
    }
}
