/*
 * MapEnum
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

public enum MapEnum {
    BROKENMOON, WORLDSEDGE, STORMPOINT, STORMPOINT_19, OLYMPUS, KINGSCANYON, DISTRICT;

    public String getText() {

        // this will refer to the object SMALL
        switch(this) {
            case BROKENMOON:
                return "Broken Moon";

            case WORLDSEDGE:
                return "World's Edge";

            case STORMPOINT:
                return "Storm Point";

            case STORMPOINT_19:
                return "Storm Point";

            case OLYMPUS:
                return "Olympus";

            case KINGSCANYON:
                return "Kings Canyon";

            case DISTRICT:
                return "E-District";

            default:
                return null;
        }
    }

    public String getSaveFileName() {

        // this will refer to the object SMALL
        switch(this) {
            case BROKENMOON:
                return "BrokenMoon";

            case WORLDSEDGE:
                return "WorldsEdge";

            case STORMPOINT:
                return "Stormpoint";

            case STORMPOINT_19:
                return "Stormpoint19";

            case OLYMPUS:
                return "Olympus";

            case KINGSCANYON:
                return "KingsCanyon";

            case DISTRICT:
                return "District";

            default:
                return null;
        }
    }

    public static MapEnum getEnumFromSaveFileName(final String text) {

        // this will refer to the object SMALL
        switch(text) {
            case "BrokenMoon":
                return BROKENMOON;

            case "WorldsEdge":
                return WORLDSEDGE;

            case "Stormpoint":
                return STORMPOINT;

            case "Stormpoint19":
                return STORMPOINT_19;

            case "Olympus":
                return OLYMPUS;

            case "KingsCanyon":
                return KINGSCANYON;

            case "District":
                return DISTRICT;

            default:
                return null;
        }
    }

    public static MapEnum parseFromDisplayNameString(final String text){
        switch(text) {
            case "Broken Moon":
                return BROKENMOON;

            case "World's Edge":
                return WORLDSEDGE;

            case "Storm Point":
                return STORMPOINT;

            case "Storm Point 19":
                return STORMPOINT_19;

            case "Olympus":
                return OLYMPUS;

            case "Kings Canyon":
                return KINGSCANYON;

            case "E-District":
                return DISTRICT;

            default:
                return null;
        }
    }

    public  String getMapAllowedLocationResourceString(){
        // this will refer to the object SMALL
        switch(this) {
            case BROKENMOON:
                return "/maps/BrokenMoonBW.png";

            case WORLDSEDGE:
                return "/maps/WorldsEdgeBW.png";

            case STORMPOINT:
                return "/maps/Stormpoint19BW.png";

            case STORMPOINT_19:
                return "/maps/Stormpoint19BW.png";

            case OLYMPUS:
                return "/maps/OlympusBW.png";

            case KINGSCANYON:
                return "/maps/KingsCanyonBW.png";

            case DISTRICT:
                return "/maps/DistrictBW.png";

            default:
                return null;
        }
    }

    public  String getMapAllowedBeaconResourceString(){
        // this will refer to the object SMALL
        switch(this) {
            case BROKENMOON:
                return "/maps/BrokenMoonBeaconAllowed.png";

            case WORLDSEDGE:
                return "/maps/WorldsEdgeBeaconAllowed.png";

            case STORMPOINT:
                return "/maps/Stormpoint19BeaconAllowed.png";

            case STORMPOINT_19:
                return "/maps/Stormpoint19BeaconAllowed.png";

            case OLYMPUS:
                return "/maps/OlympusBeaconAllowed.png";

            case KINGSCANYON:
                return "/maps/KingsCanyonBeaconAllowed.png";

            case DISTRICT:
                return "/maps/DistrictBeaconAllowed.png";

            default:
                return null;
        }
    }

    public  String getMapAllowedRCResourceString(){
        // this will refer to the object SMALL
        switch(this) {
            case BROKENMOON:
                return "/maps/BrokenMoonRCAllowed.png";

            case WORLDSEDGE:
                return "/maps/WorldsEdgeRCAllowed.png";

            case STORMPOINT:
                return "/maps/Stormpoint19RCAllowed.png";

            case STORMPOINT_19:
                return "/maps/Stormpoint19RCAllowed.png";

            case OLYMPUS:
                return "/maps/OlympusRCAllowed.png";

            case KINGSCANYON:
                return "/maps/KingsCanyonRCAllowed.png";

            case DISTRICT:
                return "/maps/DistrictRCAllowed.png";

            default:
                return null;
        }
    }

    public  String getMapResourceString(){
        // this will refer to the object SMALL
        switch(this) {
            case BROKENMOON:
                return "/maps/BrokenMoon.png";

            case WORLDSEDGE:
                return "/maps/WorldsEdge.png";

            case STORMPOINT:
                return "/maps/Stormpoint19.png";

            case STORMPOINT_19:
                return "/maps/Stormpoint19.png";

            case OLYMPUS:
                return "/maps/Olympus.png";

            case KINGSCANYON:
                return "/maps/KingsCanyon.png";

            case DISTRICT:
                return "/maps/District.png";

            default:
                return null;
        }
    }
}

