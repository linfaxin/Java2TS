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
namespace android.widget {
    import Widget = android.annotation.Widget;
    import Context = android.content.Context;
    import Configuration = android.content.res.Configuration;
    import TypedArray = android.content.res.TypedArray;
    import Parcel = android.os.Parcel;
    import Parcelable = android.os.Parcelable;
    import TextUtils = android.text.TextUtils;
    import InputType = android.text.InputType;
    import DateFormat = android.text.format.DateFormat;
    import DateUtils = android.text.format.DateUtils;
    import AttributeSet = android.util.AttributeSet;
    import Log = android.util.Log;
    import SparseArray = android.util.SparseArray;
    import LayoutInflater = android.view.LayoutInflater;
    import View = android.view.View;
    import AccessibilityEvent = android.view.accessibility.AccessibilityEvent;
    import AccessibilityNodeInfo = android.view.accessibility.AccessibilityNodeInfo;
    import EditorInfo = android.view.inputmethod.EditorInfo;
    import InputMethodManager = android.view.inputmethod.InputMethodManager;
    import OnValueChangeListener = android.widget.NumberPicker.OnValueChangeListener;
    import R = com.android.internal.R;
    import DateFormatSymbols = java.text.DateFormatSymbols;
    import ParseException = java.text.ParseException;
    import SimpleDateFormat = java.text.SimpleDateFormat;
    import Arrays = java.util.Arrays;
    import Calendar = java.util.Calendar;
    import Locale = java.util.Locale;
    import TimeZone = java.util.TimeZone;
    import ICU = libcore.icu.ICU;

    /**
     * This class is a widget for selecting a date. The date can be selected by a
     * year, month, and day spinners or a {@link CalendarView}. The set of spinners
     * and the calendar view are automatically synchronized. The client can
     * customize whether only the spinners, or only the calendar view, or both to be
     * displayed. Also the minimal and maximal date from which dates to be selected
     * can be customized.
     * <p>
     * See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
     * guide.
     * </p>
     * <p>
     * For a dialog using this view, see {@link android.app.DatePickerDialog}.
     * </p>
     *
     * @attr ref android.R.styleable#DatePicker_startYear
     * @attr ref android.R.styleable#DatePicker_endYear
     * @attr ref android.R.styleable#DatePicker_maxDate
     * @attr ref android.R.styleable#DatePicker_minDate
     * @attr ref android.R.styleable#DatePicker_spinnersShown
     * @attr ref android.R.styleable#DatePicker_calendarViewShown
     */
    /* @Widget */
    export class DatePicker extends android.widget.FrameLayout {

        // class or interface 'OnDateChangedListener' is export in module after root class

        /**
         * Class for managing state storing/restoring.
         */
        private static SavedState = class SavedState extends BaseSavedState {

            private mYear: number;

            private mMonth: number;

            private mDay: number;

            /**
             * Constructor called from {@link DatePicker#onSaveInstanceState()}
             */
            constructor(superState: Parcelable, year: number, month: number, day: number) {
                super(superState);
                mYear = year;
                mMonth = month;
                mDay = day;
            }

            /**
             * Constructor called from {@link #CREATOR}
             */
            constructor(_in: Parcel) {
                super(_in);
                mYear = _in.readInt();
                mMonth = _in.readInt();
                mDay = _in.readInt();
            }

            /* @Override */
            public writeToParcel(dest: Parcel, flags: number): void {
                super.writeToParcel(dest, flags);
                dest.writeInt(mYear);
                dest.writeInt(mMonth);
                dest.writeInt(mDay);
            }

            /* @SuppressWarnings("all") */
            public static // suppress unused and hiding
            CREATOR: Parcelable.Creator<SavedState> = new class extends Creator<SavedState> {

                public createFromParcel(_in: Parcel): SavedState {
                    return new SavedState(_in);
                }

                public newArray(size: number): SavedState[] {
                    return new SavedState[size];
                }
            }();
        }

        private static LOG_TAG: string = android.widget.DatePicker.class.getSimpleName();

        private static DATE_FORMAT: string = "MM/dd/yyyy";

        private static DEFAULT_START_YEAR: number = 1900;

        private static DEFAULT_END_YEAR: number = 2100;

        private static DEFAULT_CALENDAR_VIEW_SHOWN: boolean = true;

        private static DEFAULT_SPINNERS_SHOWN: boolean = true;

        private static DEFAULT_ENABLED_STATE: boolean = true;

        private mSpinners: android.widget.LinearLayout;

        private mDaySpinner: android.widget.NumberPicker;

        private mMonthSpinner: android.widget.NumberPicker;

        private mYearSpinner: android.widget.NumberPicker;

        private mDaySpinnerInput: android.widget.EditText;

        private mMonthSpinnerInput: android.widget.EditText;

        private mYearSpinnerInput: android.widget.EditText;

        private mCalendarView: android.widget.CalendarView;

        private mCurrentLocale: Locale;

        private mOnDateChangedListener: android.widget.DatePicker.OnDateChangedListener;

        private mShortMonths: string[];

        private mDateFormat: java.text.DateFormat = new SimpleDateFormat(DATE_FORMAT);

        private mNumberOfMonths: number;

        private mTempDate: Calendar;

        private mMinDate: Calendar;

        private mMaxDate: Calendar;

        private mCurrentDate: Calendar;

        private mIsEnabled: boolean = DEFAULT_ENABLED_STATE;

        constructor(context: Context) {
            this(context, null);
        }

        constructor(context: Context, attrs: AttributeSet) {
            this(context, attrs, R.attr.datePickerStyle);
        }

        constructor(context: Context, attrs: AttributeSet, defStyle: number) {
            super(context, attrs, defStyle);
            // initialization based on locale
            setCurrentLocale(Locale.getDefault());
            let attributesArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.DatePicker, defStyle, 0);
            let spinnersShown: boolean = attributesArray.getBoolean(R.styleable.DatePicker_spinnersShown, DEFAULT_SPINNERS_SHOWN);
            let calendarViewShown: boolean = attributesArray.getBoolean(R.styleable.DatePicker_calendarViewShown, DEFAULT_CALENDAR_VIEW_SHOWN);
            let startYear: number = attributesArray.getInt(R.styleable.DatePicker_startYear, DEFAULT_START_YEAR);
            let endYear: number = attributesArray.getInt(R.styleable.DatePicker_endYear, DEFAULT_END_YEAR);
            let minDate: string = attributesArray.getString(R.styleable.DatePicker_minDate);
            let maxDate: string = attributesArray.getString(R.styleable.DatePicker_maxDate);
            let layoutResourceId: number = attributesArray.getResourceId(R.styleable.DatePicker_internalLayout, R.layout.date_picker);
            attributesArray.recycle();
            let inflater: LayoutInflater = <LayoutInflater>context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(layoutResourceId, this, true);
            let onChangeListener: OnValueChangeListener = ((__this) => new class extends OnValueChangeListener {

                public onValueChange(picker: android.widget.NumberPicker, oldVal: number, newVal: number): void {
                    updateInputState();
                    mTempDate.setTimeInMillis(mCurrentDate.getTimeInMillis());
                    // take care of wrapping of days and months to update greater fields
                    if (picker === mDaySpinner) {
                        let maxDayOfMonth: number = mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH);
                        if (oldVal === maxDayOfMonth && newVal === 1) {
                            mTempDate.add(Calendar.DAY_OF_MONTH, 1);
                        } else if (oldVal === 1 && newVal === maxDayOfMonth) {
                            mTempDate.add(Calendar.DAY_OF_MONTH, -1);
                        } else {
                            mTempDate.add(Calendar.DAY_OF_MONTH, newVal - oldVal);
                        }
                    } else if (picker === mMonthSpinner) {
                        if (oldVal === 11 && newVal === 0) {
                            mTempDate.add(Calendar.MONTH, 1);
                        } else if (oldVal === 0 && newVal === 11) {
                            mTempDate.add(Calendar.MONTH, -1);
                        } else {
                            mTempDate.add(Calendar.MONTH, newVal - oldVal);
                        }
                    } else if (picker === mYearSpinner) {
                        mTempDate.set(Calendar.YEAR, newVal);
                    } else {
                        throw new java.lang.IllegalArgumentException();
                    }
                    // now set the date to the adjusted one
                    setDate(mTempDate.get(Calendar.YEAR), mTempDate.get(Calendar.MONTH), mTempDate.get(Calendar.DAY_OF_MONTH));
                    updateSpinners();
                    updateCalendarView();
                    notifyDateChanged();
                }
            }())(this);
            mSpinners = <android.widget.LinearLayout>findViewById(R.id.pickers);
            // calendar view day-picker
            mCalendarView = <android.widget.CalendarView>findViewById(R.id.calendar_view);
            mCalendarView.setOnDateChangeListener(((__this) => new class implements android.widget.CalendarView.OnDateChangeListener {

                public onSelectedDayChange(view: android.widget.CalendarView, year: number, month: number, monthDay: number): void {
                    setDate(year, month, monthDay);
                    updateSpinners();
                    notifyDateChanged();
                }
            }())(this));
            // day
            mDaySpinner = <android.widget.NumberPicker>findViewById(R.id.day);
            mDaySpinner.setFormatter(NumberPicker.getTwoDigitFormatter());
            mDaySpinner.setOnLongPressUpdateInterval(100);
            mDaySpinner.setOnValueChangedListener(onChangeListener);
            mDaySpinnerInput = <android.widget.EditText>mDaySpinner.findViewById(R.id.numberpicker_input);
            // month
            mMonthSpinner = <android.widget.NumberPicker>findViewById(R.id.month);
            mMonthSpinner.setMinValue(0);
            mMonthSpinner.setMaxValue(mNumberOfMonths - 1);
            mMonthSpinner.setDisplayedValues(mShortMonths);
            mMonthSpinner.setOnLongPressUpdateInterval(200);
            mMonthSpinner.setOnValueChangedListener(onChangeListener);
            mMonthSpinnerInput = <android.widget.EditText>mMonthSpinner.findViewById(R.id.numberpicker_input);
            // year
            mYearSpinner = <android.widget.NumberPicker>findViewById(R.id.year);
            mYearSpinner.setOnLongPressUpdateInterval(100);
            mYearSpinner.setOnValueChangedListener(onChangeListener);
            mYearSpinnerInput = <android.widget.EditText>mYearSpinner.findViewById(R.id.numberpicker_input);
            // show something and the spinners have higher priority
            if (!spinnersShown && !calendarViewShown) {
                setSpinnersShown(true);
            } else {
                setSpinnersShown(spinnersShown);
                setCalendarViewShown(calendarViewShown);
            }
            // set the min date giving priority of the minDate over startYear
            mTempDate.clear();
            if (!TextUtils.isEmpty(minDate)) {
                if (!parseDate(minDate, mTempDate)) {
                    mTempDate.set(startYear, 0, 1);
                }
            } else {
                mTempDate.set(startYear, 0, 1);
            }
            setMinDate(mTempDate.getTimeInMillis());
            // set the max date giving priority of the maxDate over endYear
            mTempDate.clear();
            if (!TextUtils.isEmpty(maxDate)) {
                if (!parseDate(maxDate, mTempDate)) {
                    mTempDate.set(endYear, 11, 31);
                }
            } else {
                mTempDate.set(endYear, 11, 31);
            }
            setMaxDate(mTempDate.getTimeInMillis());
            // initialize to current date
            mCurrentDate.setTimeInMillis(System.currentTimeMillis());
            init(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate.get(Calendar.DAY_OF_MONTH), null);
            // re-order the number spinners to match the current date format
            reorderSpinners();
            // accessibility
            setContentDescriptions();
            // If not explicitly specified this view is important for accessibility.
            if (getImportantForAccessibility() === IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }

        /**
         * Gets the minimal date supported by this {@link DatePicker} in
         * milliseconds since January 1, 1970 00:00:00 in
         * {@link TimeZone#getDefault()} time zone.
         * <p>
         * Note: The default minimal date is 01/01/1900.
         * <p>
         *
         * @return The minimal supported date.
         */
        public getMinDate(): number {
            return mCalendarView.getMinDate();
        }

        /**
         * Sets the minimal date supported by this {@link NumberPicker} in
         * milliseconds since January 1, 1970 00:00:00 in
         * {@link TimeZone#getDefault()} time zone.
         *
         * @param minDate The minimal supported date.
         */
        public setMinDate(minDate: number): void {
            mTempDate.setTimeInMillis(minDate);
            if (mTempDate.get(Calendar.YEAR) === mMinDate.get(Calendar.YEAR) && mTempDate.get(Calendar.DAY_OF_YEAR) !== mMinDate.get(Calendar.DAY_OF_YEAR)) {
                return;
            }
            mMinDate.setTimeInMillis(minDate);
            mCalendarView.setMinDate(minDate);
            if (mCurrentDate.before(mMinDate)) {
                mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
                updateCalendarView();
            }
            updateSpinners();
        }

        /**
         * Gets the maximal date supported by this {@link DatePicker} in
         * milliseconds since January 1, 1970 00:00:00 in
         * {@link TimeZone#getDefault()} time zone.
         * <p>
         * Note: The default maximal date is 12/31/2100.
         * <p>
         *
         * @return The maximal supported date.
         */
        public getMaxDate(): number {
            return mCalendarView.getMaxDate();
        }

        /**
         * Sets the maximal date supported by this {@link DatePicker} in
         * milliseconds since January 1, 1970 00:00:00 in
         * {@link TimeZone#getDefault()} time zone.
         *
         * @param maxDate The maximal supported date.
         */
        public setMaxDate(maxDate: number): void {
            mTempDate.setTimeInMillis(maxDate);
            if (mTempDate.get(Calendar.YEAR) === mMaxDate.get(Calendar.YEAR) && mTempDate.get(Calendar.DAY_OF_YEAR) !== mMaxDate.get(Calendar.DAY_OF_YEAR)) {
                return;
            }
            mMaxDate.setTimeInMillis(maxDate);
            mCalendarView.setMaxDate(maxDate);
            if (mCurrentDate.after(mMaxDate)) {
                mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
                updateCalendarView();
            }
            updateSpinners();
        }

        /* @Override */
        public setEnabled(enabled: boolean): void {
            if (mIsEnabled === enabled) {
                return;
            }
            super.setEnabled(enabled);
            mDaySpinner.setEnabled(enabled);
            mMonthSpinner.setEnabled(enabled);
            mYearSpinner.setEnabled(enabled);
            mCalendarView.setEnabled(enabled);
            mIsEnabled = enabled;
        }

        /* @Override */
        public isEnabled(): boolean {
            return mIsEnabled;
        }

        /* @Override */
        public dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): boolean {
            onPopulateAccessibilityEvent(event);
            return true;
        }

        /* @Override */
        public onPopulateAccessibilityEvent(event: AccessibilityEvent): void {
            super.onPopulateAccessibilityEvent(event);
             const flags: number = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            let selectedDateUtterance: string = DateUtils.formatDateTime(mContext, mCurrentDate.getTimeInMillis(), flags);
            event.getText().add(selectedDateUtterance);
        }

        /* @Override */
        public onInitializeAccessibilityEvent(event: AccessibilityEvent): void {
            super.onInitializeAccessibilityEvent(event);
            event.setClassName(android.widget.DatePicker.class.getName());
        }

        /* @Override */
        public onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo): void {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(android.widget.DatePicker.class.getName());
        }

        /* @Override */
        protected onConfigurationChanged(newConfig: Configuration): void {
            super.onConfigurationChanged(newConfig);
            setCurrentLocale(newConfig.locale);
        }

        /**
         * Gets whether the {@link CalendarView} is shown.
         *
         * @return True if the calendar view is shown.
         * @see #getCalendarView()
         */
        public getCalendarViewShown(): boolean {
            return (mCalendarView.getVisibility() === View.VISIBLE);
        }

        /**
         * Gets the {@link CalendarView}.
         *
         * @return The calendar view.
         * @see #getCalendarViewShown()
         */
        public getCalendarView(): android.widget.CalendarView {
            return mCalendarView;
        }

        /**
         * Sets whether the {@link CalendarView} is shown.
         *
         * @param shown True if the calendar view is to be shown.
         */
        public setCalendarViewShown(shown: boolean): void {
            mCalendarView.setVisibility(shown ? VISIBLE : GONE);
        }

        /**
         * Gets whether the spinners are shown.
         *
         * @return True if the spinners are shown.
         */
        public getSpinnersShown(): boolean {
            return mSpinners.isShown();
        }

        /**
         * Sets whether the spinners are shown.
         *
         * @param shown True if the spinners are to be shown.
         */
        public setSpinnersShown(shown: boolean): void {
            mSpinners.setVisibility(shown ? VISIBLE : GONE);
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
            mTempDate = getCalendarForLocale(mTempDate, locale);
            mMinDate = getCalendarForLocale(mMinDate, locale);
            mMaxDate = getCalendarForLocale(mMaxDate, locale);
            mCurrentDate = getCalendarForLocale(mCurrentDate, locale);
            mNumberOfMonths = mTempDate.getActualMaximum(Calendar.MONTH) + 1;
            mShortMonths = new DateFormatSymbols().getShortMonths();
            if (usingNumericMonths()) {
                // We're in a locale where a date should either be all-numeric, or all-text.
                // All-text would require custom NumberPicker formatters for day and year.
                mShortMonths = new string[mNumberOfMonths];
                for (let i: number = 0; i < mNumberOfMonths; ++i) {
                    mShortMonths[i] = String.format("%d", i + 1);
                }
            }
        }

        /**
         * Tests whether the current locale is one where there are no real month names,
         * such as Chinese, Japanese, or Korean locales.
         */
        private usingNumericMonths(): boolean {
            return Character.isDigit(mShortMonths[Calendar.JANUARY].charAt(0));
        }

        /**
         * Gets a calendar for locale bootstrapped with the value of a given calendar.
         *
         * @param oldCalendar The old calendar.
         * @param locale The locale.
         */
        private getCalendarForLocale(oldCalendar: Calendar, locale: Locale): Calendar {
            if (oldCalendar === null) {
                return Calendar.getInstance(locale);
            } else {
                 const currentTimeMillis: number = oldCalendar.getTimeInMillis();
                let newCalendar: Calendar = Calendar.getInstance(locale);
                newCalendar.setTimeInMillis(currentTimeMillis);
                return newCalendar;
            }
        }

        /**
         * Reorders the spinners according to the date format that is
         * explicitly set by the user and if no such is set fall back
         * to the current locale's default format.
         */
        private reorderSpinners(): void {
            mSpinners.removeAllViews();
            // We use numeric spinners for year and day, but textual months. Ask icu4c what
            // order the user's locale uses for that combination. http://b/7207103.
            let pattern: string = ICU.getBestDateTimePattern("yyyyMMMdd", Locale.getDefault().toString());
            let order: string[] = ICU.getDateFormatOrder(pattern);
             const spinnerCount: number = order.length;
            for (let i: number = 0; i < spinnerCount; i++) {
                switch(order[i]) {
                    case 'd':
                        mSpinners.addView(mDaySpinner);
                        setImeOptions(mDaySpinner, spinnerCount, i);
                        break;
                    case 'M':
                        mSpinners.addView(mMonthSpinner);
                        setImeOptions(mMonthSpinner, spinnerCount, i);
                        break;
                    case 'y':
                        mSpinners.addView(mYearSpinner);
                        setImeOptions(mYearSpinner, spinnerCount, i);
                        break;
                    default:
                        throw new java.lang.IllegalArgumentException(Arrays.toString(order));
                }
            }
        }

        /**
         * Updates the current date.
         *
         * @param year The year.
         * @param month The month which is <strong>starting from zero</strong>.
         * @param dayOfMonth The day of the month.
         */
        public updateDate(year: number, month: number, dayOfMonth: number): void {
            if (!isNewDate(year, month, dayOfMonth)) {
                return;
            }
            setDate(year, month, dayOfMonth);
            updateSpinners();
            updateCalendarView();
            notifyDateChanged();
        }

        // Override so we are in complete control of save / restore for this widget.
        /* @Override */
        protected dispatchRestoreInstanceState(container: SparseArray<Parcelable>): void {
            dispatchThawSelfOnly(container);
        }

        /* @Override */
        protected onSaveInstanceState(): Parcelable {
            let superState: Parcelable = super.onSaveInstanceState();
            return new SavedState(superState, getYear(), getMonth(), getDayOfMonth());
        }

        /* @Override */
        protected onRestoreInstanceState(state: Parcelable): void {
            let ss: SavedState = <SavedState>state;
            super.onRestoreInstanceState(ss.getSuperState());
            setDate(ss.mYear, ss.mMonth, ss.mDay);
            updateSpinners();
            updateCalendarView();
        }

        /**
         * Initialize the state. If the provided values designate an inconsistent
         * date the values are normalized before updating the spinners.
         *
         * @param year The initial year.
         * @param monthOfYear The initial month <strong>starting from zero</strong>.
         * @param dayOfMonth The initial day of the month.
         * @param onDateChangedListener How user is notified date is changed by
         *            user, can be null.
         */
        public init(year: number, monthOfYear: number, dayOfMonth: number, onDateChangedListener: android.widget.DatePicker.OnDateChangedListener): void {
            setDate(year, monthOfYear, dayOfMonth);
            updateSpinners();
            updateCalendarView();
            mOnDateChangedListener = onDateChangedListener;
        }

        /**
         * Parses the given <code>date</code> and in case of success sets the result
         * to the <code>outDate</code>.
         *
         * @return True if the date was parsed.
         */
        private parseDate(date: string, outDate: Calendar): boolean {
            try {
                outDate.setTime(mDateFormat.parse(date));
                return true;
            } catch (e: ParseException) {
                Log.w(LOG_TAG, "Date: " + date + " not in format: " + DATE_FORMAT);
                return false;
            }
        }

        private isNewDate(year: number, month: number, dayOfMonth: number): boolean {
            return (mCurrentDate.get(Calendar.YEAR) !== year || mCurrentDate.get(Calendar.MONTH) !== dayOfMonth || mCurrentDate.get(Calendar.DAY_OF_MONTH) !== month);
        }

        private setDate(year: number, month: number, dayOfMonth: number): void {
            mCurrentDate.set(year, month, dayOfMonth);
            if (mCurrentDate.before(mMinDate)) {
                mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
            } else if (mCurrentDate.after(mMaxDate)) {
                mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
            }
        }

        private updateSpinners(): void {
            // set the spinner ranges respecting the min and max dates
            if (mCurrentDate.equals(mMinDate)) {
                mDaySpinner.setMinValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
                mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
                mDaySpinner.setWrapSelectorWheel(false);
                mMonthSpinner.setDisplayedValues(null);
                mMonthSpinner.setMinValue(mCurrentDate.get(Calendar.MONTH));
                mMonthSpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.MONTH));
                mMonthSpinner.setWrapSelectorWheel(false);
            } else if (mCurrentDate.equals(mMaxDate)) {
                mDaySpinner.setMinValue(mCurrentDate.getActualMinimum(Calendar.DAY_OF_MONTH));
                mDaySpinner.setMaxValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
                mDaySpinner.setWrapSelectorWheel(false);
                mMonthSpinner.setDisplayedValues(null);
                mMonthSpinner.setMinValue(mCurrentDate.getActualMinimum(Calendar.MONTH));
                mMonthSpinner.setMaxValue(mCurrentDate.get(Calendar.MONTH));
                mMonthSpinner.setWrapSelectorWheel(false);
            } else {
                mDaySpinner.setMinValue(1);
                mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
                mDaySpinner.setWrapSelectorWheel(true);
                mMonthSpinner.setDisplayedValues(null);
                mMonthSpinner.setMinValue(0);
                mMonthSpinner.setMaxValue(11);
                mMonthSpinner.setWrapSelectorWheel(true);
            }
            // make sure the month names are a zero based array
            // with the months in the month spinner
            let displayedValues: string[] = Arrays.copyOfRange(mShortMonths, mMonthSpinner.getMinValue(), mMonthSpinner.getMaxValue() + 1);
            mMonthSpinner.setDisplayedValues(displayedValues);
            // year spinner range does not change based on the current date
            mYearSpinner.setMinValue(mMinDate.get(Calendar.YEAR));
            mYearSpinner.setMaxValue(mMaxDate.get(Calendar.YEAR));
            mYearSpinner.setWrapSelectorWheel(false);
            // set the spinner values
            mYearSpinner.setValue(mCurrentDate.get(Calendar.YEAR));
            mMonthSpinner.setValue(mCurrentDate.get(Calendar.MONTH));
            mDaySpinner.setValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            if (usingNumericMonths()) {
                mMonthSpinnerInput.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            }
        }

        /**
         * Updates the calendar view with the current date.
         */
        private updateCalendarView(): void {
            mCalendarView.setDate(mCurrentDate.getTimeInMillis(), false, false);
        }

        /**
         * @return The selected year.
         */
        public getYear(): number {
            return mCurrentDate.get(Calendar.YEAR);
        }

        /**
         * @return The selected month.
         */
        public getMonth(): number {
            return mCurrentDate.get(Calendar.MONTH);
        }

        /**
         * @return The selected day of month.
         */
        public getDayOfMonth(): number {
            return mCurrentDate.get(Calendar.DAY_OF_MONTH);
        }

        /**
         * Notifies the listener, if such, for a change in the selected date.
         */
        private notifyDateChanged(): void {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            if (mOnDateChangedListener !== null) {
                mOnDateChangedListener.onDateChanged(this, getYear(), getMonth(), getDayOfMonth());
            }
        }

        /**
         * Sets the IME options for a spinner based on its ordering.
         *
         * @param spinner The spinner.
         * @param spinnerCount The total spinner count.
         * @param spinnerIndex The index of the given spinner.
         */
        private setImeOptions(spinner: android.widget.NumberPicker, spinnerCount: number, spinnerIndex: number): void {
             const imeOptions: number;
            if (spinnerIndex < spinnerCount - 1) {
                imeOptions = EditorInfo.IME_ACTION_NEXT;
            } else {
                imeOptions = EditorInfo.IME_ACTION_DONE;
            }
            let input: android.widget.TextView = <android.widget.TextView>spinner.findViewById(R.id.numberpicker_input);
            input.setImeOptions(imeOptions);
        }

        private setContentDescriptions(): void {
            // Day
            trySetContentDescription(mDaySpinner, R.id.increment, R.string.date_picker_increment_day_button);
            trySetContentDescription(mDaySpinner, R.id.decrement, R.string.date_picker_decrement_day_button);
            // Month
            trySetContentDescription(mMonthSpinner, R.id.increment, R.string.date_picker_increment_month_button);
            trySetContentDescription(mMonthSpinner, R.id.decrement, R.string.date_picker_decrement_month_button);
            // Year
            trySetContentDescription(mYearSpinner, R.id.increment, R.string.date_picker_increment_year_button);
            trySetContentDescription(mYearSpinner, R.id.decrement, R.string.date_picker_decrement_year_button);
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
                if (inputMethodManager.isActive(mYearSpinnerInput)) {
                    mYearSpinnerInput.clearFocus();
                    inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
                } else if (inputMethodManager.isActive(mMonthSpinnerInput)) {
                    mMonthSpinnerInput.clearFocus();
                    inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
                } else if (inputMethodManager.isActive(mDaySpinnerInput)) {
                    mDaySpinnerInput.clearFocus();
                    inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
                }
            }
        }
    }
    export module DatePicker {
        /**
         * The callback used to indicate the user changes\d the date.
         */
        export interface OnDateChangedListener {

            /**
             * Called upon a date change.
             *
             * @param view The view associated with this listener.
             * @param year The year that was set.
             * @param monthOfYear The month that was set (0-11) for compatibility
             *            with {@link java.util.Calendar}.
             * @param dayOfMonth The day of the month that was set.
             */
            onDateChanged(view: android.widget.DatePicker, year: number, monthOfYear: number, dayOfMonth: number): void;
        }
    }

}
