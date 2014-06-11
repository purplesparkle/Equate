package com.llamacorp.unitcalc;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Unit {
	private static final String JSON_NAME = "name";
	private static final String JSON_VALUE = "value";

	private String mDispName;
	private double mValue;

	//intercept's only known need is temp conversions
	public Unit(String name, double value){
		mDispName = name;
		mValue = value;
	}	

	public Unit(){
		this("", 0);
	}

	public Unit(JSONObject json) throws JSONException {
		this(json.getString(JSON_NAME), 
				json.getDouble(JSON_VALUE)); 
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();

		json.put(JSON_NAME, toString());
		json.put(JSON_VALUE, getValue());
		return json;
	}

	public double getValue() {
		return mValue;
	}

	public String toString(){
		return mDispName;
	}


	public abstract String convertFrom(Unit fromUnit, String toConv);

	@Override
	public boolean equals(Object other){
		if (other == null) return false;
		if (other == this) return true;
		if (!(other instanceof UnitScalar))return false;
		UnitScalar otherUnit = (UnitScalar)other;
		return (otherUnit.getValue() == this.getValue() &&
				otherUnit.toString().equals(this.toString()));
	}
}
