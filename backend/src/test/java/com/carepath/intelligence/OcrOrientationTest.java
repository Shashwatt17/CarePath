package com.carepath.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OcrOrientationTest {
    // Independent forward rotation of synthetic word boxes on a non-square page.
    private String fixture(int turns, int count, int firstLine) {
        String[][] rows={{"LAB","REPORT"},{"Report","date:","18/Jul/2026"},{"Glucose","92.4","mg/dL","70-140"}};
        StringBuilder out=new StringBuilder();
        for(int row=0;row<count;row++)for(int col=0;col<rows[row].length;col++) {
            int x=40+col*160,y=40+row*80,w=100,h=20;
            int rx=x,ry=y,rw=w,rh=h;
            if(turns==1){rx=800-y-h;ry=x;rw=h;rh=w;}
            if(turns==2){rx=1000-x-w;ry=800-y-h;}
            if(turns==3){rx=y;ry=1000-x-w;rw=h;rh=w;}
            out.append("5\t1\t1\t1\t").append(firstLine+row).append('\t').append(col+1)
                .append('\t').append(rx).append('\t').append(ry).append('\t').append(rw).append('\t').append(rh)
                .append("\t96\t").append(rows[row][col]).append('\n');
        }
        return out.toString();
    }
    @ParameterizedTest @ValueSource(ints={1,2,3})
    void rotatedRowsPreserveReadingsAndOriginalSourceBoxes(int turns) {
        var upright=TesseractOcr.parse(fixture(0,3,1),1,1000,800);
        var rotated=TesseractOcr.parse(fixture(turns,3,1),1,turns%2==1?800:1000,turns%2==1?1000:800);
        assertEquals(upright.text(),rotated.text());
        assertEquals(upright.ocrConfidence(),rotated.ocrConfidence());
        var result=new ExtractionPipeline(null).parse(List.of(rotated));
        new ProvenanceValidator().validate(result,20,250000);
        assertEquals("92.4",result.candidates().getFirst().originalValue());
        var b=rotated.lines().get(2).box();
        double[] expected=switch(turns){case 1->new double[]{.725,.04,.025,.58};case 2->new double[]{.38,.725,.58,.025};default->new double[]{.25,.38,.025,.58};};
        assertEquals(expected[0],b.x(),1e-9);assertEquals(expected[1],b.y(),1e-9);
        assertEquals(expected[2],b.width(),1e-9);assertEquals(expected[3],b.height(),1e-9);
    }
    @Test void sparseLinesDoNotGuessOrientation(){assertEquals(0,OcrOrientation.detect(fixture(1,2,1)));}
    @Test void mixedDirectionsDoNotGuessOrientation(){assertEquals(0,OcrOrientation.detect(fixture(1,3,1)+fixture(0,2,10)));}
    @Test void uprightKeepsExistingAxes(){assertEquals(0,OcrOrientation.detect(fixture(0,3,1)));}
    @Test void invalidRotatedGeometryFailsClosed(){assertThrows(ProcessingFailure.class,()->TesseractOcr.parse(fixture(1,3,1).replace("740\t40", "940\t40"),1,800,1000));}
}
