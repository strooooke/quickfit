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

package com.lambdasoup.quickfit.model;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.google.android.gms.fitness.FitnessActivities;
import com.lambdasoup.quickfit.R;

import java.util.HashMap;
import java.util.Map;


public class FitActivity {
    private static final Map<String, Integer> RES_ID_BY_KEY = new HashMap<String, Integer>() {{
        put(FitnessActivities.AEROBICS, R.string.fit_act_aerobics);
        put(FitnessActivities.BADMINTON, R.string.fit_act_badminton);
        put(FitnessActivities.BASEBALL, R.string.fit_act_baseball);
        put(FitnessActivities.BASKETBALL, R.string.fit_act_basketball);
        put(FitnessActivities.BIATHLON, R.string.fit_act_biathlon);
        put(FitnessActivities.BIKING, R.string.fit_act_biking);
        put(FitnessActivities.BIKING_HAND, R.string.fit_act_biking_hand);
        put(FitnessActivities.BIKING_MOUNTAIN, R.string.fit_act_biking_mountain);
        put(FitnessActivities.BIKING_ROAD, R.string.fit_act_biking_road);
        put(FitnessActivities.BIKING_SPINNING, R.string.fit_act_biking_spinning);
        put(FitnessActivities.BIKING_STATIONARY, R.string.fit_act_biking_stationary);
        put(FitnessActivities.BIKING_UTILITY, R.string.fit_act_biking_utility);
        put(FitnessActivities.BOXING, R.string.fit_act_boxing);
        put(FitnessActivities.CALISTHENICS, R.string.fit_act_calisthenics);
        put(FitnessActivities.CIRCUIT_TRAINING, R.string.fit_act_circuit_training);
        put(FitnessActivities.CRICKET, R.string.fit_act_cricket);
        put(FitnessActivities.CROSSFIT, R.string.fit_act_crossfit);
        put(FitnessActivities.CURLING, R.string.fit_act_curling);
        put(FitnessActivities.DANCING, R.string.fit_act_dancing);
        put(FitnessActivities.DIVING, R.string.fit_act_diving);
        put(FitnessActivities.ELLIPTICAL, R.string.fit_act_elliptical);
        put(FitnessActivities.ERGOMETER, R.string.fit_act_ergometer);
        put(FitnessActivities.FENCING, R.string.fit_act_fencing);
        put(FitnessActivities.FOOTBALL_AMERICAN, R.string.fit_act_football_american);
        put(FitnessActivities.FOOTBALL_AUSTRALIAN, R.string.fit_act_football_australian);
        put(FitnessActivities.FOOTBALL_SOCCER, R.string.fit_act_football_soccer);
        put(FitnessActivities.FRISBEE_DISC, R.string.fit_act_frisbee_disc);
        put(FitnessActivities.GARDENING, R.string.fit_act_gardening);
        put(FitnessActivities.GOLF, R.string.fit_act_golf);
        put(FitnessActivities.GYMNASTICS, R.string.fit_act_gymnastics);
        put(FitnessActivities.HANDBALL, R.string.fit_act_handball);
        put(FitnessActivities.HIGH_INTENSITY_INTERVAL_TRAINING, R.string.fit_act_interval_training_high_intensity);
        put(FitnessActivities.HIKING, R.string.fit_act_hiking);
        put(FitnessActivities.HOCKEY, R.string.fit_act_hockey);
        put(FitnessActivities.HORSEBACK_RIDING, R.string.fit_act_horseback_riding);
        put(FitnessActivities.HOUSEWORK, R.string.fit_act_housework);
        put(FitnessActivities.ICE_SKATING, R.string.fit_act_ice_skating);
        put(FitnessActivities.INTERVAL_TRAINING, R.string.fit_act_interval_training);
        put(FitnessActivities.JUMP_ROPE, R.string.fit_act_jump_rope);
        put(FitnessActivities.KAYAKING, R.string.fit_act_kayaking);
        put(FitnessActivities.KETTLEBELL_TRAINING, R.string.fit_act_kettlebell_training);
        put(FitnessActivities.KICK_SCOOTER, R.string.fit_act_kick_scooter);
        put(FitnessActivities.KICKBOXING, R.string.fit_act_kickboxing);
        put(FitnessActivities.KITESURFING, R.string.fit_act_kitesurfing);
        put(FitnessActivities.MARTIAL_ARTS, R.string.fit_act_martial_arts);
        put(FitnessActivities.MEDITATION, R.string.fit_act_meditation);
        put(FitnessActivities.MIXED_MARTIAL_ARTS, R.string.fit_act_martial_arts_mixed);
        put(FitnessActivities.OTHER, R.string.fit_act_other);
        put(FitnessActivities.P90X, R.string.fit_act_p90x);
        put(FitnessActivities.PARAGLIDING, R.string.fit_act_paragliding);
        put(FitnessActivities.PILATES, R.string.fit_act_pilates);
        put(FitnessActivities.POLO, R.string.fit_act_polo);
        put(FitnessActivities.RACQUETBALL, R.string.fit_act_racquetball);
        put(FitnessActivities.ROCK_CLIMBING, R.string.fit_act_rock_climbing);
        put(FitnessActivities.ROWING, R.string.fit_act_rowing);
        put(FitnessActivities.ROWING_MACHINE, R.string.fit_act_rowing_machine);
        put(FitnessActivities.RUGBY, R.string.fit_act_rugby);
        put(FitnessActivities.RUNNING, R.string.fit_act_running);
        put(FitnessActivities.RUNNING_JOGGING, R.string.fit_act_running_jogging);
        put(FitnessActivities.RUNNING_SAND, R.string.fit_act_running_sand);
        put(FitnessActivities.RUNNING_TREADMILL, R.string.fit_act_running_treadmill);
        put(FitnessActivities.SAILING, R.string.fit_act_sailing);
        put(FitnessActivities.SCUBA_DIVING, R.string.fit_act_scuba_diving);
        put(FitnessActivities.SKATEBOARDING, R.string.fit_act_skateboarding);
        put(FitnessActivities.SKATING, R.string.fit_act_skating);
        put(FitnessActivities.SKATING_CROSS, R.string.fit_act_skating_cross);
        put(FitnessActivities.SKATING_INDOOR, R.string.fit_act_skating_indoor);
        put(FitnessActivities.SKATING_INLINE, R.string.fit_act_skating_inline);
        put(FitnessActivities.SKIING, R.string.fit_act_skiing);
        put(FitnessActivities.SKIING_BACK_COUNTRY, R.string.fit_act_skiing_back_country);
        put(FitnessActivities.SKIING_CROSS_COUNTRY, R.string.fit_act_skiing_cross_country);
        put(FitnessActivities.SKIING_DOWNHILL, R.string.fit_act_skiing_downhill);
        put(FitnessActivities.SKIING_KITE, R.string.fit_act_skiing_kite);
        put(FitnessActivities.SKIING_ROLLER, R.string.fit_act_skiing_roller);
        put(FitnessActivities.SLEDDING, R.string.fit_act_sledding);
        put(FitnessActivities.SNOWBOARDING, R.string.fit_act_snowboarding);
        put(FitnessActivities.SNOWSHOEING, R.string.fit_act_snowshoeing);
        put(FitnessActivities.SQUASH, R.string.fit_act_squash);
        put(FitnessActivities.STAIR_CLIMBING, R.string.fit_act_stair_climbing);
        put(FitnessActivities.STAIR_CLIMBING_MACHINE, R.string.fit_act_stair_climbing_machine);
        put(FitnessActivities.STANDUP_PADDLEBOARDING, R.string.fit_act_standup_paddleboarding);
        put(FitnessActivities.STRENGTH_TRAINING, R.string.fit_act_strength_training);
        put(FitnessActivities.SURFING, R.string.fit_act_surfing);
        put(FitnessActivities.SWIMMING, R.string.fit_act_swimming);
        put(FitnessActivities.SWIMMING_POOL, R.string.fit_act_swimming_pool);
        put(FitnessActivities.SWIMMING_OPEN_WATER, R.string.fit_act_swimming_open_water);
        put(FitnessActivities.TABLE_TENNIS, R.string.fit_act_table_tennis);
        put(FitnessActivities.TEAM_SPORTS, R.string.fit_act_team_sports);
        put(FitnessActivities.TENNIS, R.string.fit_act_tennis);
        put(FitnessActivities.TREADMILL, R.string.fit_act_treadmill);
        put(FitnessActivities.VOLLEYBALL, R.string.fit_act_volleyball);
        put(FitnessActivities.VOLLEYBALL_BEACH, R.string.fit_act_volleyball_beach);
        put(FitnessActivities.VOLLEYBALL_INDOOR, R.string.fit_act_volleyball_indoor);
        put(FitnessActivities.WAKEBOARDING, R.string.fit_act_wakeboarding);
        put(FitnessActivities.WALKING, R.string.fit_act_walking);
        put(FitnessActivities.WALKING_FITNESS, R.string.fit_act_walking_fitness);
        put(FitnessActivities.WALKING_NORDIC, R.string.fit_act_walking_nordic);
        put(FitnessActivities.WALKING_TREADMILL, R.string.fit_act_walking_treadmill);
        put(FitnessActivities.WALKING_STROLLER, R.string.fit_act_walking_stroller);
        put(FitnessActivities.WATER_POLO, R.string.fit_act_water_polo);
        put(FitnessActivities.WEIGHTLIFTING, R.string.fit_act_weightlifting);
        put(FitnessActivities.WHEELCHAIR, R.string.fit_act_wheelchair);
        put(FitnessActivities.WINDSURFING, R.string.fit_act_windsurfing);
        put(FitnessActivities.YOGA, R.string.fit_act_yoga);
        put(FitnessActivities.ZUMBA, R.string.fit_act_zumba);
    }};
    @NonNull
    public final String key;
    @NonNull
    public final String displayName;

    private FitActivity(@NonNull String key, @NonNull String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public static FitActivity fromKey(@NonNull String key, @NonNull Resources resources) {
        Integer resId = RES_ID_BY_KEY.get(key);
        if (resId == null) {
            throw new IllegalArgumentException("Unknown key " + key);
        }
        return new FitActivity(key, resources.getString(resId));
    }

    public static FitActivity[] all(@NonNull Resources resources) {
        FitActivity[] all = new FitActivity[RES_ID_BY_KEY.size()];
        int i = 0;
        for (Map.Entry<String, Integer> entry : RES_ID_BY_KEY.entrySet()) {
            all[i] = new FitActivity(entry.getKey(), resources.getString(entry.getValue()));
            i++;
        }
        return all;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FitActivity{");
        sb.append("key='").append(key).append('\'');
        sb.append(", displayName='").append(displayName).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FitActivity that = (FitActivity) o;

        return key.equals(that.key) && displayName.equals(that.displayName);

    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + displayName.hashCode();
        return result;
    }

}
