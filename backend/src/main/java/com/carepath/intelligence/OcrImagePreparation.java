package com.carepath.intelligence;
import java.awt.*;
import java.awt.image.BufferedImage;
/** Bounded grayscale/contrast/scale only. Never edits original bytes or synthesizes text. */
final class OcrImagePreparation {
    static BufferedImage prepare(BufferedImage source) {
        var gray=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<gray.getHeight();y++)for(int x=0;x<gray.getWidth();x++){int rgb=source.getRGB(x,y);int v=(((rgb>>16)&255)*299+((rgb>>8)&255)*587+(rgb&255)*114+500)/1000;gray.setRGB(x,y,(v<<16)|(v<<8)|v);}
        Graphics2D g;
        int[] histogram=new int[256];var raster=gray.getRaster();
        for(int y=0;y<gray.getHeight();y++)for(int x=0;x<gray.getWidth();x++)histogram[raster.getSample(x,y,0)]++;
        long cutoff=(long)gray.getWidth()*gray.getHeight()/100;long sum=0;int low=0,high=255;
        for(;low<254;low++){sum+=histogram[low];if(sum>cutoff)break;}
        sum=0;for(;high>low;high--){sum+=histogram[high];if(sum>cutoff)break;}
        if(high-low>20)for(int y=0;y<gray.getHeight();y++)for(int x=0;x<gray.getWidth();x++){int v=Math.clamp((raster.getSample(x,y,0)-low)*255/(high-low),0,255);gray.setRGB(x,y,(v<<16)|(v<<8)|v);}
        double scale=Math.min(3,Math.min(2700.0/gray.getWidth(),Math.sqrt(16000000.0/((double)gray.getWidth()*gray.getHeight()))));
        if(scale<=1)return gray;
        var output=new BufferedImage((int)(gray.getWidth()*scale),(int)(gray.getHeight()*scale),BufferedImage.TYPE_INT_RGB);
        g=output.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(gray,0,0,output.getWidth(),output.getHeight(),null);g.dispose();gray.flush();return output;
    }
}
