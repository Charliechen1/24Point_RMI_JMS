/**********************************************************************


												Author: Chen Jiali 
												UID: 3035085695


***********************************************************************/
package server;

import java.util.*;

public class QuestionBase {
	ArrayList<String[]> questions; // questions collection
	ArrayList<String> possList; // all possibility for a permutation
	final int COUN_LIMIT = 10000; // computation limit for JVM heap memory consideration
	String[] allPoint = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"}; 
	double error = 1E-6; // permissed error
	char[] ops = {'+', '-', '*', '/'};
	 
	QuestionBase(){
		questions = new ArrayList<String[]>();
		generateBase();
	}
	
	/**
	 * randomly get a question from the constructed question base
	 * @param randomness randomness used to tune the current seed
	 * @return a randomly picked question
	 */
	public String[] get(int randomness){
		long seed = System.currentTimeMillis()+randomness; 
		Random random = new Random(seed);
		int rand = random.nextInt();
		int idx = Math.floorMod(rand, questions.size());
		System.out.println("[DEBUG] this random index is: " + idx);
		return questions.get(idx);
	}
	
	/**
	 * generate the question base
	 */
	private void generateBase(){
		int coun = 0; // give limit to the number of computation
		InfixParser inp = new InfixParser();
		// consider all possibilities, permutation
		for (String s1 : allPoint)
		for (String s2 : allPoint)
		for (String s3 : allPoint)
		for (String s4 : allPoint){
			boolean ansFlag = false;
			for (char op1 : ops)
			for (char op2 : ops)
			for (char op3 : ops){
				if (coun < COUN_LIMIT){
				possList = new ArrayList<String>();
				// no bracket
				possList.add(s1 + op1 + s2 + op2 + s3 + op3 + s4);
				// bracket for 2 numbers inside
				possList.add('(' + s1 + op1 + s2 + ')' + op2 + s3 + op3 + s4);
				possList.add(s1 + op1 + '(' + s2 + op2 + s3 + ')' + op3 + s4);
				possList.add(s1 + op1 + s2 + op2 + '(' + s3 + op3 + s4 + ')');
				possList.add('(' + s1 + op1 + s2 + ')' + op2 + '(' + s3 + op3 + s4 + ')');
				
				// bracket for 3 numbers inside
				possList.add('(' + s1 + op1 + s2 + op2 + s3 + ')' + op3 + s4);
				possList.add(s1 + op1 + '(' + s2 + op2 + s3 + op3 + s4 + ')');
				
				// bracket for both 3 numbers inside and 2 numbers inside
				possList.add("((" + s1 + op1 + s2 + ')' + op2 + s3 + ')' + op3 + s4);
				possList.add('(' + s1 + op1 + '(' + s2 + op2 + s3 + "))" + op3 + s4);
				
				possList.add(s1 + op1 + "((" + s2 + op2 + s3 + ')' + op3 + s4 + ')');
				possList.add(s1 + op1 + '(' + s2 + op2 + '(' + s3 + op3 + s4 + "))");
				
				for (String poss : possList){
					if (Math.abs(inp.evaluate(poss) - 24) < error){
						coun++;
						ansFlag = true;
					}
				}
				}
			}
		
			if (ansFlag){ // it's a solvable case
				coun++;
				String[] thisQues = {s1, s2, s3, s4};
				questions.add(thisQues);
			}
		}
		System.out.println("[DEBUG] generation complete");
		System.out.println("[DEBUG] question base size: " + questions.size());						
	}
}
