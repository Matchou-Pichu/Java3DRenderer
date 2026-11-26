import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

class Vertex {
    double x;
    double y;
    double z;
    Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static double VdotV(Vertex v1, Vertex v2){
        return v1.getX() * v2.getX() + v1.getY() * v2.getY() + v1.getZ() * v2.getZ();
    }

    public double ComputeNorm(){
        return Math.sqrt(this.getX() * this.getX() + this.getY() * this.getY() + this.getZ() * this.getZ());
    }

    public double getX(){
        return this.x;
    }
    public double getY(){
        return this.y;
    }
    public double getZ(){
        return this.z;
    }

}

class Triangle{
    Vertex v1;
    Vertex v2;
    Vertex v3;
    Color color;

    Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
    }

    public Vertex getV1(){
        return this.v1;
    }

    public Vertex getV2(){
        return this.v2;
    }

    public Vertex getV3(){
        return this.v3;
    }

    public Color getColor(){
        return this.color;
    }
}

class Matrix3 {
    double[] values;
    Matrix3(double[] values) {
        this.values = values;
    }
    
    public Matrix3 multiply(Matrix3 other){

        double[] result = new double[9];
        for (int row = 0; row <3; row++){
            for(int col = 0; col < 3; col++){
                for (int i = 0; i < 3; i++){
                    result[row * 3 + col] += this.values[row * 3 + i] * other.values[i * 3 + col];
                }
            }
        }

        return new Matrix3(result);
    }

    public Vertex transform(Vertex v){
        return new Vertex(v.getX() * values[0] + v.getY() * values[3] + v.getZ() * values[6],
                          v.getX() * values[1] + v.getY() * values[4] + v.getZ() * values[7],
                          v.getX() * values[2] + v.getY() * values[5] + v.getZ() * values[8]);
    }
}

public class DemoViewer {

    public static Color getShade(Color color, double shade) {
        int red = (int) (color.getRed() * shade);
        int green = (int) (color.getGreen() * shade);
        int blue = (int) (color.getBlue() * shade);
        return new Color(red, green, blue);
    }
    
    public static void main(String[] args) {

        // init of graphical window
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        // Sliders to rotate in 3D
        JSlider headingSlider = new JSlider(0, 360, 180);
        pane.add(headingSlider, BorderLayout.SOUTH);
        JSlider vertSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(vertSlider, BorderLayout.EAST);

        JPanel panel = new JPanel() {
            @Override 
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Render will happen here

                List<Triangle> tris = new ArrayList<>();
                tris.add(new Triangle(new Vertex(200, 200, 200),
                                    new Vertex(-200, -200, 200),
                                    new Vertex(-200, 200, -200),
                                    Color.WHITE));
                tris.add(new Triangle(new Vertex(200, 200, 200),
                                    new Vertex(-200, -200, 200),
                                    new Vertex(200, -200, -200),
                                    Color.RED));
                tris.add(new Triangle(new Vertex(-200, 200, -200),
                                    new Vertex(200, -200, -200),
                                    new Vertex(200, 200, 200),
                                    Color.GREEN));
                tris.add(new Triangle(new Vertex(-200, 200, -200),
                                    new Vertex(200, -200, -200),
                                    new Vertex(-200, -200, 200),
                                    Color.BLUE));

                double heading = Math.toRadians(headingSlider.getValue());
                double pitch = Math.toRadians(vertSlider.getValue());

                Matrix3 transformXZ = new Matrix3(new double[]{
                        Math.cos(heading) , 0, Math.sin(heading),
                        0                 , 1,                 0,
                        -Math.sin(heading), 0, Math.cos(heading)});
                        
                // Matrix3 transformXY = new Matrix3(new double[]{
                //         Math.cos(heading),-Math.sin(heading), 0,
                //         Math.sin(heading), Math.cos(heading), 0,
                //         0                , 0                , 1});
                
                Matrix3 transformYZ = new Matrix3(new double[]{
                        1               , 0               , 0              ,
                        0               , Math.cos(pitch) , Math.sin(pitch), 
                        0               , -Math.sin(pitch), Math.cos(pitch)});

                Matrix3 transform = transformYZ.multiply(transformXZ);

                //printing the wireframe on gui
                g2.translate(0, 0);
                g2.setColor(Color.WHITE);

                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                double[] zBuffer = new double[img.getWidth() * img.getHeight()];

                for(int z = 0; z < zBuffer.length; z++){
                    zBuffer[z] = Double.NEGATIVE_INFINITY;
                }

                Vertex light = new Vertex(0, 0, 1);
                double lightNorm = light.ComputeNorm();

                for(Triangle t : tris){

                    // We are not using GPU rasterization  so we can't
                    // use graph2D anymore and have to do it manually.

                    Vertex v1 = transform.transform(t.getV1());
                    Vertex v2 = transform.transform(t.getV2());
                    Vertex v3 = transform.transform(t.getV3());

                    v1.x += getWidth() /2;
                    v1.y += getHeight()/2;
                    v2.x += getWidth()/2;
                    v2.y += getHeight()/2;
                    v3.x += getWidth()/2;
                    v3.y += getHeight()/2;

                    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);

                    Vertex norm = new Vertex(
                                            ab.getX() * ac.getZ() - ab.getX() * ac.getY(), 
                                            ab.getZ() * ac.getX() - ab.getX() * ac.getZ(),
                                            ab.getX() * ac.getY() - ab.getY() * ac.getX()
                    );

                    double normalNorm = norm.ComputeNorm();

                    norm.x /= normalNorm;
                    norm.y /= normalNorm;
                    norm.z /= normalNorm;
                    
                    double angleCos = Vertex.VdotV(norm, light) / (normalNorm * lightNorm);

                    // Retangular bounds for triangle  adn colorization
                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));

                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(img.getHeight() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {

                            // checking if the pixel is in the bound to colorize
                            double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                            double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                            double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;

                            
                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                double depth = b1 * v1.getZ() + b2 * v2.getZ() + b3 * v3.getZ();
                                int zInd = y * img.getWidth() + x;
                                if (zBuffer[zInd] < depth){
                                    img.setRGB(x, y, getShade(t.getColor(), angleCos).getRGB());
                                    zBuffer[zInd] = depth;
                                }
                            }
                            
                        }
                    }

                    // Drawing the wireframe
                    Path2D path = new Path2D.Double();
                    path.moveTo(v1.getX(), v1.getY());
                    path.lineTo(v2.getX(), v2.getY());
                    path.lineTo(v3.getX(), v3.getY());
                    
                    path.closePath();
                    g2.drawImage(img, 0,0 , null);
                }
            }
        };

        headingSlider.addChangeListener(e -> panel.repaint());
        vertSlider.addChangeListener(e -> panel.repaint());
        pane.add(panel, BorderLayout.CENTER);

        frame.setSize(800, 800);
        frame.setVisible(true);

    }
}
