package com.kwonyijun.yjmusicappv2.album;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Album implements Parcelable {
    private Bitmap albumImage;
    private String albumTitle;

    // Implement the Parcelable methods
    protected Album(Parcel in) {
        // Read data from the parcel and assign to fields
        albumImage = in.readParcelable(Bitmap.class.getClassLoader());
        albumTitle = in.readString();
    }

    public Album(Bitmap albumImage, String albumTitle) {
        this.albumImage = albumImage;
        this.albumTitle = albumTitle;
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    public Bitmap getAlbumImage() {
        return albumImage;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeParcelable(albumImage, i);
        parcel.writeString(albumTitle);
    }
}
