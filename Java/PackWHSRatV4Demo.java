import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;

import nii.Nifti1Dataset;
import parsers.ITKLabel;

public class PackWHSRatV4Demo {
    public static void main(String[] args) throws Exception {
    	
    	int compressionLevel = 0; // 0: uncompressed, fast for testing. 9: maximum compression, suggested for release
    	
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream("WHS_Rat_v4_demo.cutlas")))) {

            // file format version "v0" (2 bytes)
            dos.writeByte('v');
            dos.writeByte('0');

            // name of package
            dos.writeUTF("WHS Rat v4");

            // about text, compressed
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream header = new DataOutputStream(new DeflaterOutputStream(baos, new Deflater(9, true)));
                header.writeUTF("<b>Waxholm Space atlas of the Sprague Dawley rat brain</b><br>" + "<br>"
                        + "- MRI: T2*-weighted anatomical MRI at 39 &#xb5;m isotropic resolution<br>"
                        + "- DTI: RGB color FA map at 78 &#xb5;m isotropic resolution<br>"
                        + "- Segmentation: 224 anatomical structures at 39 &#xb5;m isotropic<br>"
                        + "<font color='#FFFFFF'>-</font> resolution<br>" + "<br>"
                        + "Demo package, not for distribution. See more at <a href='https://www.nitrc.org/projects/whs-sd-atlas'>NITRC</a>.");
                header.close();
                dos.writeInt(baos.size());
                baos.writeTo(dos);
            }

            // xdim (width)
            dos.writeShort(512);

            // ydim (length)
            dos.writeShort(1024);

            // zdim (height)
            dos.writeShort(512);

            // number of modalities (MRI,DTI,Segmentation)
            dos.writeByte(3);

            // type 2: 16-bit grayscale (two volumes) [type 1: 8-bit grayscale]
            dos.writeByte(2);

            // name
            dos.writeUTF("MRI");

            // starting volume
            dos.writeByte(0);

            // [min and max are stored for grayscale data only]
            // min value (mapped to 0)
            dos.writeDouble(10.203729);

            // max value (mapped to 65535) [mapped to 255 in case of 8-bit]
            dos.writeDouble(32766);

            // type 5: 24-bit RGB (three volumes)
            dos.writeByte(5);

            // name
            dos.writeUTF("DTI");

            // starting volume
            dos.writeByte(2);

            // type 4: 16-bit indexed [type 3: 8-bit indexed]
            dos.writeByte(4);

            // name
            dos.writeUTF("Segmentation");

            // starting volume
            dos.writeByte(5);

            // using a dense palette is good practice (though it is not vital for 8-bit WHS)
            Map<Integer, Integer> renumber = new HashMap<>();
            renumber.put(0, 0);

            // [palette is for indexed data only]
            // palette starting with id=1, compressed
            {
                Map<Integer, ITKLabel> labels = ITKLabel.parseLabels("WHS_SD_rat_atlas_v4.01.label");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream palette = new DataOutputStream(new DeflaterOutputStream(baos, new Deflater(9)));

                // number of palette entries (empty label
                palette.writeShort(labels.size() - 1);
                for (Entry<Integer, ITKLabel> entry : labels.entrySet()) {
                    if (entry.getKey() != 0) {
                        ITKLabel label = entry.getValue();

                        // palette entry
                        palette.writeByte(label.red);
                        palette.writeByte(label.green);
                        palette.writeByte(label.blue);
                        palette.writeUTF(label.name);
                        renumber.put(entry.getKey(), renumber.size());
                    }
                }
                palette.close();
                dos.writeInt(baos.size());
                baos.writeTo(dos);
            }

            // number of transformations
            dos.writeByte(6);

            // transformations are homogeneous-like 4x4 matrices
            // - for M*v transformation of column vector v
            // - the last, "0 0 0 1" row is not stored
            // name
            dos.writeUTF("WHS (mm)");

            // x-row
            dos.writeDouble(0.0390625);
            dos.writeDouble(0.0);
            dos.writeDouble(-0.0);
            dos.writeDouble(-9.53125);

            // y-row
            dos.writeDouble(0.0);
            dos.writeDouble(0.0390625);
            dos.writeDouble(-0.0);
            dos.writeDouble(-24.335938);

            // z-row
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0390625);
            dos.writeDouble(-9.6875);

            dos.writeUTF("Bregma (mm)");
            dos.writeDouble(0.0390625);
            dos.writeDouble(0.0);
            dos.writeDouble(-0.0);
            dos.writeDouble(-9.609375);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0390625);
            dos.writeDouble(-0.0);
            dos.writeDouble(-25.5078125);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0390625);
            dos.writeDouble(-17.1875);
            dos.writeUTF("Lambda (mm)");
            dos.writeDouble(0.0390625);
            dos.writeDouble(0.0);
            dos.writeDouble(-0.0);
            dos.writeDouble(-9.53125);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0390625);
            dos.writeDouble(-0.0);
            dos.writeDouble(-17.265625);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0390625);
            dos.writeDouble(-18.125);
            dos.writeUTF("WHS (voxels)");
            dos.writeDouble(1.0);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(-244);
            dos.writeDouble(0.0);
            dos.writeDouble(1.0);
            dos.writeDouble(0.0);
            dos.writeDouble(-623);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(1.0);
            dos.writeDouble(-248);
            dos.writeUTF("Bregma (voxels)");
            dos.writeDouble(1.0);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(-246);
            dos.writeDouble(0.0);
            dos.writeDouble(1.0);
            dos.writeDouble(0.0);
            dos.writeDouble(-653);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(1.0);
            dos.writeDouble(-440);
            dos.writeUTF("Lambda (voxels)");
            dos.writeDouble(1.0);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(-244);
            dos.writeDouble(0.0);
            dos.writeDouble(1.0);
            dos.writeDouble(0.0);
            dos.writeDouble(-442);
            dos.writeDouble(0.0);
            dos.writeDouble(0.0);
            dos.writeDouble(1.0);
            dos.writeDouble(-464);

            // number of volumes
            dos.writeByte(7);

            // MRI, two compressed volumes (MSB volume first, LSB volume next)
            {
                ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                DeflaterOutputStream dos1 = new DeflaterOutputStream(baos1, new Deflater(compressionLevel));
                DeflaterOutputStream dos2 = new DeflaterOutputStream(baos2, new Deflater(compressionLevel));
                Nifti1Dataset n1d = new Nifti1Dataset("WHS_SD_rat_T2star_v1.01.nii");
                n1d.readHeader();
                try (FileInputStream fis = new FileInputStream(n1d.getDataFilename());
                	 DataInputStream dis = n1d.getDataFilename().endsWith(".gz")?
                        new DataInputStream(new GZIPInputStream(fis)):
                        new DataInputStream(new BufferedInputStream(fis))) {
                    dis.skipBytes((int) n1d.vox_offset);
                    System.out.println("Compressing MRI");
                    for (int i = 0; i < 1024 * 512 * 512; i++) {
                        if ((i & 262143) == 0)
                            System.out.print("\rMRI " + (1023 - (i >> 18)) + " ");
                        int w = (int) ((Float.intBitsToFloat(Integer.reverseBytes(dis.readInt())) - 10.203729) * 65535
                                / (32766 - 10.203729));
                        dos1.write(w >> 8);
                        dos2.write(w);
                    }
                    dos1.close();
                    dos2.close();
                    System.out.println();
                    System.out.println("Writing MRI");
                    dos.writeInt(baos1.size());
                    baos1.writeTo(dos);
                    dos.writeInt(baos2.size());
                    baos2.writeTo(dos);
                }
            }

            // DTI, three compressed volumes (R,G,B)
            {
                ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
                DeflaterOutputStream dos1 = new DeflaterOutputStream(baos1, new Deflater(compressionLevel));
                DeflaterOutputStream dos2 = new DeflaterOutputStream(baos2, new Deflater(compressionLevel));
                DeflaterOutputStream dos3 = new DeflaterOutputStream(baos3, new Deflater(compressionLevel));
                Nifti1Dataset n1d = new Nifti1Dataset("WHS_SD_rat_FA_color_v1.01.nii.gz");
                n1d.readHeader();
                try (FileInputStream fis = new FileInputStream(n1d.getDataFilename());
                   	 DataInputStream dis = n1d.getDataFilename().endsWith(".gz")?
                           new DataInputStream(new GZIPInputStream(fis)):
                           new DataInputStream(new BufferedInputStream(fis))) {
                    dis.skipBytes((int) n1d.vox_offset);
                    System.out.println("Compressing DTI");
                    for (int i = 0; i < 1024 * 512 * 512; i++) {
                        if ((i & 262143) == 0)
                            System.out.print("\rDTI " + (1023 - (i >> 18)) + " ");
                        dos1.write(dis.readByte());
                        dos2.write(dis.readByte());
                        dos3.write(dis.readByte());
                    }
                    dos1.close();
                    dos2.close();
                    dos3.close();
                    System.out.println();
                    System.out.println("Writing DTI");
                    dos.writeInt(baos1.size());
                    baos1.writeTo(dos);
                    dos.writeInt(baos2.size());
                    baos2.writeTo(dos);
                    dos.writeInt(baos3.size());
                    baos3.writeTo(dos);
                }
            }

            // Segmentation, two compressed volumes (MSB volume first, LSB volume next)
            {
                ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                DeflaterOutputStream dos1 = new DeflaterOutputStream(baos1, new Deflater(compressionLevel));
                DeflaterOutputStream dos2 = new DeflaterOutputStream(baos2, new Deflater(compressionLevel));
                Nifti1Dataset n1d = new Nifti1Dataset("WHS_SD_rat_atlas_v4.01.nii.gz");
                n1d.readHeader();
                try (FileInputStream fis = new FileInputStream(n1d.getDataFilename());
                   	 DataInputStream dis = n1d.getDataFilename().endsWith(".gz")?
                           new DataInputStream(new GZIPInputStream(fis)):
                           new DataInputStream(new BufferedInputStream(fis))) {
                    dis.skipBytes((int) n1d.vox_offset);
                    System.out.println("Compressing Segmentation");
                    for (int i = 0; i < 1024 * 512 * 512; i++) {
                        if ((i & 262143) == 0)
                            System.out.print("\rSeg. " + (1023 - (i >> 18)) + " ");
                        int voxel = Short.reverseBytes(dis.readShort());
                        int w = renumber.get(voxel);
                        dos1.write(w >> 8);
                        dos2.write(w);
                    }
                    dos1.close();
                    dos2.close();
                    System.out.println();
                    System.out.println("Writing Segmentation");
                    dos.writeInt(baos1.size());
                    baos1.writeTo(dos);
                    dos.writeInt(baos2.size());
                    baos2.writeTo(dos);
                }
            }
        }
    }
}
