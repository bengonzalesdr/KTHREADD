package dev.kthreadd.render;
public final class VisualPolicy {
    private VisualPolicy() {}
    public record Highway(String axis,double offset,double dx,double dz) {}
    public static Highway highway(double x,double z) {
        double[] offsets={Math.abs(x),Math.abs(z),Math.abs(x-z)/Math.sqrt(2),Math.abs(x+z)/Math.sqrt(2)};
        int nearest=0; for(int i=1;i<offsets.length;i++) if(offsets[i]<offsets[nearest]) nearest=i;
        return switch(nearest) {
            case 0 -> new Highway("X=0",offsets[0],x,0);
            case 1 -> new Highway("Z=0",offsets[1],0,z);
            case 2 -> new Highway("X=Z",offsets[2],(x-z)/2,(z-x)/2);
            default -> new Highway("X=-Z",offsets[3],(x+z)/2,(x+z)/2);
        };
    }
    public static float healthFraction(float health,float max,float absorption) {
        if(!Float.isFinite(health) || !Float.isFinite(max) || !Float.isFinite(absorption) || max<=0) return 0;
        return Math.max(0,Math.min(1,(health+absorption)/(max+Math.max(0,absorption))));
    }
}
