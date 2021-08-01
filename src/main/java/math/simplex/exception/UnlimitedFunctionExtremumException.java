package math.simplex.exception;

public abstract class UnlimitedFunctionExtremumException extends SimplexSolveException {
    public UnlimitedFunctionExtremumException() {
        super();
    }

    public UnlimitedFunctionExtremumException(String s) {
        super(s);
    }
}
