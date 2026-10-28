package dev.kthreadd.gui;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import java.util.*;
public final class ProfilesScreen extends Screen {
    private final KthreaddClient c; private final Screen parent;
    private TextFieldWidget name; private String message="";
    private List<String> profiles=List.of(); private int first;
    public ProfilesScreen(KthreaddClient c,Screen parent) { super(Text.literal("Profiles")); this.c=c; this.parent=parent; }
    protected void init() {
        name=new TextFieldWidget(textRenderer,width/2-120,45,240,20,Text.literal("Profile name")); name.setMaxLength(64); name.setText(c.activeProfile); addDrawableChild(name);
        refresh();
        addDrawableChild(ButtonWidget.builder(Text.literal("Save as"),b -> { try { c.saveAs(name.getText()); message="Saved "+c.activeProfile; refresh(); } catch(Exception e) { message=e.getMessage(); } }).dimensions(width/2-120,72,115,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Load"),b -> { try { if(!c.store.list().contains(name.getText())) { message="Profile does not exist"; return; } c.save(); c.load(name.getText()); message="Loaded "+c.activeProfile; } catch(Exception e) { message=e.getMessage(); } }).dimensions(width/2+5,72,115,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"),b -> close()).dimensions(width/2-60,height-30,120,20).build());
    }
    public void render(DrawContext ctx,int x,int y,float delta) {
        ctx.fill(0,0,width,height,0xef101016); ctx.drawCenteredTextWithShadow(textRenderer,"KTHREADD profiles",width/2,18,c.modules.accent(0));
        int at=110;
        for(int i=first;i<profiles.size();i++) { if(at>height-70) break; String profile=profiles.get(i); if(profile.equals(name.getText())) ctx.fill(width/2-120,at-2,width/2+120,at+10,0x55333344); ctx.drawCenteredTextWithShadow(textRenderer,profile,width/2,at,profile.equals(c.activeProfile)?0xffffffff:0xffbbbbbb); at+=12; }
        ctx.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(message,width-20),width/2,height-50,0xffddbb88); super.render(ctx,x,y,delta);
    }
    private void refresh() { try { profiles=c.store.list(); first=Math.min(first,Math.max(0,profiles.size()-1)); } catch(Exception e) { message=e.getMessage(); } }
    public boolean mouseClicked(double x,double y,int button) {
        if(button==0 && x>=width/2-120 && x<=width/2+120 && y>=108 && y<=height-60) {
            int index=first+(int)((y-108)/12); if(index<profiles.size()) { name.setText(profiles.get(index)); return true; }
        }
        return super.mouseClicked(x,y,button);
    }
    public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        int visible=Math.max(1,(height-180)/12+1); first=Math.max(0,Math.min(Math.max(0,profiles.size()-visible),first-(int)vertical)); return true;
    }
    public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx,int x,int y,float delta) { }
    public void close() { c.save(); client.setScreen(parent); }
}
