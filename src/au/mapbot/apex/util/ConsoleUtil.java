package au.mapbot.apex.util;

public class ConsoleUtil {

    public static void updateConsoleStatus(final String string){
        System.out.print("\033]0;" + string + "\007");
    }
}
