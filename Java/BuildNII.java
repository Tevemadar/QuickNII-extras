import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import nii.Nifti1Dataset;
import quicknii.Series;
import quicknii.Slice;

public class BuildNII {
	public static final String JSON="json=";
	public static final String NIFTI="nifti=";
	
	public static void main(String[] args) throws Exception {
        String nifti = null;
        String json = null;
        for (String arg : args) {
            if (arg.startsWith(NIFTI))
                nifti = arg.substring(NIFTI.length());
            else if (arg.startsWith(JSON))
                json = arg.substring(JSON.length());
            else {
                System.err.println("Unknown argument: " + arg);
                help();
                return;
            }
        }
        if (json == null) {
            System.err.println("Missing argument: json=<QuickNII json file>");
            help();
            return;
        }
        if (nifti == null) {
            System.err.println("Missing argument: nifti=<nifti file>");
            help();
            return;
        }
        if(new File(nifti).exists()) {
            System.err.println("Output file ("+nifti+") exists already.");
            help();
            return;
        }

        Series series = new Series();
        try (FileReader fr = new FileReader(json)) {
            Map<String, String> resolver = new HashMap<>();
            resolver.put("resolution", "target-resolution");
            parsers.JSON.mapObject(parsers.JSON.parse(fr), series, resolver);
        }
        series.propagate();

        int xdim=series.resolution.get(0).intValue();
        int ydim=series.resolution.get(1).intValue();
        int zdim=series.resolution.get(2).intValue();
		
		final long start=System.currentTimeMillis();
		short R[]=new short[xdim*ydim*zdim];
		short G[]=new short[xdim*ydim*zdim];
		short B[]=new short[xdim*ydim*zdim];
		short C[]=new short[xdim*ydim*zdim];
		byte RGB[]=new byte[xdim*ydim*zdim*3];
		System.out.println(System.currentTimeMillis()-start);
		
		for(Slice s:series.slices) {
			System.out.println(s.filename);
			BufferedImage bi=ImageIO.read(new File(s.filename));
			double ox=s.anchoring.get(0);
			double oy=s.anchoring.get(1);
			double oz=s.anchoring.get(2);
			double ux=s.anchoring.get(3);
			double uy=s.anchoring.get(4);
			double uz=s.anchoring.get(5);
			double vx=s.anchoring.get(6);
			double vy=s.anchoring.get(7);
			double vz=s.anchoring.get(8);
			int w=bi.getWidth()-1;
			int h=bi.getHeight()-1;
			for(int y=0;y<=h;y++)
				for(int x=0;x<=w;x++)
				{
					int lx=(int)(ox+x*ux/w+y*vx/h);
					if((lx>=0) && (lx<xdim))
					{
						int ly=(int)(oy+x*uy/w+y*vy/h);
						if((ly>=0) && (ly<ydim))
						{
							int lz=(int)(oz+x*uz/w+y*vz/h);
							if((lz>=0) && (lz<zdim))
							{
								int c=bi.getRGB(x, y);
								int idx=lx+ly*xdim+lz*xdim*ydim;
								R[idx]+=(c >> 16) & 255;
								G[idx]+=(c >> 8) & 255;
								B[idx]+=c & 255;
								C[idx]++;
							}
						}
					}
				}
		}
		System.out.println(System.currentTimeMillis()-start);
		System.out.println("Pack");
		for(int i=0;i<xdim*ydim*zdim;i++)
		{
			int c=C[i];
			if(c==0)
			{
				B[i]=255;
				R[i]=255;
				G[i]=255;
				c=1;
			}
			RGB[i*3]=(byte)(R[i]/c);
			RGB[i*3+1]=(byte)(G[i]/c);
			RGB[i*3+2]=(byte)(B[i]/c);
		}
		System.out.println(System.currentTimeMillis()-start);
		System.out.println("Write");
		Nifti1Dataset nii=new Nifti1Dataset(nifti);
		nii.setDims((short)3, (short)xdim, (short)ydim, (short)zdim, (short)0, (short)0, (short)0, (short)0);
		nii.sform_code=Nifti1Dataset.NIFTI_XFORM_SCANNER_ANAT;
		nii.srow_x[0]=nii.srow_y[1]=nii.srow_z[2]=1;
		nii.setDatatype(Nifti1Dataset.NIFTI_TYPE_RGB24);
		nii.setHeaderFilename(nifti);
		nii.writeHeader();
		nii.writeData(RGB);
		System.out.println(System.currentTimeMillis()-start);
	}

    public static void help() {
        System.out.println("Usage:");
        System.out.println("java BuildNII json=<json file> nifti=<nifti file>");
        System.out.println();
        System.out.println("Where");
        System.out.println("- <json file> is a QuickNII JSON file");
        System.out.println("- <nifti file> is the uncompressed NIfTI file to be created");
        System.out.println();
        System.out.println("BuildNII will look for the actual image files in the current directory.");
    }
}
