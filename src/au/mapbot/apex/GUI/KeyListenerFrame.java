/*
 * KeyListenerFrame
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.GUI;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

public class KeyListenerFrame implements KeyListener {

    private int scrollDistance = 40;
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            int indexOfSave = 0;
            for (int i = 0; i <  MainGUI2.saveFileList.size(); i++){
                if (MainGUI2.saveFileList.get(i) == MainGUI2.mapHistoryPane.getSelected()){
                    indexOfSave = i;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_UP && indexOfSave != MainGUI2.saveFileList.size() -1) {
                MainGUI2.mapHistoryPane.setSelected(MainGUI2.saveFileList.get(indexOfSave+ 1));
                if (MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().getValue() - scrollDistance > 0){
                    MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().setValue(MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().getValue() - scrollDistance);
                }else{
                    MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().setValue(0);
                }
            }

            if (e.getKeyCode() == KeyEvent.VK_DOWN && indexOfSave != 0) {
                MainGUI2.mapHistoryPane.setSelected(MainGUI2.saveFileList.get(indexOfSave - 1));
                if (MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().getValue() + scrollDistance < MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().getMaximum()){
                    MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().setValue(MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().getValue() + scrollDistance);
                }else{
                    MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().setValue(MainGUI2.mapHistoryPane.scroll.getVerticalScrollBar().getMaximum());
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}