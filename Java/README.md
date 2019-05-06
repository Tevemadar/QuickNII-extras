# Propagation algorithm and slicer in Java
Usage:

`java NIISlicer json=<json file> nifti=<nifti file> [label=<label file>] [grayscale] [bin]`

Where
- `<json file>` is a QuickNII JSON file
- `<nifti file>` is an uncompressed NIfTI volume
- `<label file>` is an optional ITK-compatible label file for segmentation volumes
- `grayscale` optionally makes each slice to use the full range of gray levels. This option is implied for floating point volumes
- `bin` optionally outputs raw binary data as a compressed Java object stream

Example:

`java NIISlicer json=series.json nifti=WHS_SD_rat_atlas_v2.nii label=WHS_SD_rat_atlas_v2.lbl bin`

Loading `.bin` files in Matlab:

    stream = java.io.ObjectInputStream(java.util.zip.InflaterInputStream(java.io.FileInputStream("filename.bin")));
    array = stream.readObject;
    stream.close;

Where `array` is going to be a 2D array containing atlas ID-s for segmentation volumes.
