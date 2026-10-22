package dev.kthreadd.render;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import java.util.*;

public final class HudRenderer {
    public record Bounds(int x,int y,int width,int height) { public boolean contains(double px,double py) { return px>=x && py>=y && px<x+width && py<y+height; } }
    private final KthreaddClient c;
    private final MinecraftClient mc=MinecraftClient.getInstance();
    public final Map<String,Bounds> bounds=new LinkedHashMap<>();
    public HudRenderer(KthreaddClient c) { this.c=c; }
    public void render(DrawContext ctx,boolean editing) {
        bounds.clear();
        if(mc.player==null || mc.world==null || (mc.options.hudHidden && !editing) || (!c.modules.hud.enabled() && !editing)) return;
        var h=c.modules.hud; int color=c.modules.hudAccent(0);
        var engine=(dev.kthreadd.modules.combat.CrystalCombatModule)c.modules.get("CrystalAura");
        if(engine.enabled() && engine.flag("Diagnostics")) draw(ctx,"CrystalDiagnostics",Arrays.asList(engine.diagnostics().split(" \\| ")),0.01,0.64,color,editing,false);
        if(h.flag("Watermark") || editing) draw(ctx,"Watermark",List.of("KTHREADD 4.0 | 1.20.4"),0.01,0.02,color,editing,false);
        if(h.flag("ArrayList") || editing) {
            List<String> enabled=c.modules.all.stream().filter(ClientModule::enabled).map(m -> m.name).sorted(Comparator.comparingInt((String s) -> mc.textRenderer.getWidth(s)).reversed()).toList();
            draw(ctx,"ArrayList",enabled.isEmpty()?List.of("ArrayList"):enabled,0.99,0.02,color,editing,true);
        }
        if(h.flag("Coordinates") || editing) {
            if(c.privacy()) { if(editing) draw(ctx,"Coordinates",List.of("Coordinates [privacy]"),0.01,0.95,color,true,false); }
            else {
                boolean nether=mc.world.getRegistryKey().equals(net.minecraft.world.World.NETHER);
                String xyz=String.format(Locale.ROOT,"XYZ %.1f %.1f %.1f",mc.player.getX(),mc.player.getY(),mc.player.getZ());
                List<String> lines=new ArrayList<>(); lines.add(xyz);
                if(nether || mc.world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) lines.add(String.format(Locale.ROOT,"%s %.1f %.1f",nether?"Overworld":"Nether",Coordinates.convert(mc.player.getX(),!nether),Coordinates.convert(mc.player.getZ(),!nether)));
                draw(ctx,"Coordinates",lines,0.01,0.95,color,editing,false);
            }
        }
        if(h.flag("Stats") || editing) draw(ctx,"Stats",List.of(mc.getCurrentFps()+" FPS | "+Math.round(mc.player.getVelocity().horizontalLength()*20*10)/10.0+" m/s | "+mc.player.getHorizontalFacing().asString()),0.01,0.88,color,editing,false);
        if(h.flag("Combat") || editing) {
            int totems=0; for(int i=0;i<mc.player.getInventory().size();i++) { var s=mc.player.getInventory().getStack(i); if(s.isOf(Items.TOTEM_OF_UNDYING)) totems+=s.getCount(); }
            draw(ctx,"Combat",List.of("HP "+Math.round(mc.player.getHealth()+mc.player.getAbsorptionAmount())+" | Armor "+mc.player.getArmor()+" | Totems "+totems),0.01,0.82,color,editing,false);
        }
        if(h.flag("Armor") || editing) {
            var area=area(ctx,"Armor",78,22,0.43,0.87,color,editing);
            int i=0; for(var stack:mc.player.getArmorItems()) { ctx.drawItem(stack,area.x()+3+i*18,area.y()+3); ctx.drawItemInSlot(mc.textRenderer,stack,area.x()+3+i*18,area.y()+3); i++; }
        }
        if(h.flag("Inventory") || editing) {
            var area=area(ctx,"Inventory",168,58,0.01,0.5,color,editing);
            for(int i=0;i<27;i++) { var stack=mc.player.getInventory().getStack(i+9); int sx=area.x()+3+(i%9)*18,sy=area.y()+3+(i/9)*18; ctx.drawItem(stack,sx,sy); ctx.drawItemInSlot(mc.textRenderer,stack,sx,sy); }
        }
        if(h.flag("TargetHUD") || editing) {
            net.minecraft.entity.player.PlayerEntity target=mc.world.getPlayers().stream().filter(p -> p!=mc.player && p.isAlive() && !c.isFriend(p.getGameProfile().getName()) && p.distanceTo(mc.player)<12).min(Comparator.comparingDouble(p -> p.squaredDistanceTo(mc.player))).orElse(null);
            if(target!=null || editing) {
                var area=area(ctx,"TargetHUD",150,42,0.4,0.65,color,editing);
                ctx.drawText(mc.textRenderer,target==null?"TargetHUD":target.getName().getString(),area.x()+5,area.y()+5,color,true);
                float health=target==null?20:target.getHealth()+target.getAbsorptionAmount();
                String detail="HP "+Math.round(health)+(target==null?"":" | Pops "+c.totemPops.getOrDefault(target.getUuid(),0));
                ctx.drawText(mc.textRenderer,detail,area.x()+5,area.y()+17,0xffffffff,true);
                ctx.fill(area.x()+5,area.y()+31,area.x()+145,area.y()+36,0xff333333);
                ctx.fill(area.x()+5,area.y()+31,area.x()+5+(int)(140*Math.min(1,health/20)),area.y()+36,color);
            }
        }
        if((h.flag("Waypoints") && c.modules.on("Waypoints")) || editing) {
            List<String> waypoints=new ArrayList<>();
            if(!c.privacy()) for(var w:c.profile.waypoints) if(w.dimension.equals(mc.world.getRegistryKey().getValue().toString()) && waypoints.size()<8) waypoints.add(w.name+" "+(int)Math.sqrt(mc.player.squaredDistanceTo(w.x,w.y,w.z))+"m");
            if(!waypoints.isEmpty() || editing) draw(ctx,"Waypoints",waypoints.isEmpty()?List.of("Waypoints"+(c.privacy()?" [privacy]":"")):waypoints,0.7,0.65,color,editing,false);
        }
        if(c.modules.on("HighwayNavigation") || editing) {
            double x=mc.player.getX(),z=mc.player.getZ();
            double cross=Math.min(Math.min(Math.abs(x),Math.abs(z)),Math.min(Math.abs(x-z),Math.abs(x+z))/Math.sqrt(2));
            String line=String.format(Locale.ROOT,"Highway %s | yaw %.1f%s",mc.player.getHorizontalFacing().asString(),MathHelper.wrapDegrees(mc.player.getYaw()),c.privacy()?"":" | offset "+Math.round(cross)+"m");
            draw(ctx,"Highway",List.of(line),0.35,0.92,color,editing,false);
        }
        if(!editing && c.modules.notifications.enabled()) {
            int y=mc.getWindow().getScaledHeight()-25-c.notices.size()*13;
            for(var n:c.notices) { String text=mc.textRenderer.trimToWidth(n.text(),mc.getWindow().getScaledWidth()-24); int width=mc.textRenderer.getWidth(text); ctx.fill(8,y-2,width+16,y+11,0xc0181818); ctx.fill(8,y-2,10,y+11,color); ctx.drawText(mc.textRenderer,text,13,y,0xffffffff,true); y+=14; }
        }
    }
    private void draw(DrawContext ctx,String id,List<String> lines,double dx,double dy,int color,boolean editing,boolean right) {
        int width=lines.stream().mapToInt(mc.textRenderer::getWidth).max().orElse(30)+6,height=lines.size()*11+4;
        int sw=mc.getWindow().getScaledWidth(),sh=mc.getWindow().getScaledHeight();
        var p=c.profile.hud.getOrDefault(id,new Profile.Position(dx,dy));
        int x=(int)Math.max(0,Math.min(sw-width,p.x*sw)),y=(int)Math.max(0,Math.min(sh-height,p.y*sh));
        bounds.put(id,new Bounds(x,y,width,height));
        if(editing) { ctx.fill(x,y,x+width,y+height,0x80111111); ctx.drawBorder(x,y,width,height,color); }
        int lineY=y+2,i=0;
        for(String line:lines) {
            int lx=right?x+width-mc.textRenderer.getWidth(line)-3:x+3;
            if(right) { ctx.fill(lx-3,lineY-2,x+width,lineY+9,0x88111111); ctx.fill(x+width-1,lineY-2,x+width+1,lineY+9,c.modules.hudAccent(i*14)); }
            ctx.drawText(mc.textRenderer,line,lx,lineY,right?c.modules.hudAccent(i*14):color,c.modules.hud.flag("Shadow")); lineY+=11; i++;
        }
    }
    private Bounds area(DrawContext ctx,String id,int width,int height,double dx,double dy,int color,boolean editing) {
        int sw=mc.getWindow().getScaledWidth(),sh=mc.getWindow().getScaledHeight(); var p=c.profile.hud.getOrDefault(id,new Profile.Position(dx,dy));
        var b=new Bounds((int)Math.max(0,Math.min(sw-width,p.x*sw)),(int)Math.max(0,Math.min(sh-height,p.y*sh)),width,height); bounds.put(id,b);
        ctx.fill(b.x(),b.y(),b.x()+width,b.y()+height,0xb8101010); if(editing) ctx.drawBorder(b.x(),b.y(),width,height,color); return b;
    }
}
