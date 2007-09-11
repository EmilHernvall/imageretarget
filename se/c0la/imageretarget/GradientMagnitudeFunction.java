package se.c0la.imageretarget;

/**
 *
 * @author Administratör
 */
public class GradientMagnitudeFunction implements ImageEnergyFunction {

    public int[][] transform(int[][] img) {

        double maxEnergy = 0;
        double energy[][] = new double[img.length][img[0].length];
        for (int x = 0; x < img.length; x++) {
            for (int y = 0; y < img[0].length; y++) {
                int[] xWiseCoordinates = new int[] { Math.max(0,x-1), 
                                                     x,
                                                     Math.min(img.length-1,x+1) };

                int[] xWiseColors = new int[3];
                xWiseColors[0] = getAverage(img[xWiseCoordinates[0]][y]);
                xWiseColors[1] = getAverage(img[xWiseCoordinates[1]][y]);
                xWiseColors[2] = getAverage(img[xWiseCoordinates[2]][y]);

                double[] xCoefficients = getCoefficients(xWiseCoordinates, xWiseColors);

                double energyX = 0;
                for (int i = 1; i < xCoefficients.length; i++) {
                    energyX += i * xCoefficients[i] * Math.pow(x, i-1);
                }

                int[] yWiseCoordinates = new int[] { Math.max(0,y-1), 
                                                     y,
                                                     Math.min(img[0].length-1,y+1) };

                int[] yWiseColors = new int[3];
                yWiseColors[0] = getAverage(img[x][yWiseCoordinates[0]]);
                yWiseColors[1] = getAverage(img[x][yWiseCoordinates[1]]);
                yWiseColors[2] = getAverage(img[x][yWiseCoordinates[2]]);

                double[] yCoefficients = getCoefficients(yWiseCoordinates, yWiseColors);

                double energyY = 0;
                for (int i = 1; i < yCoefficients.length; i++) {
                    energyY += i * yCoefficients[i] * Math.pow(y, i-1);
                }

                energy[x][y] = Math.abs(energyX) + Math.abs(energyY);
                if (new Double(energy[x][y]).isNaN()) {
                    energy[x][y] = 0;
                }
                maxEnergy = Math.max(maxEnergy, energy[x][y]);
            }
        }
        
        for (int x = 0; x < energy.length; x++) {
            for (int y = 0; y < energy[x].length; y++) {
                int newEnergy = (int)(energy[x][y] / maxEnergy * 0xFF);
                img[x][y] = ((newEnergy & 0xFF) << 24) | img[x][y];
            }
        }
        
        return img;
    }

    protected int getAverage(int colors) {
        return (((colors >> 16) & 0xFF) + ((colors >> 8) & 0xFF) + (colors & 0xFF)) / 3;
    }

    protected double[] getCoefficients(int[] vX, int[] mC) {
        double[][] mX = new double[3][3];
        double[][] b = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double base = (double)vX[i];
                double exp = (double)j;
                mX[i][j] = Math.pow(vX[i], j);
            }
        }

        double det = mX[0][0]*mX[1][1]*mX[2][2] 
                        - mX[0][0]*mX[1][2]*mX[2][1] 
                        - mX[0][1]*mX[1][0]*mX[2][2] 
                        + mX[0][2]*mX[1][0]*mX[2][1] 
                        + mX[0][1]*mX[1][2]*mX[2][0] 
                        - mX[0][2]*mX[1][1]*mX[2][0];

        b[0][0] = 1/det * (mX[1][1] * mX[2][2] - mX[1][2] * mX[2][1]);
        b[0][1] = 1/det * (mX[0][2] * mX[2][1] - mX[0][1] * mX[2][2]);
        b[0][2] = 1/det * (mX[0][1] * mX[1][2] - mX[0][2] * mX[1][1]);

        b[1][0] = 1/det * (mX[1][2] * mX[2][0] - mX[1][0] * mX[2][2]);
        b[1][1] = 1/det * (mX[0][0] * mX[2][2] - mX[0][2] * mX[2][0]);
        b[1][2] = 1/det * (mX[0][2] * mX[1][0] - mX[0][0] * mX[1][2]);

        b[2][0] = 1/det * (mX[1][0] * mX[2][1] - mX[1][1] * mX[2][0]);
        b[2][1] = 1/det * (mX[0][1] * mX[2][0] - mX[0][0] * mX[2][1]);
        b[2][2] = 1/det * (mX[0][0] * mX[1][1] - mX[0][1] * mX[1][0]);

        double[] c = new double[3];

        c[0] = b[0][0] * mC[0] + b[0][1] * mC[1] + b[0][2] * mC[2];
        c[1] = b[1][0] * mC[0] + b[1][1] * mC[1] + b[1][2] * mC[2];
        c[2] = b[2][0] * mC[0] + b[2][1] * mC[1] + b[2][2] * mC[2];

        return c;
    }
}
