// Generated code from Butter Knife. Do not modify!
package com.example.cam;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public class UploadActivity_ViewBinding implements Unbinder {
  private UploadActivity target;

  private View view2130837506;

  private View view2130837505;

  private View view2130837504;

  private View view2130837509;

  @UiThread
  public UploadActivity_ViewBinding(UploadActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public UploadActivity_ViewBinding(final UploadActivity target, View source) {
    this.target = target;

    View view;
    target.mEditTextRfid = Utils.findRequiredViewAsType(source, R.id.editTextRfid, "field 'mEditTextRfid'", EditText.class);
    view = Utils.findRequiredView(source, R.id.buttonUpload, "field 'mButtonUpload' and method 'generateGifNUpload'");
    target.mButtonUpload = Utils.castView(view, R.id.buttonUpload, "field 'mButtonUpload'", Button.class);
    view2130837506 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.generateGifNUpload();
      }
    });
    view = Utils.findRequiredView(source, R.id.buttonPost, "field 'mButtonPost' and method 'uploadNPostToSocial'");
    target.mButtonPost = Utils.castView(view, R.id.buttonPost, "field 'mButtonPost'", Button.class);
    view2130837505 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.uploadNPostToSocial();
      }
    });
    view = Utils.findRequiredView(source, R.id.buttonEmailGif, "field 'mButtonPostTwitter' and method 'sendEmail'");
    target.mButtonPostTwitter = Utils.castView(view, R.id.buttonEmailGif, "field 'mButtonPostTwitter'", Button.class);
    view2130837504 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.sendEmail();
      }
    });
    target.mEditTextStatus = Utils.findRequiredViewAsType(source, R.id.editTextStatus, "field 'mEditTextStatus'", EditText.class);
    target.mProgressBar = Utils.findRequiredViewAsType(source, R.id.progressBar, "field 'mProgressBar'", ProgressBar.class);
    view = Utils.findRequiredView(source, R.id.imageButtonDownload, "field 'mImageButtonDownload' and method 'downloadImage'");
    target.mImageButtonDownload = Utils.castView(view, R.id.imageButtonDownload, "field 'mImageButtonDownload'", ImageButton.class);
    view2130837509 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.downloadImage();
      }
    });
  }

  @Override
  @CallSuper
  public void unbind() {
    UploadActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.mEditTextRfid = null;
    target.mButtonUpload = null;
    target.mButtonPost = null;
    target.mButtonPostTwitter = null;
    target.mEditTextStatus = null;
    target.mProgressBar = null;
    target.mImageButtonDownload = null;

    view2130837506.setOnClickListener(null);
    view2130837506 = null;
    view2130837505.setOnClickListener(null);
    view2130837505 = null;
    view2130837504.setOnClickListener(null);
    view2130837504 = null;
    view2130837509.setOnClickListener(null);
    view2130837509 = null;
  }
}
