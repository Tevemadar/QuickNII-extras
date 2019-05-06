package slicer;

import java.io.IOException;
import java.io.RandomAccessFile;

import nii.Nifti1Dataset;

public class Int32Slices {
    public final boolean bigendian;
    public final short type;
    public final short XDIM;
    public final short YDIM;
    public final short ZDIM;
    public final long offset;
    public final String blob;
    public final short BPV;

    /**
     * Class for generating 2D slices from NIfTI datasets
     * 
     * @param aNiftiFile path and filename to the actual NIfTI file
     * @throws Exception if file access fails, unsupported data format encountered,
     *                   or XML does not contain description for id.
     */
    public Int32Slices(String aNiftiFile) throws Exception {
        Nifti1Dataset n1d = new Nifti1Dataset(aNiftiFile);
        n1d.readHeader();
        blob = n1d.ds_datname;
        if (blob.endsWith(".gz"))
            throw new Exception("Compressed Nifti is not supported.");
        type = n1d.datatype;
        BPV = Nifti1Dataset.bytesPerVoxel(type);
        if (BPV > 4)
            throw new Exception(Nifti1Dataset.decodeDatatype(type) + " is not supported.");
        bigendian = n1d.big_endian;
        offset = (long) n1d.vox_offset;
        XDIM = n1d.XDIM;
        YDIM = n1d.YDIM;
        ZDIM = n1d.ZDIM;
    }

    /**
     * @param ox        origin, x coordinate
     * @param oy        origin, y coordinate
     * @param oz        origin, z coordinate
     * @param ux        horizontal axis of the slice, x component
     * @param uy        horizontal axis of the slice, y component
     * @param uz        horizontal axis of the slice, z component
     * @param vx        vertical axis of the slice, x component
     * @param vy        vertical axis of the slice, y component
     * @param vz        vertical axis of the slice, z component
     * @param grayscale scale result into the range of 0-65535. Ignored for RGB
     *                  data. Implied for floating point data
     * @return 2D integer array containing the slice
     * @throws IOException when a file operation fails
     */
    public final int[][] getInt32Slice(double ox, double oy, double oz, double ux, double uy, double uz, double vx,
            double vy, double vz, boolean grayscale) throws IOException {
        final int width = (int) Math.sqrt(ux * ux + uy * uy + uz * uz) + 1;
        final int height = (int) Math.sqrt(vx * vx + vy * vy + vz * vz) + 1;

        int slice[][] = new int[height][width];

        final long dataxlinelen = XDIM * BPV;
        final long datazslicesize = dataxlinelen * YDIM;

        try (RandomAccessFile raf = new RandomAccessFile(blob, "r")) {
            for (int y = 0; y < height; y++) {
                final double hx = ox + vx * y / (height);
                final double hy = oy + vy * y / (height);
                final double hz = oz + vz * y / (height);
                for (int x = 0; x < width; x++) {
                    final int lx = (int) (hx + ux * x / (width));
                    final int ly = (int) (hy + uy * x / (width));
                    final int lz = (int) (hz + uz * x / (width));
                    if (lx >= 0 && lx < XDIM && ly >= 0 && ly < YDIM && lz >= 0 && lz < ZDIM) {
                        raf.seek(offset + BPV * lx + ly * dataxlinelen + lz * datazslicesize);
                        switch (type) {
                        case Nifti1Dataset.NIFTI_TYPE_INT8:
                            slice[y][x] = raf.readByte();
                            break;
                        case Nifti1Dataset.NIFTI_TYPE_UINT8:
                            slice[y][x] = raf.readUnsignedByte();
                            break;
                        case Nifti1Dataset.NIFTI_TYPE_INT16:
                            slice[y][x] = bigendian ? raf.readShort() : Short.reverseBytes(raf.readShort());
                            break;
                        case Nifti1Dataset.NIFTI_TYPE_UINT16:
                            slice[y][x] = bigendian ? raf.readUnsignedShort()
                                    : Integer.reverseBytes(raf.readUnsignedShort() << 16);
                            break;
                        case Nifti1Dataset.NIFTI_TYPE_INT32:
                        case Nifti1Dataset.NIFTI_TYPE_UINT32:
                        case Nifti1Dataset.NIFTI_TYPE_FLOAT32:
                            slice[y][x] = bigendian ? raf.readInt() : Integer.reverseBytes(raf.readInt());
                            break;
                        case Nifti1Dataset.NIFTI_TYPE_RGB24:
                            slice[y][x] = (raf.readUnsignedByte() << 16) + raf.readUnsignedShort();
                            break;
                        }
                    }
                }
            }
        }

        if (type == Nifti1Dataset.NIFTI_TYPE_FLOAT32) {
            float min = Float.MAX_VALUE;
            float max = -min;
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    float w = Float.intBitsToFloat(slice[y][x]);
                    if (w < min)
                        min = w;
                    if (w > max)
                        max = w;
                }
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    slice[y][x] = (int) (65535 * (Float.intBitsToFloat(slice[y][x]) - min) / (max - min));
        } else if (grayscale && type != Nifti1Dataset.NIFTI_TYPE_RGB24) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    int w = slice[y][x];
                    if (w < min)
                        min = w;
                    if (w > max)
                        max = w;
                }
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    slice[y][x] = 65535 * (slice[y][x] - min) / (max - min);
        }

        return slice;
    }
}
