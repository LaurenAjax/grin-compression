package edu.grinnell.csc207.compression;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {

    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     *
     * @param infile the file to decode
     * @param outfile the file to output to
     * @throws java.io.IOException if file cannot be read
     */
    public static void decode(String infile, String outfile) throws IOException {
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile, false);
        if (in.readBits(32) == 1846) {
            HuffmanTree tree = new HuffmanTree(in);
            tree.decode(in, out);
        } else {
            throw new IllegalArgumentException();
        }
        in.close();
        out.close();
    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of those
     * sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     *
     * @param file the file to read
     * @return a frequency map for the given file
     * @throws java.io.IOException if file cannot be read
     */
    public static Map<Short, Integer> createFrequencyMap(String file) throws IOException {
        Map<Short, Integer> map = new HashMap();
        BitInputStream in = new BitInputStream(file);
        while (in.hasBits()) {
            short read = (short) in.readBits(8);
            if (map.containsKey(read)) {
                map.put(read, map.get(read) + 1);
            } else {
                map.put(read, 1);
            }
        }
        map.put((short) 256, 1);
        return map;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     *
     * @param infile the file to encode.
     * @param outfile the file to write the output to.
     * @throws java.io.IOException if file cannot be read
     */
    public static void encode(String infile, String outfile) throws IOException {
        Map<Short, Integer> map = createFrequencyMap(infile);
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile, false);
        HuffmanTree tree = new HuffmanTree(map);
        tree.encode(in, out);
        in.close();
        out.close();
    }

    /**
     * The entry point to the program.
     *
     * @param args the command-line arguments.
     * @throws java.io.IOException if file cannot be read
     */
    public static void main(String[] args) throws IOException {
        File file = new File(args[1]);
        if (file.exists()) {
            if (args.length != 3) {
                System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
                System.exit(-1);
            }
            switch (args[0]) {
                case "encode":
                    encode(args[1], args[2]);
                    break;
                case "decode":
                    decode(args[1], args[2]);
                    break;
                default:
                    System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
                    System.exit(-1);
            }
            System.exit(0);
        } else {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            System.exit(-1);
        }
    }
}
