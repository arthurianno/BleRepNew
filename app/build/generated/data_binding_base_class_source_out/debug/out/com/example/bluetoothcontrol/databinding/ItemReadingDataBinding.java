// Generated by view binder compiler. Do not edit!
package com.example.bluetoothcontrol.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.bluetoothcontrol.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ItemReadingDataBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final TextView hexData;

  @NonNull
  public final TextView nameOfAddressData;

  @NonNull
  public final TextView nameOfAtributeData;

  @NonNull
  public final TextView nameOfData;

  private ItemReadingDataBinding(@NonNull LinearLayout rootView, @NonNull TextView hexData,
      @NonNull TextView nameOfAddressData, @NonNull TextView nameOfAtributeData,
      @NonNull TextView nameOfData) {
    this.rootView = rootView;
    this.hexData = hexData;
    this.nameOfAddressData = nameOfAddressData;
    this.nameOfAtributeData = nameOfAtributeData;
    this.nameOfData = nameOfData;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemReadingDataBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemReadingDataBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_reading_data, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemReadingDataBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.hexData;
      TextView hexData = ViewBindings.findChildViewById(rootView, id);
      if (hexData == null) {
        break missingId;
      }

      id = R.id.nameOfAddressData;
      TextView nameOfAddressData = ViewBindings.findChildViewById(rootView, id);
      if (nameOfAddressData == null) {
        break missingId;
      }

      id = R.id.nameOfAtributeData;
      TextView nameOfAtributeData = ViewBindings.findChildViewById(rootView, id);
      if (nameOfAtributeData == null) {
        break missingId;
      }

      id = R.id.nameOfData;
      TextView nameOfData = ViewBindings.findChildViewById(rootView, id);
      if (nameOfData == null) {
        break missingId;
      }

      return new ItemReadingDataBinding((LinearLayout) rootView, hexData, nameOfAddressData,
          nameOfAtributeData, nameOfData);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
