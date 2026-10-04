package com.carepath.intelligence;
import java.nio.file.Path;
public interface OcrProvider { ExtractionData.Page recognize(Path image,int page,int width,int height); }
