package co.surumene.simplecoins.domain;

import java.io.*;
import java.util.*;

/** Read-only Java-edition named-root NBT. Budgets include inflated bytes and node counts. */
public final class NbtReader {
    private final DataInputStream input;
    private int nodes;
    private final int maximum;
    private NbtReader(InputStream stream, int maximum) {
        if (maximum < 1) throw new IllegalArgumentException("NBT budget must be positive");
        this.maximum = maximum;
        input = new DataInputStream(new FilterInputStream(stream) {
            private long consumed;
            private void charge(int n) throws IOException {
                if (n > 0 && (consumed += n) > maximum) throw new IOException("NBT byte budget exceeded");
            }
            @Override public int read() throws IOException { int n=super.read(); if(n!=-1)charge(1); return n; }
            @Override public int read(byte[] b,int o,int n) throws IOException {
                int count=in.read(b,o,n); charge(count); return count;
            }
        });
    }
    public static Map<String,Object> read(InputStream stream, int maximum) throws IOException {
        NbtReader reader = new NbtReader(stream, maximum);
        if (reader.input.readUnsignedByte() != 10) throw new IOException("NBT root must be compound");
        reader.input.readUTF();
        @SuppressWarnings("unchecked") var root=(Map<String,Object>)reader.value(10,0);
        if (reader.input.read() != -1) throw new IOException("Trailing NBT data");
        return root;
    }
    private int length(int width) throws IOException {
        int n=input.readInt();
        if(n<0 || n > maximum / width) throw new IOException("Invalid NBT array/list size");
        return n;
    }
    private Object value(int type,int depth) throws IOException {
        if(depth>64 || ++nodes>Math.min(maximum,1_000_000)) throw new IOException("NBT structure budget exceeded");
        return switch(type) {
            case 1 -> input.readByte();
            case 2 -> input.readShort();
            case 3 -> input.readInt();
            case 4 -> input.readLong();
            case 5 -> input.readFloat();
            case 6 -> input.readDouble();
            case 7 -> { byte[] a=new byte[length(1)]; input.readFully(a); yield a; }
            case 8 -> input.readUTF();
            case 9 -> {
                int child=input.readUnsignedByte(), n=length(1);
                if(child>12 || (child==0 && n!=0)) throw new IOException("Invalid NBT list tag");
                List<Object> a=new ArrayList<>();
                for(int i=0;i<n;i++)a.add(value(child,depth+1));
                yield a;
            }
            case 10 -> {
                Map<String,Object> a=new LinkedHashMap<>();
                for(int child;(child=input.readUnsignedByte())!=0;) {
                    String name=input.readUTF();
                    if(a.containsKey(name))throw new IOException("Duplicate NBT key: "+name);
                    a.put(name,value(child,depth+1));
                }
                yield a;
            }
            case 11 -> { int[] a=new int[length(4)]; for(int i=0;i<a.length;i++)a[i]=input.readInt(); yield a; }
            case 12 -> { long[] a=new long[length(8)]; for(int i=0;i<a.length;i++)a[i]=input.readLong(); yield a; }
            default -> throw new IOException("Unsupported NBT tag: "+type);
        };
    }
}
