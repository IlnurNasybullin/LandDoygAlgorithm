package math.simplex;

import math.simplex.Inequality;

public class Constraint {

    private double[] ai;
    private Inequality inequality;
    private double bi;

    public double[] getAi() {
        return ai;
    }

    public void setAi(double[] ai) {
        this.ai = ai;
    }

    public Inequality getInequality() {
        return inequality;
    }

    public void setInequality(Inequality inequality) {
        this.inequality = inequality;
    }

    public double getBi() {
        return bi;
    }

    public void setBi(double bi) {
        this.bi = bi;
    }
}
