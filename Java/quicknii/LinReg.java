package quicknii;

public class LinReg implements Estimator {
    private int n = 0;
    private double Sx = 0;
    private double Sy = 0;
    private double Sxx = 0;
    private double Sxy = 0;
    private double a = 0;
    private double b = 0;

    public void add(double x, double y) {
        n++;
        Sx += x;
        Sy += y;
        Sxx += x * x;
        Sxy += x * y;
        if (n >= 2) {
            b = (n * Sxy - Sx * Sy) / (n * Sxx - Sx * Sx);
            a = Sy / n - b * Sx / n;
        }
    }

    public double get(double x) {
        return a + b * x;
    }
}
