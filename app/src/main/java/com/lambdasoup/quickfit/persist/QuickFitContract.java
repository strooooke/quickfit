/*
 * Copyright 2016 Juliane Lehmann <jl@lambdasoup.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.lambdasoup.quickfit.persist;

import android.provider.BaseColumns;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class QuickFitContract {
    abstract public static class WorkoutEntry {
        public static final String TABLE_NAME = "workout";
        public static final String COL_ID = "_id";
        public static final String COL_ACTIVITY_TYPE = "activity_type";
        public static final String COL_DURATION_MINUTES = "duration_minutes";
        public static final String COL_LABEL = "label";
        public static final String COL_CALORIES = "calories";
        public static final String WORKOUT_ID = "workout_id";
        public static final String SCHEDULE_ID = "schedule_id";
        public static final String ACTIVITY_TYPE = "workout_activity_type";
        public static final String DURATION_MINUTES = "workout_duration_minutes";
        public static final String LABEL = "workout_label";
        public static final String CALORIES = "workout_calories";
        public static final String DAY_OF_WEEK = "schedule_day_of_week";
        public static final String HOUR = "schedule_hour";
        public static final String MINUTE = "schedule_minute";
        public static final String NEXT_ALARM_MILLIS = "schedule_next_alarm_millis";
        public static final String CURRENT_STATE = "schedule_current_state";
        public static final String[] COLUMNS_FULL = {WORKOUT_ID, SCHEDULE_ID, ACTIVITY_TYPE, DURATION_MINUTES, LABEL, CALORIES, DAY_OF_WEEK, HOUR, MINUTE};
        public static final String[] COLUMNS_WORKOUT_ONLY = {WORKOUT_ID, ACTIVITY_TYPE, DURATION_MINUTES, LABEL, CALORIES};
        public static final String[] COLUMNS_SCHEDULE_ONLY = {WORKOUT_ID, SCHEDULE_ID, DAY_OF_WEEK, HOUR, MINUTE};

        private WorkoutEntry() {
            // do not instantiate
        }

        public static TableAndAlias toAlias(String contractColumn) {
            StringBuilder aliased = new StringBuilder();
            String table;
            switch (contractColumn) {
                case WORKOUT_ID:
                    throw new IllegalArgumentException("WORKOUT_ID needs special handling");
                case ACTIVITY_TYPE:
                case DURATION_MINUTES:
                case LABEL:
                case CALORIES:
                    aliased.append(WorkoutEntry.TABLE_NAME);
                    table = WorkoutEntry.TABLE_NAME;
                    break;
                case SCHEDULE_ID:
                case DAY_OF_WEEK:
                case HOUR:
                case MINUTE:
                case NEXT_ALARM_MILLIS:
                case CURRENT_STATE:
                    aliased.append(ScheduleEntry.TABLE_NAME);
                    table = ScheduleEntry.TABLE_NAME;
                    break;
                default:
                    throw new IllegalArgumentException("Not a WorkoutScheduleEntry column name: " + contractColumn);
            }
            aliased.append(".");
            switch (contractColumn) {
                case ACTIVITY_TYPE:
                    aliased.append(WorkoutEntry.COL_ACTIVITY_TYPE);
                    break;
                case DURATION_MINUTES:
                    aliased.append(WorkoutEntry.COL_DURATION_MINUTES);
                    break;
                case LABEL:
                    aliased.append(WorkoutEntry.COL_LABEL);
                    break;
                case CALORIES:
                    aliased.append(WorkoutEntry.COL_CALORIES);
                    break;
                case SCHEDULE_ID:
                    aliased.append(ScheduleEntry.COL_ID);
                    break;
                case DAY_OF_WEEK:
                    aliased.append(ScheduleEntry.COL_DAY_OF_WEEK);
                    break;
                case HOUR:
                    aliased.append(ScheduleEntry.COL_HOUR);
                    break;
                case MINUTE:
                    aliased.append(ScheduleEntry.COL_MINUTE);
                    break;
                case NEXT_ALARM_MILLIS:
                    aliased.append(ScheduleEntry.COL_NEXT_ALARM_MILLIS);
                    break;
                case CURRENT_STATE:
                    aliased.append(ScheduleEntry.COL_CURRENT_STATE);
                    break;
            }
            aliased.append(" as ");
            aliased.append(contractColumn);
            return new TableAndAlias(table, aliased.toString());
        }

        public static TablesAndAliases toAlias(String[] contractConstants) {
            String[] aliased = new String[contractConstants.length];
            Set<String> tables = new HashSet<>();
            boolean withWorkoutId = false;
            for (int i = 0, j = 0; i < contractConstants.length; i++) {
                if (WORKOUT_ID.equals(contractConstants[i])) {
                    // handle this after collecting all other tables and aliases
                    withWorkoutId = true;
                } else {
                    TableAndAlias tableAndAlias = toAlias(contractConstants[i]);
                    aliased[j] = tableAndAlias.alias;
                    tables.add(tableAndAlias.table);
                    j++;
                }
            }

            if (withWorkoutId) {
                if (tables.size() == 1 && tables.iterator().next().equals(ScheduleEntry.TABLE_NAME)) {
                    aliased[aliased.length - 1] = ScheduleEntry.TABLE_NAME + "." + ScheduleEntry.COL_WORKOUT_ID + " as " + WORKOUT_ID;
                } else {
                    tables.add(WorkoutEntry.TABLE_NAME);
                    aliased[aliased.length - 1] = WorkoutEntry.TABLE_NAME + "." + WorkoutEntry.COL_ID + " as " + WORKOUT_ID;
                }
            }

            String tablesExpression;
            switch (tables.size()) {
                case 1:
                    tablesExpression = tables.iterator().next();
                    break;
                case 2:
                    tablesExpression = WorkoutEntry.TABLE_NAME + " left outer join " + ScheduleEntry.TABLE_NAME
                            + " on " + WorkoutEntry.TABLE_NAME + "." + WorkoutEntry.COL_ID + "="
                            + ScheduleEntry.TABLE_NAME + "." + ScheduleEntry.COL_WORKOUT_ID;
                    break;
                default:
                    throw new IllegalArgumentException("what do you want to alias an empty array for?");
            }
            return new TablesAndAliases(tables, aliased, tablesExpression);
        }
    }

    abstract public static class ScheduleEntry {
        public static final String TABLE_NAME = "schedule";
        public static final String COL_ID = "_id";
        public static final String COL_WORKOUT_ID = "workout_id";
        public static final String COL_DAY_OF_WEEK = "day_of_week";
        public static final String COL_HOUR = "hour";
        public static final String COL_MINUTE = "minute";
        public static final String COL_NEXT_ALARM_MILLIS = "next_alarm_millis";
        public static final String COL_CURRENT_STATE = "current_state";
        public static final String[] COLUMNS = {COL_ID, COL_WORKOUT_ID, COL_DAY_OF_WEEK, COL_HOUR, COL_MINUTE, COL_NEXT_ALARM_MILLIS, COL_CURRENT_STATE};

        public static final int SHOW_NOTIFICATION_NO = 0;
        public static final int SHOW_NOTIFICATION_YES = 1;

        public static final String CURRENT_STATE_ACKNOWLEDGED = "acknowledged";
        public static final String CURRENT_STATE_DISPLAYING = "displaying";
        public static final String CURRENT_STATE_SNOOZED = "snoozed";

        private ScheduleEntry() {
            // do not instantiate
        }
    }

    abstract public static class SessionEntry implements BaseColumns {
        public static final String TABLE_NAME = "session";
        public static final String ACTIVITY_TYPE = "activity_type";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String STATUS = "status";
        public static final String NAME = "title";
        public static final String CALORIES = "calories";
        public static final String[] COLUMNS = {_ID, ACTIVITY_TYPE, START_TIME, END_TIME, STATUS, NAME, CALORIES};

        private SessionEntry() {
            // do not instantiate
        }

        public enum SessionStatus {
            NEW, SYNCED
        }
    }

    public static class TableAndAlias {
        public final String table;
        public final String alias;

        TableAndAlias(String table, String alias) {
            this.table = table;
            this.alias = alias;
        }
    }

    public static class TablesAndAliases {
        public final Set<String> tables;
        public final String[] aliases;
        public final String tableExpression;

        TablesAndAliases(Set<String> tables, String[] aliases, String tableExpression) {
            this.tables = Collections.unmodifiableSet(tables);
            this.aliases = aliases;
            this.tableExpression = tableExpression;
        }
    }
}
