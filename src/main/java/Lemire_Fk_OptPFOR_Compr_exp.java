import me.lemire.integercompression.*;

import java.io.*;
import java.nio.file.FileSystems;

public class Lemire_Fk_OptPFOR_Compr_exp {

    private Lemire_Fk_OptPFOR_Compr_exp () {}

    protected static void execute (String path, String Fk_To_compr_in, String scenario, String approx_Suffix, int debug) throws InterruptedException, IOException {
        /*
            0   ->  /home/user/IdeaProjects/TradeOff/exp-fragments/                 (initial path)
            1   ->  Staph-k4-DSK-verbatim                                           (Fk folder with fragments)
            2   ->  b                                                               (a: DPx scenario; b: Gaps scenario)
            3   ->  "" / "_approx"                                                  approx_suffix
            4   ->  0/1                                                             no/yes verbose mode
         */
        
        //start time
        long start_time = System.currentTimeMillis();

        String Fk_EXT = ".txt";
        String Fk_compr_EXT = ".txt.opt";
        String nfo_EXT = ".nfo";

        // File System File Separator
        String FS_SEPARATOR = FileSystems.getDefault().getSeparator();

        int num_rows_file = 0;
        long total_rows = 0;
        int num_fragments = 0;
        int k = 0;

        // Lettura informazioni .nfo per ricavare le informazioni: total_rows, k, num_rows_file, num_fragments
        BufferedReader Header_NFO = null;
        try {
            if (scenario.compareTo("a") == 0) {
                Header_NFO = new BufferedReader(new FileReader(path + Fk_To_compr_in + FS_SEPARATOR + Fk_To_compr_in + approx_Suffix + nfo_EXT));
                if (debug==1) System.out.println("OptPFOR COMPR" + "\t" + "Read .nfo file from: " + path + Fk_To_compr_in + FS_SEPARATOR + Fk_To_compr_in + approx_Suffix + nfo_EXT);
            }
            else {
                // delete "-Gk" suffix from Fk_To_compr_in for reading .nfo file !!!.
                Header_NFO = new BufferedReader(new FileReader(path + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + FS_SEPARATOR + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + approx_Suffix + nfo_EXT));
                if (debug==1) System.out.println("OptPFOR COMPR 001" + "\t" + "Read .nfo file from: " + path + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + FS_SEPARATOR + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + approx_Suffix + nfo_EXT);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Extract info from .nfo file with auxiliary variable for reading lines from file
        String l;

        l = Header_NFO.readLine();
        total_rows = Long.parseLong(l);

        l = Header_NFO.readLine();
        k = Integer.parseInt(l);

        l = Header_NFO.readLine();
        num_rows_file = Integer.parseInt(l);

        l = Header_NFO.readLine();
        num_fragments = Integer.parseInt(l);

        // Closing Header File
        Header_NFO.close();


        // add codec for OptPFOR integer compression
        SkippableIntegerCODEC[] codec = {
                new SkippableComposition(new OptPFD(), new VariableByte())
        };


        //FIXME: Reading Fragment Sequence
        int i = 0;
        while (i < num_fragments) {
            // lavoro con i frammenti denotati da _0 .. _(num_fragments - 1), ovvero tutti tranne l'ultimo
            int rows_i;

            if (i != num_fragments - 1) rows_i = num_rows_file;
            else {
                rows_i = (int) (total_rows - ((num_fragments - 1) * num_rows_file));
            }
            //System.out.println("Load Fragment " + i + ". Reading: " + rows_i + " rows.");

            //TODO: storage fragment frequencies to int array
            long start_time_storage_frequencies = System.currentTimeMillis();

            // opening in reading fragment frequencies
            BufferedReader Fk_in = null;
            try {

                if (scenario.compareTo("a") == 0) {
                    Fk_in = new BufferedReader(new FileReader(path + Fk_To_compr_in + FS_SEPARATOR + Fk_To_compr_in + approx_Suffix + "_" + i + Fk_EXT));
                    if (debug==1) System.out.println("\nOptPFOR Compr - Read frequencies from: " + path + Fk_To_compr_in + FS_SEPARATOR + Fk_To_compr_in + approx_Suffix + "_" + i + Fk_EXT);
                }
                else {
                    // delete "-Gk" suffix from Fk_To_compr_in for reading .nfo file !!!.
                    Fk_in = new BufferedReader(new FileReader(path + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + FS_SEPARATOR + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + approx_Suffix + "-Gk_" + i + Fk_EXT));
                    if (debug==1) System.out.println("\nOptPFOR Compr - Read frequencies from: " + path + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + FS_SEPARATOR + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + approx_Suffix + "-Gk_" + i + Fk_EXT);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // Static allocation of the vector of integers with cardinality equal to rows_i
            int[] data = new int[rows_i];
            int aux_freq;

            for (int j = 0; j < data.length; ++j) {

                l = Fk_in.readLine();
                aux_freq = Integer.parseInt(l);
                data[j] = aux_freq;
            }
            // Closing Fk_in input fragment file
            Fk_in.close();

            long end_time_storage_frequencies = System.currentTimeMillis();
            long elapsed_time_storage_frequencies = (end_time_storage_frequencies - start_time_storage_frequencies);
            //System.out.println("Elapsed time for storing frequencies into int array: " + elapsed_time_storage_frequencies + " ms");
            // Occupazione in memoria dell'array data
            long dim_data = 16 + ( 4 * data.length );   // 16 represent overheah (12 array header + 4 array length)
            //System.out.println("Memory size occupation of array data: " + dim_data + " bytes");

            //FIXME: compression phase
            long start_time_compression_integers = System.currentTimeMillis();

            IntCompressor iic = new IntCompressor(codec[0]);
            int[] compressed = iic.compress(data);

            // Memory occupation of the compressed array
            long dim_compressed = 16 + ( 4 * compressed.length );
            //System.out.println("Memory size occupation of array compressed: " + dim_compressed + " bytes");

            long end_time_compression_integers = System.currentTimeMillis();
            long elapsed_time_compression_integers = (end_time_compression_integers - start_time_compression_integers);
            //System.out.println("Elapsed time for compression of integers: " + elapsed_time_compression_integers + " ms");

            // DEBUG
            // int[] recovered = iic.uncompress(compressed);
            // Test
            // Assert.assertArrayEquals(recovered, data);
            // DEBUG
            // System.out.println("Dimensione [#integers] Integer Array Data Uncompressed: " + data.length);
            // System.out.println("Dimensione [#integers] Integer array Data Compressed (OptPFOR): " + compressed.length);
            // System.out.println("Dimensione [#integers] Integer array Data Decompressed (OptPFOR): " + recovered.length);

            //TODO: storage to file phase
            long start_time_storage_to_file = System.currentTimeMillis();
            // Write Output Lemire_out file in .txt.opt format
            PrintWriter Lemire_OptPFOR_out = null;
            try {

                if (scenario.compareTo("a") == 0) {
                    Lemire_OptPFOR_out = new PrintWriter(new FileWriter(path + Fk_To_compr_in + FS_SEPARATOR + Fk_To_compr_in + approx_Suffix + "_" + i + Fk_compr_EXT));
                    if (debug==1) System.out.println("OptPFOR COMPR 002" + "\t" + "Write data to: " + path + Fk_To_compr_in + FS_SEPARATOR + Fk_To_compr_in + approx_Suffix + "_" + i + Fk_compr_EXT);
                }
                else {
                    // delete "-Gk" suffix from Fk_To_compr_in path for reading .nfo file !!!.
                    Lemire_OptPFOR_out = new PrintWriter(new FileWriter(path + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + FS_SEPARATOR + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + approx_Suffix + "-Gk_" + i + Fk_compr_EXT));
                    if (debug==1) System.out.println("OptPFOR COMPR 002" + "\t" + "Write data to: " + path + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + FS_SEPARATOR + Fk_To_compr_in.substring(0, Fk_To_compr_in.length() -3) + approx_Suffix + "-Gk_" + i + Fk_compr_EXT);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // Saving compressed to Lemire_BIC_out file
            for (int x = 0; x < compressed.length; x++) {
                Lemire_OptPFOR_out.println(compressed[x]);
            }
            // Lemire_BIC_output file closure
            Lemire_OptPFOR_out.close();

            long end_time_storage_to_file = System.currentTimeMillis();
            long elapsed_time_storage_to_file = (end_time_storage_to_file - start_time_storage_to_file);
            //System.out.println("Elapsed time for storage to file: " + elapsed_time_storage_to_file + " ms");

            long end_time = System.currentTimeMillis();
            long elapsed_time=(end_time - start_time);
            //System.out.println("Elapsed time: " + elapsed_time + " ms" + "\n\n");

            // update fragment counter
            i++;
        }
        // deallocate int arrays compressed and data
        //TODO: free compressed and data arrays from memory
        System.gc();
    }


    public static void main(String[] args) throws InterruptedException, IOException {

        /*
            0   ->  /home/user/IdeaProjects/TradeOff/exp-fragments/                 (initial path)
            1   ->  Staph-k4-DSK-verbatim                                           (Fk folder with fragments)
            2   ->  b                                                               (a: DPx scenario; b: Gaps scenario)
            3   ->  "" / "_approx"                                                  approx_suffix
            4   ->  0/1                                                             no/yes verbose mode
         */

        Lemire_Fk_OptPFOR_Compr_exp.execute("/home/user/IdeaProjects/TradeOff/exp-fragments/", "Staph-k4-DSK-verbatim", "b", "", 1);
    }
}
