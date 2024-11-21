package kr.akotis.recyclehelper.community;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment implements Parcelable {
    private String content;
    private long date;
    private int pwd;
    private int report;

    public Comment() {}

    public Comment(String content, long date, int pwd, int report) {
        this.content = content;
        this.date = date;
        this.pwd = pwd;
        this.report = report;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getPwd() {
        return pwd;
    }

    public void setPwd(int pwd) {
        this.pwd = pwd;
    }

    public int getReport() {
        return report;
    }

    public void setReport(int report) {
        this.report = report;
    }

    // Parcelable 구현
    protected Comment(Parcel in) {
        content = in.readString();
        date = in.readLong();
        pwd = in.readInt();
        report = in.readInt();
    }

    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(content);
        dest.writeLong(date);
        dest.writeInt(pwd);
        dest.writeInt(report);
    }
}
