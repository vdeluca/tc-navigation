package app.organicmaps.help;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.organicmaps.BuildConfig;
import app.organicmaps.Framework;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.util.Config;
import app.organicmaps.util.Constants;
import app.organicmaps.util.DateUtils;
import app.organicmaps.util.Graphics;
import app.organicmaps.util.Utils;

public class HelpFragment extends BaseMwmFragment implements View.OnClickListener
{
  private String mDonateUrl;

  private TextView setupItem(@IdRes int id, boolean tint, @NonNull View frame)
  {
    final TextView view = frame.findViewById(id);
    view.setOnClickListener(this);
    if (tint)
      Graphics.tint(view);
    return view;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    mDonateUrl = Config.getDonateUrl(requireContext());
    View root = inflater.inflate(R.layout.about, container, false);

    ((TextView) root.findViewById(R.id.version))
        .setText(BuildConfig.VERSION_NAME);

    final boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

    final String dataVersion = DateUtils.getShortDateFormatter().format(Framework.getDataVersion());

    setupItem(R.id.web, true, root);
    setupItem(R.id.email, true, root);



    if (BuildConfig.REVIEW_URL.isEmpty())
      root.findViewById(R.id.rate).setVisibility(View.GONE);
    else
      setupItem(R.id.rate, true, root);

    setupItem(R.id.copyright, false, root);
    View termOfUseView = root.findViewById(R.id.term_of_use_link);
    View privacyPolicyView = root.findViewById(R.id.privacy_policy);
    termOfUseView.setOnClickListener(v -> Utils.openUrl(requireActivity(), getResources().getString(R.string.translated_om_site_url) + "terms/"));
    privacyPolicyView.setOnClickListener(v -> Utils.openUrl(requireActivity(), getResources().getString(R.string.translated_om_site_url) + "privacy/"));

    return root;
  }

  @Override
  public void onClick(View v)
  {
    final int id = v.getId();
    if (id == R.id.web)
      Utils.openUrl(requireActivity(), getResources().getString(R.string.translated_om_site_url));
    else if (id == R.id.email)
      Utils.sendTo(requireContext(), BuildConfig.SUPPORT_MAIL, "Organic Maps");
    else if (id == R.id.rate)
      Utils.openAppInMarket(requireActivity(), BuildConfig.REVIEW_URL);
    else if (id == R.id.copyright)
      ((HelpActivity) requireActivity()).stackFragment(CopyrightFragment.class, getString(R.string.copyright), null);
  }
}
