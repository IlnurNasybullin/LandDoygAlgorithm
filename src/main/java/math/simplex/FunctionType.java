package math.simplex;

public enum FunctionType {
    MIN("min"),
    MAX("max");

    FunctionType(String type) {
        this.type = type;
    }

    private final String type;

    public static FunctionType inversion(FunctionType type) {
        if (type == MIN) {
            return MAX;
        }

        return MIN;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return getType();
    }
}
