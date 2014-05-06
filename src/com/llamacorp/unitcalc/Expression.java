package com.llamacorp.unitcalc;

import java.math.BigDecimal;
import java.math.MathContext;

public class Expression {
	//the main expression string
	private String mExpression;
	//this string stores the more precise result after solving
	private String mPreciseResult;
	private MathContext mMcDisp;
	private int mIntDisplayPrecision;

	//highlighted text selection
	private int mSelectionStart;
	private int mSelectionEnd;

	//stores whether or not this expression was just solved
	private boolean mSolved;


	public static final String regexGroupedExponent = "(\\^)";
	public static final String regexGroupedMultDiv = "([/*])";
	public static final String regexGroupedAddSub = "([+-])";

	//note that in []'s only ^, -, and ] need escapes. - doen't need one if invalid
	public static final String regexInvalidChars = ".*[^0-9()E.+*^/-].*";
	public static final String regexOperators = "+/*^-";
	public static final String regexInvalidStartChar = "[E*^/+]";
	public static final String regexAnyValidOperator = "[" + regexOperators + "]";
	public static final String regexAnyOperatorOrE = "[E" + regexOperators + "]";
	public static final String regexGroupedNumber = "(\\-?\\d*\\.?\\d+\\.?(?:E[\\-\\+]?\\d+)?)";


	public Expression(int dispPrecision){
		clearExpression();
		mPreciseResult="";
		setSelection(0, 0);
		//skip precise unit usage if precision is set to 0
		if(dispPrecision>0){
			mIntDisplayPrecision = dispPrecision;
			mMcDisp = new MathContext(mIntDisplayPrecision);
		}
	}

	public Expression(){
		//precision of zero means any precise result converting will be skipped
		this(0);
	}

	/**
	 * This function will try to add a number or operator, or entire prevExpression to the current expression
	 * Note that there is lots of error checking to be sure user can't entire an invalid operator/number
	 * @param sKey should only be single vaild number or operator character, or longer previous expressions
	 */
	public void keyPresses(String sKey){
		//for now, if we're adding a prev expression, just add it without error checking
		if(sKey.length()>1){
			if(mSolved) replaceExpression(sKey);
			else insertAtSelection(sKey);
			mSolved=false;
			return;
		}


		//check for invalid entries
		if(sKey.matches(regexInvalidChars))
			throw new IllegalArgumentException("In addToExpression, invalid sKey..."); 

		//if we're inserting a character, don't bother with case checking
		if(getSelectionEnd() < mExpression.length()){
			insertAtSelection(sKey);
			return;
		}

		//don't start with [*/^E] when the expression string is empty or if we opened a para
		if(sKey.matches(regexInvalidStartChar) && (lastNumb().equals("") || mExpression.matches(".*\\($")))
			return;

		//if just hit equals, and we hit [.0-9(], then clear expression
		if(mSolved && sKey.matches("[.0-9(]"))
			clearExpression();

		//when adding (, if the previous character was any number or decimal, or close para, add mult
		if(sKey.equals("(") && mExpression.matches(".*[\\d).]$"))
			sKey = "*" + sKey;

		//when adding # after ), add multiply
		if(sKey.matches("[0-9]") && mExpression.matches(".*\\)$"))
			sKey = "*" + sKey;

		//add auto completion for close parentheses
		if(sKey.equals(")"))	
			if(numOpenPara() <= 0) //if more close than open, add an open
				addToExpressionStart("(");

		//if we already have a decimal in the number, don't add another
		if(sKey.equals(".") && lastNumb().matches(".*[.E].*"))
			//lastNumb returns the last num; if expression="4.3+", returns "4.3"; if last key was an operator, allow decimals
			//if(expression.matches(".*[^-+/*]$"))
			if(!mExpression.matches(".*" + regexAnyValidOperator + "$"))
				return;

		//if we already have a E in the number, don't add another; also don't add E immediately after an operator
		//if(sKey.equals("E") && (lastNumb().contains("E")))
		if(sKey.equals("E") && (lastNumb().contains("E") || mExpression.matches(".*" + regexAnyValidOperator + "$")))
			return;		

		//if we E was last pressed, only allow [1-9+-(]
		if(lastNumb().matches(".*E$"))
			if(sKey.matches("[^\\d(-]"))
				return;

		//if last digit was only a decimal, don't add any operator or E
		if(sKey.matches(regexAnyOperatorOrE) && lastNumb().equals("."))
			return;	

		//don't allow "--" or "65E--"
		if(sKey.matches("[-]") && mExpression.matches(".*E?[-]"))
			return;	

		//if we have "84*-", replace both the * and the - with the operator
		if(sKey.matches(regexAnyValidOperator) && mExpression.matches(".*" + regexAnyValidOperator + regexAnyValidOperator + "$"))
			replaceExpression(mExpression.substring(0, mExpression.length()-2) + sKey);
		//if there's already an operator, replace it with the new operator, except for -, let that stack up
		else if(sKey.matches(regexInvalidStartChar) && mExpression.matches(".*" + regexAnyValidOperator + "$"))
			replaceExpression(mExpression.substring(0, mExpression.length()-1) + sKey);
		//otherwise load the new keypress
		else
			insertAtSelection(sKey);

		//we added a num/op, reset the solved flag
		mSolved=false;
	}


	/**
	 * Rounds expression down by a MathContext mcDisp
	 * @throws NumberFormatException if Expression not formatted correctly
	 */	
	public void roundAndCleanExpression() {
		//if expression was displaying error (with invalid chars) leave
		if(mExpression.matches(regexInvalidChars) || mExpression.equals(""))
			return;

		//if there's any messed formatting, or if number is too big, throw syntax error
		BigDecimal bd;
		//try{
		//round the answer for viewer's pleasure
		bd = new BigDecimal(mExpression,mMcDisp);
		//}
		//catch (NumberFormatException e){
		//	mExpression=strSyntaxError;
		//	return;
		//}

		//save the original to precise result for potential later use
		mPreciseResult = mExpression;

		//determine if exponent (number after E) is small enough for non-engineering style print, otherwise do regular style
		if(lastNumbExponent()<mIntDisplayPrecision)
			replaceExpression(bd.toPlainString());
		else
			replaceExpression(bd.toString());

		//finally clean the result off
		replaceExpression(cleanFormatting(mExpression));
	}



	/** Close any open parentheses in this expression */
	public void closeOpenPar(){
		//if more open parentheses then close, add corresponding close para's
		int numCloseParaToAdd = numOpenPara();
		for(int i=0; i<numCloseParaToAdd; i++){
			insertAtSelection(")");
		}
	}

	/**
	 * Load in more precise result if possible
	 * @param mMcDisp is the amount to round
	 */		
	public void loadPreciseResult(){
		//make sure we have valid precise result and rounding Mathcontext first
		if(mPreciseResult.equals("") || mMcDisp == null)
			return;

		//make the precise string not precise temporarily for comparison 
		BigDecimal formallyPrec = new BigDecimal(mPreciseResult, mMcDisp);
		String formallyPrecCleaned = cleanFormatting(formallyPrec.toString());

		//find out if expression's first term matches first part of the precise result, if so replace with more precise term
		if(firstNumb().equals(formallyPrecCleaned)){
			replaceExpression(mExpression.replaceFirst(regexGroupedNumber, mPreciseResult.toString()));
		}
	}

	/** Clean off any dangling operators and E's (not parentheses!!) at the END ONLY */
	public void cleanDanglingOps(){
		replaceExpression(mExpression.replaceAll(regexAnyOperatorOrE + "+$", ""));
	}




	/** Returns if this expression is empty */
	public boolean isEmpty(){
		if(mExpression.equals("")) 
			return true;
		else 
			return false;
	}



	/** Returns if this expression is has invalid characters */
	public boolean isInvalid(){
		if(mExpression.matches(regexInvalidChars)) 
			return true;
		else 
			return false;
	}


	/** Returns the post rounded result */
	public String getPreciseResult(){
		return mPreciseResult;
	}

	public int getSelectionStart() {
		return mSelectionStart;
	}

	public int getSelectionEnd() {
		return mSelectionEnd;
	}

	public void setSelectionToEnd() {
		setSelection(mExpression.length(), mExpression.length());	
	}
	
	public void setSelection(int selectionStart, int selectionEnd ) {
		if(selectionEnd>mExpression.length() || selectionStart>mExpression.length())
			throw new IllegalArgumentException("In Expression.setSelection, selection end or start > expression length");
		//this occurs if the use drags the end selector before the start selector; need the following code so expression doesn't get confused
		if(selectionEnd < selectionStart) {
			int temp = selectionEnd;
			selectionEnd = selectionStart;
			selectionStart = temp;
		}
		
		mSelectionStart = selectionStart;
		mSelectionEnd = selectionEnd;		
	}


	public boolean isSolved() {
		return mSolved;
	}

	public void setSolved(boolean solved) {
		mSolved = solved;
	}

	/** Replaces entire expression. Selection moves to end of the expression.
	 * @param tempExp String to replace expression with*/
	public void replaceExpression(String tempExp) {
		mExpression=tempExp;
		setSelection(mExpression.length(), mExpression.length());	
	}	

	/** Clears entire expression. Note selection will move to 0,0 */
	public void clearExpression(){
		replaceExpression("");
		mSolved = false;		
	}

	public void backspaceAtSelection(){
		int selStart = getSelectionStart();
		int selEnd = getSelectionEnd();

		//return if nothing to delete or selection at beginning of expression
		if(isEmpty() || selEnd==0)
			return;
		
		//if something is highlighted, delete highlighted part (replace with "")
		if(selStart!=selEnd)
			insertAtSelection("");
		else {
			mExpression = mExpression.substring(0, selStart-1) + mExpression.substring(selStart, mExpression.length());
			setSelection(selStart-1, selStart-1);
		}
	}

	@Override
	public String toString(){
		return mExpression;
	}


	/** Adds String to beginning of expression. Note selection will NOT move relative 
	 * to the rest of the expression
	 * @param str String to add to beginning of expression */
	private void addToExpressionStart(String str) {
		int selStart = getSelectionStart();
		int selEnd = getSelectionEnd();
		int strLen = str.length();
		mExpression = str + mExpression;
		setSelection(selStart + strLen, selEnd + strLen);
	}
	
	
	/**
	 * Add a String to this expression at the correct selection point
	 * @param toAdd the String to add
	 */
	private void insertAtSelection(String toAdd){
		//just add the string if expression is empty
		if(isEmpty())
			replaceExpression(toAdd);
		//replace selection with toAdd
		else {
			//used to later tell where to place the selection (cursor)
			int selStart = getSelectionStart();
			int selEnd = getSelectionEnd();
			int expLen = mExpression.length();

			mExpression = mExpression.substring(0, selStart) + toAdd + mExpression.substring(selEnd, expLen);
			//move cursor down by newly added key.  note there is not "selection" just a cursor (since start=end)
			setSelection(selStart+toAdd.length(), selStart+toAdd.length());
		}
	}
	

	/**
	 * Clean up a string's formatting 
	 * @param sToClean is the string that will be cleaned
	 */		
	static private String cleanFormatting(String sToClean){
		//clean off any dangling .'s and .0's 
		sToClean = sToClean.replaceAll("\\.0*$", "");	

		//clean off 0's after decimal
		sToClean = sToClean.replaceAll("(\\.\\d*[1-9])0+$", "$1");	

		//remove +'s from #E+#
		sToClean = sToClean.replaceAll("E\\+", "E");	

		//remove 0's before E ei 6.1000E4 to 6.1E4; or 6.000E4 to 6.1E4; but leave 0E8 as itself
		sToClean = sToClean.replaceAll("([\\d.]+?)0+E", "$1E");

		return sToClean;
	}

	/**
	 * Counts the number of open vs number of closed parentheses in the given 
	 * @return 0 if equal num of open/close para, positive # if more open, neg # if more close
	 */
	private int numOpenPara() {
		int numOpen = 0;
		int numClose = 0;
		for(int i=0; i<mExpression.length(); i++){
			if (mExpression.charAt(i) == '(')
				numOpen++;
			if (mExpression.charAt(i) == ')')
				numClose++;
		}

		return numOpen - numClose;
	}


	/** Gets the number after the E in expression (not including + and -) */	
	private int lastNumbExponent(){
		//func returns "" if expression empty, and expression if doesn't contain E[+-]?
		if(mExpression.contains("E")){
			String [] strA = mExpression.split("E[+-]?");
			return Integer.parseInt(strA[strA.length-1]);
		}
		else 
			//need to be bigger than intDisplayPrecision so calling func uses toString instead of toPlainString
			return mIntDisplayPrecision+2;
	}


	/**
	 * Gets the first number (returned as a String) in the current expression
	 * @return anything before the first valid operator, or "" if expression empty, or entire expression if doesn't contain regexAnyValidOperator
	 */
	private String firstNumb(){
		String [] strA = mExpression.split(regexAnyValidOperator);
		return strA[0];
	}

	/**
	 * Gets the last double (returned as a String) in the current expression
	 * @return anything after last valid operator, or "" if expression empty, or entire expression if doesn't contain regexAnyValidOperator
	 */
	private String lastNumb(){
		String [] strA = mExpression.split(regexAnyValidOperator);
		if(strA.length==0) return "";
		else return strA[strA.length-1];
	}


}