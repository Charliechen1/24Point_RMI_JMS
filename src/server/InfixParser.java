package server;

import java.util.*;

public class InfixParser {
	Stack<Character> oprStack;
	Stack<Double> valStack;
	
	protected InfixParser(){
		oprStack = new Stack<Character>();
		valStack = new Stack<Double>();
	}
	
	public static void main(String[] args){
		InfixParser ip = new InfixParser();
		ip.evaluate("Q + K - 1");
	}
	
	protected double evaluate(String input){
		try{
		double res = -1;
		for(int i=0; i<input.length();i++){
			String s="";
			char c=input.charAt(i);
			switch(c){
				case '1':
					s += c;
					if (i < input.length()-1 && input.charAt(i+1)=='0'){
						s += input.charAt(i+1);
						i++;
					}
					valStack.push(valueOf(s));
					break;
				case 'A': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': case 'J': case 'Q': case 'K':
					s += c;
					valStack.push(valueOf(s));
					break;
				case '(':
					oprStack.push(c);
					break;
				case '+': case '-': case '*': case '/': case '^': 
					if (oprStack.isEmpty()){
						oprStack.push(c);
					} else if (precedence(c) > precedence(oprStack.peek())){
						oprStack.push(c);
					} else {
						while(!oprStack.isEmpty() && precedence(c) <= precedence(oprStack.peek())){
							double result = compute(valStack.pop(), valStack.pop(), oprStack.pop());
							valStack.push(result);
						}
						oprStack.push(c);
					}
					break;
				case ')':
					while(oprStack.peek() != '('){
                        double result = compute(valStack.pop(), valStack.pop(), oprStack.pop());
                        valStack.push(result);
                    }
                    oprStack.pop();
					break;
			}
		}
		while(!oprStack.isEmpty()){
			double result = compute(valStack.pop(), valStack.pop(), oprStack.pop());
            valStack.push(result);
        }
		res = valStack.peek();
<<<<<<< HEAD
        //System.out.println("result of the infex expression: " + res);
=======
        System.out.println("result of the infex expression: " + res);
>>>>>>> origin/master
        return res;
		} catch (Exception e){
			return -99999;
		}
	}
	
	protected static double valueOf(String s){
		switch (s){
		case "A": return 1.0;
		case "2": return 2.0;
		case "3": return 3.0;
		case "4": return 4.0;
		case "5": return 5.0;
		case "6": return 6.0;
		case "7": return 7.0;
		case "8": return 8.0;
		case "9": return 9.0;
		case "10": return 10.0;
		case "J": return 11.0;
		case "Q": return 12.0;
		case "K": return 13.0;
		}
		return Double.POSITIVE_INFINITY;
	}
	
	private static int precedence(char op)
    {
        switch(op){
            case '(': case ')': return 0;
            case '+': case '-': return 1;
            case '*': case '/': return 2;
            case '^': return 3;
        }
        return -1;
    }
	
	private static Double compute(Double ope1, Double ope2, char op)
    {
        if(op == '+'){
            return ope2 + ope1;
        } else if(op == '-'){
            return ope2 - ope1;
        } else if(op == '*'){
            return ope2 * ope1;
        } else if(op == '/'){
            return ope2 / ope1;        
        } else if(op == '^'){
            return Math.pow(ope2, ope1);
        } else {
            return null;
        }
    }
}
