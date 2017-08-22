package com.llamacorp.equate.test;


import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import com.llamacorp.equate.R;
import com.llamacorp.equate.ResourceArrayParser;
import com.llamacorp.equate.test.IdlingResource.ViewPagerIdlingResource;
import com.llamacorp.equate.view.CalcActivity;
import com.llamacorp.equate.view.IdlingResource.SimpleIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.registerIdlingResources;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.llamacorp.equate.test.EspressoTestUtils.assertExpressionEquals;
import static com.llamacorp.equate.test.EspressoTestUtils.checkUnitButtonVisible;
import static com.llamacorp.equate.test.EspressoTestUtils.checkUnitButtonVisibleWithArrow;
import static com.llamacorp.equate.test.EspressoTestUtils.clickButtons;
import static com.llamacorp.equate.test.EspressoTestUtils.clickPrevAnswer;
import static com.llamacorp.equate.test.EspressoTestUtils.clickUnit;
import static com.llamacorp.equate.test.EspressoTestUtils.getPagerIdle;
import static com.llamacorp.equate.test.EspressoTestUtils.longClickUnit;
import static com.llamacorp.equate.test.EspressoTestUtils.resetCalculator;
import static com.llamacorp.equate.test.EspressoTestUtils.selectUnitTypeDirect;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class TestEspressoUnitTypeVisibility {

	private ViewPagerIdlingResource mPagerIdle;
	private SimpleIdlingResource mSimpleIdle;

	@Rule
	public MyActivityTestRule<CalcActivity> mActivityTestRule =
			  new MyActivityTestRule<>(CalcActivity.class);

	@Before
	public void setUpTest() {
		mPagerIdle = getPagerIdle(mActivityTestRule);

		mSimpleIdle = mActivityTestRule.getActivity().getIdlingResource();
		registerIdlingResources(mPagerIdle, mSimpleIdle);
	}

	@After
	public void unregisterIntentServiceIdlingResource() {
		if (mPagerIdle != null)
			Espresso.unregisterIdlingResources(mPagerIdle);
		if (mSimpleIdle != null)
			Espresso.unregisterIdlingResources(mPagerIdle);
	}

	@Test
	public void testCheckUnitTypeNames() {
		clickButtons("C");
		// check that all unit types are visible (none removed)
		checkUnitTypesRemoved(new ArrayList<String>());

		// move to length tab
		selectUnitTypeDirect("Length");
		clickUnit("ft");
		clickUnit("in");
		// move to Volume tab
		selectUnitTypeDirect("Volume");
		clickUnit("qt");
		assertExpressionEquals("Convert 12 qt to:");

		// check arrows appeared
		checkUnitButtonVisible("qt");
		checkUnitButtonVisibleWithArrow("cup");

		// change to a non adjacent tab
		selectUnitTypeDirect("Length");
		assertExpressionEquals("12");
		// now that we switched to a new unit type, expression should be not solved
		//TODO fix this
		//assertResultPreviewEquals("12");

		//check no arrows here
		checkUnitButtonVisible("yd");
		checkUnitButtonVisible("mi");

		// move back and make sure unit is not selected
		selectUnitTypeDirect("Volume");
		// make sure arrows are gone
		checkUnitButtonVisible("pt");


		ArrayList<String> toRemoveArray = new ArrayList<>();
		toRemoveArray.add("Weight");
		toRemoveArray.add("Length");
		toRemoveArray.add("Energy");
		toRemoveArray.add("Temperature");

		// hide some unit types
		hideUnitTypes(toRemoveArray);

		//check hidden unit types are gone
		checkUnitTypesRemoved(toRemoveArray);

		// click on the previous answer with "in" to re-enable Length units
		clickButtons("CC"); //clears out 12, need two since sometimes one doesn't work
		assertExpressionEquals("");
		clickPrevAnswer();
		toRemoveArray.remove("Length");

		checkUnitTypesRemoved(toRemoveArray);


		// clear remaining unit types
		ArrayList<String> toRemoveArray2 = new ArrayList<>();
		toRemoveArray2.add("Currency");
		toRemoveArray2.add("Area");
		toRemoveArray2.add("Length");
		toRemoveArray2.add("Volume");
		toRemoveArray2.add("Speed");
		toRemoveArray2.add("Time");
		toRemoveArray2.add("Fuel Economy");
		toRemoveArray2.add("Power");
		toRemoveArray2.add("Force");
		toRemoveArray2.add("Torque");
		toRemoveArray2.add("Pressure");
		toRemoveArray2.add("Digital Storage");

		// hide all unit types
		hideUnitTypes(toRemoveArray2);

		// add together all removed elements
		toRemoveArray.addAll(toRemoveArray2);

		//check hidden unit types are gone
		checkUnitTypesRemoved(toRemoveArray);
	}

	@Test
	public void testUnitSearch() {
		resetCalculator();

		selectUnitTypeDirect("Currency");
		longClickUnit("CHF");

		// type "sato" into filter
		onView(allOf(withClassName(is("android.widget.EditText")),
				  isDisplayed())).perform(typeText("sato"));

		onView(allOf(withClassName(is("android.widget.EditText")),
				  isDisplayed())).check(matches(withText("sato")));

		// click on "Satochi"
		onView(allOf(withId(R.id.search_dialog_name_textView),
				  withText("Satoshi"))).perform(click());

		clickUnit("BTC");

		// even after we do a unit update, 1 BTC should be equal to 100,000,000 sat
		clickUnit("SAT");

		assertExpressionEquals("100,000,000 SAT");

		// now make sure when we click sat in the search, it brings up the correct unit
		clickButtons("C");
		onView(allOf(withId(R.id.convert_button10), withText("More…"), isDisplayed())).perform(click());
		// type "sato" into filter
		onView(allOf(withClassName(is("android.widget.EditText")),
				  isDisplayed())).perform(typeText("sato"));

		// click on "Satochi"
		onView(allOf(withId(R.id.search_dialog_name_textView),
				  withText("Satoshi"))).perform(click());
		clickUnit("BTC");
		assertExpressionEquals("0.00000001 BTC");


		clickButtons("C");
		longClickUnit("SAT");

		// type "sato" into filter
		onView(allOf(withClassName(is("android.widget.EditText")),
				  isDisplayed())).perform(typeText("CHF"));

		// click on "Satochi"
		onView(allOf(withId(R.id.search_dialog_name_textView),
				  withText("Swiss Francs"))).perform(click());

		clickUnit("CHF");
		clickUnit("USD");





		clickButtons("C");

		searchForUnit("mil");

		onView(allOf(withId(R.id.search_dialog_name_textView),
				  withText("Thousandths of an inch"))).perform(click());

		assertExpressionEquals("Convert 1 mil to:");
		checkUnitButtonVisibleWithArrow("in");
		checkUnitButtonVisibleWithArrow("ft");
		clickUnit("in");
		assertExpressionEquals("0.001 in");

		searchForUnit("pt");

		onView(allOf(withId(R.id.search_dialog_name_textView),
				  withText("Pints (US)"))).perform(click());
		//TODO fix this
		//assertExpressionEquals("Convert 0.0001 pt to:");

		searchForUnit("tur");
		onView(allOf(withId(R.id.search_dialog_name_textView),
				  withText("Turkish Lira"))).perform(click());

	}


	private void searchForUnit(String searchString) {
		// Open Drawer to click on navigation.
		onView(withId(R.id.drawer_layout))
				  .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.
				  .perform(open()); // Open Drawer

		onView(allOf(withId(R.id.design_menu_item_text),  withText("Find Unit"),
				  isDisplayed())).perform(click());

		onView(allOf(withClassName(is("android.widget.EditText")),
				  isDisplayed())).perform(typeText(searchString));
	}

		private void checkUnitTypesRemoved(ArrayList<String> removedUnitTypes) {
		Context targetContext = InstrumentationRegistry.getTargetContext();
		Resources resources = targetContext.getResources();

		ArrayList<String> removedTabNames = ResourceArrayParser
				  .getTabNamesFromNames(removedUnitTypes, resources);

		ArrayList<String> visibleUnitTypes = ResourceArrayParser.
				  getUnitTypeTabNameArrayList(resources);

		visibleUnitTypes.removeAll(removedTabNames);

		// check visible unit types are visible
		for (String s : visibleUnitTypes) {
			onView(allOf(withText(s), isDescendantOfA(withId(R.id.unit_container))))
					  .check(matches(withEffectiveVisibility(
								 ViewMatchers.Visibility.VISIBLE)));
		}

		if (visibleUnitTypes.size() == 0)
			onView(withId(R.id.unit_container)).check(matches(
					  withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
		else {
			// check hidden unit types are in fact gone
			for (String s : removedTabNames) {
				onView(allOf(withText(s), isDescendantOfA(withId(R.id.unit_container))))
						  .check(doesNotExist());
			}
		}
	}

	private void hideUnitTypes(ArrayList<String> unitTypesToHide) {
		// Open Drawer to click on navigation.
		onView(withId(R.id.drawer_layout))
				  .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.
				  .perform(open()); // Open Drawer

		// Click settings
		onView(allOf(withId(R.id.design_menu_item_text), withText("Settings"),
				  isDisplayed())).perform(click());

		// Open dialog to select displayed unit types
		onView(allOf(withText("Displayed Unit Types"), isDisplayed()))
				  .perform(click());

		// Uncheck some unit types
		for (String unitName : unitTypesToHide) {
			onData(hasToString(unitName)).check(matches(isChecked())).perform(click());
		}

//		// this will click the 0th element of the adapter view
//		onData(is(instanceOf(String.class)))
//				.inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")), isDisplayed()))
//				.atPosition(0).perform(click());

		onView(allOf(withText("OK"), isDisplayed())).perform(click());

		// Leave settings activity, go back to calculator
		onView(allOf(withContentDescription("Navigate up"),
				  withParent(allOf(withId(R.id.action_bar),
							 withParent(withId(R.id.action_bar_container)))),
				  isDisplayed())).perform(click());

	}
}
