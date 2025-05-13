package edu.grinnell.csc207.compression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    private static class Node {

        private short leaf;
        private int size;
        private Node left = null;
        private Node right = null;

        public Node(short item, int count) {
            leaf = item;
            size = count;
        }

        public Node(int count, Node l, Node r) {
            size = count;
            left = l;
            right = r;
        }
        
        public int getSize() {
            return size;
        }
    }

    private Node root;

    /**
     * Constructs a new HuffmanTree from a frequency map.
     *
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        List<Entry<Short, Integer>> list = new ArrayList(freqs.entrySet());
        List<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            nodeList.add(new Node(list.get(i).getKey(), list.get(i).getValue()));
        }
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(Node::getSize));
        queue.addAll(nodeList);
        Node first;
        Node second;
        int count;
        Node node;
        while (queue.size() >= 2) {
            first = queue.poll();
            second = queue.poll();
            count = first.size + second.size;
            node = new Node(count, first, second);
            queue.add(node);
        }
        root = queue.poll();
    }

    /**
     * Recursively constructs a HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     * @return the top node of the tree
     */
    public Node HuffmanTreeH(BitInputStream in) {
        if (in.hasBits()) {
            int bit = in.readBit();
            if (bit == 0) {
                short bits = (short) in.readBits(9);
                Node cur = new Node(bits, bit);
                return cur;
            } else {
                return new Node(bit, HuffmanTreeH(in), HuffmanTreeH(in));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree(BitInputStream in) {
        root = HuffmanTreeH(in);
    }

    /**
     * Recursively writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     * @param cur the node of the tree we are currently in
     */
    public void serializeH(BitOutputStream out, Node cur) {
        if (cur.left == null && cur.right == null) {
            out.writeBit(0);
            out.writeBits(cur.leaf, 9);
        } else {
            out.writeBit(1);
            serializeH(out, cur.left);
            serializeH(out, cur.right);
        }
    }
    
    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        serializeH(out, root);
    }
   
    /**
     * Makes a map to the HuffmanTree that uses the data as a key and the
     * elements of the code to that data as a value.
     * @param node the node we are currently in
     * @param binary the code for the value as an integer
     * @param depth the number of binary digits of the code to print out
     * @param map the map this is all being stored in
     * @return a map used to find the code of the data
     */
    private Map<Short, Integer[]> makeHuffmanTreeMap(Node node, int binary, 
            int depth, Map<Short, Integer[]> map) {
        if (node.left == null && node.right == null) {
            Integer[] code;
            code = new Integer[2];
            code[0] = binary;
            code[1] = depth;
            map.put(node.leaf, code);
        } else {
            depth++;
            binary = binary << 1;
            map = makeHuffmanTreeMap(node.left, binary, depth, map);
            binary = binary + 1;
            map = makeHuffmanTreeMap(node.right, binary, depth, map);
        }
        return map;
    }
    
    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {
        out.writeBits(1846, 32);
        serialize(out);
        Map<Short, Integer[]> map = new HashMap();
        map = makeHuffmanTreeMap(root, 0, 0, map);
        short bits;
        Integer[] code;
        while (in.hasBits()) {
            bits = (short) in.readBits(8);
            code = map.get(bits);
            out.writeBits(code[0], code[1]);
        }
        code = map.get((short) 256);
        out.writeBits(code[0], code[1]);
    }
    
    /**
     * Recursively decodes a stream of huffman codes to bits.
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     * @param cur the node we are currently in.
     * @return whether or not the decoding has been completed.
     */
    private boolean decodeH(BitInputStream in, BitOutputStream out, Node cur) {
        if (cur.left == null && cur.right == null && cur.leaf == (short) 256) {
            return false;
        } else if (cur.left == null && cur.right == null) {
            out.writeBits(cur.leaf, 8);
            return true;
        } else {
            int bit = in.readBit();
            if (bit == 0) {
                return decodeH(in, out, cur.left);
            } else {
                return decodeH(in, out, cur.right);
            }
        }
    }
    
    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) {
        boolean bool = true;
        while (bool) {
            bool = decodeH(in, out, root);
        }
    }
}
