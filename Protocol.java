
public final class Protocol {
    private Protocol() {}

    public static final int DEFAULT_PORT = 5000;
    public static final String SEP = "|";

    public static final String JOIN = "JOIN";
    public static final String CHAT = "CHAT";
    public static final String VOTE = "VOTE";
    public static final String READY = "READY";

    public static final String SYS = "SYS";
    public static final String ERR = "ERR";
    public static final String USERS = "USERS";
    public static final String VOTES = "VOTES";
    public static final String READY_LIST = "READY_LIST";

    public static final String[] MAPS = {"Dust Yard", "Neon City", "Frost Peak", "Iron Docks"};

    public static String build(String command, String... parts) {
        StringBuilder sb = new StringBuilder(command);
        for (String p : parts) sb.append(SEP).append(p);
        return sb.toString();
    }

    public static String[] parse(String line, int limit) {
        return line.split("\\|", limit);
    }

    public static String cleanNick(String nick) {
        return nick.replace("|", "").replace(",", "").replace(":", "").trim();
    }
}
