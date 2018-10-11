package strategies;

public enum Strategies {
    DROP_CREATE,
    UPDATE;

    @Override
    public String toString() {
        switch (this) {
            case UPDATE:
                return "update";
            case DROP_CREATE:
                return "dropcreate";
            default:
                throw new IllegalArgumentException("Non existing strategy.");
        }
    }
}
