package dev.kthreadd.render;

import dev.kthreadd.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

public final class WorldOverlay {
    private final KthreaddClient client;
    private final MinecraftClient mc=MinecraftClient.getInstance();
    private final List<BlockPos> storage=new ArrayList<>();
    private final LinkedHashSet<BlockPos> found=new LinkedHashSet<>();
    private final Set<Long> notified=new HashSet<>();
    private final Map<Long,List<BlockPos>> storageChunks=new LinkedHashMap<>();
    private final ArrayDeque<ChunkPos> chunkQueue=new ArrayDeque<>();
    private final ScanCursor searchCursor=new ScanCursor();
    private Object worldIdentity;
    private Block previousSearch;
    private int previousRadius=-1;
    private BlockPos origin;
    private final VertexConsumerProvider.Immediate overlayConsumers=VertexConsumerProvider.immediate(new BufferBuilder(4096));
    public boolean draws(String name) { return client.modules.on(name); }
    private static boolean storageEntity(BlockEntity entity) { return entity instanceof LockableContainerBlockEntity || entity instanceof EnderChestBlockEntity; }
    public Block searchBlock=Blocks.DIAMOND_ORE;
    public WorldOverlay(KthreaddClient client) { this.client=client; }
    public void reset() { storage.clear(); storageChunks.clear(); chunkQueue.clear(); found.clear(); notified.clear(); origin=null; worldIdentity=null; searchCursor.reset(); }
    public void tick() {
        if(mc.world==null || mc.player==null) { reset(); return; }
        if(worldIdentity!=mc.world) { reset(); worldIdentity=mc.world; }
        var mods=client.modules;
        boolean wantStorage=mods.on("StorageESP") || mods.on("StashFinder");
        if(wantStorage) {
            double activeRange=Math.max(mods.on("StorageESP")?mods.get("StorageESP").value("Range"):0,mods.on("StashFinder")?mods.get("StashFinder").value("Range"):0);
            int range=(int)Math.ceil(activeRange/16);
            ChunkPos center=mc.player.getChunkPos();
            storageChunks.keySet().removeIf(key -> { var p=new ChunkPos(key); return Math.abs(p.x-center.x)>range || Math.abs(p.z-center.z)>range || !mc.world.isChunkLoaded(p.x,p.z); });
            if(chunkQueue.isEmpty()) {
                List<ChunkPos> chunks=new ArrayList<>();
                for(int x=-range;x<=range;x++) for(int z=-range;z<=range;z++) chunks.add(new ChunkPos(center.x+x,center.z+z));
                chunks.sort(Comparator.comparingInt(p -> Math.abs(p.x-center.x)+Math.abs(p.z-center.z))); chunkQueue.addAll(chunks);
            }
            for(int scans=0;scans<2 && !chunkQueue.isEmpty();scans++) {
                var pos=chunkQueue.removeFirst();
                if(Math.abs(pos.x-center.x)>range || Math.abs(pos.z-center.z)>range) continue;
                WorldChunk chunk=mc.world.getChunkManager().getWorldChunk(pos.x,pos.z);
                if(chunk==null) continue;
                int count=0,visited=0; List<BlockPos> entries=new ArrayList<>();
                for(var entry:chunk.getBlockEntities().entrySet()) {
                    if(visited++>=512) break;
                    var b=entry.getValue();
                    if(storageEntity(b)) {
                        if(entry.getKey().getSquaredDistance(mc.player.getBlockPos())<=Math.pow(mods.get("StashFinder").value("Range"),2)) count++;
                        if((b instanceof EnderChestBlockEntity ? mods.get("StorageESP").flag("EnderChests") : mods.get("StorageESP").flag("Containers"))) entries.add(entry.getKey().toImmutable());
                    }
                }
                storageChunks.put(pos.toLong(),entries);
                if(mods.on("StashFinder") && count>=mods.get("StashFinder").value("Threshold") && notified.size()<4096 && notified.add(chunk.getPos().toLong()))
                    client.notify(client.privacy()?"Storage cluster found (location hidden)":"Storage cluster: " + count + " blocks in chunk " + chunk.getPos().x + ", " + chunk.getPos().z);
            }
            storage.clear(); fill: for(var positions:storageChunks.values()) for(var p:positions) { if(storage.size()>=4096) break fill; storage.add(p); }
        } else { storage.clear(); storageChunks.clear(); chunkQueue.clear(); }
        if(!mods.on("Search")) { found.clear(); origin=null; return; }
        int radius=mods.get("Search").setting("Range").integer();
        if(origin==null || origin.getSquaredDistance(mc.player.getBlockPos())>64 || previousSearch!=searchBlock || previousRadius!=radius) { origin=mc.player.getBlockPos(); searchCursor.configure(radius); searchCursor.reset(); found.clear(); previousSearch=searchBlock; previousRadius=radius; }
        found.removeIf(p -> !mc.world.isChunkLoaded(p.getX()>>4,p.getZ()>>4) || !mc.world.getBlockState(p).isOf(searchBlock));
        for(int i=0;i<mods.get("Search").value("Budget");i++) {
            int[] offset=searchCursor.next(); BlockPos p=origin.add(offset[0],offset[1],offset[2]);
            if(mc.world.isChunkLoaded(p.getX()>>4,p.getZ()>>4) && mc.world.getBlockState(p).isOf(searchBlock)) { if(found.size()<2048) found.add(p); } else found.remove(p);
        }
    }
    public void render(WorldRenderContext ctx) {
        if(mc.world==null || mc.player==null || mc.options.hudHidden || mc.world!=worldIdentity) return;
        var matrices=ctx.matrixStack(); if(matrices==null) return;
        var camera=ctx.camera().getPos();
        matrices.push();
        var consumers=overlayConsumers;
        var entityModule=client.modules.get("EntityESP");var storageModule=client.modules.get("StorageESP");var tracerModule=client.modules.get("Tracers");
        var entityLayer=EspRenderLayers.lines(entityModule.value("LineWidth"),entityModule.flag("ThroughWalls"));
        var storageLayer=EspRenderLayers.lines(storageModule.value("LineWidth"),storageModule.flag("ThroughWalls"));
        var tracerLayer=EspRenderLayers.lines(tracerModule.value("LineWidth"),tracerModule.flag("ThroughWalls"));
        record Label(Vec3d position,String text) {}
        List<Label> labels=new ArrayList<>();
        int color=client.modules.accent(0); float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
        List<Entity> nearby=new ArrayList<>();
        if(draws("EntityESP") || draws("Tracers") || draws("Nametags")) {
            double range=Math.max(entityModule.value("Range"),Math.max(tracerModule.value("Range"),client.modules.get("Nametags").value("Range")));
            var farthestFirst=Comparator.comparingDouble((Entity e) -> e.squaredDistanceTo(mc.player)).reversed();
            var espCandidates=new PriorityQueue<Entity>(farthestFirst);
            var tracerCandidates=new PriorityQueue<Entity>(farthestFirst);
            var nameCandidates=new PriorityQueue<Entity>(farthestFirst);
            var tags=client.modules.get("Nametags");
            for(var entity:mc.world.getEntities()) if(entity!=mc.player && entity.isAlive() && entity.squaredDistanceTo(mc.player)<=range*range) {
                boolean friend=entity instanceof PlayerEntity p && client.isFriend(p.getGameProfile().getName());
                if(draws("EntityESP") && visible(entityModule,entity,friend) && (!entityModule.flag("PlayersOnly") || entity instanceof PlayerEntity) && entity.squaredDistanceTo(mc.player)<=Math.pow(entityModule.value("Range"),2)) nearest(espCandidates,entity,entityModule.setting("MaxEntities").integer());
                if(entity instanceof PlayerEntity) {
                    if(draws("Tracers") && visible(tracerModule,entity,friend) && entity.squaredDistanceTo(mc.player)<=Math.pow(tracerModule.value("Range"),2)) nearest(tracerCandidates,entity,tracerModule.setting("MaxEntities").integer());
                    if(draws("Nametags") && visible(tags,entity,friend) && entity.squaredDistanceTo(mc.player)<=Math.pow(tags.value("Range"),2)) nearest(nameCandidates,entity,tags.setting("MaxEntities").integer());
                }
            }
            Set<Entity> unique=new HashSet<>(espCandidates); unique.addAll(tracerCandidates); unique.addAll(nameCandidates); nearby.addAll(unique);
            nearby.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));
        }
        int boxes=0,traces=0,names=0;
        for(var entity:nearby) {
            if(entity==mc.player || !entity.isAlive()) continue;
            double distance=entity.distanceTo(mc.player);
            var esp=client.modules.get("EntityESP");
            boolean friend=entity instanceof PlayerEntity p && client.isFriend(p.getGameProfile().getName());
            if(draws("EntityESP") && boxes<esp.value("MaxEntities") && visible(esp,entity,friend) && distance<esp.value("Range") && (!esp.flag("PlayersOnly") || entity instanceof PlayerEntity)) { WorldRenderer.drawBox(matrices,consumers.getBuffer(entityLayer),OverlayGeometry.entityBox(entity,ctx.tickDelta(),camera),r,g,b,1); boxes++; }
            if(entity instanceof PlayerEntity player) {
                if(draws("Tracers") && traces<tracerModule.value("MaxEntities") && visible(tracerModule,entity,friend) && distance<tracerModule.value("Range")) {
                    traces++;
                    Vec3d from=OverlayGeometry.tracerOrigin(matrices.peek().getPositionMatrix());
                    Vec3d to=player.getLerpedPos(ctx.tickDelta()).add(0,player.getHeight()/2,0).subtract(camera), normal=to.subtract(from).normalize();
                    var lines=consumers.getBuffer(tracerLayer);
                    var entry=matrices.peek();
                    lines.vertex(entry.getPositionMatrix(),(float)from.x,(float)from.y,(float)from.z).color(r,g,b,1).normal(entry.getNormalMatrix(),(float)normal.x,(float)normal.y,(float)normal.z).next();
                    lines.vertex(entry.getPositionMatrix(),(float)to.x,(float)to.y,(float)to.z).color(r,g,b,1).normal(entry.getNormalMatrix(),(float)normal.x,(float)normal.y,(float)normal.z).next();
                }
                var tags=client.modules.get("Nametags");
                if(draws("Nametags") && names<tags.value("MaxEntities") && visible(tags,entity,friend) && distance<tags.value("Range")) {
                    String text=player.getName().getString()+" " + Math.round(player.getHealth()+player.getAbsorptionAmount())+" HP | "+Math.round(distance)+"m";
                    if(tags.flag("Equipment")) text+=" | Armor "+player.getArmor()+" | "+minimumDurability(player)+"% | Pops "+client.totemPops.getOrDefault(player.getUuid(),0);
                    labels.add(new Label(player.getLerpedPos(ctx.tickDelta()).add(0,player.getHeight()+0.5,0),text)); names++;
                }
            }
        }
        if(draws("StorageESP")) for(var p:storage) if(p.getSquaredDistance(mc.player.getBlockPos())<Math.pow(storageModule.value("Range"),2) && mc.world.isChunkLoaded(p.getX()>>4,p.getZ()>>4)) {
            var entity=mc.world.getBlockEntity(p);
            if(storageEntity(entity) && (entity instanceof EnderChestBlockEntity?storageModule.flag("EnderChests"):storageModule.flag("Containers"))) WorldRenderer.drawBox(matrices,consumers.getBuffer(storageLayer),new Box(p).expand(0.002).offset(-camera.x,-camera.y,-camera.z),1,0.65f,0.2f,1);
        }
        var search=client.modules.get("Search");
        if(draws("Search")) for(var p:found) if(mc.world.isChunkLoaded(p.getX()>>4,p.getZ()>>4) && mc.world.getBlockState(p).isOf(searchBlock)) WorldRenderer.drawBox(matrices,consumers.getBuffer(EspRenderLayers.lines(search.value("LineWidth"),search.flag("ThroughWalls"))),new Box(p).expand(0.002).offset(-camera.x,-camera.y,-camera.z),r,g,b,1);
        if(client.modules.on("Waypoints") && !client.privacy()) for(var w:client.profile.waypoints) if(w.dimension.equals(mc.world.getRegistryKey().getValue().toString()) && mc.player.squaredDistanceTo(w.x,w.y,w.z)<Math.pow(client.modules.get("Waypoints").value("Range"),2)) {
            labels.add(new Label(new Vec3d(w.x,w.y+1,w.z),w.name+" ["+(int)Math.sqrt(mc.player.squaredDistanceTo(w.x,w.y,w.z))+"m]"));
        }
        consumers.draw();
        for(var label:labels) label(ctx,consumers,label.position(),label.text(),color);
        matrices.pop(); consumers.draw();
    }
    private static void nearest(PriorityQueue<Entity> queue,Entity entity,int maximum) { queue.add(entity); if(queue.size()>maximum) queue.poll(); }
    private boolean visible(dev.kthreadd.core.ClientModule module,Entity entity,boolean friend) {
        return !(entity instanceof PlayerEntity p && p.isSpectator()) && !(module.flag("IgnoreFriends") && friend) && !(module.flag("IgnoreInvisible") && entity.isInvisible());
    }
    public static int minimumDurability(PlayerEntity player) {
        int minimum=100,count=0;
        for(var stack:player.getArmorItems()) { if(stack.isEmpty()) continue; count++; if(stack.isDamageable()) minimum=Math.min(minimum,(int)(100L*(stack.getMaxDamage()-stack.getDamage())/stack.getMaxDamage())); }
        return count<4?0:minimum;
    }
    private void label(WorldRenderContext ctx,net.minecraft.client.render.VertexConsumerProvider consumers,Vec3d pos,String text,int color) {
        var matrices=ctx.matrixStack(); matrices.push(); matrices.translate(pos.x-ctx.camera().getPos().x,pos.y-ctx.camera().getPos().y,pos.z-ctx.camera().getPos().z); matrices.multiply(ctx.camera().getRotation()); matrices.scale(-0.025f,-0.025f,0.025f);
        mc.textRenderer.draw(text,-mc.textRenderer.getWidth(text)/2f,0,color,false,matrices.peek().getPositionMatrix(),consumers,net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,0x66000000,15728880);
        matrices.pop();
    }
}
