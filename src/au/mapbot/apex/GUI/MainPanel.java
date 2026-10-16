/*
 * MainPanel
 * v0.94
 * 2025-04-18
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.GUI;

import au.mapbot.apex.classes.PointList;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class MainPanel extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener {

    private final BufferedImage image;

    private java.util.List<PointList> points;

    private List<Integer> kills;

    private double zoomFactor = 1;
    private double prevZoomFactor = 1;
    private boolean zoomer;
    private boolean dragger;
    private boolean released;
    private double xOffset = 0;
    private double yOffset = 0;
    private int xDiff;
    private int yDiff;
    private Point startPoint;

    public MainPanel(BufferedImage image) {
        this.image = image;
        this.setPreferredSize(new Dimension(800,800));
        initComponent();
        this.kills = new ArrayList<>();
        this.points = new ArrayList<>();
    }

    private void initComponent() {
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public void setPoints(final List<PointList> newPoints){
        points.clear();
        points.addAll(newPoints);
    };

    public void setKills(final List<Integer> newKills){
        kills.clear();
        kills.addAll(newKills);
    };

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        if (zoomer) {
            AffineTransform at = new AffineTransform();

            double xRel = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX();
            double yRel = MouseInfo.getPointerInfo().getLocation().getY() - getLocationOnScreen().getY();

            double zoomDiv = zoomFactor / prevZoomFactor;

            xOffset = (zoomDiv) * (xOffset) + (1 - zoomDiv) * xRel;
            yOffset = (zoomDiv) * (yOffset) + (1 - zoomDiv) * yRel;

            at.translate(xOffset, yOffset);
            at.scale(zoomFactor, zoomFactor);
            prevZoomFactor = zoomFactor;
            g2.transform(at);
            zoomer = false;
        }

        if (dragger) {
            AffineTransform at = new AffineTransform();
            at.translate(xOffset + xDiff, yOffset + yDiff);
            at.scale(zoomFactor, zoomFactor);
            g2.transform(at);

            if (released) {
                xOffset += xDiff;
                yOffset += yDiff;
                dragger = false;
            }

        }

        // All drawings go here

        g2.drawImage(image, 0, 0, this);

        Stroke stroke = new BasicStroke(2f);
        g2.setStroke(stroke);

        if (!points.isEmpty()) {
            //current player location
        }

        stroke = new BasicStroke(1f);
        g2.setStroke(stroke);
        g2.setColor(Color.GREEN);
        for (int i = 1; i < points.size(); i++){
            g2.drawLine((int)points.get(i-1).getPoint().x/3,(int)points.get(i-1).getPoint().y/3,(int)points.get(i).getPoint().x/3,(int)points.get(i).getPoint().y/3);

        }
        stroke = new BasicStroke(2f);
        g2.setStroke(stroke);
        g2.setColor(Color.RED);
        //always draw kills ontop of lines
        for (int i = 1; i < points.size(); i++){
            for (Integer kill: kills){
                if (i == kill){
//                        try {
                    g2.drawLine((int) points.get(i).getPoint().x / 3 - 4, (int) points.get(i).getPoint().y / 3 - 4, (int) points.get(i).getPoint().x / 3 + 4, (int) points.get(i).getPoint().y / 3 + 4);
                    g2.drawLine((int) points.get(i).getPoint().x / 3 + 4, (int) points.get(i).getPoint().y / 3 - 4, (int) points.get(i).getPoint().x / 3 - 4, (int) points.get(i).getPoint().y / 3 + 4);

                    //kill indicator (skull icon)
//                            BufferedImage skull = ImageIO.read(new File("src/main/resources/skull.png"));
//                            BufferedImage skullSmall = ImageUtil.resizeBufferedImage(skull, Double.valueOf(skull.getWidth()/1.5).intValue(), Double.valueOf(skull.getHeight()/1.5).intValue());
//                            g2d.drawImage(skullSmall,null,(int)points.get(i).getPoint().x/3-(skullSmall.getWidth()/2),(int)points.get(i).getPoint().y/3-(skullSmall.getHeight()/2));

//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
                }
            }
        }

        if (!points.isEmpty()) {
            //current player location
            g2.setColor(Color.CYAN);
            g2.drawOval((int) points.get(points.size()-1).getPoint().x / 3 - 2, (int) points.get(points.size()-1).getPoint().y / 3 - 2,4,4);
        }

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        zoomer = true;

        //Zoom in
        if (e.getWheelRotation() < 0) {
            zoomFactor *= 1.1;
            repaint();
        }
        //Zoom out
        if (e.getWheelRotation() > 0) {
            zoomFactor /= 1.1;
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point curPoint = e.getLocationOnScreen();
        xDiff = curPoint.x - startPoint.x;
        yDiff = curPoint.y - startPoint.y;

        dragger = true;
        repaint();

    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        released = false;
        startPoint = MouseInfo.getPointerInfo().getLocation();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        released = true;
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

}