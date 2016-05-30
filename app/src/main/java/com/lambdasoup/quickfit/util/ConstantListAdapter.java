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

package com.lambdasoup.quickfit.util;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Reusable adapter for use in Spinner and list dialog.
 * Does not support modification of contents.
 */
public class ConstantListAdapter<T> extends BaseAdapter implements Serializable {
    private final Function<? super T, ? extends CharSequence> transformation;
    private final LayoutInflater inflater;
    private final int resource;
    private final int dropDownResource;

    private final List<T> objects;

    public ConstantListAdapter(Context context, @LayoutRes int resource, @NonNull T[] objects, @NonNull Function<? super T, ? extends CharSequence> transformation) {
        this(context, resource, resource, objects, transformation);
    }

    public ConstantListAdapter(Context context, @LayoutRes int resource, @LayoutRes int dropDownResource, @NonNull T[] objects, @NonNull Function<? super T, ? extends CharSequence> transformation) {
        this(context, resource, dropDownResource, Arrays.asList(objects), transformation);
    }

    public ConstantListAdapter(Context context, @LayoutRes int resource, @NonNull List<T> objects, @NonNull Function<? super T, ? extends CharSequence> transformation) {
        this(context, resource, resource, objects, transformation);
    }

    public ConstantListAdapter(Context context, @LayoutRes int resource, @LayoutRes int dropDownResource, @NonNull List<T> objects, @NonNull Function<? super T, ? extends CharSequence> transformation) {
        this.resource = resource;
        this.dropDownResource = dropDownResource;
        this.objects = objects;
        this.transformation = transformation;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public T getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPosition(T item) {
        return objects.indexOf(item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, resource);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, dropDownResource);
    }

    private View createViewFromResource(int position, View convertView,
                                        ViewGroup parent, int resource) {
        View view;
        TextView text;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        try {
            text = (TextView) view;
        } catch (ClassCastException e) {
            throw new IllegalStateException(
                    "ConstantListAdapter requires the resource Id to be a TextView", e);
        }

        T item = getItem(position);
        text.setText(transformation.apply(item));

        return view;
    }
}
