package co.surumene.simplecoins.domain;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
public final class OwnerSavedStorageChecks {
    public static void main(String[] args) throws Exception {
        Path directory=Files.createTempDirectory("owner-saved-");
        Path path=directory.resolve("r.0.0.mca");
        try {
            var nbt=new ByteArrayOutputStream();
            try(var out=new DataOutputStream(nbt)){
                out.writeByte(10);out.writeUTF("");
                out.writeByte(9);out.writeUTF("block_entities");out.writeByte(10);out.writeInt(2);
                for(int x=1;x<=2;x++){
                    out.writeByte(8);out.writeUTF("id");out.writeUTF("minecraft:chest");
                    out.writeByte(3);out.writeUTF("x");out.writeInt(x);
                    out.writeByte(3);out.writeUTF("y");out.writeInt(64);
                    out.writeByte(3);out.writeUTF("z");out.writeInt(1);out.writeByte(0);
                }
                out.writeByte(0);
            }
            byte[] payload=nbt.toByteArray(),region=new byte[4096*3];
            ByteBuffer.wrap(region).putInt(0,(2<<8)|1).putInt(8192,payload.length+1);
            region[8196]=3;System.arraycopy(payload,0,region,8197,payload.length);Files.write(path,region);
            var target=OwnerStorageIndex.Holder.block("world",1,64,1);
            var selected=OwnerSavedStorage.read(path,Set.of(target),1024,1024);
            if(!selected.keySet().equals(Set.of(target))||((List<?>)selected.get(target).nbt().get("block_entities")).size()!=1)throw new AssertionError("wrong selected holder");
            try{OwnerSavedStorage.read(path,Set.of(OwnerStorageIndex.Holder.block("world",3,64,1)),1024,1024);throw new AssertionError("missing holder accepted");}catch(IOException expected){}
            System.out.println("Actual Anvil holder selection and missing-holder rejection passed");
        }finally{Files.deleteIfExists(path);Files.deleteIfExists(directory);}
    }
}
