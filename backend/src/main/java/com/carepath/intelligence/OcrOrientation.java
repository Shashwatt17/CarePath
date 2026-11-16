package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
/** Infer reading axes only from consistent word progression in multiple OCR lines. */
final class OcrOrientation {
    static int detect(String tsv) {
        Map<String,List<double[]>> lines=new LinkedHashMap<>();
        for(String row:tsv.split("\n")) {
            String[] c=row.split("\t",12);
            if(c.length!=12||!c[0].equals("5")||c[11].isBlank())continue;
            try { double x=Integer.parseInt(c[6])+Integer.parseInt(c[8])/2.0,y=Integer.parseInt(c[7])+Integer.parseInt(c[9])/2.0;
                lines.computeIfAbsent(c[2]+":"+c[3]+":"+c[4],k->new ArrayList<>()).add(new double[]{x,y,Integer.parseInt(c[5])});
            } catch(NumberFormatException e){return 0;}
        }
        int[] votes=new int[4];int total=0;
        for(var words:lines.values()) {
            if(words.size()<2)continue;
            words.sort(Comparator.comparingDouble(w->w[2]));var a=words.getFirst();var b=words.getLast();double dx=b[0]-a[0],dy=b[1]-a[1];
            if(Math.max(Math.abs(dx),Math.abs(dy))<10)continue;
            total++;
            if(Math.abs(dx)>Math.abs(dy)*3)votes[dx>0?0:2]++;
            else if(Math.abs(dy)>Math.abs(dx)*3)votes[dy>0?1:3]++;
        }
        for(int i=1;i<4;i++)if(votes[i]>=3 && votes[i]>=total*.8)return i;
        return 0;
    }
    static String transform(String tsv,int turns,int width,int height) {
        StringBuilder out=new StringBuilder();
        for(String row:tsv.split("\n")) {
            String[] c=row.split("\t",12);
            if(c.length==12&&c[0].equals("5")&&!c[11].isBlank()) {
                try {
                    int x=Integer.parseInt(c[6]),y=Integer.parseInt(c[7]),w=Integer.parseInt(c[8]),h=Integer.parseInt(c[9]);
                    if(x<0||y<0||w<1||h<1||(long)x+w>width||(long)y+h>height)throw new ProcessingFailure("OCR_FAILED",true);
                    int nx=turns==1?y:turns==2?width-x-w:height-y-h;
                    int ny=turns==1?width-x-w:turns==2?height-y-h:x;
                    c[6]=""+nx;c[7]=""+ny;c[8]=""+(turns%2==1?h:w);c[9]=""+(turns%2==1?w:h);
                    row=String.join("\t",c);
                }catch(NumberFormatException e){throw new ProcessingFailure("OCR_FAILED",true);}
            }
            out.append(row).append('\n');
        }
        return out.toString();
    }
    static Box original(Box b,int turns) {
        if(b==null)return null;
        return switch(turns){
            case 1 -> new Box(Math.max(0,1-b.y()-b.height()),b.x(),b.height(),b.width());
            case 2 -> new Box(Math.max(0,1-b.x()-b.width()),Math.max(0,1-b.y()-b.height()),b.width(),b.height());
            case 3 -> new Box(b.y(),Math.max(0,1-b.x()-b.width()),b.height(),b.width());
            default -> b;
        };
    }
}
