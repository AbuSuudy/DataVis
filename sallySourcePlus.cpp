#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <fstream>
#include <iostream>

using namespace std;

  int m_N = 256;
  float dataArray [256][256][256][3];
  float m_size = 255.0f;


void gen_sally( int xs, int ys, int zs, int time, float *sally )
/*
 *  Gen_Sally creates a vector field of dimension [xs,ys,zs,3] from
 *  a proceedural function. By passing in different time arguements,
 *  a slightly different and rotating field is created.
 *
 *  The magnitude of the vector field is highest at some funnel shape
 *  and values range from 0.0 to around 0.4 (I think).
 *
 *  I just wrote these comments, 8 years after I wrote the function.
 *  
 *  Developed by Sally of Sally University
 *
 */
{
  int counter = 0;
  float x, y, z;
  int ix, iy, iz;
  float r, xc, yc, scale, temp, z0;
  float r2 = 8;
  float SMALL = 0.00000000001;
  float xdelta = 1.0 / (xs-1.0);
  float ydelta = 1.0 / (ys-1.0);
  float zdelta = 1.0 / (zs-1.0);



  for( iz = 0; iz < zs; iz++ )
  {
     z = iz * zdelta;                        // map z to 0->1
     xc = 0.5 + 0.1*sin(0.04*time+10.0*z);   // For each z-slice, determine the spiral circle.
     yc = 0.5 + 0.1*cos(0.03*time+3.0*z);    //    (xc,yc) determine the center of the circle.
     r = 0.1 + 0.4 * z*z + 0.1 * z * sin(8.0*z); //  The radius also changes at each z-slice.
     r2 = 0.2 + 0.1*z;                           //    r is the center radius, r2 is for damping
     for( iy = 0; iy < ys; iy++ )
     {
		y = iy * ydelta;
		for( ix = 0; ix < xs; ix++ )
		{
			x = ix * xdelta;
			temp = sqrt( (y-yc)*(y-yc) + (x-xc)*(x-xc) );
			scale = fabs( r - temp );
/*
 *  I do not like this next line. It produces a discontinuity 
 *  in the magnitude. Fix it later.
 *
 */
           if ( scale > r2 )
              scale = 0.8 - scale;
           else
              scale = 1.0;
			z0 = 0.1 * (0.1 - temp*z );
		   if ( z0 < 0.0 )  z0 = 0.0;
		   temp = sqrt( temp*temp + z0*z0 );
			scale = (r + r2 - temp) * scale / (temp + SMALL);
			scale = scale / (1+z);
       

          *sally++ = scale * (y-yc) + 0.1*(x-xc);


          *sally++ = scale * -(x-xc) + 0.1*(y-yc);


          *sally++ = scale * z0;
		}
     }
  }
}

int main(void){
 std::ofstream output;
 output.open("sallyData.raw", std::ios::out|std::ios::binary);

 gen_sally( m_N,m_N,m_N,7,(float*)dataArray);
 	if( output.is_open()){
	  for(int m_x = 0; m_x<m_N; m_x++){
	  
	  	for(int m_y = 0; m_y<m_N; m_y++){

	  		for(int m_z = 0; m_z<m_N; m_z++){

	  			int m_powV = 2;
	  			int m_xPlane = 0;
	  			int m_yPlane = 1;
	  			int m_zPlane = 2;

	  			float m_xF = dataArray[m_x][m_y][m_z][m_xPlane];
	  			float m_yF = dataArray[m_x][m_y][m_z][m_yPlane];
	  			float m_zF = dataArray[m_x][m_y][m_z][m_zPlane];

	  			unsigned char m_value = (unsigned char) (sqrt(pow(m_xF,m_powV) + 
	  				pow(m_yF,m_powV) + pow(m_zF,m_powV))*m_size);

	  			output.write((char*)&m_value,1);
	  		}
	  	}	
	  }
	}
	output.close();
	return 0;
}