/*
 * MessagePromptEnum
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

public enum MessagePromptEnum {
    NONE, WAITING_GAME_START, WAITING_PLAYER_TO_LAND, CANNOT_FIND_WINDOW, FINDING_MAP, CHROME, STREAM_OFFLINE;

    public String getText() {

        // this will refer to the object SMALL
        switch (this) {
            case NONE:
                return "";

            case WAITING_GAME_START:
                return "Waiting for game to start";

            case WAITING_PLAYER_TO_LAND:
                return "Waiting for player to land to begin recording";

            case CANNOT_FIND_WINDOW:
                return "Unable to find active game window";

            case FINDING_MAP:
                return "Attempting to identify current map";

            case CHROME:
                return "Error: Chrome must be installed to use Stream/VOD capture";

            case STREAM_OFFLINE:
                return "Stream is offline or not playing apex";

            default:
                return "";
        }
    }

}
