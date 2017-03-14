import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import java.util.ArrayList;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

// OK this is not best practice - maybe you'd like to create
// a volume data class?
// I won't give extra marks for that though.

public class Example extends JFrame {
	ArrayList<JLabel> thumbnailtop = new ArrayList<JLabel>();
	ArrayList<JLabel> thumbnailside = new ArrayList<JLabel>();
	ArrayList<JLabel> thumbnailfront = new ArrayList<JLabel>();

	JButton mip_button, non_mip, resize, histogramEqualization,
			thumbNailViewTop, thumbNailViewSide, thumbNailViewFront; // an
																		// example
	// button to
	// switch to MIP
	// mode
	JLabel warningLabel;

	JLabel image_icon1; // using JLabel to display an image (check online
						// documentation)
	JLabel image_icon2; // using JLabel to display an image (check online
						// documentation)
	JLabel image_icon3;
	JLabel thumb;
	JSlider non_Mip_z, non_Mip_y, non_Mip_x, resizeSlider; // sliders to step
															// through the
	// slices (z and y
	// directions) (remember 113
	// slices in z direction
	// 0-112)
	BufferedImage image1, image2, image3, newImage; // storing the image in
	JTextField resizeInput; // memory
	short cthead[][][]; // store the 3D volume data set
	short min, max;
	float histogram[];
	float mapping[];// min/max value in the 3D volume data set

	/*
	 * This function sets up the GUI and reads the data set
	 */
	public void Example() throws IOException {
		// File name is hardcoded here - much nicer to have a dialog to select
		// it and capture the size from the user
		File file = new File("CThead");

		// Create a BufferedImage to store the image data
		image1 = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
		image2 = new BufferedImage(256, 112, BufferedImage.TYPE_3BYTE_BGR);
		image3 = new BufferedImage(256, 112, BufferedImage.TYPE_3BYTE_BGR);

		// Read the data quickly via a buffer (in C++ you can just do a single
		// fread - I couldn't find the equivalent in Java)
		DataInputStream in = new DataInputStream(new BufferedInputStream(
				new FileInputStream(file)));

		int i, j, k; // loop through the 3D data set

		min = Short.MAX_VALUE;
		max = Short.MIN_VALUE; // set to extreme values
		short read; // value read in
		int b1, b2; // data is wrong Endian (check wikipedia) for Java so we
					// need to swap the bytes around

		cthead = new short[113][256][256]; // allocate the memory - note this is
											// fixed for this data set
		// loop through the data reading it in
		for (k = 0; k < 113; k++) {
			for (j = 0; j < 256; j++) {
				for (i = 0; i < 256; i++) {
					// because the Endianess is wrong, it needs to be read byte
					// at a time and swapped
					b1 = ((int) in.readByte()) & 0xff; // the 0xff is because
														// Java does not have
														// unsigned types (C++
														// is so much easier!)
					b2 = ((int) in.readByte()) & 0xff; // the 0xff is because
														// Java does not have
														// unsigned types (C++
														// is so much easier!)
					read = (short) ((b2 << 8) | b1); // and swizzle the bytes
														// around
					if (read < min)
						min = read; // update the minimum
					if (read > max)
						max = read; // update the maximum
					cthead[k][j][i] = read; // put the short into memory (in C++
											// you can replace all this code
											// with one fread)
				}
			}
		}
		System.out.println(min + " " + max); // diagnostic - for CThead this
												// should be -1117, 2248
		// (i.e. there are 3366 levels of grey (we are trying to display on 256
		// levels of grey)
		// therefore histogram equalization would be a good thing

		// Set up the simple GUI
		// First the container:
		Container container = getContentPane();
		container.setLayout(new FlowLayout());

		warningLabel = new JLabel();
		warningLabel.setText("Full screen to view all sliders");
		warningLabel.setVisible(true);
		container.add(warningLabel);

		// Then our image (as a label icon)
		image_icon1 = new JLabel(new ImageIcon(image1));
		container.add(image_icon1);
		image_icon2 = new JLabel(new ImageIcon(image2));
		container.add(image_icon2);
		image_icon3 = new JLabel(new ImageIcon(image3));
		container.add(image_icon3);

		// Then the invert button
		mip_button = new JButton("MIP");
		container.add(mip_button);

		resize = new JButton("resize");
		container.add(resize);

		thumbNailViewTop = new JButton("ThumbNail Top");
		container.add(thumbNailViewTop);

		thumbNailViewSide = new JButton("ThumbNailside");
		container.add(thumbNailViewSide);

		thumbNailViewFront = new JButton("thumbNailFront");
		container.add(thumbNailViewFront);

		histogramEqualization = new JButton("Histogram Eq");
		container.add(histogramEqualization);

		non_mip = new JButton("Non-MIP");
		container.add(non_mip);

		resizeSlider = new JSlider(20, 256);
		container.add(resizeSlider);

		resizeInput = new JTextField();
		resizeInput.setColumns(20);
		container.add(resizeInput);

		non_Mip_z = new JSlider(0, 112);
		container.add(non_Mip_z);
		non_Mip_z.setMajorTickSpacing(50);
		non_Mip_z.setMinorTickSpacing(10);
		non_Mip_z.setPaintTicks(true);
		non_Mip_z.setPaintLabels(true);

		non_Mip_x = new JSlider(0, 255);
		container.add(non_Mip_x);
		non_Mip_x.setMajorTickSpacing(50);
		non_Mip_x.setMinorTickSpacing(10);
		non_Mip_x.setPaintTicks(true);
		non_Mip_x.setPaintLabels(true);

		non_Mip_y = new JSlider(0, 255);
		container.add(non_Mip_y);
		// Add labels (y slider as example)
		non_Mip_y.setMajorTickSpacing(50);
		non_Mip_y.setMinorTickSpacing(10);
		non_Mip_y.setPaintTicks(true);
		non_Mip_y.setPaintLabels(true);
		// see
		// https://docs.oracle.com/javase/7/docs/api/javax/swing/JSlider.html
		// for documentation (e.g. how to get the value, how to display
		// vertically if you want)

		// Now all the handlers class
		GUIEventHandler handler = new GUIEventHandler();

		// associate appropriate handlers
		mip_button.addActionListener(handler);
		histogramEqualization.addActionListener(handler);
		resize.addActionListener(handler);
		thumbNailViewFront.addActionListener(handler);
		thumbNailViewSide.addActionListener(handler);
		non_mip.addActionListener(handler);
		thumbNailViewTop.addActionListener(handler);
		non_Mip_z.addChangeListener(handler);
		non_Mip_x.addChangeListener(handler);
		non_Mip_y.addChangeListener(handler);

		// ... and display everything
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		// NearestNeighbour(image1, resizeInput);
		HistogramEqualization();
	}

	/*
	 * This is the event handler for the application
	 */
	private class GUIEventHandler implements ActionListener, ChangeListener {

		// Change handler (e.g. for sliders)
		public void stateChanged(ChangeEvent e) {

			if (e.getSource() == non_Mip_z) {
				image1 = NonMipTop(image1); // (although mine is called MIP, it
											// doesn't do MIP)

				// Update image
				image_icon1.setIcon(new ImageIcon(image1));

			}
			if (e.getSource() == non_Mip_x) {
				image2 = NonMipSide(image2);

				image_icon2.setIcon(new ImageIcon(image2));
			}
			if (e.getSource() == non_Mip_y) {
				image3 = NonMipFront(image3);

				// Update image
				image_icon3.setIcon(new ImageIcon(image3));
			}

		}

		// action handlers (e.g. for buttons)
		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == resize) {
				image1 = NearestNeighbour(image1);

				image_icon1.setIcon(new ImageIcon(image1));

			}

			if (event.getSource() == thumbNailViewFront) {
				ThumbNailFront();
			}
			if (event.getSource() == thumbNailViewTop) {
				ThumbNailTop();
			}
			if (event.getSource() == thumbNailViewSide) {
				ThumbNailSide();
			}
			if (event.getSource() == histogramEqualization) {
				image1 = HistogramEqualizationTop(image1);
				image2 = HistogramEqualizationSide(image2);
				image3 = HistogramEqualizationFront(image3);

				image_icon1.setIcon(new ImageIcon(image1));
				image_icon2.setIcon(new ImageIcon(image2));
				image_icon3.setIcon(new ImageIcon(image3));
			}
			if (event.getSource() == mip_button) {
				// e.g. do something to change the image here
				// e.g. call MIP function
				image1 = MIP(image1); // (although mine is called MIP, it
										// doesn't do MIP)
				image2 = Side(image2);
				image3 = Front(image3);

				// Update image
				image_icon1.setIcon(new ImageIcon(image1));
				image_icon2.setIcon(new ImageIcon(image2));
				image_icon3.setIcon(new ImageIcon(image3));
			}
			if (event.getSource() == non_mip) {
				image1 = NonMipTop(image1); // (although mine is called MIP, it
											// doesn't do MIP)
				image2 = NonMipSide(image2);
				image3 = NonMipFront(image3);

				// Update image
				image_icon1.setIcon(new ImageIcon(image1));
				image_icon2.setIcon(new ImageIcon(image2));
				image_icon3.setIcon(new ImageIcon(image3));
			}

		}
	}

	/*
	 * This function will return a pointer to an array of bytes which represent
	 * the image data in memory. Using such a pointer allows fast access to the
	 * image data for processing (rather than getting/setting individual pixels)
	 */
	public static byte[] GetImageData(BufferedImage image) {
		WritableRaster WR = image.getRaster();
		DataBuffer DB = WR.getDataBuffer();
		if (DB.getDataType() != DataBuffer.TYPE_BYTE)
			throw new IllegalStateException("That's not of type byte");

		return ((DataBufferByte) DB).getData();
	}

	/*
	 * This function shows how to carry out an operation on an image. It obtains
	 * the dimensions of the image, and then loops through the image carrying
	 * out the copying of a slice of data into the image.
	 */
	public BufferedImage MIP(BufferedImage image) {
		int w = image.getWidth(), h = image.getHeight(), i, j, c, k;
		byte[] data = GetImageData(image);
		float col;
		short datum;
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				datum = cthead[non_Mip_z.getValue()][j][i];
				// maximum voxel
				int maximum = -1117;
				for (k = 0; k < 113; k++) {
					maximum = Math.max(cthead[k][j][i], maximum);
				}
				col = (255.0f * ((float) maximum - (float) min) / ((float) (max - min)));

				for (c = 0; c < 3; c++) {
					data[c + 3 * i + 3 * j * w] = (byte) col;
				} // colour loop
			} // column loop
		} // row loop
		return image;
	}

	public BufferedImage NearestNeighbour(BufferedImage image) {
		int resizeInput = 100;

		int j, i, c, ya = image.getHeight(), xa = image.getWidth(), xb = resizeInput, yb = resizeInput;
		int x, y;

		BufferedImage imagenew = new BufferedImage(resizeInput, resizeInput,
				BufferedImage.TYPE_3BYTE_BGR);
		byte[] data1 = GetImageData(imagenew);

		for (j = 0; j < yb; j++) {
			for (i = 0; i < xb; i++) {
				for (c = 0; c < 3; c++) {

					y = j * ya / yb;
					x = i * xa / xb;
					short datum = cthead[non_Mip_z.getValue()][y][x];

					float col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));
					// sets colour of each pixel (i,j)
					data1[c + 3 * i + 3 * j * resizeInput] = (byte) col;
				}
			}
		}
		return imagenew;
	}

	public void HistogramEqualization() {
		int min = -1117;
		int max = 2248;
		short datum;
		int index = 0;

		int size = max - min + 1; // 3366
		histogram = new float[size];
		mapping = new float[size];

		// number of elements in data that has values to t[i]
		float t[] = new float[size];

		// total num of elements data set
		int RenameL8r = 256 * 256 * 113;
		// Counts every element of a set intensity
		for (int k = 0; k < 113; k++) {
			for (int i = 0; i < 256; i++) {
				for (int j = 0; j < 256; j++) {
					// pointer to data
					datum = cthead[non_Mip_z.getValue()][i][j];
					// Can't access negative index values
					index = cthead[k][i][j] - min;
					histogram[index]++;
				}
			}
		}
		//t[i] is the number of elements in histogram have i intensity
		t[0] = histogram[0];
		for (int n = 1; n < size; n++) {
			t[n] = t[n - 1] + histogram[n];
			// map element of data set to a new range
			mapping[n] = 255.0f * (t[n] / RenameL8r);
		}
	}

	public BufferedImage HistogramEqualizationTop(BufferedImage image) {
		int w = image.getWidth(), h = image.getHeight(), i, j, c;
		byte[] data = GetImageData(image);
		float col;
		short datum;
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				datum = cthead[non_Mip_z.getValue()][j][i];
				col = mapping[datum - min];

				for (c = 0; c < 3; c++) {
					// sets colour of each pixel (i,j)
					data[c + 3 * i + 3 * j * w] = (byte) col;
				}
			}
		}
		return image;
	}

	public BufferedImage HistogramEqualizationSide(BufferedImage image) {
		int w = image.getWidth(), h = image.getHeight(), i, j, c;
		byte[] data = GetImageData(image);
		float col;
		short datum;
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				datum = cthead[j][i][non_Mip_x.getValue()];
				col = mapping[datum - min];

				for (c = 0; c < 3; c++) {
					data[c + 3 * i + 3 * j * w] = (byte) col;
				}
			}
		}
		return image;
	}

	public BufferedImage HistogramEqualizationFront(BufferedImage image) {
		int w = image.getWidth(), h = image.getHeight(), i, j, c;
		byte[] data = GetImageData(image);
		float col;
		short datum;
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				datum = cthead[j][non_Mip_y.getValue()][i];
				col = mapping[datum - min];

				for (c = 0; c < 3; c++) {
					data[c + 3 * i + 3 * j * w] = (byte) col;
				}
			}
		}
		return image;
	}

	public void ThumbNailTop() {
		JFrame window = new JFrame();
		window.setLayout(new BoxLayout(window.getContentPane(),
				BoxLayout.X_AXIS));
		JPanel panel = new JPanel();
		JScrollPane scrPane = new JScrollPane();
		window.add(scrPane);
		panel.add(scrPane);

		short datum = 0;
		float col;
		int i, j, k, c;

		for (i = 0; i < 113; i++) {
			image1 = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
			byte[] data = GetImageData(image1);

			for (j = 0; j < 256; j++) {
				for (k = 0; k < 256; k++) {
					datum = cthead[i][j][k];

					col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));

					for (c = 0; c < 3; c++) {
						data[c + 3 * k + 3 * j * 256] = (byte) col;
					}
				}

			}
			thumbnailtop.add(new JLabel(new ImageIcon(image1)));
			panel.add(thumbnailtop.get(i));

		}

		window.setSize(800, 800);
		window.add(panel);
		window.setVisible(true);
	}

	public void ThumbNailSide() {
		JFrame window = new JFrame();
		JPanel panel = new JPanel();

		short datum = 0;
		float col;
		int i, j, k, c;
		int w = image2.getWidth();
		int h = image2.getHeight();

		for (i = 0; i < 256; i++) {
			image2 = new BufferedImage(256, 112, BufferedImage.TYPE_3BYTE_BGR);
			byte[] data = GetImageData(image2);

			for (j = 0; j < h; j++) {
				for (k = 0; k < w; k++) {
					datum = cthead[j][i][i];

					col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));

					for (c = 0; c < 3; c++) {
						data[c + 3 * i + 3 * j * w] = (byte) col;
					}
				}

			}
			thumbnailside.add(new JLabel(new ImageIcon(image2)));
			panel.add(thumbnailside.get(i));

		}

		window.setSize(800, 800);
		window.add(panel);
		window.setVisible(true);
	}

	public void ThumbNailFront() {
		JFrame window = new JFrame();
		JPanel panel = new JPanel();

		short datum = 0;
		float col;
		int i, j, k, c;
		image3 = new BufferedImage(256, 112, BufferedImage.TYPE_3BYTE_BGR);
		int h = image3.getHeight();
		int w = image3.getWidth();

		for (i = 0; i < 256; i++) {

			byte[] data = GetImageData(image3);

			for (j = 0; j < h; j++) {
				for (k = 0; k < w; k++) {
					datum = cthead[j][k][i];

					col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));

					for (c = 0; c < 3; c++) {
						data[c + 3 * k + 3 * j * 256] = (byte) col;
					}
				}

			}
			thumbnailfront.add(new JLabel(new ImageIcon(image3)));
			panel.add(thumbnailfront.get(i));

		}

		window.setSize(800, 800);
		window.add(panel);
		window.setVisible(true);
	}

	public BufferedImage Side(BufferedImage image) {
		// Get image dimensions, and declare loop variables
		int w = image.getWidth(), h = image.getHeight(), i, j, c, k;
		// Obtain pointer to data for fast processing
		byte[] data = GetImageData(image);
		float col;
		short datum;
		// Shows how to loop through each pixel and colour
		// Try to always use j for loops in y, and i for loops in x
		// as this makes the code more readable
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				// at this point (i,j) is a single pixel in the image
				// here you would need to do something to (i,j) if the image
				// size
				// does not match the slice size (e.g. during an image resizing
				// operation
				// If you don't do this, your j,i could be outside the array
				// bounds
				// In the framework, the image is 256x256 and the data set
				// slices are 256x256
				// so I don't do anything - this also leaves you something to do
				// for the assignment
				datum = cthead[j][i][non_Mip_x.getValue()]; // get values
															// from slice 76
															// (change this
															// in your
															// assignment)
				int maximum = -1117;
				for (k = 0; k < 256; k++) {
					maximum = Math.max(cthead[j][i][k], maximum);

				}
				// calculate the colour by performing a mapping from [min,max]
				// -> [0,255]
				col = (255.0f * ((float) maximum - (float) min) / ((float) (max - min)));

				for (c = 0; c < 3; c++) {
					// and now we are looping through the bgr components of the
					// pixel
					// set the colour component c of pixel (i,j)
					// * 3 three colour values * w to hold different position in
					// memeory
					data[c + 3 * i + 3 * j * w] = (byte) col;
				} // colour loop
			} // column loop
		} // row loop

		return image;
	}

	public BufferedImage Front(BufferedImage image) {
		// Get image dimensions, and declare loop variables
		int w = image.getWidth(), h = image.getHeight(), i, j, c, k;
		// Obtain pointer to data for fast processing
		byte[] data = GetImageData(image);
		float col;
		short datum;
		// Shows how to loop through each pixel and colour
		// Try to always use j for loops in y, and i for loops in x
		// as this makes the code more readable
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				// at this point (i,j) is a single pixel in the image
				// here you would need to do something to (i,j) if the image
				// size
				// does not match the slice size (e.g. during an image resizing
				// operation
				// If you don't do this, your j,i could be outside the array
				// bounds
				// In the framework, the image is 256x256 and the data set
				// slices are 256x256
				// so I don't do anything - this also leaves you something to do
				// for the assignment
				datum = cthead[j][non_Mip_y.getValue()][i]; // get values
															// from slice 76
															// (change this
															// in your
															// assignment)
				int maximum = -1117;
				for (k = 0; k < 256; k++) {
					maximum = Math.max(cthead[j][k][i], maximum);

				}
				// calculate the colour by performing a mapping from [min,max]
				// -> [0,255]
				col = (255.0f * ((float) maximum - (float) min) / ((float) (max - min)));
				for (c = 0; c < 3; c++) {
					// and now we are looping through the bgr components of the
					// pixel
					// set the colour component c of pixel (i,j)
					data[c + 3 * i + 3 * j * w] = (byte) col;
				} // colour loop
			} // column loop
		} // row loop

		return image;
	}

	public BufferedImage NonMipTop(BufferedImage image) {
		// Get image dimensions, and declare loop variables
		int w = image.getWidth(), h = image.getHeight(), i, j, c, k;
		// Obtain pointer to data for fast processing
		byte[] data = GetImageData(image);
		float col;
		short datum;
		// Shows how to loop through each pixel and colour
		// Try to always use j for loops in y, and i for loops in x
		// as this makes the code more readable

		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				// at this point (i,j) is a single pixel in the image
				// here you would need to do something to (i,j) if the image
				// size
				// does not match the slice size (e.g. during an image resizing
				// operation
				// If you don't do this, your j,i could be outside the array
				// bounds
				// In the framework, the image is 256x256 and the data set
				// slices are 256x256
				// so I don't do anything - this also leaves you something to do
				// for the assignment

				datum = cthead[non_Mip_z.getValue()][j][i]; // get values from
															// slice 76 (change
															// this in your
															// assignment)

				// calculate the colour by performing a mapping from [min,max]
				// -> [0,255]
				col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));

				for (c = 0; c < 3; c++) {
					// and now we are looping through the bgr components of the
					// pixel
					// set the colour component c of pixel (i,j)
					data[c + 3 * i + 3 * j * w] = (byte) col;
				} // colour loop
			} // column loop
		} // row loop

		return image;
	}

	public BufferedImage NonMipSide(BufferedImage image) {
		// Get image dimensions, and declare loop variables
		int w = image.getWidth(), h = image.getHeight(), i, j, c, k;
		// Obtain pointer to data for fast processing
		byte[] data = GetImageData(image);
		float col;
		short datum;
		// Shows how to loop through each pixel and colour
		// Try to always use j for loops in y, and i for loops in x
		// as this makes the code more readable
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				// at this point (i,j) is a single pixel in the image
				// here you would need to do something to (i,j) if the image
				// size
				// does not match the slice size (e.g. during an image resizing
				// operation
				// If you don't do this, your j,i could be outside the array
				// bounds
				// In the framework, the image is 256x256 and the data set
				// slices are 256x256
				// so I don't do anything - this also leaves you something to do
				// for the assignment
				datum = cthead[j][i][non_Mip_x.getValue()]; // get values from
															// slice 76 (change
															// this in your
															// assignment)

				// calculate the colour by performing a mapping from [min,max]
				// -> [0,255]
				col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));
				for (c = 0; c < 3; c++) {
					// and now we are looping through the bgr components of the
					// pixel
					// set the colour component c of pixel (i,j)
					data[c + 3 * i + 3 * j * w] = (byte) col;
				} // colour loop
			} // column loop
		} // row loop

		return image;
	}

	public BufferedImage NonMipFront(BufferedImage image) {
		// Get image dimensions, and declare loop variables
		int w = image.getWidth(), h = image.getHeight(), i, j, c, k;
		// Obtain pointer to data for fast processing
		byte[] data = GetImageData(image);
		float col;
		short datum;
		// Shows how to loop through each pixel and colour
		// Try to always use j for loops in y, and i for loops in x
		// as this makes the code more readable
		for (j = 0; j < h; j++) {
			for (i = 0; i < w; i++) {
				// at this point (i,j) is a single pixel in the image
				// here you would need to do something to (i,j) if the image
				// size
				// does not match the slice size (e.g. during an image resizing
				// operation
				// If you don't do this, your j,i could be outside the array
				// bounds
				// In the framework, the image is 256x256 and the data set
				// slices are 256x256
				// so I don't do anything - this also leaves you something to do
				// for the assignment
				datum = cthead[j][non_Mip_y.getValue()][i]; // get values from
															// slice 76 (change
															// this in your
															// assignment)
				// calculate the colour by performing a mapping from [min,max]
				// -> [0,255]
				col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));
				for (c = 0; c < 3; c++) {
					// and now we are looping through the bgr components of the
					// pixel
					// set the colour component c of pixel (i,j)
					data[c + 3 * i + 3 * j * w] = (byte) col;
				} // colour loop
			} // column loop
		} // row loop

		return image;
	}

	public static void main(String[] args) throws IOException {

		Example e = new Example();
		e.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		e.Example();
	}
}