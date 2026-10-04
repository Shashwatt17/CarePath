package com.carepath.nearby;
import java.net.URI;
public final class PlaceSafety {
 private PlaceSafety() {}
 public static String web(String value) {
  if(value==null || value.length()>2048) return null;
  try { URI u=URI.create(value); if(!("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme())) || u.getHost()==null || u.getUserInfo()!=null) return null;
   String h=u.getHost().toLowerCase(java.util.Locale.ROOT);
   if(h.equals("localhost") || h.endsWith(".localhost") || h.endsWith(".local") || !h.contains(".") || h.matches("[0-9.]+") || h.contains(":")) return null;
   return u.toASCIIString();
  }catch(IllegalArgumentException e){return null;}
 }
 public static String phone(String value) { if(value==null || !value.matches("[+0-9 ()-]{5,40}"))return null; String n=value.replaceAll("[ ()-]",""); return n.matches("\\+?[0-9]{5,18}")?n:null; }
 public static long distance(double lat,double lon,double otherLat,double otherLon) {
  double a=Math.pow(Math.sin(Math.toRadians(otherLat-lat)/2),2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(otherLat))*Math.pow(Math.sin(Math.toRadians(otherLon-lon)/2),2);
  return Math.round(6371008.8*2*Math.asin(Math.sqrt(Math.max(0,Math.min(1,a)))));
 }
}
