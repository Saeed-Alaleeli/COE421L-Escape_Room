package escaperoom.state;

/**
 * The 6 levels of the escape room, in order.
 * Each level has its number (also used in the messages to and from the Mega),
 * a title and a hint that are shown on the game screen.
 * LevelState uses this enum, so one LevelState class covers all 6 levels.
 */
public enum Level {
    L1(1, "Level 1 - Copy the Lights",
          "Watch the colored buttons light up, then press them in the same order."),
    L2(2, "Level 2 - Tilt the Platform",
          "Tilt the platform until it registers, and hold it there."),
    L3(3, "Level 3 - Distance",
          "Hold your hand 5-25 cm in front of the ultrasonic sensor."),
    L4(4, "Level 4 - Darkness",
          "Cover the light sensor completely and keep it covered."),
    L5(5, "Level 5 - Enter the Code",
          "Type the 4 digits you collected on the keypad. A = enter, B = clear, C = delete."),
    L6(6, "Level 6 - ESCAPE!",
          "Press the big red button to escape!");

    public final int number;    // level number 1-6
    public final String title;  // shown as the big title on screen
    public final String hint;   // shown under the title to tell players what to do

    Level(int number, String title, String hint) {
        this.number = number; this.title = title; this.hint = hint;
    }

    /** The level after this one, or null after the last level. */
    public Level next() {
        Level[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }
}
