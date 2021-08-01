package math.simplex;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Inequality {
    EQ("="),
    LQ("<="),
    LE("<"),
    GE(">="),
    GR(">");

    Inequality(String symbol) {
        this.symbol = symbol;
    }

    private final String symbol;

    @JsonValue
    public String getSymbol() {
        return symbol;
    }

    public static Inequality inversion(Inequality inequalities) {
        return switch (inequalities) {
            case EQ -> EQ;
            case GE -> LQ;
            case LE -> GR;
            case LQ -> GE;
            case GR -> LE;
        };
    }

    @Override
    public String toString() {
        return getSymbol();
    }
}
