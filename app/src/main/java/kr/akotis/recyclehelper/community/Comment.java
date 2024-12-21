package kr.akotis.recyclehelper.community;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment implements Parcelable {
    private String id;  // 추가된 id 필드
    private String content;
    private long date;
    private String pwd;
    private int report;

    public Comment() {}

    public Comment(String id, String content, long date, String pwd, int report) {
        this.id = id;
        this.content = content;
        this.date = date;
        this.pwd = pwd;
        this.report = report;
    }

    // id getter, setter 추가
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
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
        id = in.readString();  // id 읽기
        content = in.readString();
        date = in.readLong();
        pwd = in.readString();
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
        dest.writeString(id);  // id 저장
        dest.writeString(content);
        dest.writeLong(date);
        dest.writeString(pwd);
        dest.writeInt(report);
    }
}
