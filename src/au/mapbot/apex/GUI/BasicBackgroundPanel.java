/*
 * BasicBackgroundPanel
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.GUI;

import au.mapbot.apex.classes.SaveFile;

import javax.swing.*;
import java.awt.*;

public class BasicBackgroundPanel extends JButton
{
    private Image background;

    private SaveFile save;

    public BasicBackgroundPanel(Image background, SaveFile saveFile)
    {
        this.background = background;
        this.setBackground(Color.BLACK);
        this.save = saveFile;
        setLayout( new BorderLayout() );

    }

    public SaveFile getSave() {
        return save;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        //g.drawImage(background, 0, 0, null); // image full size
        g.drawImage(background, 0, 0, getWidth(), getHeight(), null); // image scaled
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(250,50);
        //return new Dimension(background.getWidth(this), background.getHeight(this));
    }
}