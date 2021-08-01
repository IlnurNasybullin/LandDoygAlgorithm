package math.simplex.exception;

/**
 * Class for raising exceptions in case any problem during solving simplex task
 */
public class SimplexSolveException extends IllegalStateException {
    public SimplexSolveException() {
        super();
    }

    public SimplexSolveException(String s) {
        super(s);
    }
}
