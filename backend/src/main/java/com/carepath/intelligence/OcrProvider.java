package com.carepath.intelligence;
import java.nio.file.Path;
public interface OcrProvider { ExtractionData.Page recognize(Path image,int page,int width,int height);
    default ExtractionData.Page recognize(Path path,int page,java.awt.image.BufferedImage raster){
        return recognize(path,page,raster.getWidth(),raster.getHeight());
    } }
