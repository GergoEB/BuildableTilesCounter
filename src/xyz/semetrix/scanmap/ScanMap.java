package xyz.semetrix.scanmap;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.Block;
import mindustry.world.Tile;

import java.util.ArrayList;
import java.util.List;

import static mindustry.Vars.state;
import static mindustry.Vars.world;
import static mindustry.world.Build.contactsShallows;

@SuppressWarnings("unused")
public class ScanMap extends Plugin{

    //called when game initializes
    @Override
    public void init(){
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("counttiles", "List all buildable tiles", args ->
                Log.info(scanMap()));
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.register("counttiles", "List all buildable tiles", (String[] args, Player player) ->
                player.sendMessage(scanMap()));
    }

    public String scanMap() {
        long startTime = System.currentTimeMillis();

        List<Block> builds = new ArrayList<>();
        builds.add(Blocks.copperWall);
        builds.add(Blocks.copperWallLarge);       //Gradle couldn't find List.of?????
        builds.add(Blocks.liquidTank);
        builds.add(Blocks.impactReactor);
        builds.add(Blocks.multiplicativeReconstructor);
        builds.add(Blocks.largeLogicDisplay);
        builds.add(Blocks.exponentialReconstructor);

        MaxTiles maxTiles = new MaxTiles();

        for (Block block : builds) {
            int blockSize = block.size;

            for (int x = 0; x < state.map.width; x += blockSize) {
                for (int y = 0; y < state.map.height; y += blockSize) {
                    if (validPlace(block, x, y, 0)) {
                        maxTiles.increment(blockSize);
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        int totalTiles = state.map.height * state.map.width;

        return String.format("""
                [accent]On %s [accent], which is %sx%s, you can place:
                - [red]%s  [accent]1x1 Builds[accent]
                - [red]%s  [accent]2x2 Builds[accent]
                - [red]%s  [accent]3x3 Builds[accent]
                - [red]%s  [accent]4x4 Builds[accent]
                Also, this took [red]%s[accent]ms!
                """,
                state.map.name(), state.map.width, state.map.height,
                maxTiles.one, maxTiles.one / totalTiles * 100,
                maxTiles.two, maxTiles.one / totalTiles * 100,
                maxTiles.three, maxTiles.one / totalTiles * 100,
                maxTiles.four, maxTiles.one / totalTiles * 100,
                elapsedTime
        );
    }

    public static boolean validPlace(Block type, int x, int y, int rotation){
        Tile tile = world.tile(x, y);

        if(tile == null) return false;

        //campaign darkness check
        if(world.getDarkness(x, y) >= 3){
            return false;
        }

        if(!type.requiresWater && !contactsShallows(tile.x, tile.y, type) && !type.placeableLiquid){
            return false;
        }

        if((type.isFloor() && tile.floor() == type) || (type.isOverlay() && tile.overlay() == type)){
            return false;
        }

        int offsetx = -(type.size - 1) / 2;
        int offsety = -(type.size - 1) / 2;

        for(int dx = 0; dx < type.size; dx++){
            for(int dy = 0; dy < type.size; dy++){
                int wx = dx + offsetx + tile.x, wy = dy + offsety + tile.y;

                Tile check = world.tile(wx, wy);

                if(
                        check == null || //nothing there
                                (check.floor().isDeep() && !type.floating && !type.placeableLiquid) || //deep water
                                (type == check.block() && check.build != null && rotation == check.build.rotation && type.rotate) || //same block, same rotation
                                !check.floor().placeableOn || //solid wall
                                (type.requiresWater && check.floor().liquidDrop != Liquids.water) //requires water but none found
                ) return false;
            }
        }

        return true;
    }

    public static class MaxTiles {
        int one, two, three, four;

        void increment(int size) {
            switch (size) {
                case 1 -> one++;
                case 2 -> two++;
                case 3 -> three++;
                case 4 -> four++;
            }
        }
    }
}
