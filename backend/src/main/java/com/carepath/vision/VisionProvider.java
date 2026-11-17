package com.carepath.vision;
public interface VisionProvider {
    boolean available();
    String model();
    String read(byte[] image,String mime);
}
