import javax.swing.*;
import javax.swing.event.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Random;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.lang.Math;

import net.objecthunter.exp4j.*;


public class VectorFieldDemo {

   public static int timeDelay = 4;
   
   public static void main(String[] args) throws Exception {
      VFFrame mframe = new VFFrame();
      while(true) {
         mframe.tick();
         Thread.sleep(timeDelay);
         mframe.repaint();
      }
   }
}

class VFFrame extends JFrame {

   private JPanel mainPanel;
   private JPanel controlPanel;
   private JPanel eqnPanel;
   private JButton update;
   
   private JTextField iComp;
   private JTextField jComp;
   private String iCompExpr;
   private String jCompExpr;
   
   private JSlider speedControl;
   private JSlider resControl;
   
   private VFPanel vf;
   private boolean isPlaying = true;
   
   private ConcurrentLinkedDeque<TrackPoint> tracking = new ConcurrentLinkedDeque<TrackPoint>();
   
   private static final Color darkColor = new Color(0,0,0);
   private static final Color lightShade = new Color(175,175,175);
   private static final Color highlightColor = new Color(255,0,0);
   private static final int baseres = 100;
   private static final double factor = 100.0;
   
   private static int resolution = 100;
   
   private static final boolean enableTracking = true;
   private static final int numLastPoints = 15;
   
  
   private TrackPoint mpoint = new TrackPoint(0,0);
   
   //private TrackPoint[] rect = new TrackPoint[4];
   private ArrayList<TrackPoint> figure = new ArrayList<TrackPoint>();
   private int figureResolution = 20;
   
   private TrackPoint startPoint = new TrackPoint(0,0);
   
   private boolean isPoint = false;
   private boolean isRect = false;
   private boolean isSelectingRect = false;
   
   private int tickTime = 0;

   public VFFrame() {
      super("Vector Field Demo");
      this.setResizable(false);
      this.setSize(400,400); // gets repacked
      this.setVisible(true);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      Container c = this.getContentPane();
      controlPanel = new JPanel();
      controlPanel.setLayout(new GridLayout(6, 1));
      iComp = new JTextField("-y",5);
      jComp = new JTextField("x",5);
      iCompExpr = iComp.getText();
      jCompExpr = jComp.getText();
      iComp.setHorizontalAlignment(JTextField.RIGHT);
      jComp.setHorizontalAlignment(JTextField.RIGHT);
      eqnPanel = new JPanel();
      eqnPanel.add(new JLabel("f(x,y) = "));
      eqnPanel.add(iComp);
      eqnPanel.add(new JLabel("i + "));
      eqnPanel.add(jComp);
      eqnPanel.add(new JLabel("j"));
      controlPanel.add(eqnPanel);
      update = new JButton("Update");
      update.addActionListener(new ActionListener() { 
         public void actionPerformed(ActionEvent e) {
            iCompExpr = iComp.getText();
            jCompExpr = jComp.getText();
         }
      });
      controlPanel.add(update);
      speedControl = new JSlider(JSlider.HORIZONTAL, 1, 10, 6);
      speedControl.addChangeListener(new SpeedSliderListener());
      speedControl.setMajorTickSpacing(5);
      speedControl.setMinorTickSpacing(1);
      speedControl.setPaintTicks(true);
      controlPanel.add(new JLabel("Speed:"));
      controlPanel.add(speedControl);
      resControl = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
      resControl.addChangeListener(new ResSliderListener());
      resControl.setMajorTickSpacing(5);
      resControl.setMinorTickSpacing(1);
      resControl.setPaintTicks(true);
      controlPanel.add(new JLabel("Resolution:"));
      controlPanel.add(resControl);
      //controlPanel.add(new JLabel("placeholder 1"));
      //controlPanel.add(new JLabel("placeholder 2"));
      vf = new VFPanel();
      vf.addMouseListener(new VFMouseListener(vf));
      mainPanel = new JPanel();
      mainPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
      mainPanel.add(vf);
      mainPanel.add(controlPanel);
      this.add(mainPanel);
      this.pack();
   }
   
   public void tick() {
      tickTime ++;
      double dx,dy;
      if(isPlaying) {
         if(isPoint) {
            if(tickTime % 5 == 0) {
               tracking.add(new TrackPoint(mpoint.x, mpoint.y));
               if(tracking.size() > numLastPoints) {
                  tracking.pollFirst();
               }
            }
            for(int i = 0; i < resolution; i ++) {
               dx = vf.evalXAt(mpoint.x, mpoint.y);
               dy = vf.evalYAt(mpoint.x, mpoint.y);
               mpoint.x += dx / (factor * resolution);
               mpoint.y += dy / (factor * resolution);
            }
         }
         else if(isRect) {
            for(int j = 0; j < figure.size(); j ++) {
               for(int i = 0; i < resolution/10.0; i ++) {
                  dx = vf.evalXAt(figure.get(j).x, figure.get(j).y);
                  dy = vf.evalYAt(figure.get(j).x, figure.get(j).y);
                  figure.get(j).x += dx / (factor * resolution);
                  figure.get(j).y += dy / (factor * resolution);
               }
            }
         }
      }
   }
   
   private class SpeedSliderListener implements ChangeListener {
      public void stateChanged(ChangeEvent e) {
         JSlider source = (JSlider)e.getSource();
         if(!source.getValueIsAdjusting()) {
            VectorFieldDemo.timeDelay = 10 - source.getValue();
         }
      }
   }
   
   
   private class ResSliderListener implements ChangeListener {
      public void stateChanged(ChangeEvent e) {
         JSlider source = (JSlider)e.getSource();
         if(!source.getValueIsAdjusting()) {
            int val = source.getValue();
            if(val < 10) {
               resolution = baseres / (11-val);
            }
            else if(val > 10) {
               resolution = baseres * (val-9);
            }
            else {
               resolution = baseres;
            }
         }
      }
   }
   
   private class TrackPoint {
      public double x;
      public double y;
      
      public TrackPoint(double x, double y) {
         this.x = x;
         this.y = y;
      }
   }
   
   private class VFMouseListener implements MouseListener {
      private VFPanel parent;
      
      public VFMouseListener(VFPanel myParent) {
         parent = myParent;
      }
      
      public void mousePressed(MouseEvent e) {
         isPoint = false;
         isRect = false;
         isSelectingRect = true;
         startPoint.x = (double)e.getX() / VFPanel.wWidth * (parent.maxX - parent.minX + 2) + parent.minX - 1;
         startPoint.y = - ((double)e.getY() / VFPanel.wHeight * (parent.maxY - parent.minY + 2) + parent.minY - 1);
      }
      
      public void mouseReleased(MouseEvent e) {
         if(isPoint)
            return;
            
         figure = new ArrayList<TrackPoint>();
         
         isSelectingRect = false;
         isRect = true;
         
         double tx, ty;
         tx = (double)e.getX() / VFPanel.wWidth * (parent.maxX - parent.minX + 2) + parent.minX - 1;
         ty = - ((double)e.getY() / VFPanel.wHeight * (parent.maxY - parent.minY + 2) + parent.minY - 1);
         for(int i = 0; i < figureResolution; i ++) { // along x = startPoint.x
            figure.add(new TrackPoint(startPoint.x, startPoint.y + (i * (ty - startPoint.y) / figureResolution)));
         }
         for(int i = 0; i < figureResolution; i ++) { // along y = ty
            figure.add(new TrackPoint(startPoint.x + (i * (tx - startPoint.x) / figureResolution), ty));
         }
         for(int i = 0; i < figureResolution; i ++) { // along x = tx
            figure.add(new TrackPoint(tx, ty + (i * (startPoint.y - ty) / figureResolution)));
         }
         for(int i = 0; i < figureResolution; i ++) { // along y = startPoint.y
            figure.add(new TrackPoint(tx + (i * (startPoint.x - tx) / figureResolution), startPoint.y));
         }
      }
      
      public void mouseClicked(MouseEvent e) {
         isSelectingRect = false;
         isRect = false;
         isPoint = true;
         mpoint.x = (double)e.getX() / VFPanel.wWidth * (parent.maxX - parent.minX + 2) + parent.minX - 1;
         mpoint.y = - ((double)e.getY() / VFPanel.wHeight * (parent.maxY - parent.minY + 2) + parent.minY - 1);
      }
      
      public void mouseExited(MouseEvent e) {}
      public void mouseEntered(MouseEvent e) {}
   }

   private class VFPanel extends JPanel {
      public static final int wWidth = 400;
      public static final int wHeight = 400;
      
      public int minX = -5;
      public int maxX = 5;
      public int minY = -5;
      public int maxY = 5;
      private final double vHat = 5.0;
      private final double branchAngle = Math.PI / 6.0;
      
      public VFPanel() {
         super();
      }

      public void paintComponent(Graphics g) {
         g.setColor(new Color(255,255,255));
         g.fillRect(0,0,wWidth,wHeight);
         g.setColor(darkColor);
         int numXRegions = maxX - minX + 2;
         int numYRegions = maxY - minY + 2;
         double xWidth = ((double)wWidth) / numXRegions;
         double yWidth = ((double)wHeight) / numYRegions;
         
         g.setColor(lightShade);
         for(int i = 1; i < numXRegions; i ++) {
            g.drawLine((int)(i * wWidth / numXRegions), 0,(int)( i * 400.0 / numXRegions), 400);
         }
         
         for(int i = 1; i < numYRegions; i ++) {
            g.drawLine(0, (int)(i * wHeight / numYRegions), 400, (int)( i * 400.0 / numYRegions));
         }
         
         g.setColor(darkColor);
         g.drawLine(0, (int)((1-minY) *  ((double)wHeight) / numYRegions) + 1, wWidth, (int)( (1-minY) * ((double)wHeight) / numYRegions) + 1);
         g.drawLine(0, (int)((1-minY) *  ((double)wHeight) / numYRegions), wWidth, (int)( (1-minY) * ((double)wHeight) / numYRegions));
         g.drawLine(0, (int)((1-minY) *  ((double)wHeight) / numYRegions) - 1, wWidth, (int)( (1-minY) * ((double)wHeight) / numYRegions) - 1);
         
         g.drawLine((int)((1-minX) * ((double)wWidth) / numXRegions) + 1, 0,(int)( (1-minX) * ((double)wWidth) / numXRegions) + 1, wHeight);
         g.drawLine((int)((1-minX) * ((double)wWidth) / numXRegions), 0,(int)( (1-minX) * ((double)wWidth) / numXRegions), wHeight);
         g.drawLine((int)((1-minX) * ((double)wWidth) / numXRegions) - 1, 0,(int)( (1-minX) * ((double)wWidth) / numXRegions) - 1, wHeight);
               
         double[][][] values = new double[maxX-minX+3][maxY-minY+3][2];
         double maxDist = -1;
         double d;
         for(int i = 0; i < maxX-minX+3; i ++) {
            for(int j = 0; j < maxY-minY+3; j ++) {
               values[i][j][0] = evalXAt(minX + i - 1, maxY - j + 1);
               values[i][j][1] = evalYAt(minX + i - 1, maxY - j + 1);
               d = Math.hypot(values[i][j][0], values[i][j][1]);
               if(d > maxDist) maxDist = d;
            }
         }
         double sx, sy, dx, dy, theta;
         for(int i = 0; i < numXRegions+1; i ++) {
            for(int j = 0; j < numYRegions+1; j ++) {
               sx = i * xWidth;
               sy = j * yWidth;
               dx = xWidth * values[i][j][0] / maxDist;
               dy = yWidth * values[i][j][1] / maxDist;
               theta = Math.atan2(dy, dx);
               g.drawLine((int)(sx), (int)(sy), (int)(sx+dx), (int)(sy-dy));
               g.drawLine((int)(sx+dx), (int)(sy-dy), (int)(sx+dx-vHat*Math.cos(theta+branchAngle)), (int)(sy-dy+vHat*Math.sin(theta+branchAngle)));
               g.drawLine((int)(sx+dx), (int)(sy-dy), (int)(sx+dx-vHat*Math.cos(theta-branchAngle)), (int)(sy-dy+vHat*Math.sin(theta-branchAngle)));
            }
         }
         double px, py;
         if(isPoint) {
            g.setColor(highlightColor);
            px = (mpoint.x - minX + 1) * wWidth / (maxX - minX + 2);
            py = (-mpoint.y - minY + 1) * wHeight / (maxY - minY + 2); 
            g.fillOval((int)(px-2.5), (int)(py-2.5), 6, 6);
         }
         else if(isRect) {
            g.setColor(highlightColor);
            int[] pxs = new int[figure.size()];
            int[] pys = new int[figure.size()];
            for(int j = 0; j < figure.size(); j ++) {
               pxs[j] = (int)(0.5 + (figure.get(j).x - minX + 1) * wWidth / (maxX - minX + 2));
               pys[j] = (int)(0.5 + (-figure.get(j).y - minY + 1) * wHeight / (maxY - minY + 2)); 
            }
            g.fillPolygon(pxs, pys, figure.size());
         }
         if(isPoint) {
            for(TrackPoint tp : tracking) {
               px = (tp.x - minX + 1) * wWidth / (maxX - minX + 2);
               py = (-tp.y - minY + 1) * wHeight / (maxY - minY + 2); 
               g.drawLine((int)(px+0.5), (int)(py+0.5), (int)(px+0.5), (int)(py+0.5));
            }
         }
      }

      public double evalXAt(double x, double y) {
         try {
            Expression e = new ExpressionBuilder(iCompExpr).variables("x","y").build().setVariable("x",x).setVariable("y",y);
            return e.evaluate();
         } catch (ArithmeticException e) {
            return 0;
         }
      }
      
      public double evalYAt(double x, double y) {
         try {
            Expression e = new ExpressionBuilder(jCompExpr).variables("x","y").build().setVariable("x",x).setVariable("y",y);
            return e.evaluate();
         } catch (ArithmeticException e) {
            return 0;
         }   
      }

      public Dimension getPreferredSize() {
         return new Dimension(wWidth,wHeight);
      }
   }
}
