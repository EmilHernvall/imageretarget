package se.c0la.imageretarget;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

/**
 *
 * @author Administratör
 */
public class ImageRetarget {
    
    ImageEnergyFunction energyFunction = null;
    
    /** Creates a new instance of RetargetImage */
    public ImageRetarget(ImageEnergyFunction energyFunction) {
        this.energyFunction = energyFunction;
    }
    
    public BufferedImage retarget(BufferedImage src, int newX, int newY) {

        System.out.println("Converting.");
        int[][] img = convert(src,
                              Math.max(src.getWidth(), newX),
                              Math.max(src.getHeight(), newY));
        
        System.out.println("Calculating energy.");
        img = energyFunction.transform(img);
        
        int width = src.getWidth();
        int height = src.getHeight();
        
        int dx = width - newX;
        int unitX = dx != 0 ? -dx / Math.abs(dx) : 1;
        for (int i = 0; i < Math.abs(dx); i++) {
            System.out.println("Vertical carving: " + Math.round((float)i/(float)Math.abs(dx)*100) + "% done.");
            
            int[][] seam = getOptimalSeamX(img, width, height);
            for (int[] xy : seam) {
                int y = xy[1];
                if (unitX > 0) {
                    img[xy[0]][xy[1]] = img[xy[0]][xy[1]] | (0xFF << 24);
                    for (int x = width-1; x >= xy[0]; x--) {
                        img[x+1][y] = img[x][y];
                    }
                    int c = 0;
                    c |= ((((img[xy[0]-1][xy[1]] >> 24) & 0xFF) + ((img[xy[0]+1][xy[1]] >> 24) & 0xFF)) / 2) << 24;
                    c |= ((((img[xy[0]-1][xy[1]] >> 16) & 0xFF) + ((img[xy[0]+1][xy[1]] >> 16) & 0xFF)) / 2) << 16;
                    c |= ((((img[xy[0]-1][xy[1]] >> 8) & 0xFF) + ((img[xy[0]+1][xy[1]] >> 8) & 0xFF)) / 2) << 8;
                    c |= (((img[xy[0]-1][xy[1]] & 0xFF) + (img[xy[0]+1][xy[1]] & 0xFF)) / 2);
                    img[xy[0]][xy[1]] = c;
                } else {
                    for (int x = xy[0]; x < width-1; x++) {
                        img[x][y] = img[x+1][y];
                    }
                }
            }
            
            width += unitX;
        }
        
        int dy = height - newY;
        int unitY = dy != 0 ? -dy / Math.abs(dy) : 1;
        for (int i = 0; i < Math.abs(dy); i++) {
            System.out.println("Horizontal carving: " + Math.round((float)i/(float)Math.abs(dy)*100) + "% done.");

            int[][] seam = getOptimalSeamY(img, width, height);
            for (int[] xy : seam) {
                int x = xy[0];
                if (unitY > 0) {
                    img[xy[0]][xy[1]] = img[xy[0]][xy[1]] | (0xFF << 24);
                    for (int y = height-1; y > xy[1]; y--) {
                        img[x][y+1] = img[x][y];
                    }
                    int c = 0;
                    c |= ((((img[xy[0]][xy[1]-1] >> 24) & 0xFF) + ((img[xy[0]][xy[1]+1] >> 24) & 0xFF)) / 2) << 24;
                    c |= ((((img[xy[0]][xy[1]-1] >> 16) & 0xFF) + ((img[xy[0]][xy[1]+1] >> 16) & 0xFF)) / 2) << 16;
                    c |= ((((img[xy[0]][xy[1]-1] >> 8) & 0xFF) + ((img[xy[0]][xy[1]+1] >> 8) & 0xFF)) / 2) << 8;
                    c |= (((img[xy[0]][xy[1]-1] & 0xFF) + (img[xy[0]][xy[1]+1] & 0xFF)) / 2);
                    img[xy[0]][xy[1]] = c;
                } else {
                    for (int y = xy[1]; y < height-1; y++) {
                        img[x][y] = img[x][y+1];
                    }
                }
            }
            
            height += unitY;
        }
        
        BufferedImage dst = convert(img, width, height, false);
        
        return dst;
    }
    
    protected int[][] convert(BufferedImage img, int allocateX, int allocateY) {
        WritableRaster raster = img.getRaster();
        int[][] res = new int[allocateX][allocateY];
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int[] colors = raster.getPixel(x,y,new int[3]);
                res[x][y] = ((colors[0] & 0xFF) << 16) | ((colors[1] & 0xFF) << 8) | colors[2] & 0xFF;
            }
        }
        return res;
    }
    
    protected BufferedImage convert(int[][] img, int width, int height, boolean energy) {
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = res.getRaster();
        for (int x = 0; x < res.getWidth(); x++) {
            for (int y = 0; y < res.getHeight(); y++) {
                int[] colors;
                if (energy) {
                    colors = new int[] { (img[x][y] >> 24) & 0xFF,
                                         (img[x][y] >> 24) & 0xFF,
                                         (img[x][y] >> 24) & 0xFF };
                } else {
                    colors = new int[] { (img[x][y] >> 16) & 0xFF,
                                         (img[x][y] >> 8) & 0xFF,
                                         img[x][y] & 0xFF };
                }
                raster.setPixel(x,y,colors);
            }
        }
        return res;
    }
    
    protected double[][] getCostTableX(int[][] img, int width, int height) {
	double[][] q = new double[height][width];
	for (int i = 0; i < width; i++) {
            q[0][i] = getEnergy(img[i][0]);
	}
        
	for (int y = 0; y < height; y++) {
            q[y][0] = Double.POSITIVE_INFINITY;
            q[y][width-1] = Double.POSITIVE_INFINITY;
	}
	
	for (int y = 1; y < height; y++) {
            for (int x = 1; x < width-1; x++) {
                double m = min(new double[] { q[y-1][x-1],q[y-1][x],q[y-1][x+1] });
                q[y][x] = m + getEnergy(img[x][y]);
            }
	}
        
        return q;
    }
    
    protected int[][] getOptimalSeamX(int[][] img, int width, int height) {
	double[][] q = getCostTableX(img, width, height);
        
	int n = q.length - 1;
	double min = -1;
	int minX = 0;
	for (int i = 0; i < q[0].length; i++) {
            if (min == -1 || q[n][i] < min) {
                min = q[n][i];
                minX = i;
            }
	}
        
        int[][] path = new int[q.length][];
        int x = minX;
	for (int y = q.length - 1; y >= 0; y--) {
            path[y] = new int[] { x, y };
            if (y == 0) {
                break;
            }
            double m = min(new double[] { q[y-1][x-1], q[y-1][x], q[y-1][x+1] });
            if (m == q[y-1][x-1]) {
                x--;
            } else if (m == q[y-1][x+1]) {
                x++;
            }
	}
	
	return path;
    }
    
    protected double[][] getCostTableY(int[][] img, int width, int height) {
        double[][] q = new double[width][height];
        
	for (int i = 0; i < height; i++) {
            q[0][i] = getEnergy(img[0][i]);
	}
        
	for (int x = 0; x < width; x++) {
            q[x][0] = Double.POSITIVE_INFINITY;
            q[x][height-1] = Double.POSITIVE_INFINITY;
	}
        
        for (int x = 1; x < width; x++) {
            for (int y = 1; y < height-1; y++) {
                double m = min(new double[] { q[x-1][y-1],q[x-1][y],q[x-1][y+1] });
                q[x][y] = m + getEnergy(img[x][y]);
            }
	}

        return q;
    }
    
    protected int[][] getOptimalSeamY(int[][] img, int width, int height) {
	double[][] q = getCostTableY(img, width, height);
        
	int n = width - 1;
	double min = -1;
	int minY = 0;
	for (int i = 0; i < height; i++) {
            if (min == -1 || q[n][i] < min) {
                min = q[n][i];
                minY = i; 
            }
	}
        
        int[][] path = new int[q.length][];
        int y = minY;
	for (int x = q.length - 1; x >= 0; x--) {
            path[x] = new int[] { x, y };
            if (x == 0) {
                break;
            }
            double m = min(new double[] { q[x-1][y-1], q[x-1][y], q[x-1][y+1] });
            if (m == q[x-1][y-1]) {
                y--;
            } else if (m == q[x-1][y+1]) {
                y++;
            }
	}
	
	return path;
    }
    
    protected int getAverage(int colors) {
        return (((colors >> 16) & 0xFF) + ((colors >> 8) & 0xFF) + (colors & 0xFF)) / 3;
    }
    
    protected int getEnergy(int colors) {
        return (colors >> 24) & 0xFF;
    }
    
    protected double min(double[] a) {
        double o = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            o = Math.min(o, a[i]);
        }
        return o;
    }
    
}
