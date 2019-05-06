package quicknii;

import java.util.ArrayList;
import java.util.List;

public class Slice implements Comparable<Slice> {
    public String filename;
    public double nr;
    public double width;
    public double height;
    public List<Double> anchoring = new ArrayList<>();

    @Override
    public int compareTo(Slice s) {
        return (int) (nr - s.nr);
    }

    private double normalize(int idx) {
        double len = 0;
        for (int i = 0; i < 3; i++)
            len += anchoring.get(idx + i) * anchoring.get(idx + i);
        len = Math.sqrt(len);
        for (int i = 0; i < 3; i++)
            anchoring.set(idx + i, anchoring.get(idx + i) / len);
        return len;
    }

    private void orthonormalize() {
        normalize(3);
        double dot = 0;
        for (int i = 0; i < 3; i++)
            dot += anchoring.get(i + 3) * anchoring.get(i + 6);
        for (int i = 0; i < 3; i++)
            anchoring.set(i + 6, anchoring.get(i + 6) - anchoring.get(i + 3) * dot);
        normalize(6);
    }

    boolean decompose() {
        if (anchoring.size() == 9) {
            for (int i = 0; i < 3; i++)
                anchoring.set(i, anchoring.get(i) + (anchoring.get(i + 3) + anchoring.get(i + 6)) / 2);
            anchoring.add(normalize(3) / width);
            anchoring.add(normalize(6) / height);
            return true;
        }
        return false;
    }

    boolean recompose() {
        if (anchoring.size() == 11) {
            orthonormalize();
            final double wr = anchoring.get(9);
            final double hr = anchoring.get(10);
            for (int i = 0; i < 3; i++) {
                anchoring.set(i + 3, anchoring.get(i + 3) * wr * width);
                anchoring.set(i + 6, anchoring.get(i + 6) * hr * height);
                anchoring.set(i, anchoring.get(i) - (anchoring.get(i + 3) + anchoring.get(i + 6)) / 2);
            }
            anchoring.remove(10);
            anchoring.remove(9);
            return true;
        }
        return false;
    }

    public boolean from(Estimator ests[]) {
        if (anchoring.size() == 0) {
            for (int i = 0; i < ests.length; i++)
                anchoring.add(ests[i].get(nr));
            orthonormalize();
            return true;
        }
        return false;
    }
}
