package dev.kthreadd.render;
/** Main-thread cube iterator whose cursor never grows beyond the active volume. */
public final class ScanCursor {
    private int radius=-1,cursor,width,total;
    public void configure(int nextRadius) {
        nextRadius=Math.max(0,Math.min(128,nextRadius));
        if(radius==nextRadius) return;
        radius=nextRadius; width=radius*2+1; total=width*width*width; cursor=0;
    }
    public int[] next() {
        if(radius<0) configure(0);
        int at=cursor; cursor=(cursor+1)%total;
        return new int[]{at%width-radius,(at/width)%width-radius,at/(width*width)-radius};
    }
    public void reset() { cursor=0; }
}
