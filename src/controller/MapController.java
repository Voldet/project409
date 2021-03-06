package controller;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import bean.Earthquake;
/**
 * @author Vold
 */
@SuppressWarnings("rawtypes")
public class MapController implements ViewController {
    
    private StackPane stackPane;

    private String mapType;

    private ImageView imageView;

    private int W;

    private int H;
    /**
     * Assume it is the worldMap
     */
    WorldMap worldMap;
    /**
     * To control the map
	 * @param stackPane
	 * @param map type
	 */
    public MapController(StackPane stackPane,String mapType){
        this.stackPane = stackPane;
        this.imageView = (ImageView) stackPane.getChildren().get(0);
        this.mapType = mapType;
        this.W = (int) imageView.getImage().getWidth();
        this.H = (int) imageView.getImage().getHeight();
        worldMap = new WorldMap(imageView.getImage(), WorldMap.Projection.valueOf(mapType), 180);
    }

    /**
     * Not use, just  overload this function
     */
    @Override
    public void refresh() {
    }

    /**
     * refresh the Map StackPane 
     * @param quakeList
     */
    public void refresh(ObservableList<Earthquake> quakeList) {
        ObservableList<Node> paneChildren = stackPane.getChildren();
        if (paneChildren.size() > 1) {
            paneChildren.remove(1, paneChildren.size());
        }

        Canvas cv = new Canvas(W, H);
        GraphicsContext gc = cv.getGraphicsContext2D();
        gc.setLineWidth(1.8);
        if (mapType == "MERCATOR"){
            gc.setFill(Color.rgb(252,1,10,0.8));
            refreshMERCATOR(quakeList,gc);
        }else if (mapType == "ECKERT_IV"){
            gc.setFill(Color.rgb(255,1,10,0.8));
            refreshECKERT_IV(quakeList,gc);
        }
        stackPane.getChildren().add(cv);
    }

    /**
     * private details using for refresh map
     * @param quakeList given "ObservableList<Earthquake> quakeList" data for refresh Map
     * @param gc the GraphicsContext where to draw all the shape
     */
    private void refreshMERCATOR(ObservableList<Earthquake> quakeList,GraphicsContext gc) {
        int diameter;
        int[] xy;
        for (Earthquake q : quakeList) {
            diameter = (int) (5 * q.getMagnitude() - 4);
            xy = worldMap.imgxy(q.getLatitude(), q.getLongitude());
            gc.fillOval(xy[0] - diameter / 2, xy[1] - diameter / 2, diameter, diameter);
        }
    }

    /**
     * private details using for refresh
     * @param quakeList given "ObservableList<Earthquake> quakeList" data for refresh Map
     * @param gc the GraphicsContext where to draw all the shape
     */
    private void refreshECKERT_IV(ObservableList<Earthquake> quakeList,GraphicsContext gc){
        int diameter;
        int[] xy;
        for (Earthquake q : quakeList) {
            diameter = (int) (5 * q.getMagnitude() - 4);
            xy = worldMap.imgxy(q.getLatitude(), q.getLongitude());
            gc.fillOval(xy[0] - diameter / 2, xy[1] - diameter / 2, diameter, diameter);
        }
    }

    /** A class able to transform a (latitude, longitude) into pixel coordinates for a large number of projections.
    *  As it claim, the background image is assumed to be a world map
    */
    public static class WorldMap {
        public enum Projection {MERCATOR, ECKERT_IV}

        private WorldMap.Projection proj;
     // O for GreenWich,
        private double meridian; 
        
        // Hufnagel projections parameters
        private double[] paramAngleTable;
        private double[] latitudeTable;
        private double[] yTable;
        private final int tabSize = 101;
        private double A;
        private double B;
        private double C;
        private double K;
        private double psiMax;
        private final double epsilon = 0.001;

        //image parameters
        private double pixelWidth;
        private double pixelHeight;
        private double xcoef;
        private double ycoef;
        /**
         * to project the map into the world map
         * @param background
         * @param proj
         * @param meridian
         */
        WorldMap(Image background, WorldMap.Projection proj, double meridian) {
            double alpha = 0;
            double[] ref;

            this.proj = proj;
            this.pixelWidth = background.getWidth();
            this.pixelHeight = background.getHeight();
            this.meridian = meridian;
            switch (proj) {
                case ECKERT_IV: this.A = 1.0; this.B = 0.0; this.psiMax = Math.PI / 4; alpha = 2.0;
                    this.C = Math.sqrt(alpha*Math.sin(psiMax) * Math.sqrt((1+this.A*Math.cos(2*psiMax) 
                    + this.B*Math.cos(4*psiMax)) / (1+this.A+this.B)));
                    initHufnagel();
                    
                    ref = getXY(0, 180 + meridian);
                    xcoef = pixelWidth / ref[0] / 2;
                    ref = getXY(90, 0);
                    ycoef = pixelHeight / ref[1] / 2;
                    break;
                case MERCATOR:
                    xcoef = 1.0;
                    ycoef = 1.0;
                    break;
                default:
                    break;
            }
        }
        /**
         * To initialize the Hufnagel
         */
        private void initHufnagel() {
            double psi;
            double phi;
            double r;
            double y;
            paramAngleTable = new double[tabSize];
            latitudeTable = new double[tabSize];
            yTable = new double[tabSize];

            this.K = (2 * Math.sqrt(Math.PI)) / Math.sqrt(2 * psiMax + (1 + A - B / 2) * Math.sin(2 * psiMax)
                    + ((A + B) / 2) * Math.sin(4 * psiMax) + (B / 2) * Math.sin(6 * psiMax));
            for (int i = 0; i < tabSize; i++) {
                psi = (i * psiMax) / (tabSize - 1);
                if (i == 0) {
                    phi = 0;
                } else if (i == tabSize - 1) {
                    phi = Math.PI / 2.0;
                } else {
                    phi = Math.asin((2*psi + (1+A-B/2)*Math.sin(2*psi) + ((A+B)/2) * Math.sin(4*psi) + (B/2) * Math.sin(6*psi)) * (K*K) / 4 / Math.PI);
                }
                r = Math.sqrt(1+A*Math.cos(2*psi) + B * Math.cos(4*psi));
                y = (K/C) * r * Math.sin(psi);
                if (i > 0) {
                    if ((y<yTable[i-1]) || (phi<latitudeTable[i-1])) {
                        throw new IllegalStateException();
                    }
                }
                paramAngleTable[i] = psi;
                latitudeTable[i] = phi;
                yTable[i] = y;
            }
        }

        /**To initialize the Hufnagelseed
         * @param phi
         * @return
         */
        private double findHufnagelSeed(double phi) {
            int     iMin = 0;
            int     iMax = tabSize;
            int     iMid;
            boolean loop = true;
            double  phi1;
            double  phi2;
            double  weight;
            double  psi0;
            double  psi1;
            double  psi2;

            while (loop) {
                iMid = (iMin + iMax) / 2;
                if (iMid == iMin) {
                    loop = false;
                } else if (Math.abs(phi) > latitudeTable[iMid]) {
                    iMin = iMid;
                } else {
                    iMax = iMid;
                }
            }
            phi1 = latitudeTable[iMin];
            phi2 = latitudeTable[iMin+1];
            weight = (Math.abs(phi) - phi1) / (phi2 - - phi1);
            psi1 = paramAngleTable[iMin];
            psi2 = paramAngleTable[iMin+1];
            psi0 = weight * (psi2 - psi1) + psi1;
            return (phi < 0 ? -1 * psi0 : psi0);
        }

        /**
         * @param lambda
         * @param phi
         * @return
         */
        public double[] getHufnagelXY(double lambda, double phi) {
            double psi = findHufnagelSeed(phi);
            double deltaPsiNumerator;
            double deltaPsiDenominator;
            double deltaPsi;
            double r;
            boolean loop = true;
            int cnt = 0;
            while (loop) {
                deltaPsiNumerator = (K * K / 4) * (2 * psi + (1 + A - B / 2) * Math.sin(2*psi)
                        + ((A + B) / 2) * Math.sin(4*psi) + (B / 2) * Math.sin(6*psi))- Math.PI * Math.sin(phi);
                if (Math.abs(deltaPsiNumerator) < epsilon) {
                    loop = false;
                } else {
                    deltaPsiDenominator = (K * K / 2) * (1 + (1 + A - B / 2) * Math.cos(2*psi)
                            + (A + B) * Math.cos(4*psi) + ((3 * B) / 2) * Math.cos(6*psi));
                    deltaPsi = deltaPsiNumerator / deltaPsiDenominator;
                    psi -= deltaPsi;
                }
                cnt++;
                if (cnt > 20) {
                    loop = false;
                }
            }
            r =  Math.sqrt(1 + A * Math.cos(2*psi) + B * Math.cos(4*psi)); // Equation 1
            double[] xy = new double[2];
            xy[0] =  (K * C) * lambda * r * Math.cos(psi); // Equation 5
            xy[1] =  (K / C) * r * Math.sin(psi); // Equation 5
            return xy;
        }

        /**
         * @param lambda
         * @param phi
         * @return
         */
        private double[] getMercatorXY(double lambda, double phi) {
            double[] coordinates = new double[2];
            coordinates[0] = (pixelWidth / 2.0 / Math.PI) * lambda;
            coordinates[1] = (pixelWidth / 2.0 / Math.PI) * Math.log(Math.tan(Math.PI / 4.0 + (phi / 2.0)));
            return coordinates;
        }

        /**
         * To set the two maps longitude and magnitude
         * @param latitude
         * @param longitude
         * @return
         */
        private double[] getXY(double latitude, double longitude) {
            double new_longitude;

            new_longitude = longitude - meridian;
            if (new_longitude < -180) {
                new_longitude += 360;
            }
            if (new_longitude > 180) {
                new_longitude -= 360;
            }
            switch (proj) {
                case ECKERT_IV:
                    return getHufnagelXY(new_longitude * Math.PI / 180.0,latitude * Math.PI / 180.0);
                case MERCATOR:
                    return getMercatorXY(new_longitude * Math.PI / 180.0,latitude * Math.PI / 180.0);
                default:
                    return null;
            }
        }

        /**
         * To define the latitude and longitude in the map
         * @param latitude
         * @param longitude
         * @return
         */
        public int[] imgxy(double latitude,double longitude) {
            double[] xy = getXY(latitude, longitude);
            if (xy == null) {
                return null;
            }
            int[] imgcoord = new int[2];
            imgcoord[0] = (int)(Math.round(xy[0] * xcoef) + Math.round(pixelWidth/2));
            imgcoord[1] = (int)(Math.round(pixelHeight/2) - Math.round(xy[1] * ycoef));
            return imgcoord;
        }
    }
}