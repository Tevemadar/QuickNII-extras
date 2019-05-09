package parsers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ITKLabel {
    public final String name;
    public final byte red;
    public final byte green;
    public final byte blue;

    public ITKLabel(String name, byte red, byte green, byte blue) {
        this.name = name;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public static Map<Integer, ITKLabel> parseLabels(String labelFile) throws Exception {
        Map<Integer, ITKLabel> palette = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(labelFile))) {
            Pattern p = Pattern.compile("\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)[^\\\"]*\"(.*)\"\\s*");
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    int id = Integer.parseInt(m.group(1));
                    if (palette.containsKey(id))
                        throw new Exception("Duplicate label #" + id + " (" + m.group(4) + ")");
                    palette.put(id, new ITKLabel(m.group(5), (byte) Integer.parseInt(m.group(2)),
                            (byte) Integer.parseInt(m.group(3)), (byte) Integer.parseInt(m.group(4))));
                }
            }
        }
        return palette;
    }
}
