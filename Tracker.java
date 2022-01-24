import java.util.LinkedList;
import java.util.Stack;

/**
 * Steps through time stamp data and smoothed accel data 
 * to estimate number of mobile user's steps. 
 * @author maxkramer
 *
 */
public class Tracker {
	
	int holdLastIndex = 0; // used to interleave step and turn signals in MapDetail
	Stack <Integer> seperatedSteps; // used to interleave step and turn signals in MapDetail
	int turns; //number of 90+ degree turns taken by user over some period of time
	LinkedList <String> userMap; //List of actions mapping using turns and steps over some period of time
	
	LinkedList <Double> times;
	LinkedList <Double> smoothAccelZ; //we will use accel_z data because it is the most sensitive
	LinkedList <Double> gyroZ;
	
	double stdDevSmoothZ;
	double stdDevGyroZ;
	double normalGravity = 9.82; //standard acceleration due to gravity
	
	public Tracker(LinkedList <Double> times, LinkedList <Double> smoothAccelZ, LinkedList <Double> gyroZ, double stdDevSmoothZ, double stdDevGyroZ) {
		this.times = times;
		this.smoothAccelZ = smoothAccelZ;		
		this.stdDevSmoothZ = stdDevSmoothZ;
		this.gyroZ = gyroZ;
		this.stdDevGyroZ = stdDevGyroZ;
		userMap = new LinkedList<String>();
		seperatedSteps = new Stack<Integer>();
	}
	
	/**
	 * This algorithm tracks the peaks of accel_z smoothed data to count 
	 * the number of steps traveled by the mobile phone.
	 * 
	 * Smoothed accel_z is used because it is the most sensitive 
	 * of the acceleration data, considering that our data was collected 
	 * with phone face up and steps on level ground.  
	 * 
	 * Peaks are identified by iterating through the smoothed accel_z data and tracking
	 * when the data has ten unique instances above the base 9.82 m/s^2 plus 
	 * 1/2 standard deviations of accel_z in a row. 
	 */
	public int CountSteps(int startIndex, int endIndex) {		
		int steps = 0;
		double stepMarker = normalGravity + (stdDevSmoothZ * .5); //base case plus 0.5 standard deviations of sensitive acceleration
		LinkedList <Boolean> confirmStep = new LinkedList<Boolean>();
		
		//use of index allows for timestamp identification if needed
		for (int i = startIndex; i < endIndex; i++) {
			if (smoothAccelZ.get(i) > stepMarker) {
				confirmStep.add(true);
			} else {
				confirmStep.clear();
			}
			
			//step confirmed 
			//note it should just check that we got 10 in a row
			if (confirmStep.size() == 10) {
				steps++;
			}
			
		}
		
		return steps;
	}
	
	/**
	 * This algorithm calculates the simple number of turns that the mobile user made. 
	 * 
	 * Using the gyro_z data, and it's standard deviation, the algorithm identifies peaks in
	 * gyro_z above one standard deviation to count 90 degree clockwise turns. It again uses 
	 * gyro_z data, and one negative standard deviation, to count 90 degree counter clockwise turns.
	 * 
	 * The algorithm will do two passes over the data, the first count Clockwise turns, and the
	 * second counting counterclockwise turns. 
	 * 
	 * Once one of either turns is identified, the turns counter is incremented, returning turns
	 * at the end of data collection.
	 * @return
	 */
	public void CountTurns(int startIndex) {
		LinkedList <Integer> holdData = new LinkedList<Integer>(); //holds time period of a turn by csv index
		turns = 0;
		
		boolean confirmedBreak = false;
		
		//Iterate through times and gyroZ data to find peaks and valleys indicating acceleration change due to turn
		for (int i = startIndex; i < times.size(); i++) {
			
			//estimate base of peak as close to 0 as possible to calculate Integral
			if (gyroZ.get(i) < 0.05 && gyroZ.get(i) > -0.05) {
				if (confirmedBreak) {
					confirmedBreak = false;
					turns++;
					MapDetail(holdData);
					holdData.clear();			
				} else {
					holdData.clear();
				}
			}
			
			holdData.add(i);
			
			//find points where gyroZ breaks up through standard deviation -> CW turn
			if (gyroZ.get(i) > stdDevGyroZ && !confirmedBreak) {
				confirmedBreak = true;				
			}
			
			//find points where gyroZ breaks down through standard deviation -> CCW turn
			if (gyroZ.get(i) < stdDevGyroZ * -1 && !confirmedBreak) {
				confirmedBreak = true;
			}
			
		}
		
		//count last steps if any
		String lastSteps = "WALKED " + this.CountSteps(holdLastIndex, times.size()) + " STEPS";
		userMap.add(lastSteps);
		
	}
	
	/**
	 * This helper algorithm interprets turn signals from count turns. 
	 * It's primary purposes are to round validated turns to their nearest 
	 * 45 degree estimation, assign the turn direction CW or CCW, and update userMap.
	 * @param holdData - start to end time of gyro_z peak or valley 
	 */
	public void MapDetail(LinkedList <Integer> holdData) {
		String update = ""; //use update string to add mobile user turn behavior
		double degOfTurn = 0;
		double start = times.get(holdData.get(0));
		double end = times.get(holdData.get(holdData.size() - 1));
		double radsOfTurn = 0; 
		
		for (int i = holdData.get(0); i < holdData.get(holdData.size() - 1); i++) {
			
			radsOfTurn = radsOfTurn + (gyroZ.get(i) * 0.005); //box intervals for steps of 5ms
			
		}
		
		degOfTurn = Math.toDegrees(radsOfTurn);
		
		System.out.println(degOfTurn);
		
		double estTurn = 0;
		String direction = "";
		
		//build update string for CW turn
		if (degOfTurn > 36) {
			
			//round to nearest 45 degrees
			while (degOfTurn > 0) {
				degOfTurn = degOfTurn - 45;
				
				if (degOfTurn < 0) {
					if (degOfTurn >= -9) {
						estTurn = estTurn + 45;
					}
				} else {
					estTurn = estTurn + 45;
				}
				
			}
			
			update = "TURNED " + estTurn + " DEGREES CW"; 
			
		} else if (degOfTurn < -36) { //build update string for CCW		
			//round to nearest 45 degrees
			while (degOfTurn < 0) {
				degOfTurn = degOfTurn + 45;
				
				if (degOfTurn > 0) {
					if (degOfTurn <= 9) {
						estTurn = estTurn + 45;
					}
				} else {
					estTurn = estTurn + 45;
				}				
			}
			
			update = "TURNED " + estTurn + " DEGREES CCW"; 
			
		}
		
		//add steps leading up to start of turn
		String addSteps = "";
		int steps = this.CountSteps(holdLastIndex, holdData.get(0));
		
		if (update == "") seperatedSteps.add(steps);
		
		holdLastIndex = holdData.get(holdData.size() - 1);
		
		if (update != "") {
			
			while (seperatedSteps.size() != 0) {
				steps = steps + seperatedSteps.pop();
			}
			
			addSteps = "WALKED " + steps + " STEPS";
			userMap.add(addSteps);
			userMap.add(update);
		}
		
	}
	
	public void TrackUser() {		
		for (String turn: userMap) {
			System.out.println(turn);
		}		
		
	}
}
