/*
 * Other implementations:
 *
 * http://kennberg.com/project_resize.php
 * http://www.thegedanken.com/retarget/
 * http://www.ultra-premium.com/b/photos/resize.html 
 * http://gabeiscoding.com/
 * http://www.semanticmetadata.net/2007/08/30/content-aware-image-resizing-gpl-implementation/
 * http://home.arcor.de/tit4tat/SeamCarving.jnlp
 * http://www.zeropointnine.com/blog/seam-carving-in-as3-with-source
 * http://swieskowski.net/carve/
 **/

package se.c0la.imageretarget;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.IIOException;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

/**
 *
 * @author Administratör
 */
public class Main {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String inName = "";
        String outName = "";
        int width = 0;
        int height = 0;
        
        try {
            inName = args[0];
            outName = args[1];
            width = Integer.parseInt(args[2]);
            height = Integer.parseInt(args[3]);
        } catch (Exception e) {
            System.out.println("Usage: java -jar ImageRetarget.jar infile outfile width height");
            return;
        }
        
        BufferedImage img;
        try {
            img = ImageIO.read(new File(inName));
        } 
        catch (IIOException e2) {
            System.out.println("Couldn't open source image!");
            return;
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        ImageRetarget ir = new ImageRetarget(new GradientMagnitudeFunction());
        BufferedImage result = ir.retarget(img, width, height);
        
        try {
            String[] out = outName.split("\\.");
            String type = out[out.length-1];
            ImageIO.write(result, type, new File(outName));
        } 
        catch (Exception e) {
            System.out.println("Failed to write output image!");
        }
    }
    
}
