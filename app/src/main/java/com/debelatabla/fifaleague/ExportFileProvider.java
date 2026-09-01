package com.debelatabla.fifaleague;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public class ExportFileProvider extends ContentProvider {
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public String getType(Uri uri) {
    return XLSX;
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    if (!"r".equals(mode)) throw new FileNotFoundException("Exports are read-only");
    File file = exportFile(uri);
    if (!file.isFile()) throw new FileNotFoundException(file.getName());
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
  }

  @Override
  public Cursor query(
      Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    File file = exportFile(uri);
    String[] requested =
        projection == null ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
    MatrixCursor cursor = new MatrixCursor(requested, 1);
    MatrixCursor.RowBuilder row = cursor.newRow();
    for (String column : requested) {
      if (OpenableColumns.DISPLAY_NAME.equals(column)) row.add(file.getName());
      else if (OpenableColumns.SIZE.equals(column)) row.add(file.length());
      else row.add(null);
    }
    return cursor;
  }

  private File exportFile(Uri uri) {
    String name = uri.getLastPathSegment();
    if (name == null || name.contains("/") || name.contains("..")) name = "invalid";
    return new File(new File(getContext().getCacheDir(), "exports"), name);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }
}
