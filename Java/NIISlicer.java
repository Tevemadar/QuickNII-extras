import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import javax.imageio.ImageIO;

import nii.Nifti1Dataset;
import parsers.ITKLabel;
import quicknii.Series;
import quicknii.Slice;
import slicer.Int32Slices;

public class NIISlicer {
    public static final String NIFTI = "nifti=";
    public static final String JSON = "json=";
    public static final String LABEL = "label=";
    public static final String GRAYSCALE = "grayscale";
    public static final String BIN = "bin";

    public static void main(String[] args) throws Exception {
        String nifti = null;
        String json = null;
        String label = null;
        boolean grayscale = false;
        boolean bin = false;
        for (String arg : args) {
            if (arg.startsWith(NIFTI))
                nifti = arg.substring(NIFTI.length());
            else if (arg.startsWith(JSON))
                json = arg.substring(JSON.length());
            else if (arg.startsWith(LABEL))
                label = arg.substring(LABEL.length());
            else if (arg.equals(GRAYSCALE))
                grayscale = true;
            else if (arg.equals(BIN))
                bin = true;
            else {
                System.err.println("Unknown argument: " + arg);
                help();
                return;
            }
        }
        if (nifti == null) {
            System.err.println("Missing argument: nifti=<nifti file>");
            help();
            return;
        }
        if (json == null) {
            System.err.println("Missing argument: json=<QuickNII json file>");
            help();
            return;
        }
        Map<Integer, ITKLabel> labels = null;
        if (label != null)
            labels = ITKLabel.parseLabels(label);
        Series series = new Series();
        try (FileReader fr = new FileReader(json)) {
            Map<String, String> resolver = new HashMap<>();
            resolver.put("resolution", "target-resolution");
            parsers.JSON.mapObject(parsers.JSON.parse(fr), series, resolver);
        }
        series.propagate();
        Int32Slices slicer = new Int32Slices(nifti);
        grayscale |= slicer.type == Nifti1Dataset.NIFTI_TYPE_FLOAT32;
        String mod = nifti.substring(0, nifti.lastIndexOf('.'));
        for (Slice s : series.slices) {
            Double a[] = s.anchoring.toArray(new Double[0]);
            int slice[][] = slicer.getInt32Slice(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], grayscale);
            int h = slice.length;
            int w = slice[0].length;
            byte pix[] = new byte[w * h * (grayscale ? 1 : 3)];
            if (grayscale)
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++)
                        pix[x + y * w] = (byte) (slice[y][x] >> 8);
            else if (labels == null)
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++)
                        for (int i = 0; i < 3; i++)
                            pix[(x + y * w) * 3 + 2 - i] = (byte) (slice[y][x] >> (8 * i));
            else
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++) {
                        ITKLabel l = labels.get(slice[y][x]);
                        pix[(x + y * w) * 3] = l.red;
                        pix[(x + y * w) * 3 + 1] = l.green;
                        pix[(x + y * w) * 3 + 2] = l.blue;
                    }
            BufferedImage bi = new BufferedImage(w, h,
                    grayscale ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR);
            bi.getRaster().setDataElements(0, 0, w, h, pix);
            ImageIO.write(bi, "png", new File(s.filename.substring(0, s.filename.lastIndexOf('.')) + mod + ".png"));
            if (bin)
                try (ObjectOutputStream oos = new ObjectOutputStream(new DeflaterOutputStream(
                        new FileOutputStream(s.filename.substring(0, s.filename.lastIndexOf('.')) + mod + ".bin")))) {
                    oos.writeObject(slice);
                }
        }
    }

    public static void help() {
        System.out.println("Usage:");
        System.out.println("java NIISlicer nifti=<nifti file> json=<json file> [label=<label file>] [grayscale] [bin]");
        System.out.println();
        System.out.println("Where");
        System.out.println("- <nifti file> is an uncompressed NIfTI volume");
        System.out.println("- <json file> is a QuickNII JSON file");
        System.out.println("- <label file> is an optional ITK-compatible label file for segmentation volumes");
        System.out.println("- grayscale optionally makes each slice to use the full range of gray levels");
        System.out.println("- bin optionally outputs raw binary data as a compressed Java object stream (can be loaded in Matlab)");
        System.out.println();
        System.out.println("grayscale option is implied for floating point volumes.");
    }
}
