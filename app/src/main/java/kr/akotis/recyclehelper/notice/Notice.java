package kr.akotis.recyclehelper.notice;

import android.os.Parcel;
import android.os.Parcelable;

public class Notice implements Parcelable {
    private String title;
    private String content;
    private String date;
    private String imgUrls;

    public Notice() {
    }

    public Notice(String title, String content, String date, String imgUrls) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.imgUrls = imgUrls;
    }

    protected Notice(Parcel in) {
        title = in.readString();
        content = in.readString();
        date = in.readString();
        imgUrls = in.readString();
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

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getImgUrls() {
        return imgUrls;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(date);
        dest.writeString(imgUrls);
    }
}
