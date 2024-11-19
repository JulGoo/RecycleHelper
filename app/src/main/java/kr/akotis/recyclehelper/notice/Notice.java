package kr.akotis.recyclehelper.notice;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Notice implements Parcelable {
    private String title;
    private String content;
    private String date;
    private String imgUrl;

    public Notice() {}

    public Notice(String title, String content, String date, String imgUrl){
        this.title = title;
        this.content = content;
        this.date = date;
        this.imgUrl = imgUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    protected Notice(Parcel in) {
        title = in.readString();
        content = in.readString();
        imgUrl = in.readString();
        date = in.readString();
    }

    public static final Creator<Notice> CREATOR = new Creator<Notice>() {
        @Override
        public Notice createFromParcel(Parcel in) {
            return new Notice(in);
        }

        @Override
        public Notice[] newArray(int size) {
            return new Notice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(imgUrl);
        dest.writeString(date);
    }
}
