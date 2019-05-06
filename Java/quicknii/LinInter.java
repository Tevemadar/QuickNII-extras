package quicknii;

public class LinInter implements Estimator {
    public final double x1;
    public final double y1;
    public final double x2;
    public final double y2;

    public LinInter(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double get(double x) {
        return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
    }
}
