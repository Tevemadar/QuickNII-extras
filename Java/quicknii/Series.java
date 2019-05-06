package quicknii;

import java.util.ArrayList;
import java.util.List;

public class Series {
    public List<Double> resolution = new ArrayList<>();
    public String name;
    public String target;
    public List<Slice> slices = new ArrayList<>();

    public void propagate() {
        slices.sort(null);
        LinReg linregs[] = new LinReg[11];
        for (int i = 0; i < linregs.length; i++)
            linregs[i] = new LinReg();
        int count = 0;
        for (Slice s : slices) {
            if (s.decompose()) {
                count++;
                for (int i = 0; i < linregs.length; i++)
                    linregs[i].add(s.nr, s.anchoring.get(i));
            }
        }
        if (count >= 2) {
            if (slices.get(0).from(linregs))
                count++;
            if (slices.get(slices.size() - 1).from(linregs))
                count++;
            int start = 1;
            while (count < slices.size()) {
                while (slices.get(start).anchoring.size() > 0)
                    start++;
                int next = start + 1;
                while (slices.get(next).anchoring.size() == 0)
                    next++;
                Slice prevs = slices.get(start - 1);
                Slice nexts = slices.get(next);
                LinInter linints[] = new LinInter[11];
                for (int i = 0; i < linints.length; i++)
                    linints[i] = new LinInter(prevs.nr, prevs.anchoring.get(i), nexts.nr, nexts.anchoring.get(i));
                for (int i = start; i < next; i++) {
                    Slice s = slices.get(i);
                    for (int j = 0; j < linints.length; j++)
                        s.anchoring.add(linints[j].get(s.nr));
                    count++;
                }
                start = next + 1;
            }
        }
        for (Slice s : slices) {
            s.recompose();
        }
    }
}
