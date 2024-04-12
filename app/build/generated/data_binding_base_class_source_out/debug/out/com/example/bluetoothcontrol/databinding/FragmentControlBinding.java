// Generated by view binder compiler. Do not edit!
package com.example.bluetoothcontrol.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.bluetoothcontrol.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentControlBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final Button button;

  @NonNull
  public final Button buttonProcessFiles;

  @NonNull
  public final TextView fileOne;

  @NonNull
  public final TextView fileTwo;

  @NonNull
  public final ProgressBar progressBarHor;

  @NonNull
  public final TextView textViewHint;

  private FragmentControlBinding(@NonNull ConstraintLayout rootView, @NonNull Button button,
      @NonNull Button buttonProcessFiles, @NonNull TextView fileOne, @NonNull TextView fileTwo,
      @NonNull ProgressBar progressBarHor, @NonNull TextView textViewHint) {
    this.rootView = rootView;
    this.button = button;
    this.buttonProcessFiles = buttonProcessFiles;
    this.fileOne = fileOne;
    this.fileTwo = fileTwo;
    this.progressBarHor = progressBarHor;
    this.textViewHint = textViewHint;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentControlBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentControlBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_control, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentControlBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.button;
      Button button = ViewBindings.findChildViewById(rootView, id);
      if (button == null) {
        break missingId;
      }

      id = R.id.buttonProcessFiles;
      Button buttonProcessFiles = ViewBindings.findChildViewById(rootView, id);
      if (buttonProcessFiles == null) {
        break missingId;
      }

      id = R.id.fileOne;
      TextView fileOne = ViewBindings.findChildViewById(rootView, id);
      if (fileOne == null) {
        break missingId;
      }

      id = R.id.fileTwo;
      TextView fileTwo = ViewBindings.findChildViewById(rootView, id);
      if (fileTwo == null) {
        break missingId;
      }

      id = R.id.progressBarHor;
      ProgressBar progressBarHor = ViewBindings.findChildViewById(rootView, id);
      if (progressBarHor == null) {
        break missingId;
      }

      id = R.id.textViewHint;
      TextView textViewHint = ViewBindings.findChildViewById(rootView, id);
      if (textViewHint == null) {
        break missingId;
      }

      return new FragmentControlBinding((ConstraintLayout) rootView, button, buttonProcessFiles,
          fileOne, fileTwo, progressBarHor, textViewHint);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
