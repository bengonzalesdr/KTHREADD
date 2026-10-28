package dev.kthreadd.core;
/** Short motion history with teleport and reversal rejection; independent of live entity mutation. */
public final class MotionHistory {
    public record Forecast(double x,double y,double z) {
        public double lengthSquared(){return x*x+y*y+z*z;}
    }
    private long tick=Long.MIN_VALUE;
    private double x,y,z,vx,vy,vz;
    public void sample(long nextTick,double nextX,double nextY,double nextZ){
        if(nextTick==tick)return;
        if(!Double.isFinite(nextX+nextY+nextZ)){tick=Long.MIN_VALUE;vx=vy=vz=0;return;}
        long elapsed=nextTick-tick;
        double dx=nextX-x,dy=nextY-y,dz=nextZ-z;
        if(tick==Long.MIN_VALUE||elapsed<1||elapsed>10||dx*dx+dy*dy+dz*dz>4){vx=vy=vz=0;}
        else{
            dx/=elapsed;dy/=elapsed;dz/=elapsed;
            if(dx*vx+dy*vy+dz*vz<0){vx=vy=vz=0;}
            else{vx=vx*.4+dx*.6;vy=vy*.4+dy*.6;vz=vz*.4+dz*.6;}
        }
        tick=nextTick;x=nextX;y=nextY;z=nextZ;
    }
    public Forecast predict(double ticks){
        double px=vx*ticks,py=vy*ticks,pz=vz*ticks,length=Math.sqrt(px*px+py*py+pz*pz);
        double scale=length>1.5?1.5/length:1;
        return new Forecast(px*scale,py*scale,pz*scale);
    }
}
