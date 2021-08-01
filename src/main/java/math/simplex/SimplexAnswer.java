package math.simplex;

import java.io.Serializable;

public class SimplexAnswer implements Serializable {

    private double[] X;
    private double fx;

    public SimplexAnswer() {}

    public SimplexAnswer(double[] X, double fx) {
        this.X = X;
        this.fx = fx;
    }

    /** Alias for {@link #getX()} */
    public double[] X() {
        return X;
    }

    /** Alias for {@link #getFx()}  */
    public double fx() {
        return fx;
    }

    public double[] getX() {
        return X;
    }

    public double getFx() {
        return fx;
    }

    public void setX(double[] x) {
        X = x;
    }

    public void setFx(double fx) {
        this.fx = fx;
    }
}
