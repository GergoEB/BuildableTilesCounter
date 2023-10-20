package xyz.semetrix.scanmap;

import arc.struct.ObjectMap;
import arc.struct.StringMap;
import arc.util.io.CounterInputStream;
import mindustry.ctype.ContentType;
import mindustry.ctype.MappableContent;
import mindustry.io.SaveIO;
import mindustry.io.SaveVersion;
import mindustry.world.Tile;
import mindustry.world.WorldContext;

import java.awt.image.BufferedImage;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.content;

public class CustomMapIO {
    public Map map;
    public MappableContent[][] mappableContent;
    public int width, height;

    CustomMapIO() {}

    public void readMap(InputStream is) throws IOException{
        try(InputStream ifs = new InflaterInputStream(is); CounterInputStream counter = new CounterInputStream(ifs); DataInputStream stream = new DataInputStream(counter)){
            this.map = new Map();

            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            StringMap[] metaOut = {null};
            ver.region("meta", stream, counter, in -> metaOut[0] = ver.readStringMap(in));

            StringMap meta = metaOut[0];

            this.map.name = meta.get("name", "Unknown");
            this.map.author = meta.get("author");
            this.map.description = meta.get("description");
            this.map.tags = meta;

            this.width = meta.getInt("width");
            this.height = meta.getInt("height");

            ver.region("content", stream, counter, this::readContentHeader);
            ver.region("preview_map", stream, counter, in -> {});
        }
    }

    public static class Map{
        public String name, author, description;
        public ObjectMap<String, String> tags = new ObjectMap<>();
        public BufferedImage image;
    }

    public void readContentHeader(DataInput stream) throws IOException{
        byte mapped = stream.readByte();

        MappableContent[][] map = new MappableContent[ContentType.all.length][0];

        for(int i = 0; i < mapped; i++){
            ContentType type = ContentType.all[stream.readByte()];
            short total = stream.readShort();
            map[type.ordinal()] = new MappableContent[total];

            for(int j = 0; j < total; j++){
                String name = stream.readUTF();
                //fallback only for blocks
                map[type.ordinal()][j] = content.getByName(type, name);
            }
        }
        this.mappableContent = map;
    }
}
