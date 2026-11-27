/** Shared message format between ChatClient and ChatServer. One message per line, fields split by '|'. */
public final class Protocol {
    private Protocol() {}

    public static final int DEFAULT_PORT = 5000;
    public static final String SEP = "|";

    // client -> server
    public static final String JOIN = "JOIN";
    public static final String CHAT = "CHAT";
    public static final String VOTE = "VOTE";
    public static final String READY = "READY";

    // server -> client
    public static final String SYS = "SYS";
    public static final String ERR = "ERR";
    public static final String USERS = "USERS";
    public static final String VOTES = "VOTES";
    public static final String READY_LIST = "READY_LIST";

    public static final String[] MAPS = {"Dust Yard", "Neon City", "Frost Peak", "Iron Docks"};

    /** Build a message line, e.g. build(CHAT, "nick", "hi") -> CHAT|nick|hi */
    public static String build(String command, String... parts) {
        StringBuilder sb = new StringBuilder(command);
        for (String p : parts) sb.append(SEP).append(p);
        return sb.toString();
    }

    /** Split a line into at most 'limit' fields. */
    public static String[] parse(String line, int limit) {
        return line.split("\\|", limit);
    }

    /** Remove characters that would break the protocol from a nickname. */
    public static String cleanNick(String nick) {
        return nick.replace("|", "").replace(",", "").replace(":", "").trim();
    }
}
