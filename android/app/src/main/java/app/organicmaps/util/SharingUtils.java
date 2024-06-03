package app.organicmaps.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.location.Location;
import android.net.Uri;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import java.io.IOException;

import app.organicmaps.Framework;
import app.organicmaps.R;
import app.organicmaps.bookmarks.data.BookmarkInfo;
import app.organicmaps.bookmarks.data.MapObject;

public class SharingUtils
{
  private static final String KMZ_MIME_TYPE = "application/vnd.google-earth.kmz";
  private static final String GPX_MIME_TYPE = "application/gpx";
  private static final String TEXT_MIME_TYPE = "text/plain";

  private static Uri sourceFileUri;

  // This utility class has only static methods
  private SharingUtils()
  {
  }

  public static void shareLocation(@NonNull Context context, @NonNull Location loc)
  {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType(TEXT_MIME_TYPE);

    final String subject = context.getString(R.string.share);
    intent.putExtra(Intent.EXTRA_SUBJECT, subject);

    final String geoUrl = Framework.nativeGetGe0Url(loc.getLatitude(), loc.getLongitude(), Framework
        .nativeGetDrawScale(), "");
    final String httpUrl = Framework.getHttpGe0Url(loc.getLatitude(), loc.getLongitude(), Framework
        .nativeGetDrawScale(), "");
    final String text = context.getString(R.string.my_position_share_sms, geoUrl, httpUrl);
    intent.putExtra(Intent.EXTRA_TEXT, text);

    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
  }

  public static void shareMapObject(@NonNull Context context, @NonNull MapObject object)
  {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType(TEXT_MIME_TYPE);

    final String subject = object.isMyPosition() ?
                           context.getString(R.string.my_position_share_email_subject) :
                           context.getString(R.string.bookmark_share_email_subject);
    intent.putExtra(Intent.EXTRA_SUBJECT, subject);

    final String geoUrl = Framework.nativeGetGe0Url(object.getLat(), object.getLon(),
                                                    object.getScale(), object.getName());
    final String httpUrl = Framework.getHttpGe0Url(object.getLat(), object.getLon(),
                                                   object.getScale(), object.getName());
    final String address = TextUtils.isEmpty(object.getAddress()) ? object.getName() : object.getAddress();
    final String text = context.getString(R.string.my_position_share_email, address, geoUrl, httpUrl);
    intent.putExtra(Intent.EXTRA_TEXT, text);

    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
  }

  public static void shareBookmark(@NonNull Context context, @NonNull BookmarkInfo bookmark)
  {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType(TEXT_MIME_TYPE);

    final String subject = context.getString(R.string.bookmark_share_email_subject);
    intent.putExtra(Intent.EXTRA_SUBJECT, subject);

    final String geoUrl = Framework.nativeGetGe0Url(bookmark.getLat(), bookmark.getLon(),
                                                    bookmark.getScale(), bookmark.getName());
    final String httpUrl = Framework.getHttpGe0Url(bookmark.getLat(), bookmark.getLon(),
                                                   bookmark.getScale(), bookmark.getName());
    StringBuilder text = new StringBuilder();
    text.append(bookmark.getName());
    if (!TextUtils.isEmpty(bookmark.getAddress()))
    {
      text.append(UiUtils.NEW_STRING_DELIMITER);
      text.append(bookmark.getAddress());
    }
    text.append(UiUtils.NEW_STRING_DELIMITER);
    text.append(geoUrl);
    text.append(UiUtils.NEW_STRING_DELIMITER);
    text.append(httpUrl);
    intent.putExtra(Intent.EXTRA_TEXT, text.toString());

    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
  }

  public static ActivityResultLauncher<Intent> RegisterLauncher(@NonNull Fragment fragment)
  {
    return fragment.registerForActivityResult(
      new ActivityResultContracts.StartActivityForResult(), result ->
      {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
        {
          Uri destinationUri = result.getData().getData();
          Uri sourceUri = sourceFileUri;
          sourceFileUri = null;
          try
          {
            if (sourceUri != null && destinationUri != null)
              StorageUtils.copyFile(fragment.requireContext().getContentResolver(), sourceUri, destinationUri);
          }
          catch (IOException e)
          {
            throw new RuntimeException(e);
          }
        }
      });
  }
  public static void shareBookmarkFile(Context context, ActivityResultLauncher<Intent> launcher, String fileName)
  {
    final String ext = fileName.substring(fileName.lastIndexOf("."));
    final String mimeType = switch (ext) {
      case ".gpx" -> GPX_MIME_TYPE;
      case ".kmz" -> KMZ_MIME_TYPE;
      default -> throw new IllegalArgumentException("Unknown bookmark type: " + fileName);
    };

    Intent intent = new Intent(Intent.ACTION_SEND);

    final String subject = context.getString(R.string.share_bookmarks_email_subject);
    intent.putExtra(Intent.EXTRA_SUBJECT, subject);

    final String text = context.getString(R.string.share_bookmarks_email_body);
    intent.putExtra(Intent.EXTRA_TEXT, text);

    final Uri fileUri = StorageUtils.getUriForFilePath(context, fileName);
    intent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
    // Properly set permissions for intent, see
    // https://developer.android.com/reference/androidx/core/content/FileProvider#include-the-permission-in-an-intent
    intent.setDataAndType(fileUri, mimeType);
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
      intent.setClipData(ClipData.newRawUri("", fileUri));
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    Intent saveIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    saveIntent.setType(mimeType);
    DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
    if (documentFile != null)
      saveIntent.putExtra(Intent.EXTRA_TITLE, documentFile.getName());
    sourceFileUri = fileUri;

    Intent[] extraIntents = {saveIntent};

    Intent chooser = Intent.createChooser(intent, context.getString(R.string.share));

    // Prevent sharing to ourselves (supported from API Level 24).
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
    {
      ComponentName[] excludeSelf = { new ComponentName(context, app.organicmaps.SplashActivity.class) };
      chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludeSelf);
    }

    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);

    launcher.launch(chooser);
  }
}
