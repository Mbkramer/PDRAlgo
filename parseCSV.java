import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Read CSV data 
 * @author maxkramer
 *
 */
public class parseCSV {
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String tenStepTest = "resources/TEN_STEP_TEST.csv";
		String walking = "resources/WALKING.csv";
		String turning = "resources/TURNING.csv";
		String walkingTurning = "resources/WALKING_AND_TURNING.csv";
		
		String line = "";
		
		//parse and analyze WALKING.csv
		try (BufferedReader rWalking = new BufferedReader(new FileReader(walking))) {
			
			LinkedList <Double> times = new LinkedList<Double>();
			LinkedList <Double> smoothZ = new LinkedList<Double>(); //most sensitive
			double stdDevAccelZ = 0;
			
			while ((line = rWalking.readLine()) != null) {
				String[] data = line.split(",");
				
				//skip first line 
				if (data[0].equals("timestamp")) continue;
				
				if (data[0] != null && data[13] != null) {
					
					times.add(Double.parseDouble(data[0]));
					smoothZ.add(Double.parseDouble(data[13]));
					stdDevAccelZ = Double.parseDouble(data[10]);
					
				} else {
					break;
				}
			}
			
			double stdDevGyroZ = 0; //wont be needed
			
			Tracker trackWALKING = new Tracker(times, smoothZ, null, stdDevAccelZ, stdDevGyroZ);
			
			System.out.println("Number of steps counted from WALKING.csv");
			System.out.println("STEPS: " + trackWALKING.CountSteps(0, times.size()));
			
		}
		
		line = "";
		
		//parse and analyze TURNING.csv
		try (BufferedReader rTurning = new BufferedReader(new FileReader(turning))) {
			
			LinkedList <Double> times = new LinkedList<Double>();
			LinkedList <Double>  gyroZ = new LinkedList<Double>();
			LinkedList <Double>  smoothMagX = new LinkedList<Double>();
			LinkedList <Double>  smoothMagY = new LinkedList<Double>();
			double stdDevGyroZ = 0;
			
			while ((line = rTurning.readLine()) != null) {
				String[] data = line.split(",");
				
				
				if (data[0].equals("timestamp")) {
					stdDevGyroZ = Double.parseDouble(data[16]);
					continue;
				}
				
				if (data[0] != null && data[6] != null && data[11].equals("BLANK") && data[12].equals("BLANK")) {
					
					times.add(Double.parseDouble(data[0]));
					gyroZ.add(Double.parseDouble(data[6]));
					
				} else if (data[0] != null && data[6] != null && !data[11].equals("BLANK") && !data[12].equals("BLANK")){

					times.add(Double.parseDouble(data[0]));
					gyroZ.add(Double.parseDouble(data[6]));
					smoothMagX.add(Double.parseDouble(data[11]));
					smoothMagY.add(Double.parseDouble(data[12]));
					
				} else {
					break;
				}
			}
			
			double stdDevAccelZ = 0; // wont be needed
			
			Tracker trackTurning = new Tracker(smoothMagY, smoothMagY, gyroZ, stdDevAccelZ, stdDevGyroZ); 
			trackTurning.CountTurns(0);
			System.out.println("Number of turns counted from TURNING.csv");
			System.out.println("TURNS: " + trackTurning.turns);
			
		}
		
		line = "";
		
		//parse and analyze TURNING_AND_WALKING.csv
		try (BufferedReader rWalkingAndTurning = new BufferedReader(new FileReader(walkingTurning))) {
			
			LinkedList <Double> times = new LinkedList<Double>();
			LinkedList <Double> smoothZ = new LinkedList<Double>();
			LinkedList <Double> smoothGyroZ = new LinkedList<Double>();
	
			double stdDevSmoothZ = 0;
			double stdDevGyroZ = 0;
			
			while ((line = rWalkingAndTurning.readLine()) != null) {
				String[] data = line.split(",");
				
				if (data[0].equals("timestamp")) {
					stdDevSmoothZ = Double.parseDouble(data[11]);
					continue;
					
				} else if (data[0] != null) {
					
					stdDevGyroZ = Double.parseDouble(data[16]);
					times.add(Double.parseDouble(data[0]));
					smoothZ.add(Double.parseDouble(data[10]));
					smoothGyroZ.add(Double.parseDouble(data[17]));
					
				} else {
					break;
				}										
			}
			
			Tracker trackWT = new Tracker(times, smoothZ, smoothGyroZ, stdDevSmoothZ, stdDevGyroZ);
			trackWT.CountTurns(0);
			
			System.out.println("Total number of steps counted from WALKING_AND_TURNING.csv");
			System.out.println("STEPS: " + trackWT.CountSteps(0, times.size()));
			
			System.out.println("Mobile user path from WALKING_AND_TURNING.csv");
			trackWT.TrackUser();
			
		}
		
		line = "";
		
		try (BufferedReader rTenStep = new BufferedReader(new FileReader(tenStepTest))) {
			
			LinkedList <Double> nonSmoothZ = new LinkedList<Double>();
			LinkedList <Double> times = new LinkedList<Double>();
			
			double stdDev = 0;
			
			while ((line = rTenStep.readLine()) != null) {
				String[] data = line.split(",");
				
				if (data[0].equals("Time (s)")) continue;
				
				times.add(Double.parseDouble(data[0]));
			    nonSmoothZ.add(Double.parseDouble(data[3]) + 9.82);
			    stdDev = Double.parseDouble(data[5]);
				
  			}
			
			Tracker tenStep = new Tracker(times, nonSmoothZ, nonSmoothZ, stdDev, stdDev);
			System.out.println("NUMBER OF STEPS COUNTED FROM 10 STEP: " + tenStep.CountSteps(0, times.size()-1));
		}
		
	}
}

