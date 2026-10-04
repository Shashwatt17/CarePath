package com.carepath.vault;
import com.carepath.foundation.ApiFailure;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
@Component
public class FileValidation {
    private final VaultProperties properties;
    public FileValidation(VaultProperties properties) { this.properties=properties; }
    public record Validated(String filename,String mime,byte[] bytes,int pages) {}
    static ApiFailure invalid() { return new ApiFailure(422,"INVALID_FILE","Use a valid, unencrypted PDF, JPEG or PNG without active content."); }
    public Validated validate(MultipartFile file) {
        String name=file.getOriginalFilename();
        if(name==null || name.isBlank() || name.length()>255 || name.contains("/") || name.contains("\\") || name.contains(":") || name.contains("%") || name.codePoints().anyMatch(Character::isISOControl) || name.contains("..")) throw invalid();
        name=Normalizer.normalize(name,Normalizer.Form.NFC).replaceAll("[\\p{Cf}]","");
        if(name.isBlank() || name.length()>255) throw invalid();
        String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        String mime=switch(ext) { case "pdf" -> "application/pdf"; case "jpg","jpeg" -> "image/jpeg"; case "png" -> "image/png"; default -> throw invalid(); };
        if(!mime.equals(file.getContentType())) throw invalid();
        if(file.isEmpty()) throw invalid();
        if(file.getSize()>properties.maxBytes()) throw new ApiFailure(413,"FILE_TOO_LARGE","The file exceeds the upload limit.");
        try(var in=file.getInputStream()) {
            byte[] bytes=in.readNBytes(properties.maxBytes()+1);
            if(bytes.length>properties.maxBytes()) throw new ApiFailure(413,"FILE_TOO_LARGE","The file exceeds the upload limit.");
            int pages=1;
            if(mime.equals("application/pdf")) {
                if(bytes.length<12 || !new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-") ||
                   !new String(bytes,Math.max(0,bytes.length-1024),Math.min(1024,bytes.length),StandardCharsets.ISO_8859_1).stripTrailing().endsWith("%%EOF")) throw invalid();
                try(var pdf=openPdf(bytes)) { pages=pdf.getNumberOfPages(); if(pages<1 || pages>200 || pdf.isEncrypted()) throw invalid();
                    inspect(pdf.getDocumentCatalog().getCOSObject(),Collections.newSetFromMap(new IdentityHashMap<>()));
                    for(var page:pdf.getPages()) {
                        float width=page.getMediaBox().getWidth(),height=page.getMediaBox().getHeight();
                        if(!Float.isFinite(width) || !Float.isFinite(height) || width<=0 || height<=0 || width>3000 || height>3000) throw invalid();
                    }
                }
            } else {
                boolean magic=mime.equals("image/png") ? bytes.length>8 && Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}) : bytes.length>3 && bytes[0]==(byte)255 && bytes[1]==(byte)216 && bytes[2]==(byte)255;
                if(!magic) throw invalid();
                try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                    var readers=ImageIO.getImageReaders(stream); if(!readers.hasNext()) throw invalid(); var reader=readers.next();
                    try {
                        boolean[] warned={false};reader.addIIOReadWarningListener((source,warning)->warned[0]=true);
                        reader.setInput(stream); int width=reader.getWidth(0),height=reader.getHeight(0);
                        if(width<=0 || height<=0 || (long)width*height>20000000) throw invalid();
                        if(reader.read(0)==null || warned[0]) throw invalid();
                    } finally { reader.dispose(); }
                }
                if(mime.equals("image/jpeg") && (bytes[bytes.length-2]!=(byte)255 || bytes[bytes.length-1]!=(byte)217)) throw invalid();
                if(mime.equals("image/png") && (bytes.length<12 || !Arrays.equals(Arrays.copyOfRange(bytes,bytes.length-8,bytes.length),new byte[]{73,69,78,68,(byte)174,66,96,(byte)130}))) throw invalid();
            }
            return new Validated(name,mime,bytes,pages);
        } catch(IOException | IllegalArgumentException e) { throw invalid(); }
    }
    public static PDDocument openPdf(byte[] bytes) throws IOException {
        var parser=new PDFParser(new RandomAccessReadBuffer(bytes), "", null, null,
            () -> org.apache.pdfbox.io.ScratchFile.getMainMemoryOnlyInstance(52428800)); return parser.parse(false);
    }
    private void inspect(COSBase root,Set<COSBase> seen) throws IOException {
        var pending=new ArrayDeque<COSBase>();pending.add(root);long decoded=0,pixels=0;
        while(!pending.isEmpty()) {
            COSBase value=pending.removeLast(); if(!seen.add(value)) continue; if(seen.size()>100000) throw invalid();
            if(value instanceof COSObject object) { if(object.getObject()!=null) pending.add(object.getObject()); }
            else if(value instanceof COSArray array) { for(var item:array) if(item!=null) pending.add(item); }
            else if(value instanceof COSDictionary dict) {
                if("Image".equals(dict.getNameAsString(COSName.SUBTYPE))) {
                    long width=dict.getLong(COSName.WIDTH),height=dict.getLong(COSName.HEIGHT);
                    if(width<=0 || height<=0 || width>20000 || height>20000) throw invalid();
                    pixels+=width*height;if(pixels>20000000) throw invalid();
                }
                if(dict instanceof COSStream stream) {
                    try(var in=stream.createInputStream()) {
                        byte[] buffer=new byte[8192];int n;
                        while((n=in.read(buffer))!=-1) { decoded+=n;if(decoded>52428800) throw invalid(); }
                    }
                }
               for (var key : dict.keySet()) {
    String keyName = key.getName();

    // Reject PDF features capable of executing code or automatically
    // performing potentially unsafe actions.
    if (Set.of(
            "JS",
            "JavaScript",
            "OpenAction",
            "AA",
            "XFA",
            "RichMediaContent",
            "Launch"
    ).contains(keyName)) {
        throw invalid();
    }

    // Inspect action dictionaries by their /S action type.
    if ("S".equals(keyName)) {
        String action = dict.getNameAsString(key, "");

        if (Set.of(
                "JavaScript",
                "Launch",
                "SubmitForm",
                "ImportData",
                "GoToR"
        ).contains(action)) {
            throw invalid();
        }
    }

    COSBase child = dict.getItem(key);

    if (child != null) {
        pending.add(child);
    }
}
            }
        }
    }
}
