/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Widget from "android.annotation.Widget";
import Context from "android.content.Context";
import Configuration from "android.content.res.Configuration";
import TypedArray from "android.content.res.TypedArray";
import Parcel from "android.os.Parcel";
import Parcelable from "android.os.Parcelable";
import DateFormat from "android.text.format.DateFormat";
import DateUtils from "android.text.format.DateUtils";
import AttributeSet from "android.util.AttributeSet";
import LayoutInflater from "android.view.LayoutInflater";
import View from "android.view.View";
import ViewGroup from "android.view.ViewGroup";
import AccessibilityEvent from "android.view.accessibility.AccessibilityEvent";
import AccessibilityNodeInfo from "android.view.accessibility.AccessibilityNodeInfo";
import EditorInfo from "android.view.inputmethod.EditorInfo";
import InputMethodManager from "android.view.inputmethod.InputMethodManager";
import OnValueChangeListener from "android.widget.NumberPicker.OnValueChangeListener";
import R from "com.android.internal.R";
import DateFormatSymbols from "java.text.DateFormatSymbols";
import Calendar from "java.util.Calendar";
import Locale from "java.util.Locale";

/**
 * A view for selecting the time of day, in either 24 hour or AM/PM mode. The
 * hour, each minute digit, and AM/PM (if applicable) can be conrolled by
 * vertical spinners. The hour can be entered by keyboard input. Entering in two
 * digit hours can be accomplished by hitting two digits within a timeout of
 * about a second (e.g. '1' then '2' to select 12). The minutes can be entered
 * by entering single digits. Under AM/PM mode, the user can hit 'a', 'A", 'p'
 * or 'P' to pick. For a dialog using this view, see
 * {@link android.app.TimePickerDialog}.
 *<p>
 * See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.
 * </p>
 */
/* @Widget */
export class TimePicker extends FrameLayout {

    // class or interface 'OnTimeChangedListener' is export in module after root class

    /**
     * Used to save / restore state of time picker
     */
    private static SavedState = class SavedState extends BaseSavedState {

        private mHour: number;

        private mMinute: number;

        constructor(superState: Parcelable, hour: number, minute: number) {
            super(superState);
            mHour = hour;
            mMinute = minute;
        }

        constructor(_in: Parcel) {
            super(_in);
            mHour = _in.readInt();
            mMinute = _in.readInt();
        }

        public getHour(): number {
            return mHour;
        }

        public getMinute(): number {
            return mMinute;
        }

        /* @Override */
        public writeToParcel(dest: Parcel, flags: number): void {
            super.writeToParcel(dest, flags);
            dest.writeInt(mHour);
            dest.writeInt(mMinute);
        }

        /* @SuppressWarnings({ "unused", "hiding" }) */
        public static CREATOR: Parcelable.Creator<SavedState> = new class extends Creator<SavedState> {

            public createFromParcel(_in: Parcel): SavedState {
                return new SavedState(_in);
            }

            public newArray(size: number): SavedState[] {
                return new SavedState[size];
            }
        }();
    }

    private static DEFAULT_ENABLED_STATE: boolean = true;

    private static HOURS_IN_HALF_DAY: number = 12;

    /**
     * A no-op callback used in the constructor to avoid null checks later in
     * the code.
     */
    private static NO_OP_CHANGE_LISTENER: OnTimeChangedListener = new class extends OnTimeChangedListener {

        public onTimeChanged(view: TimePicker, hourOfDay: number, minute: number): void {
        }
    }();

    // state
    private mIs24HourView: boolean;

    private mIsAm: boolean;

    // ui components
    private mHourSpinner: NumberPicker;

    private mMinuteSpinner: NumberPicker;

    private mAmPmSpinner: NumberPicker;

    private mHourSpinnerInput: EditText;

    private mMinuteSpinnerInput: EditText;

    private mAmPmSpinnerInput: EditText;

    private mDivider: TextView;

    // Note that the legacy implementation of the TimePicker is
    // using a button for toggling between AM/PM while the new
    // version uses a NumberPicker spinner. Therefore the code
    // accommodates these two cases to be backwards compatible.
    private mAmPmButton: Button;

    private mAmPmStrings: string[];

    private mIsEnabled: boolean = DEFAULT_ENABLED_STATE;

    // callbacks
    private mOnTimeChangedListener: OnTimeChangedListener;

    private mTempCalendar: Calendar;

    private mCurrentLocale: Locale;

    private mHourWithTwoDigit: boolean;

    private mHourFormat: string;

    constructor(context: Context) {
        this(context, null);
    }

    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, R.attr.timePickerStyle);
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: number) {
        super(context, attrs, defStyle);
        // initialization based on locale
        setCurrentLocale(Locale.getDefault());
        // process style attributes
        let attributesArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.TimePicker, defStyle, 0);
        let layoutResourceId: number = attributesArray.getResourceId(R.styleable.TimePicker_internalLayout, R.layout.time_picker);
        attributesArray.recycle();
        let inflater: LayoutInflater = <LayoutInflater>context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layoutResourceId, this, true);
        // hour
        mHourSpinner = <NumberPicker>findViewById(R.id.hour);
        mHourSpinner.setOnValueChangedListener(((__this) => new class extends NumberPicker.OnValueChangeListener {

            public onValueChange(spinner: NumberPicker, oldVal: number, newVal: number): void {
                updateInputState();
                if (!is24HourView()) {
                    if ((oldVal === HOURS_IN_HALF_DAY - 1 && newVal === HOURS_IN_HALF_DAY) || (oldVal === HOURS_IN_HALF_DAY && newVal === HOURS_IN_HALF_DAY - 1)) {
                        mIsAm = !mIsAm;
                        updateAmPmControl();
                    }
                }
                onTimeChanged();
            }
        }())(this));
        mHourSpinnerInput = <EditText>mHourSpinner.findViewById(R.id.numberpicker_input);
        mHourSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        // divider (only for the new widget style)
        mDivider = <TextView>findViewById(R.id.divider);
        if (mDivider !== null) {
            setDividerText();
        }
        // minute
        mMinuteSpinner = <NumberPicker>findViewById(R.id.minute);
        mMinuteSpinner.setMinValue(0);
        mMinuteSpinner.setMaxValue(59);
        mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setFormatter(NumberPicker.getTwoDigitFormatter());
        mMinuteSpinner.setOnValueChangedListener(((__this) => new class extends NumberPicker.OnValueChangeListener {

            public onValueChange(spinner: NumberPicker, oldVal: number, newVal: number): void {
                updateInputState();
                let minValue: number = mMinuteSpinner.getMinValue();
                let maxValue: number = mMinuteSpinner.getMaxValue();
                if (oldVal === maxValue && newVal === minValue) {
                    let newHour: number = mHourSpinner.getValue() + 1;
                    if (!is24HourView() && newHour === HOURS_IN_HALF_DAY) {
                        mIsAm = !mIsAm;
                        updateAmPmControl();
                    }
                    mHourSpinner.setValue(newHour);
                } else if (oldVal === minValue && newVal === maxValue) {
                    let newHour: number = mHourSpinner.getValue() - 1;
                    if (!is24HourView() && newHour === HOURS_IN_HALF_DAY - 1) {
                        mIsAm = !mIsAm;
                        updateAmPmControl();
                    }
                    mHourSpinner.setValue(newHour);
                }
                onTimeChanged();
            }
        }())(this));
        mMinuteSpinnerInput = <EditText>mMinuteSpinner.findViewById(R.id.numberpicker_input);
        mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        /* Get the localized am/pm strings and use them in the spinner */
        mAmPmStrings = new DateFormatSymbols().getAmPmStrings();
        // am/pm
        let amPmView: View = findViewById(R.id.amPm);
        if (amPmView instanceof Button) {
            mAmPmSpinner = null;
            mAmPmSpinnerInput = null;
            mAmPmButton = <Button>amPmView;
            mAmPmButton.setOnClickListener(((__this) => new class extends OnClickListener {

                public onClick(button: View): void {
                    button.requestFocus();
                    mIsAm = !mIsAm;
                    updateAmPmControl();
                    onTimeChanged();
                }
            }())(this));
        } else {
            mAmPmButton = null;
            mAmPmSpinner = <NumberPicker>amPmView;
            mAmPmSpinner.setMinValue(0);
            mAmPmSpinner.setMaxValue(1);
            mAmPmSpinner.setDisplayedValues(mAmPmStrings);
            mAmPmSpinner.setOnValueChangedListener(((__this) => new class extends OnValueChangeListener {

                public onValueChange(picker: NumberPicker, oldVal: number, newVal: number): void {
                    updateInputState();
                    picker.requestFocus();
                    mIsAm = !mIsAm;
                    updateAmPmControl();
                    onTimeChanged();
                }
            }())(this));
            mAmPmSpinnerInput = <EditText>mAmPmSpinner.findViewById(R.id.numberpicker_input);
            mAmPmSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
        if (isAmPmAtStart()) {
            // Move the am/pm view to the beginning
            let amPmParent: ViewGroup = <ViewGroup>findViewById(R.id.timePickerLayout);
            amPmParent.removeView(amPmView);
            amPmParent.addView(amPmView, 0);
            // Swap layout margins if needed. They may be not symmetrical (Old Standard Theme for
            // example and not for Holo Theme)
            let lp: ViewGroup.MarginLayoutParams = <ViewGroup.MarginLayoutParams>amPmView.getLayoutParams();
             const startMargin: number = lp.getMarginStart();
             const endMargin: number = lp.getMarginEnd();
            if (startMargin !== endMargin) {
                lp.setMarginStart(endMargin);
                lp.setMarginEnd(startMargin);
            }
        }
        getHourFormatData();
        // update controls to initial state
        updateHourControl();
        updateMinuteControl();
        updateAmPmControl();
        setOnTimeChangedListener(NO_OP_CHANGE_LISTENER);
        // set to current time
        setCurrentHour(mTempCalendar.get(Calendar.HOUR_OF_DAY));
        setCurrentMinute(mTempCalendar.get(Calendar.MINUTE));
        if (!isEnabled()) {
            setEnabled(false);
        }
        // set the content descriptions
        setContentDescriptions();
        // If not explicitly specified this view is important for accessibility.
        if (getImportantForAccessibility() === IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private getHourFormatData(): void {
         const defaultLocale: Locale = Locale.getDefault();
         const bestDateTimePattern: string = DateFormat.getBestDateTimePattern(defaultLocale, (mIs24HourView) ? "Hm" : "hm");
         const lengthPattern: number = bestDateTimePattern.length();
        mHourWithTwoDigit = false;
        let hourFormat: string = '\0';
        // the hour format that we found.
        for (let i: number = 0; i < lengthPattern; i++) {
             const c: string = bestDateTimePattern.charAt(i);
            if (c === 'H' || c === 'h' || c === 'K' || c === 'k') {
                mHourFormat = c;
                if (i + 1 < lengthPattern && c === bestDateTimePattern.charAt(i + 1)) {
                    mHourWithTwoDigit = true;
                }
                break;
            }
        }
    }

    private isAmPmAtStart(): boolean {
         const defaultLocale: Locale = Locale.getDefault();
         const bestDateTimePattern: string = DateFormat.getBestDateTimePattern(defaultLocale, "hm");
        return bestDateTimePattern.startsWith("a");
    }

    /* @Override */
    public setEnabled(enabled: boolean): void {
        if (mIsEnabled === enabled) {
            return;
        }
        super.setEnabled(enabled);
        mMinuteSpinner.setEnabled(enabled);
        if (mDivider !== null) {
            mDivider.setEnabled(enabled);
        }
        mHourSpinner.setEnabled(enabled);
        if (mAmPmSpinner !== null) {
            mAmPmSpinner.setEnabled(enabled);
        } else {
            mAmPmButton.setEnabled(enabled);
        }
        mIsEnabled = enabled;
    }

    /* @Override */
    public isEnabled(): boolean {
        return mIsEnabled;
    }

    /* @Override */
    protected onConfigurationChanged(newConfig: Configuration): void {
        super.onConfigurationChanged(newConfig);
        setCurrentLocale(newConfig.locale);
    }

    /**
     * Sets the current locale.
     *
     * @param locale The current locale.
     */
    private setCurrentLocale(locale: Locale): void {
        if (locale.equals(mCurrentLocale)) {
            return;
        }
        mCurrentLocale = locale;
        mTempCalendar = Calendar.getInstance(locale);
    }

    /* @Override */
    protected onSaveInstanceState(): Parcelable {
        let superState: Parcelable = super.onSaveInstanceState();
        return new SavedState(superState, getCurrentHour(), getCurrentMinute());
    }

    /* @Override */
    protected onRestoreInstanceState(state: Parcelable): void {
        let ss: SavedState = <SavedState>state;
        super.onRestoreInstanceState(ss.getSuperState());
        setCurrentHour(ss.getHour());
        setCurrentMinute(ss.getMinute());
    }

    /**
     * Set the callback that indicates the time has been adjusted by the user.
     *
     * @param onTimeChangedListener the callback, should not be null.
     */
    public setOnTimeChangedListener(onTimeChangedListener: OnTimeChangedListener): void {
        mOnTimeChangedListener = onTimeChangedListener;
    }

    /**
     * @return The current hour in the range (0-23).
     */
    public getCurrentHour(): number {
        let currentHour: number = mHourSpinner.getValue();
        if (is24HourView()) {
            return currentHour;
        } else if (mIsAm) {
            return currentHour % HOURS_IN_HALF_DAY;
        } else {
            return (currentHour % HOURS_IN_HALF_DAY) + HOURS_IN_HALF_DAY;
        }
    }

    /**
     * Set the current hour.
     */
    public setCurrentHour(currentHour: number): void {
        setCurrentHour(currentHour, true);
    }

    private setCurrentHour(currentHour: number, notifyTimeChanged: boolean): void {
        // why was Integer used in the first place?
        if (currentHour === null || currentHour === getCurrentHour()) {
            return;
        }
        if (!is24HourView()) {
            // convert [0,23] ordinal to wall clock display
            if (currentHour >= HOURS_IN_HALF_DAY) {
                mIsAm = false;
                if (currentHour > HOURS_IN_HALF_DAY) {
                    currentHour = currentHour - HOURS_IN_HALF_DAY;
                }
            } else {
                mIsAm = true;
                if (currentHour === 0) {
                    currentHour = HOURS_IN_HALF_DAY;
                }
            }
            updateAmPmControl();
        }
        mHourSpinner.setValue(currentHour);
        if (notifyTimeChanged) {
            onTimeChanged();
        }
    }

    /**
     * Set whether in 24 hour or AM/PM mode.
     *
     * @param is24HourView True = 24 hour mode. False = AM/PM.
     */
    public setIs24HourView(is24HourView: java.lang.Boolean): void {
        if (mIs24HourView === is24HourView) {
            return;
        }
        // cache the current hour since spinner range changes and BEFORE changing mIs24HourView!!
        let currentHour: number = getCurrentHour();
        // Order is important here.
        mIs24HourView = is24HourView;
        getHourFormatData();
        updateHourControl();
        // set value after spinner range is updated - be aware that because mIs24HourView has
        // changed then getCurrentHour() is not equal to the currentHour we cached before so
        // explicitly ask for *not* propagating any onTimeChanged()
        setCurrentHour(currentHour, false);
        updateMinuteControl();
        updateAmPmControl();
    }

    /**
     * @return true if this is in 24 hour view else false.
     */
    public is24HourView(): boolean {
        return mIs24HourView;
    }

    /**
     * @return The current minute.
     */
    public getCurrentMinute(): number {
        return mMinuteSpinner.getValue();
    }

    /**
     * Set the current minute (0-59).
     */
    public setCurrentMinute(currentMinute: number): void {
        if (currentMinute === getCurrentMinute()) {
            return;
        }
        mMinuteSpinner.setValue(currentMinute);
        onTimeChanged();
    }

    /**
     * The time separator is defined in the Unicode CLDR and cannot be supposed to be ":".
     *
     * See http://unicode.org/cldr/trac/browser/trunk/common/main
     *
     * We pass the correct "skeleton" depending on 12 or 24 hours view and then extract the
     * separator as the character which is just after the hour marker in the returned pattern.
     */
    private setDividerText(): void {
         const defaultLocale: Locale = Locale.getDefault();
         const skeleton: string = (mIs24HourView) ? "Hm" : "hm";
         const bestDateTimePattern: string = DateFormat.getBestDateTimePattern(defaultLocale, skeleton);
         const separatorText: string;
        let hourIndex: number = bestDateTimePattern.lastIndexOf('H');
        if (hourIndex === -1) {
            hourIndex = bestDateTimePattern.lastIndexOf('h');
        }
        if (hourIndex === -1) {
            // Default case
            separatorText = ":";
        } else {
            let minuteIndex: number = bestDateTimePattern.indexOf('m', hourIndex + 1);
            if (minuteIndex === -1) {
                separatorText = Character.toString(bestDateTimePattern.charAt(hourIndex + 1));
            } else {
                separatorText = bestDateTimePattern.substring(hourIndex + 1, minuteIndex);
            }
        }
        mDivider.setText(separatorText);
    }

    /* @Override */
    public getBaseline(): number {
        return mHourSpinner.getBaseline();
    }

    /* @Override */
    public dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): boolean {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    /* @Override */
    public onPopulateAccessibilityEvent(event: AccessibilityEvent): void {
        super.onPopulateAccessibilityEvent(event);
        let flags: number = DateUtils.FORMAT_SHOW_TIME;
        if (mIs24HourView) {
            flags |= DateUtils.FORMAT_24HOUR;
        } else {
            flags |= DateUtils.FORMAT_12HOUR;
        }
        mTempCalendar.set(Calendar.HOUR_OF_DAY, getCurrentHour());
        mTempCalendar.set(Calendar.MINUTE, getCurrentMinute());
        let selectedDateUtterance: string = DateUtils.formatDateTime(mContext, mTempCalendar.getTimeInMillis(), flags);
        event.getText().add(selectedDateUtterance);
    }

    /* @Override */
    public onInitializeAccessibilityEvent(event: AccessibilityEvent): void {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TimePicker.class.getName());
    }

    /* @Override */
    public onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo): void {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TimePicker.class.getName());
    }

    private updateHourControl(): void {
        if (is24HourView()) {
            // 'k' means 1-24 hour
            if (mHourFormat === 'k') {
                mHourSpinner.setMinValue(1);
                mHourSpinner.setMaxValue(24);
            } else {
                mHourSpinner.setMinValue(0);
                mHourSpinner.setMaxValue(23);
            }
        } else {
            // 'K' means 0-11 hour
            if (mHourFormat === 'K') {
                mHourSpinner.setMinValue(0);
                mHourSpinner.setMaxValue(11);
            } else {
                mHourSpinner.setMinValue(1);
                mHourSpinner.setMaxValue(12);
            }
        }
        mHourSpinner.setFormatter(mHourWithTwoDigit ? NumberPicker.getTwoDigitFormatter() : null);
    }

    private updateMinuteControl(): void {
        if (is24HourView()) {
            mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        } else {
            mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        }
    }

    private updateAmPmControl(): void {
        if (is24HourView()) {
            if (mAmPmSpinner !== null) {
                mAmPmSpinner.setVisibility(View.GONE);
            } else {
                mAmPmButton.setVisibility(View.GONE);
            }
        } else {
            let index: number = mIsAm ? Calendar.AM : Calendar.PM;
            if (mAmPmSpinner !== null) {
                mAmPmSpinner.setValue(index);
                mAmPmSpinner.setVisibility(View.VISIBLE);
            } else {
                mAmPmButton.setText(mAmPmStrings[index]);
                mAmPmButton.setVisibility(View.VISIBLE);
            }
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    private onTimeChanged(): void {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnTimeChangedListener !== null) {
            mOnTimeChangedListener.onTimeChanged(this, getCurrentHour(), getCurrentMinute());
        }
    }

    private setContentDescriptions(): void {
        // Minute
        trySetContentDescription(mMinuteSpinner, R.id.increment, R.string.time_picker_increment_minute_button);
        trySetContentDescription(mMinuteSpinner, R.id.decrement, R.string.time_picker_decrement_minute_button);
        // Hour
        trySetContentDescription(mHourSpinner, R.id.increment, R.string.time_picker_increment_hour_button);
        trySetContentDescription(mHourSpinner, R.id.decrement, R.string.time_picker_decrement_hour_button);
        // AM/PM
        if (mAmPmSpinner !== null) {
            trySetContentDescription(mAmPmSpinner, R.id.increment, R.string.time_picker_increment_set_pm_button);
            trySetContentDescription(mAmPmSpinner, R.id.decrement, R.string.time_picker_decrement_set_am_button);
        }
    }

    private trySetContentDescription(root: View, viewId: number, contDescResId: number): void {
        let target: View = root.findViewById(viewId);
        if (target !== null) {
            target.setContentDescription(mContext.getString(contDescResId));
        }
    }

    private updateInputState(): void {
        // Make sure that if the user changes the value and the IME is active
        // for one of the inputs if this widget, the IME is closed. If the user
        // changed the value via the IME and there is a next input the IME will
        // be shown, otherwise the user chose another means of changing the
        // value and having the IME up makes no sense.
        let inputMethodManager: InputMethodManager = InputMethodManager.peekInstance();
        if (inputMethodManager !== null) {
            if (inputMethodManager.isActive(mHourSpinnerInput)) {
                mHourSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mMinuteSpinnerInput)) {
                mMinuteSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mAmPmSpinnerInput)) {
                mAmPmSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }
}
export module TimePicker {
    /**
     * The callback interface used to indicate the time has been adjusted.
     */
    export interface OnTimeChangedListener {

        /**
         * @param view The view associated with this listener.
         * @param hourOfDay The current hour.
         * @param minute The current minute.
         */
        onTimeChanged(view: TimePicker, hourOfDay: number, minute: number): void;
    }
}

