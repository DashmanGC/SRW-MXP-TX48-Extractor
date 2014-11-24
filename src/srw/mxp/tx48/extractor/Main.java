/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package srw.mxp.tx48.extractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jonatan
 */
public class Main {
    
    public static class IndexEntry{
        public String name;
        public long offset;
        //public int size;

        public IndexEntry(){
            name = "";
            offset = 0;
            //size = 0;
        }

        public IndexEntry(String n, long o){
            name = n;
            offset = o;
            //size = s;
        }
    }
    
    static String bin_file = "";
    static int tex_counter = 0;
    static String file_list = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        if (args.length == 2){
            bin_file = args[1];
            
            if (args[0].equals("-i")){ // Insert files
                try{
                    insertFiles();
                }catch (IOException ex) {
                    System.err.println("ERROR: Couldn't read file.");   // END
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else{
                if (args[0].equals("-e")){  // Extract files
                    try{
                        extractFiles();
                        
                        System.out.println("\n" + tex_counter + " files extracted");
                    }catch (IOException ex) {
                        System.err.println("ERROR: Couldn't read file.");   // END
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else{
                    /*if (args[0].equals("-e2")){  // Extract files without padding
                        try{
                            extractFiles(1);

                            System.out.println("\n" + tex_counter + " files extracted");
                        }catch (IOException ex) {
                            System.err.println("ERROR: Couldn't read file.");   // END
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else{*/
                        System.out.println("ERROR: Wrong number of parameters: " + args.length);
                        System.out.println("EXTRACT:\n java -jar tx48_extract -e <file>");
                        //System.out.println("EXTRACT (padding 2):\n java -jar tx48_extract -e2 <file>");
                        System.out.println("INSERT:\n java -jar tx48_extract -i <file>");
                    //}
                }
            }
        }
    }
    
    
    // Parse the file in search of TX48 files
    public static void extractFiles() throws IOException{
        RandomAccessFile f = new RandomAccessFile(bin_file, "r");
        
        long offset = 0;
        byte[] aux = new byte[4];
        
        while (offset < f.length()){
            f.seek(offset);
            
            f.read(aux);
            
            if (aux[0] == 'T' && aux[1] == 'X' && aux[2] == '4' && aux[3] == '8'){  // TX48 found
                file_list += "TX48_";
                
                if (tex_counter < 1000)
                    file_list += "0";
                if (tex_counter < 100)
                    file_list += "0";
                if (tex_counter < 10)
                    file_list += "0";
                
                file_list += tex_counter + ".bmp\n";
                file_list += Long.toString(offset) + "\n";
                
                //System.out.println("Offset before A: " + offset);
                
                //offset = extractTX48(offset, padding);
                offset = extractTX48(offset);
                
                //System.out.println("Offset after A: " + offset);
            }
            else{
                //offset += 2048;
                offset += 64;
                //offset++;
            }
        }
        
        f.close();
        
        if (tex_counter > 0){
            //writeOther(file_list.getBytes(), "_files", ".list");
            writeFileList(bin_file + "_extract/" + bin_file + "_files.list");
        }
    }
    
    
    // Takes a 4-byte hex little endian and returns its int value
    public static int byteSeqToInt(byte[] byteSequence){
        if (byteSequence.length != 4)
            return -1;

        int value = 0;
        value += byteSequence[0] & 0xff;
        value += (byteSequence[1] & 0xff) << 8;
        value += (byteSequence[2] & 0xff) << 16;
        value += (byteSequence[3] & 0xff) << 24;
        return value;
    }


    // Extract a TX48 file from the file at the given offset
    // Returns the offset of the end of the file + padding
    public static long extractTX48(long offset) throws IOException{
        RandomAccessFile file = new RandomAccessFile(bin_file, "r");

        byte[] header = new byte[64];

        boolean bpp8;
        int width = 0;
        int height = 0;
        int size = 0;

        byte[] CLUT = new byte[64];
        byte[] image;
        byte[] aux = new byte[4];

        int padding = 0;
        
        file.seek(offset);

        file.read(header);

        offset += 64;
        
        bpp8 = false;
        if (header[4] == 1)
            bpp8 = true;

        aux[0] = header[8];
        aux[1] = header[9];
        aux[2] = header[10];
        aux[3] = header[11];

        width = byteSeqToInt(aux);

        aux[0] = header[12];
        aux[1] = header[13];
        aux[2] = header[14];
        aux[3] = header[15];

        height = byteSeqToInt(aux);

        aux[0] = header[28];
        aux[1] = header[29];
        aux[2] = header[30];
        aux[3] = header[31];

        size = byteSeqToInt(aux);

        if (bpp8)
            CLUT = new byte[1024];
        else
            CLUT = new byte[64];

        image = new byte[size];

        file.read(CLUT);

        offset += CLUT.length;

        // Colours in the CLUT are in the RGBA format. BMP uses BGRA format, so we need to swap Rs and Bs
        for (int i = 0; i < CLUT.length; i+= 4){
            byte swap = CLUT[i];
            CLUT[i] = CLUT[i+2];
            CLUT[i+2] = swap;
        }

        file.read(image);

        offset += image.length;

        // The image has to be flipped to show it properly in BMP
        int dimX = width;
        if (!bpp8)
            dimX = dimX / 2;

        byte[] pixels_R = image.clone();
        for (int i = 0, j = image.length - dimX; i < image.length; i+=dimX, j-=dimX){
            for (int k = 0; k < dimX; ++k){
                image[i + k] = pixels_R[j + k];
            }
        }

        // If it's a 4bpp image, the nibbles have to be reversed for BMP
        if (!bpp8){
            for (int i = 0; i < image.length; i++){
                image[i] = (byte) ( ( (image[i] & 0x0f) << 4) | ( (image[i] & 0xf0) >> 4) );
            }
        }

        byte depth = 4;

        if (bpp8)
            depth = 8;

        writeBMP("TX48", CLUT, image, width, height, depth, tex_counter);

        // Calculate padding. Files are 2048-byte alligned.
        //int pad_size = 2048;
        //if (pad == 1)
        int pad_size = 64;
        
        int total_size = 64 + CLUT.length + image.length;
        int rest = total_size % pad_size;
        padding = 0;

        if (rest != 0){
            padding = pad_size - rest;
            offset += padding;
        }
        
        
        file.close();
        
        return offset;
    }

    
    // Puts the extracted TX48 files back into the file
    public static void insertFiles() throws IOException{
        // 1) Get the list of files and offsets inside <bin_file>_files.list
        //System.out.println("Reading from " +  bin_file + "_files.list");
        BufferedReader br = new BufferedReader(new FileReader(bin_file + "_files.list"));
        String line;
        int offset;
        
        ArrayList<IndexEntry> entries = new ArrayList<IndexEntry>();

        IndexEntry ie;

        while ((line = br.readLine()) != null) {
            if (!line.isEmpty()){
                offset = Integer.parseInt( br.readLine() );

                ie = new IndexEntry(line, offset);

                entries.add(ie);
            }
        }
        br.close();
        
        // 2) For each file in the list, write it back into the file at the specified offset
        for (int i = 0; i < entries.size(); i++){
            insertTX48(entries.get(i).name, entries.get(i).offset);
        }
    }
    
    
    // Puts one given BMP back into the bin file as a TX48 texture
    public static void insertTX48(String path, long bin_offset) throws IOException{
        RandomAccessFile bmp_file = new RandomAccessFile(path, "r");

        byte[] header = new byte[54];
        byte[] aux = new byte[4];
        byte[] pixels = null;
        byte[] CLUT = null;

        int offset; // Start of image data
        int width = 0;
        //int height = 0;

        byte col_depth; // 04 for 4bpp, 08 for 8bpp

        bmp_file.read(header);

        aux[0] = header[10];
        aux[1] = header[11];
        aux[2] = header[12];
        aux[3] = header[13];
        offset = byteSeqToInt(aux);

        aux[0] = header[18];
        aux[1] = header[19];
        aux[2] = header[20];
        aux[3] = header[21];
        width = byteSeqToInt(aux);

        /*aux[0] = header[22];
        aux[1] = header[23];
        aux[2] = header[24];
        aux[3] = header[25];
        height = byteSeqToInt(aux);*/

        col_depth = header[28];

        // 1) Get the CLUT
        CLUT = new byte[64];
        if (col_depth == 8)
            CLUT = new byte[1024];

        bmp_file.seek(offset - CLUT.length);

        bmp_file.read(CLUT);   // This places us at the beginning of the image data

        // Colours in the CLUT are in the RGBA format. BMP uses BGRA format, so we need to swap Rs and Bs
        for (int i = 0; i < CLUT.length; i+= 4){
            byte swap = CLUT[i];
            CLUT[i] = CLUT[i+2];
            CLUT[i+2] = swap;
        }

        pixels = new byte[(int) bmp_file.length() - offset];

        // 2) Grab the image data
        bmp_file.read(pixels);
        bmp_file.close();

        // 3) Turn it upside down
        byte[] pixels_R = pixels.clone();
        int dimX = width;
        if (col_depth != 8)
            dimX = dimX / 2;

        for (int i = 0, j = pixels.length - dimX; i < pixels.length; i+=dimX, j-=dimX){
            for (int k = 0; k < dimX; ++k){
                pixels[i + k] = pixels_R[j + k];
            }
        }

        // Turns out that if the image is stored as 4bpp, the nibbles have to be reversed
        if (col_depth == 4){
            for (int i = 0; i < pixels.length; i++){
                pixels[i] = (byte) ( ( (pixels[i] & 0x0f) << 4) | ( (pixels[i] & 0xf0) >> 4) );
            }
        }

        // 4) Prepare the TX48 header
        // * This step might be unnecessary, since we're not allowing the images to grow as of now...
        byte[] tx48hed = new byte[64];

        tx48hed[0] = 'T';
        tx48hed[1] = 'X';
        tx48hed[2] = '4';
        tx48hed[3] = '8';

        if (col_depth == 8)
            tx48hed[4] = 1;

        // Next 4 bytes: Width
        tx48hed[8] = (byte) header[18];
        tx48hed[9] = (byte) header[19];
        tx48hed[10] = (byte) header[20];
        tx48hed[11] = (byte) header[21];

        // Next 4 bytes: Height
        tx48hed[12] = (byte) header[22];
        tx48hed[13] = (byte) header[23];
        tx48hed[14] = (byte) header[24];
        tx48hed[15] = (byte) header[25];

        // Next 4 bytes: start of palette (always 64)
        tx48hed[16] = 64;

        // Next 4 bytes: size of palette (64 for 4bpp, 1024 for 8bpp)
        if (col_depth == 4)
            tx48hed[20] = 64;
        else
            tx48hed[21] = 4;    // 0x400 is 1024

        // Next 4 bytes: start of image data
        int start_image = 64 + CLUT.length;

        tx48hed[24] = (byte) (start_image & 0xff);
        tx48hed[25] = (byte) ( (start_image >> 8 ) & 0xff);
        tx48hed[26] = (byte) ( (start_image >> 16 ) & 0xff);
        tx48hed[27] = (byte) ( (start_image >> 24 ) & 0xff);

        // Next 4 bytes: size of image data
        tx48hed[28] = (byte) (pixels.length & 0xff);
        tx48hed[29] = (byte) ( (pixels.length >> 8 ) & 0xff);
        tx48hed[30] = (byte) ( (pixels.length >> 16 ) & 0xff);
        tx48hed[31] = (byte) ( (pixels.length >> 24 ) & 0xff);

        // 5) Overwrite everything
        RandomAccessFile f_bin = new RandomAccessFile(bin_file, "rw");
        
        f_bin.seek(bin_offset);

        f_bin.write(tx48hed);

        bin_offset += 64;

        f_bin.write(CLUT);

        bin_offset += CLUT.length;

        f_bin.write(pixels);

        bin_offset += pixels.length;

        // 6) Calculate padding. Files are 2048-byte alligned.
        int total_size = 64 + CLUT.length + pixels.length;
        int rest = total_size % 2048;
        int padding = 0;

        if (rest != 0){
            padding = 2048 - rest;
            bin_offset += padding;
        }

        System.out.println(path + " inserted successfully.");
    }
    
    
    
    public static void writeFileList(String path) throws IOException{
        PrintWriter pw = new PrintWriter(path);

        pw.print(file_list);

        pw.close();
    }
    
    
    // Outputs a BMP file with the given data
    public static void writeBMP(String filename, byte[] CLUT, byte[] imageData, int width, int height, byte depth, int number){
        byte[] header = new byte[54];

        // Prepare the header
        // * All sizes are little endian

        // Byte 0: '42' (B) Byte 1: '4d' (M)
        header[0] = 0x42;
        header[1] = 0x4d;

        // Next 4 bytes: file size (header + CLUT + pixels)
        int file_size = 54 + CLUT.length + imageData.length;
        header[2] = (byte) (file_size & 0xff);
        header[3] = (byte) ((file_size >> 8) & 0xff);
        header[4] = (byte) ((file_size >> 16) & 0xff);
        header[5] = (byte) ((file_size >> 24) & 0xff);

        // Next 4 bytes: all 0
        header[6] = 0;
        header[7] = 0;
        header[8] = 0;
        header[9] = 0;

        // Next 4 bytes: offset to start of image (header + CLUT)
        int offset = file_size - imageData.length;
        header[10] = (byte) (offset & 0xff);
        header[11] = (byte) ((offset >> 8) & 0xff);
        header[12] = (byte) ((offset >> 16) & 0xff);
        header[13] = (byte) ((offset >> 24) & 0xff);

        // Next 4 bytes: 28 00 00 00
        header[14] = 40;
        header[15] = 0;
        header[16] = 0;
        header[17] = 0;

        // Next 4 bytes: Width
        header[18] = (byte) (width & 0xff);
        header[19] = (byte) ((width >> 8) & 0xff);
        header[20] = (byte) ((width >> 16) & 0xff);
        header[21] = (byte) ((width >> 24) & 0xff);

        // Next 4 bytes: Height
        header[22] = (byte) (height & 0xff);
        header[23] = (byte) ((height >> 8) & 0xff);
        header[24] = (byte) ((height >> 16) & 0xff);
        header[25] = (byte) ((height >> 24) & 0xff);

        // Next 2 bytes: 01 00 (number of planes in the image)
        header[26] = 1;
        header[27] = 0;

        // Next 2 bytes: bits per pixel ( 04 00 or 08 00 )
        header[28] = depth;
        header[29] = 0;

        // Next 4 bytes: 00 00 00 00 (compression)
        header[30] = 0;
        header[31] = 0;
        header[32] = 0;
        header[33] = 0;

        // Next 4 bytes: image size in bytes (pixels)
        header[34] = (byte) (imageData.length & 0xff);
        header[35] = (byte) ((imageData.length >> 8) & 0xff);
        header[36] = (byte) ((imageData.length >> 16) & 0xff);
        header[37] = (byte) ((imageData.length >> 24) & 0xff);

        // Next 12 bytes: all 0 (horizontal and vertical resolution, number of colours)
        header[38] = 0;
        header[39] = 0;
        header[40] = 0;
        header[41] = 0;
        header[42] = 0;
        header[43] = 0;
        header[44] = 0;
        header[45] = 0;
        header[46] = 0;
        header[47] = 0;
        header[48] = 0;
        header[49] = 0;

        // Next 4 bytes: important colours (= number of colours)
        header[50] = 0;
        header[51] = (byte)(CLUT.length / 4);
        header[52] = 0;
        header[53] = 0;

        // Check if folder with the name of the bin_file exists. If not, create it.
        String path = bin_file + "_extract";
        File folder = new File(path);
        if (!folder.exists()){
            boolean success = folder.mkdir();
            if (!success){
                System.err.println("ERROR: Couldn't create folder.");
                return;
            }
        }

        // Create the bmp file inside said folder
        String file_path = filename + "_";

        if (number < 1000)
            file_path += "0";
        if (number < 100)
            file_path += "0";
        if (number < 10)
            file_path += "0";
        
        file_path += number + ".bmp";
        path += "/" + file_path;
        try {
            RandomAccessFile bmp = new RandomAccessFile(path, "rw");

            bmp.write(header);
            bmp.write(CLUT);
            bmp.write(imageData);

            bmp.close();

            System.out.println(file_path + " saved successfully.");
            tex_counter++;
        } catch (IOException ex) {
            System.err.println("ERROR: Couldn't write " + file_path);
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
